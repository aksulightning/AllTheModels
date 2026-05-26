# FBX Player Models Code Guide

## Fabric-only support

This repository is Fabric-only. It does not use Forge, NeoForge, Quilt, or Architectury.

The project is structured for separate Fabric jars per Minecraft target. A universal jar is not implemented.

## Supported targets

| Module | Minecraft | Yarn mappings | Fabric Loader | Fabric API | Mod version | Status |
| --- | --- | --- | --- | --- | --- | --- |
| `fabric-1.21.1` | `1.21.1` | `1.21.1+build.3` | `0.16.14` | `0.116.6+1.21.1` | `2.0.0+1.21.1` | Builds |
| `fabric-26.1.2` | `1.21.1` | `1.21.1+build.3` | `0.16.14` | `0.116.6+1.21.1` | `2.0.0+1.21.1` | Builds |

`26.1.2` was requested as a supported target, but it is not a Minecraft version in this project. It does not appear in the original project metadata as a Minecraft version, Fabric Loader version, Fabric API version, or mod version. The `fabric-26.1.2` module therefore preserves the requested folder name while targeting the same confirmed Fabric `1.21.1` dependency set used by the existing port.

## Build commands

Build all configured modules:

```bash
./gradlew build
```

Build the Fabric 1.21.1 jar:

```bash
./gradlew :fabric-1.21.1:build
```

Build the Fabric 26.1.2-labeled jar:

```bash
./gradlew :fabric-26.1.2:build
```

The 1.21.1 remapped jar is written under `fabric-1.21.1/build/libs/`.
The 26.1.2-labeled remapped jar is written under `fabric-26.1.2/build/libs/`.

## Project layout

```text
root/
  settings.gradle
  build.gradle
  gradle.properties
  common/
    build.gradle
    src/main/java/...
    src/main/resources/...
  fabric-1.21.1/
    build.gradle
    src/main/java/...
    src/client/java/...
    src/main/resources/fabric.mod.json
    src/main/resources/fbx-player-models.mixins.json
    src/client/resources/fbx-player-models.client.mixins.json
  fabric-26.1.2/
    build.gradle
    src/main/java/...
    src/client/java/...
    src/main/resources/fabric.mod.json
    src/main/resources/fbx-player-models.mixins.json
    src/client/resources/fbx-player-models.client.mixins.json
```

## Common code

Put version-independent code in `common`, including:

- Constants such as `FBXPlayerModels.MOD_ID`.
- Config/save model classes.
- Pure Java utilities and model data structures.
- Shared assets that are valid for every configured Fabric target.
- Platform-neutral interfaces under `com.aksulightning.platform`.

Common code must not import Minecraft, Fabric, Mixin, Mod Menu, or mapping-specific classes.

## Fabric version code

Put Minecraft- and Fabric-sensitive code in a Fabric target module, such as `fabric-1.21.1`, including:

- `ModInitializer` and `ClientModInitializer` entrypoints.
- Fabric event registration.
- Client commands.
- Mod Menu integration.
- Screens, renderers, texture upload, and key client setup.
- Mixins and mixin config files.
- `fabric.mod.json`.
- Access wideners, if any are added later.

Fabric platform implementations live under `com.aksulightning.platform.fabric`.

## Current entrypoints

`fabric-1.21.1/src/main/resources/fabric.mod.json` declares:

- `main`: `me.onethecrazy.FBXPlayerModelsMod`
- `client`: `me.onethecrazy.FBXPlayerModelsClient`
- `modmenu`: `me.onethecrazy.ModMenuIntegration`

## Mixins and access wideners

The current mixin configs are version-specific:

- `fbx-player-models.mixins.json`
- `fbx-player-models.client.mixins.json`

The client mixins are:

- `me.onethecrazy.mixin.client.RenderMixin`
- `me.onethecrazy.mixin.client.MainMenuMixin`

There are currently no project access wideners.

## Version-sensitive code

Code that imports `net.minecraft.*`, `com.mojang.*`, `net.fabricmc.*`, Mixin, or Mod Menu APIs remains in `fabric-1.21.1`. This includes rendering, screens, commands, dynamic texture loading, Fabric events, Fabric Loader config paths, and the mixins.

Shared code currently includes platform interfaces, constants, save/config models, rig binding metadata, simple model structures, `Float2`, `Float3`, and shared resources.

## FBX view entity

The mod registers one summonable inert entity for displaying an FBX model:

```text
fbxplayermodels:view_entity
```

The entity is intentionally not a pathfinding mob. It extends the base Minecraft entity type in each Fabric target, has no AI goals, no attacks, no wandering, no fleeing, and no natural spawning for the MVP. It exists as a server-synced display entity with a client renderer.

Summon example:

```mcfunction
/summon fbxplayermodels:view_entity ~ ~ ~ {Model:"this_file.fbx"}
```

The `Model` NBT value is persisted on the entity and synced to clients with tracked entity data. The value must be a safe flat FBX filename. Absolute paths, nested paths, path traversal, blank values, and non-`.fbx` files are rejected by sanitizing to an empty model value.

Server-side model storage:

```text
<world>/fbx-player-models/mobskins/this_file.fbx
```

Clients do not load `mobskins` as local source data. When a view entity renders, the client requests the named model from the server through the mod networking layer. The server validates the safe filename, reads only from its world-local `mobskins` directory, enforces the shared model size limit, and sends the FBX bytes back to that client.

Client-side received model cache:

```text
.fbxplayermodels/mobskins-cache/
```

This cache is only a parsing cache for server-sent bytes. It is not the authoritative model folder.

Important classes, duplicated per Fabric target where mappings differ:

- `com.aksulightning.fbxplayermodels.ModEntities`
- `com.aksulightning.fbxplayermodels.ViewEntity`
- `com.aksulightning.fbxplayermodels.ViewEntityModelPath`
- `com.aksulightning.fbxplayermodels.client.ViewEntityRenderer`
- `com.aksulightning.fbxplayermodels.client.ViewEntityModelCache`
- `me.onethecrazy.network.ModelPackets`
- `me.onethecrazy.server.ServerModelStore`
- `me.onethecrazy.server.ServerModelNetworking`
- `me.onethecrazy.util.network.BackendInteractor`

Rendering reuses the existing FBX loading pipeline: `UniversalParser`, `ModelNormalizer`, `CacheSkin`, static vertices, and `SkinnedModel.render("Idle", ...)`.

## Adding another Fabric Minecraft version

1. Confirm the actual Minecraft version, Yarn mappings, Fabric Loader version, and Fabric API version.
2. Add target-specific properties to `gradle.properties`.
3. Add `include("fabric-<minecraft-version>")` to `settings.gradle`.
4. Create `fabric-<minecraft-version>/build.gradle` based on `fabric-1.21.1/build.gradle`.
5. Copy only the necessary Fabric glue, resources, entrypoints, and mixins into the new module.
6. Adapt mappings, dependencies, `fabric.mod.json`, mixin targets, injection descriptors, and any access wideners for that Minecraft version.
7. Keep shared logic in `common`; isolate mapping-sensitive differences inside the Fabric target module.
8. Run `./gradlew :fabric-<minecraft-version>:build` and fix compile or mapping errors in that module.
