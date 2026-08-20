<img src="https://raw.githubusercontent.com/AnCarsenat/WayTooManyBinds/master/icon.png" align="right" height=48>  

# Way Too Many Binds
Fabric mod for searching and simulating keybind presses.

This project is not associated with NebelNidas and dzwdz's [too-many-binds](https://github.com/ReviversMC/too-many-binds). The upstream mod it is based on was given permission to use the name "Too Many Binds"; this one goes by "Way Too Many Binds".  
This project aims to target newer versions of Minecraft (1.20+), please use the aforementioned mod if you wish to use a similar mod for older versions.

## Usage

- **Enter** (rebindable) opens the search screen
- Type to filter, **Up/Down** or scroll to pick, **Enter** or click to press
- Matching covers the keybind's name, its category, and their translation keys
- The **ID** button shows raw ids next to each entry
- The **↑** button also lists entries scrolled off the top

Config lives in `config/waytoomanybinds/config.json` and there is a second,
unbound key to reload it in place.

## Notes

Client-side only — nothing is required on the server. Needs Fabric API.

GPL-3.0. Based on [TooManyBinds](https://modrinth.com/mod/toomanybinds) by
Ryhon0.

## Supported versions
| Minecraft | Java | Notes |
| --- | --- | --- |
| 26.2 | 25 | Minecraft ships unobfuscated, so Loom does not remap the mod |
| 1.21.11 | 21 | Built against Mojang mappings and remapped to intermediary |

## Building
The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to target several Minecraft
versions from a single source tree. Version-specific code is selected with `//?` comments,
and each target has its own properties in `versions/<version>/gradle.properties`.

```sh
./gradlew buildAll          # build every supported version
./gradlew ":26.2:build"     # build a single version
```

Each version writes its jar to `versions/<version>/build/libs/`. There is no jar in the
repository root — the root project is only the Stonecutter controller.

To switch which version the IDE resolves against, edit the `stonecutter.active(...)` call in
`stonecutter.gradle` or run the generated `stonecutterSwitchTo<version>` task.

### Adding a version
1. Add it to `versions(...)` in `settings.gradle`.
2. Create `versions/<version>/gradle.properties` with `minecraft_version`, `fabric_version`
   and `java_version`.
3. Add it to the `minecraft` matrix in `.github/workflows/build.yml`.
