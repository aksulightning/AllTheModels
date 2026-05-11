# AllTheModels
**Use any 3D model as your Minecraft skin.**

## Disclaimer

This mod is a fork and was made mostly for fun.

It is vibe-coded, experimental, and not meant to be a perfectly engineered project. Things may break, behave strangely, or be held together by optimism and questionable decisions.

Use it at your own risk, and please do not expect polished support or guaranteed compatibility.

## Overview
AllTheModels is a fork of [AllTheSkins](https://github.com/1TheCrazy/AllTheSkins). 

Tired of flat skins? *AllTheModels* lets you upload any 3D model and use it as your player skin — fully visible to everyone using the mod, even in multiplayer.

[Download the mod from nightly release](https://github.com/aksulightning/AllTheModels/releases/tag/nightly)

## Features
- 🧍‍♂️ **Custom 3D Skins** – Upload your own `.obj`, `.glb`/`.gltf`, or `.fbx` models, complete with textures where supported.
- ⚙️ **In-Game Configuration** – Use the `/skin` command to open the config screen anytime.
- 🎨 **Main Menu Preview** – Instantly see your model right in the title screen.
- 🌐 **Multiplayer Support** – Other players using the mod can see your custom skin.
- 📦 **Lightweight & Client-Side** – No server installation required.

## FBX support
FBX support is an MVP importer/editor workflow intended for Blender-exported models.

Supported today:
- Static and skinned FBX mesh import through LWJGL Assimp, with an internal binary/ascii parser fallback.
- Mesh positions, normals, UVs, materials, diffuse colors, and referenced diffuse textures where the file exposes them.
- Armature/bone detection and up to four vertex bone weights.
- In-game binding of imported bones to the six logical Minecraft body parts: `HEAD`, `CHEST`, `RIGHT_ARM`, `LEFT_ARM`, `RIGHT_LEG`, and `LEFT_LEG`.
- Rotation-only procedural `Idle`, `Walk`, and `Sneak` animation for the six-part logical rig.
- Basic imported clip detection. In the model rig editor, clips can be mapped to `Idle`, `Walk`, and `Sneak`.

Known limitations:
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
1. Run `/skin` and choose an `.fbx`, `.obj`, `.glb`, or `.gltf` file.
2. Open **Edit Model Rig** from the config screen.
3. Use **Auto Bind** to map common Blender bone names to the six Minecraft body parts.
4. Click each body-part row to cycle through detected bones and manually override the binding.
5. Inspect each logical bind pivot in the editor. Animations rotate around these pivots.
6. Optionally map imported animation clips to `Idle`, `Walk`, and `Sneak`.

The binding, source format, scale field, and animation clip mappings are saved with the selected skin in the existing config JSON.

## Constraints
⚠️ **Use at your own risk.**  
Nothing is moderated — any player can upload any model, including inappropriate ones.

- 🚫 Accounts may be blocked from the service or community-made custom backends with or without reason.
- 📁 File size is limited to **20 MB** to allow free use.
- 📴 The service and custom backends may be shut down at any time.
- 🔄 Subject to change without notice.

## 👥 Community & Contribution
*AllTheSkins* is open source and community-driven — contributions and feedback are always welcome!

- 🐛 **Found a bug or have a feature idea?**  
  → [Open an issue](https://github.com/aksulightning/alltheskins-fork/issues)

- 💡 **Want to contribute?**  
  → [Submit a pull request](https://github.com/aksulightning/alltheskins-fork/pulls)


Your creativity shapes this project — thank you for helping make *AllTheSkins* better for everyone!

## Links
- 🧾 [AllTheSkins GitHub Repository](https://github.com/1TheCrazy/AllTheSkins)
- [Support 1TheCrazy on Ko-Fi](https://ko-fi.com/1TheCrazy)