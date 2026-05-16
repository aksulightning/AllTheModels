# FBX Player Models
**Use any 3D model as your Minecraft skin.**

## Overview
FBX Player Models is an AI-assisted fork of [AllTheSkins](https://github.com/1TheCrazy/AllTheSkins). 

Tired of vanilla skins? *FBX Player Models* lets you upload any 3D model and use it as your player skin — fully visible to everyone using the mod, even in multiplayer.

##### Disclaimer

This mod is a fork and was made mostly for fun.

It is vibe-coded, experimental, and not meant to be a perfectly engineered project. Things may break, behave strangely, or be held together by optimism and questionable decisions.

Use it at your own risk, and please do not expect polished support or guaranteed compatibility.

## Releases

[Download nightly for 1.21.1](https://github.com/aksulightning/FBXPlayerModels/releases/tag/nightly-1.21.1) (Supported, experimental.)

[Download nightly for 26.1.2](https://github.com/aksulightning/FBXPlayerModels/releases/tag/nightly-26.1.2) (Fully supported, experimental.) 

## Features
- 🧍‍♂️ **Custom 3D Skins** – Upload your own `.fbx` models, complete with textures where supported.
- ⚙️ **In-Game Configuration** – Use the `/skin` command to open the config screen anytime.
- 🎨 **Main Menu Preview** – Instantly see your model right in the title screen.
- 🌐 **Multiplayer Support** – Other players using the mod can see your custom skin.
- 📦 **Lightweight & Client-Side** – No server installation required.

## FBX support
FBX support is an MVP importer/editor workflow intended for Blender-exported models. This feature is truly experimental, low poly models are recommended and include a single material with texture.

- Static and skinned FBX mesh import through LWJGL Assimp, with an internal binary/ascii parser fallback.
- Mesh positions, normals, UVs, materials, diffuse colors, and referenced diffuse textures where the file exposes them.
- Armature/bone detection and up to four vertex bone weights.
- In-game binding of imported bones to the six logical Minecraft body parts: `HEAD`, `CHEST`, `RIGHT_ARM`, `LEFT_ARM`, `RIGHT_LEG`, and `LEFT_LEG`.
- Rotation-only procedural `Idle`, `Walk`, and `Sneak` animation for the six-part logical rig.
- Basic imported clip detection. In the model rig editor, clips can be mapped to `Idle`, `Walk`, and `Sneak`.

Known limitations:
- The rigging menu is still to-do. Specific features may not work.
- FBX is a broad format. Blender FBX exports are the main target.
- Shape keys/blend shapes, advanced material graphs, constraints, IK, and complex animation stacks are not fully supported yet.
- If a model has no usable skin weights, the fallback is best-effort and may need manual rig binding.
- Translation keys in imported animation clips are ignored. Runtime logical rig animation rotates around each configured bind pivot and does not move body parts away from their bind positions.
- Bad, oversized, malformed, or unsupported FBX files should fail gracefully instead of crashing the client.

## Blender FBX export notes
- Apply transforms before export when possible.
- Use a simple armature and clear bone names.
- Keep the model under the mod upload limit of **20 MB**.
- Export with UVs and materials enabled.
- Prefer embedded textures or clearly referenced texture files next to the FBX.
- Names like `head`, `neck`, `spine`, `chest`, `torso`, `upper_arm.R`, `upper_arm.L`, `leg.R`, and `leg.L` help the auto-binder.

## Binding bones in game
1. Run `/skin` and choose an `.fbx` file.
2. Open **Edit Model Rig** from the config screen.
3. Use **Auto Bind** to map common Blender bone names to the six Minecraft body parts.
4. Click each body-part row to cycle through detected bones and manually override the binding.
5. Inspect each logical bind pivot in the editor. Animations rotate around these pivots.
6. Optionally map imported animation clips to `Idle`, `Walk`, and `Sneak`.

The binding, source format, scale field, and animation clip mappings are saved with the selected skin in the existing config JSON.

## About custom backends
Default builds always point to a local backend at `http://127.0.0.1:6969` / `localhost`. They are intended to talk only to a backend process running on your own machine, such as the included development emulator.

Community-made custom backends are not official, not moderated by this project, and are used entirely at your own risk. A custom backend can see uploaded skin files and request metadata, serve altered or unsafe model data, log identifying information, disappear without warning, or block accounts for any reason. Only use a backend run by someone you trust, and avoid uploading private, sensitive, copyrighted, or personally identifying content.

If you intentionally change the source to use a remote backend, review that backend's code, hosting, moderation policy, data retention, and abuse controls first. Do not expose a local emulator to the internet unless you understand the security implications and have added proper access controls.

## Constraints
⚠️ **Use at your own risk.**  
Nothing is moderated — any player can upload any model, including inappropriate ones.

- 📁 File size is limited to **20 MB** to allow free use.
- 🔄 Subject to change without notice.

## 👥 Community & Contribution
*FBX Player Models* is open source — contributions and feedback are always welcome!

- 🐛 **Have a feature idea?**  
  → [Open an issue](https://github.com/aksulightning/FBXPlayerModels/issues)

- 💡 **Want to contribute?**  
  → [Submit a pull request](https://github.com/aksulightning/FBXPlayerModels/pulls)


Your creativity shapes this project — thank you for helping make *FBX Player Models* better for everyone!

## Links
- 🧾 [AllTheSkins GitHub Repository](https://github.com/1TheCrazy/AllTheSkins)
- [Support 1TheCrazy on Ko-Fi](https://ko-fi.com/1TheCrazy)
