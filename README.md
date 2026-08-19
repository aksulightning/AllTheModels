# FBX Player Models

Use a 3D model as your Minecraft player skin.

FBX Player Models lets you pick an `.fbx` model and show it on your player. Other players can see it too when they also have the mod installed.

This mod is experimental. It was made for fun, so some models may not work perfectly.

## Download

- [Minecraft 1.21.1 nightly](https://github.com/aksulightning/FBXPlayerModels/releases/tag/nightly-1.21.1)
- [Minecraft 26.1.2 nightly](https://github.com/aksulightning/FBXPlayerModels/releases/tag/nightly-26.1.2)
- [Minecraft 26.2 nightly](https://github.com/aksulightning/FBXPlayerModels/releases/tag/nightly-26.2)

Both downloads are experimental builds.

## What You Can Do

- Use an `.fbx` model instead of a normal Minecraft skin.
- See your model on the main menu.
- Change your model in game with `/skin`.
- Use custom models on multiplayer servers.
- Let server players upload models if they have permission.

## How To Use A Model

1. Install the mod.
2. Join a world or server.
3. Run `/skin`.
4. Choose your `.fbx` file.
5. Open **Edit Model Rig** if the arms, legs, head, or body do not move correctly.
6. Use **Auto Bind** first. If needed, click each body part and choose the right bone by hand.

Your settings are saved after you choose and edit your model.

## Best Model Type

For the best chance of working, use a simple model exported from Blender.

Good models usually have:

- Low detail.
- One material.
- A texture.
- Clear bone names.
- A file size under **3 MB**.

The rig names like `Head`, `Chest`, `Right Arm`, `Left Arm`, `Right Leg`, and `Left Leg` can help the mod guess the right body parts.

If you’re looking for a model template, you can find one here:

[https://github.com/aksulightning/FBXPlayerModels/tree/template](https://github.com/aksulightning/FBXPlayerModels/tree/template)

## Multiplayer Uploads

Servers store uploaded models in the world save.

Operators can upload models by default. Operators can also allow or block uploads for other players:

Allow players:
```
/fbxplayermodels uploadperm <playername> yes
```
Deny players:
```
/fbxplayermodels uploadperm <playername> no
```

Uploaded files must be **3 MB or smaller**.

## FBX Mobs

The mod can also show FBX models on special entities.

Example commands:

```
/summon fbxplayermodels:view_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:hostile_entity ~ ~ ~ {Model:"this_file.fbx"}
/summon fbxplayermodels:tameable_entity ~ ~ ~ {Model:"this_file.fbx",TameItem:"minecraft:apple"}
```

The model file must already be on the server in its FBX mob model folder.

## Help And Feedback

- [Report a bug or suggest an idea](https://github.com/aksulightning/FBXPlayerModels/issues)
- [Contribute code](https://github.com/aksulightning/FBXPlayerModels/pulls)

## Credits

FBX Player Models is based on [AllTheSkins by 1TheCrazy](https://github.com/1TheCrazy/AllTheSkins).
