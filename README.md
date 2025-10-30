# ElytraShahed

[![Download on Modrinth](https://img.shields.io/badge/Modrinth-coming%20soon-lightgrey)](https://modrinth.com/project/elytrashahed)

ElytraShahed is a PaperMC plugin that turns Elytra pilots into controllable “Shahed” kamikaze drones, complete with configurable blast power, AA support gear, and particle-free ghost fireworks for stealth boosts.

## Table of Contents

1. [Features](#features)
2. [Commands](#commands)
3. [Permissions](#permissions)
4. [Configuration](#configuration)
5. [Installation](#installation)
6. [Development & Building](#development--building)
7. [Logging](#logging)
8. [Compatibility](#compatibility)
9. [Roadmap](#roadmap)
10. [License](#license)

## Features

- **Shahed Mode Toggle** – `/shahed [power]` equips a TNT helmet, clamps power by config, and detonates on *any* collision (blocks, entities, firework hits). Automatically restores the player’s original helmet when disabled or on logout.
- **Full Collision Detection** – Bounding-box checks ensure that even glancing contact with solid blocks triggers the explosion exactly when expected.
- **AA-Gun Command** – `/aagun` hands out an indestructible, permanently charged crossbow that instantly recharges with a large red firework.
- **Ghost Fireworks** – `/ghostfirework [amount]` delivers level-3 Elytra rockets with no particles or hitbox; in flight they provide a smooth velocity boost without spawning entities.
- **Russian Localisation** – All player-facing messages are in Russian to match the original audience; console logs remain in English for clarity.
- **Rich Logging** – Enables/disables, Shahed activations, explosions, AA-Gun deliveries, ghost fireworks grants, and boosts all leave messages in the server log.
- **Configurable Blast Power** – `max-shahed-power` resides in `config.yml`; the plugin prints the current maximum during startup.

## Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/shahed [power]` | Toggle Shahed mode or set its explosive multiplier. | `elytrashahed.shahed` | OP |
| `/aagun` | Receive the instant-recharge AA-Gun crossbow. | `elytrashahed.aagun` | OP |
| `/ghostfirework [amount]` | Get particle-free level-3 rocket boosts (1-64). | `elytrashahed.ghostfirework` | OP |

> All commands send localized feedback and log significant actions in the console.

## Permissions

- `elytrashahed.shahed` – Allow toggling and configuring Shahed mode.
- `elytrashahed.aagun` – Allow issuing the AA-Gun.
- `elytrashahed.ghostfirework` – Allow giving ghost fireworks.

Grant these permissions via your preferred permission plugin if you want non-OP users to access them.

## Configuration

`config.yml` is generated automatically on first launch:

```yaml
max-shahed-power: 16
```

- Set `max-shahed-power` to clamp `/shahed` power arguments.
- Use `/reload` (not recommended) or restart the server after editing, or wire a manual reload command in the future.

## Installation

1. Download the latest release JAR from GitHub or [Modrinth](MODRINTH_URL_PLACEHOLDER).
2. Drop `ElytraShahed-<version>.jar` into your Paper server’s `plugins/` directory.
3. Start the server — the plugin writes `config.yml`, logs a startup banner, and is ready to use.

> This plugin targets **Paper 1.21.x** with **Java 21**. Folia is also supported per `plugin.yml`.

## Development & Building

The project is Gradle-based. To build from source:

```bash
./gradlew clean build
```

The assembled plugin is placed in `build/libs/ElytraShahed-<version>.jar`.

### Project Structure

- `src/main/java/com/pon4ikisdonut/elytrashahed/` – Java sources (plugin entry point, commands, state).
- `src/main/resources/` – `plugin.yml` and `config.yml`.
- `changelog.txt` – Running history of changes per release.

Pull requests and issues are welcome. If you contribute new features, please document them in the changelog and bump the semantic version (major/minor/patch) according to the project policy.

## Logging

Key events recorded in the console:

- Plugin enable/disable (with version and max Shahed power).
- Shahed activations/deactivations and resulting explosions.
- AA-Gun delivery.
- Ghost firework grants and in-flight boosts.

This makes it easy to audit player actions and debug any suspicious activity.

## Compatibility

- **Server:** PaperMC 1.21.x (Folia supported).
- **Java:** 21+
- **Dependencies:** None required; the plugin ships as a standalone JAR.

Older Paper versions are not officially supported. Behaviour on Bukkit/Spigot is untested.

## Roadmap

- Config reload command.
- Additional localization files.
- Advanced permissions for scaling power or restricting ghost fireworks.

Feel free to open feature requests!

## License

This project is distributed under the MIT License. See `LICENSE` for the full text.

---

**Useful Links**

- GitHub Releases: https://github.com/pon4ikisdonut/ElytraShahed/releases
- Modrinth Page: [https://modrinth.com/project/elytrashahed]

