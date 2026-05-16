#!/usr/bin/env python3
"""
Small stdlib-only emulator for the FBX Player Models backend service.

The Minecraft client talks to:
  GET  /getSkins   JSON body: {"uuids": ["..."]}
  GET  /files/<sha256>.fbx
  POST /setSkin    JSON body: {"uuid": "...", "data3d": {"base64": "...", "format": "..."}}
  GET  /banner

By default this listens on port 6969 to match BackendInteractor.java.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import logging
import mimetypes
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import unquote


SUPPORTED_FORMATS = {"fbx"}
LOGGER = logging.getLogger("backend_emulator")


class SkinStore:
    def __init__(self, root: Path) -> None:
        self.root = root
        self.files_dir = root / "files"
        self.index_path = root / "skins.json"
        self.files_dir.mkdir(parents=True, exist_ok=True)
        self.index = self._load_index()

    def _load_index(self) -> dict[str, dict[str, str]]:
        if not self.index_path.exists():
            return {}

        with self.index_path.open("r", encoding="utf-8") as handle:
            data = json.load(handle)

        if not isinstance(data, dict):
            raise ValueError(f"{self.index_path} must contain a JSON object")

        return data

    def _save_index(self) -> None:
        tmp_path = self.index_path.with_suffix(".json.tmp")
        with tmp_path.open("w", encoding="utf-8") as handle:
            json.dump(self.index, handle, indent=2, sort_keys=True)
            handle.write("\n")
        tmp_path.replace(self.index_path)

    def lookup(self, uuids: list[str]) -> list[dict[str, str]]:
        rows = []
        for uuid in uuids:
            entry = self.index.get(uuid)
            if entry is None:
                continue

            rows.append(
                {
                    "uuid": uuid,
                    "id": entry["id"],
                    "format": entry["format"],
                }
            )
        return rows

    def set_skin(self, uuid: str, raw_data: bytes, fmt: str) -> dict[str, str]:
        fmt = fmt.lower()
        if fmt not in SUPPORTED_FORMATS:
            raise ValueError(f"unsupported format {fmt!r}; expected fbx")

        if not raw_data:
            self.index.pop(uuid, None)
            self._save_index()
            return {"uuid": uuid, "id": "", "format": fmt, "deleted": "true"}

        skin_id = hashlib.sha256(raw_data).hexdigest()
        file_path = self.files_dir / f"{skin_id}.{fmt}"
        file_path.write_bytes(raw_data)

        self.index[uuid] = {"id": skin_id, "format": fmt}
        self._save_index()

        return {"uuid": uuid, "id": skin_id, "format": fmt}

    def file_path(self, filename: str) -> Path | None:
        decoded = unquote(filename)
        candidate = (self.files_dir / decoded).resolve()
        if not candidate.is_relative_to(self.files_dir.resolve()):
            return None
        return candidate


class BackendHandler(BaseHTTPRequestHandler):
    server_version = "FBXPlayerModelsBackendEmulator/1.0"

    @property
    def store(self) -> SkinStore:
        return self.server.store  # type: ignore[attr-defined]

    @property
    def banner_text(self) -> str:
        return self.server.banner_text  # type: ignore[attr-defined]

    def do_GET(self) -> None:
        if self.path == "/banner":
            LOGGER.info("banner requested from %s", self.client_address[0])
            self._send_text(HTTPStatus.OK, self.banner_text)
            return

        if self.path == "/getSkins":
            body = self._read_json_body(default={})
            uuids = body.get("uuids", [])
            if not isinstance(uuids, list) or not all(isinstance(item, str) for item in uuids):
                self._send_json(HTTPStatus.BAD_REQUEST, {"error": "uuids must be a list of strings"})
                return

            result = self.store.lookup(uuids)
            LOGGER.info("skin lookup from %s: requested=%d found=%d", self.client_address[0], len(uuids), len(result))
            self._send_json(HTTPStatus.OK, result)
            return

        if self.path.startswith("/files/"):
            filename = self.path.removeprefix("/files/")
            file_path = self.store.file_path(filename)
            if file_path is None or not file_path.is_file():
                LOGGER.warning("file miss from %s: %s", self.client_address[0], filename)
                self._send_bytes(HTTPStatus.NOT_FOUND, b"")
                return

            content_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
            payload = file_path.read_bytes()
            LOGGER.info("file served to %s: %s bytes=%d", self.client_address[0], file_path.name, len(payload))
            self._send_bytes(HTTPStatus.OK, payload, content_type=content_type)
            return

        self._send_json(HTTPStatus.NOT_FOUND, {"error": "not found"})

    def do_POST(self) -> None:
        if self.path != "/setSkin":
            self._send_json(HTTPStatus.NOT_FOUND, {"error": "not found"})
            return

        try:
            body = self._read_json_body()
            uuid = body["uuid"]
            data3d = body["data3d"]
            encoded = data3d["base64"]
            fmt = data3d["format"]

            if not isinstance(uuid, str) or not isinstance(encoded, str) or not isinstance(fmt, str):
                raise ValueError("uuid, base64, and format must be strings")

            raw_data = base64.b64decode(encoded, validate=True)
            result = self.store.set_skin(uuid, raw_data, fmt)
        except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
            LOGGER.warning("bad upload from %s: %s", self.client_address[0], exc)
            self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})
            return

        if result.get("deleted") == "true":
            LOGGER.info("skin reset from %s: uuid=%s format=%s", self.client_address[0], result["uuid"], result["format"])
        else:
            LOGGER.info(
                "skin upload from %s: uuid=%s id=%s format=%s bytes=%d",
                self.client_address[0],
                result["uuid"],
                result["id"],
                result["format"],
                len(raw_data),
            )
        self._send_json(HTTPStatus.OK, result)

    def log_message(self, fmt: str, *args: Any) -> None:
        LOGGER.info("%s - %s", self.client_address[0], fmt % args)

    def _read_json_body(self, default: Any | None = None) -> Any:
        content_length = int(self.headers.get("Content-Length", "0"))
        if content_length == 0 and default is not None:
            return default

        raw = self.rfile.read(content_length)
        if not raw and default is not None:
            return default

        return json.loads(raw.decode("utf-8"))

    def _send_json(self, status: HTTPStatus, payload: Any) -> None:
        data = json.dumps(payload).encode("utf-8")
        self._send_bytes(status, data, content_type="application/json")

    def _send_text(self, status: HTTPStatus, payload: str) -> None:
        self._send_bytes(status, payload.encode("utf-8"), content_type="text/plain; charset=utf-8")

    def _send_bytes(
        self,
        status: HTTPStatus,
        payload: bytes,
        *,
        content_type: str = "application/octet-stream",
    ) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(payload)))
        self.end_headers()
        self.wfile.write(payload)


class BackendServer(ThreadingHTTPServer):
    def __init__(self, server_address: tuple[str, int], handler: type[BackendHandler], store: SkinStore, banner_text: str) -> None:
        super().__init__(server_address, handler)
        self.store = store
        self.banner_text = banner_text


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Emulate the FBX Player Models HTTP backend.")
    parser.add_argument("--host", default="127.0.0.1", help="Host/interface to bind. Use 0.0.0.0 for LAN access.")
    parser.add_argument("--port", type=int, default=6969, help="Port to bind.")
    parser.add_argument("--data-dir", type=Path, default=Path(".backend-emulator"), help="Persistent data directory.")
    parser.add_argument("--banner", default="FBX Player Models backend emulator", help="Text returned by GET /banner.")
    parser.add_argument("--log-file", type=Path, help="Write logs to this file. Defaults to <data-dir>/backend.log.")
    parser.add_argument("--no-log-file", action="store_true", help="Only log to stdout.")
    parser.add_argument("--debug", action="store_true", help="Enable debug logging.")
    return parser.parse_args()


def configure_logging(args: argparse.Namespace) -> Path | None:
    level = logging.DEBUG if args.debug else logging.INFO
    formatter = logging.Formatter("%(asctime)s %(levelname)s %(message)s")

    handlers: list[logging.Handler] = [logging.StreamHandler()]
    log_path = None

    if not args.no_log_file:
        log_path = args.log_file or args.data_dir / "backend.log"
        log_path.parent.mkdir(parents=True, exist_ok=True)
        handlers.append(logging.FileHandler(log_path, encoding="utf-8"))

    for handler in handlers:
        handler.setFormatter(formatter)

    logging.basicConfig(level=level, handlers=handlers)
    return log_path


def main() -> None:
    args = parse_args()
    log_path = configure_logging(args)
    store = SkinStore(args.data_dir)
    server = BackendServer((args.host, args.port), BackendHandler, store, args.banner)
    print(f"Serving FBX Player Models backend emulator at http://{args.host}:{args.port}")
    print(f"Data directory: {args.data_dir.resolve()}")
    if log_path is not None:
        print(f"Log file: {log_path.resolve()}")
    server.serve_forever()


if __name__ == "__main__":
    main()
