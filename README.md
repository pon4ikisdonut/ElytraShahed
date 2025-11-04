# ElytraShahed

[![Download on Modrinth](https://img.shields.io/modrinth/dt/elytrashahed?label=Modrinth&logo=modrinth)](https://modrinth.com/plugin/elytrashahed)

ElytraShahed is a Paper/Folia plugin that arms Elytra pilots with configurable TNT payloads, optional crater carving, reactive booster thrusters, and an endlessly charged anti-air crossbow for server operators who like their dogfights loud.

## Table of Contents

1. [Features](#features)
2. [Gameplay Overview](#gameplay-overview)
3. [Commands](#commands)
4. [Permissions](#permissions)
5. [Configuration](#configuration)
6. [Localization](#localization)
7. [Installation](#installation)
8. [Development & Building](#development--building)
9. [Logging](#logging)
10. [Compatibility](#compatibility)
11. [Roadmap](#roadmap)
12. [License](#license)

## Features

- **Shahed Mode Arm/Detonate** – `/shahed [power]` swaps the pilot’s helmet to TNT, consumes the required TNT stacks, and detonates on *any* block/entity collision (including Elytra wall hits and firework impacts). Helmets are restored automatically on disable, logout, or death cleanup.
- **Collision-Perfect Trigger** – Uses expanded bounding boxes and voxel collision shapes so even grazing a block or an entity sets the player off at the right tick.
- **Dynamic Blast Scaling** – Explosion power is `base-explosion-power × requested scale`; optional funnel mode sculpts a crater beneath the impact using configurable depth/radius multipliers.
- **Reactive Boost Thrusters** – Right-click the configured material (default `NETHERITE_SCRAP`) while gliding to gain a timed velocity boost with upward thrust, respecting a permission gate and consuming the item in survival.
- **Anti-Air Crossbow** – `/aagun` drops an unbreakable, permanently-charged crossbow that instantly reloads a custom firework rocket after every shot (Folia-safe scheduling included).
- **Inventory Safeguards** – Shahed pilots cannot drop or replace their TNT helmet while armed; `/shahed` scale adjustments re-check TNT availability before changing power.
- **Rich Server Logging** – Startup banner, config reloads, Shahed arming/disarming, detonations, reactive boosts, and AA-Gun handouts are all echoed to console with versioned context.
- **Folia-Friendly Implementation** – Uses Folia schedulers when available, with Bukkit fallbacks, so both Paper and Folia servers run smoothly.

## Gameplay Overview

1. **Arm** – Pilot runs `/shahed [power]`; the plugin verifies TNT is in inventory (unless Creative), equips TNT helmet, and stores the old helmet.
2. **Flight** – While gliding, the plugin continuously monitors collisions against blocks, entities, fireworks, and Elytra wall impacts.
3. **Impact** – On collision the plugin restores the old helmet, creates an explosion using the configured scaling, optionally carves a funnel-shaped crater, and forces lethal self-damage with a custom death message.
4. **Boosting** – Pilots can spend the configured item mid-flight to gain several ticks of extra forward/upward momentum, capped at a maximum velocity.
5. **Support Fire** – Operators with permission can keep the skies dangerous by issuing the endlessly-charged AA-Gun crossbow.

## Commands

| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/shahed [power]` | Toggle Shahed mode or set its explosive multiplier. | `elytrashahed.shahed` | OP |
| `/aagun` | Receive the instant-recharge AA-Gun crossbow. | `elytrashahed.aagun` | OP |
| `/elytrareload` | Reload ElytraShahed configuration without restarting. | `elytrashahed.reload` | OP |

> All commands send localized feedback and log significant actions in the console.

## Permissions

- `elytrashahed.shahed` – Allow toggling and configuring Shahed mode.
- `elytrashahed.aagun` – Allow issuing the AA-Gun.
- `elytrashahed.reactive` – Allow using the reactive Shahed boost item.
- `elytrashahed.reload` – Allow reloading the plugin configuration.

Grant these permissions via your preferred permission plugin if you want non-OP users to access them.

## Configuration

`config.yml` is generated automatically on first launch:

```yaml
language: en
max-shahed-power: 16
reactive-boost-item: NETHERITE_SCRAP
reactive-boost-power: 3
base-explosion-power: 5.0
crater-mode: vanilla
funnel-radius-per-scale: 1.2
funnel-depth-per-scale: 0.6
```

- `language` – Active locale for in-game messages, fallback is English.
- `max-shahed-power` – Caps the scale argument accepted by `/shahed`.
- `reactive-boost-item` – Material ID that is consumed to trigger boosters.
- `reactive-boost-power` – Controls boost duration, speed, and lift (higher = longer/faster).
- `base-explosion-power` – Multiplier used before applying the chosen scale.
- `crater-mode` – `vanilla` uses only the Minecraft explosion; `funnel` additionally digs a conical crater.
- `funnel-radius-per-scale` / `funnel-depth-per-scale` – Radius and depth multipliers per Shahed scale when funnel mode is active.

Edit the file and run `/elytrareload` to apply changes without restarting.

## Localization

Language files are bundled for `en`, `ru`, `uk`, `pl`, `de`, `kk`, and `es`. On first launch they are copied into `plugins/ElytraShahed/lang/`. To customize:

1. Pick the language in `config.yml`.
2. Edit the corresponding `lang/<code>.yml` file to adjust messages (supports classic `&` color codes).
3. Use `/elytrareload` to refresh the active language without a restart.

## Installation

1. Download the latest release JAR from GitHub or [Modrinth](https://modrinth.com/plugin/elytrashahed).
2. Drop `ElytraShahed-<version>.jar` into your Paper server's `plugins/` directory.
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

- Plugin enable/disable (with version, max Shahed power, language, crater mode, reactive item).
- Shahed activations, scale updates, deactivations, and resulting explosions.
- AA-Gun deliveries and reactive boost usage.
- Config reloads and the chosen language code.
- Custom death messages for Shahed pilots who go out with a bang.

This makes it easy to audit player actions and debug any suspicious activity.

## Compatibility

- **Server:** PaperMC 1.21.x (Folia supported).
- **Java:** 21+
- **Dependencies:** None required; the plugin ships as a standalone JAR.

Older Paper versions are not officially supported. Behaviour on Bukkit/Spigot is untested.

## Roadmap

- More granular cooldown or warm-up rules for Shahed arming.
- Optional permission tiers for higher explosion scales.
- Expanded booster mechanics (cooldowns, particles, sounds).
- Additional language packs and community translations.

Feel free to open feature requests!

## License

This project is distributed under the MIT License. See `LICENSE` for the full text.

---

**Useful Links**

- GitHub Releases: https://github.com/pon4ikisdonut/ElytraShahed/releases
- Modrinth Page: https://modrinth.com/plugin/elytrashahed


