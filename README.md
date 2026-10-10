# Syrup Essentials

This repository builds ten Fabric, Forge, and NeoForge targets.

- Minecraft 1.20.1: Fabric and Forge
- Minecraft 1.21.1: Fabric and NeoForge
- Minecraft 1.21.11: Fabric and NeoForge
- Minecraft 26.2: Fabric and NeoForge
- Minecraft 26.3: Fabric and NeoForge

```sh
./gradlew build
./gradlew buildAndCollect
```

The active target is `1.20.1-fabric`, selected in `stonecutter.gradle.kts`. Target-specific settings are in `stonecutter.properties.yaml`. Collected jars are written to `build/libs/0.4.2/`.

Gradle downloads the Java 17, 21, or 25 toolchain for the selected target.

## Command permissions

Each command uses `syrup_essentials.command.<command>`. User commands default to all sources. `setwarp`, `teleport_last`, `delwarp`, `tpx`, `jump`, and `reload` default to game masters. On Fabric 26.2+, the node is represented as `syrup_essentials:command.<command>`; Forge, NeoForge, and older Fabric use `syrup_essentials.command.<command>`.

`/leaderboard` lists available statistics. Select `deaths`, `time_played`, `deaths_per_hour`, `player_kills`, `mob_kills`, `damage_dealt`, `jumps`, `distance_walked`, or `time_since_death` to display the top 20 players. Offline player statistics are read from the world's `stats` directory. `/nickname [name]` sets or clears your display name. Nicknames are saved with player data and are limited to 32 characters. The `miscellaneous` config section can disable either command.
