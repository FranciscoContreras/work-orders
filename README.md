# Work Orders

<p>
  <img alt="Minecraft 26.1.x" src="https://img.shields.io/badge/Minecraft-26.1.x-44aa33">
  <img alt="Platform: Purpur / Paper / Spigot / Bukkit" src="https://img.shields.io/badge/platform-Purpur%20%C2%B7%20Paper%20%C2%B7%20Spigot-2b6cb0">
  <img alt="Side: server" src="https://img.shields.io/badge/side-server-555">
  <img alt="License: All Rights Reserved" src="https://img.shields.io/badge/license-All%20Rights%20Reserved-c0392b">
</p>

**Put your copper golems to work.** Work Orders turns vanilla Copper Golems into assignable little workers — **couriers, stokers, restockers, janitors, sorters, and farmhands** — with no commands or menus to memorize. You craft a **Work Order**, hand it to a golem, and tap the containers it should use. It feels like part of the game.

> Copper golems already tidy items into copper chests in vanilla. Work Orders gives them real jobs and lets them move items *anywhere* you point them — while keeping the cozy, hands-on feel of the base game.

---

## Contents

- [Features](#features)
- [Installation](#installation)
- [How it works](#how-it-works)
- [The jobs](#the-jobs)
- [Recipes](#recipes)
- [Caring for your golems](#caring-for-your-golems)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Under the hood](#under-the-hood)
- [Compatibility](#compatibility)
- [Building from source](#building-from-source)
- [Issues &amp; contributing](#issues--contributing)
- [License](#license)

---

## Features

- **Six jobs**, each assigned by a craftable **Work Order** item — no commands required.
- **Tap-to-route:** tap a container to set the pickup, tap another for the drop-off. That's the whole setup.
- **Vanilla at heart:** golems walk, carry one stack at a time, and visibly wear the tool of their trade.
- **Safe with your items:** every transfer is a real move — items are **never duplicated and never voided**. If a destination fills up, the golem just holds the cargo.
- **Performance-friendly:** golems only work when a player is nearby; otherwise they hibernate. No offline item flow, no wasted CPU.
- **Ownership &amp; trust:** only the golem's owner (or a trusted friend) can re-task it.
- **Fully configurable:** every recipe (shape + ingredients), gating, and feedback level is editable. All player-facing text lives in `messages.yml`.
- **Optional integrations:** PlaceholderAPI and Vault (both soft — not required).

---

## Installation

**Requirements**

- A **Purpur / Paper** server on **Minecraft 26.1.x** (Spigot/Bukkit also work).
- **Java 21+**.
- Copper Golems must exist on your version (Minecraft's Copper Age update or later).

**Steps**

1. Download `WorkOrders-x.y.z.jar` (from [Releases](../../releases) or Modrinth).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.
4. *(Optional)* Install **PlaceholderAPI** and/or **Vault** for the optional integrations.

That's it — `config.yml` and `messages.yml` are generated on first start.

---

## How it works

The whole plugin runs on three inputs: **right-click**, **sneak + right-click**, and **what's in your hand**. Every action gives multi-channel feedback — the golem's held item, an action-bar line, particles, and a sound — so it's discoverable without docs.

1. **Assign a job** — craft a Work Order and right-click a copper golem with it. The golem takes the job and picks up its tool.
2. **Set its route** — tap a container to set the **pickup**, then tap another for the **drop-off**.
3. **Done** — it starts hauling.

| Action | How | Who |
| --- | --- | --- |
| **Assign a job** | Right-click the golem holding a Work Order | Owner / trusted |
| **Set pickup → drop-off** | Tap a container, then tap another (within 30s) | Owner / trusted |
| **Open the filter** | Sneak + right-click the golem holding the Work Order *(Courier / Restocker)* | Owner / trusted |
| **Whistle (locate)** | Sneak + right-click the air holding a **copper ingot** — nearby owned golems glow | Owner |
| **Wax** | Right-click with a **honeycomb** (vanilla waxing) | Anyone* |
| **Follow / stay** | Sneak + right-click empty-handed | Owner / trusted |
| **Inspect** | Right-click empty-handed — shows job, route, oxidation, owner | Anyone |

<sub>*Configurable — see `ownership.wax-anyone`.</sub>

Vanilla interactions (name tags, leashing, shearing, axe-scraping, honeycombing) are **never** intercepted.

---

## The jobs

| Job | Tool | What it does |
| --- | :---: | --- |
| **Courier** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/hopper_outside.png" width="34"> | Hauls items from one container to another (with an optional filter). |
| **Stoker** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/furnace_front.png" width="34"> | Feeds furnaces, smokers &amp; blast furnaces — fuel to the fuel slot, smeltables to the input, never the result slot. |
| **Restocker** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/barrel_top.png" width="34"> | Keeps a destination topped up to a set amount, then stops. |
| **Janitor** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/brush.png" width="34"> | Vacuums up dropped items in a radius and banks them in a chest. |
| **Sorter** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/comparator.png" width="34"> | Splits items from one source across several destinations, routed by what each chest already holds. |
| **Farmhand** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/iron_hoe.png" width="34"> | Harvests and replants crops in a small radius. Pair with a Janitor to gather the drops. |

---

## Recipes

Every Work Order uses the same frame — **copper ingots around a sheet of paper** — with the **job's tool** in the bottom-center slot. Swap the tool to change the job. Recipes are fully configurable in `config.yml`.

**Example — Courier:**

| | | |
| :-: | :-: | :-: |
| | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/copper_ingot.png" width="34"> | |
| <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/copper_ingot.png" width="34"> | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/paper.png" width="34"> | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/copper_ingot.png" width="34"> |
| <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/copper_ingot.png" width="34"> | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/hopper_outside.png" width="34"> | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/item/copper_ingot.png" width="34"> |

→ **Work Order: Courier** &nbsp;(5× Copper Ingot · 1× Paper · 1× the job tool)

Keep the copper-and-paper frame and swap the bottom-center tool for the other jobs:

| Job | Bottom-center tool |
| --- | --- |
| Courier | Hopper |
| Stoker | Furnace |
| Restocker | Barrel |
| Janitor | Brush |
| Sorter | Comparator |
| Farmhand | Iron Hoe |

---

## Caring for your golems

- **Wax** — right-click with a honeycomb to stop oxidation (vanilla).
- **Whistle** — sneak + right-click the air holding a copper ingot to glow your nearby golems.
- **Inspect** — right-click empty-handed to see a golem's job, route, and oxidation at a glance.
- **Follow / stay** — sneak + right-click empty-handed to toggle a golem following you (pauses hauling while it follows).
- **Oxidation** — golems oxidise slowly over time. It's a *soft* cost: a fully-oxidised (frozen) golem stops working and glows so you can find it; scrape it with an axe to bring it back. Never lethal. Optionally, efficiency can scale with oxidation (`oxidation.efficiency-scales-with-oxidation`).

---

## Commands

There are **no player commands** — the whole plugin is interaction-driven. The single command is for admins:

| Command | Description | Permission |
| --- | --- | --- |
| `/workorders reload` | Reload `config.yml` + `messages.yml` and re-register recipes (cost edits apply live) | `workorders.admin` |
| `/workorders status` | Show tracked golems, tick settings, and active config | `workorders.admin` |
| `/workorders spike` | Probe the nearest copper golem's native AI goals (diagnostic) | `workorders.admin` |

Alias: **`/wo`**.

---

## Permissions

| Node | Default | Description |
| --- | --- | --- |
| `workorders.admin` | `op` | Run `/workorders`, bypass ownership, and bypass job gating. |
| `workorders.job.courier` | `true` | Allowed to assign the Courier job. |
| `workorders.job.stoker` | `true` | Allowed to assign the Stoker job. |
| `workorders.job.restocker` | `true` | Allowed to assign the Restocker job. |
| `workorders.job.janitor` | `true` | Allowed to assign the Janitor job. |
| `workorders.job.sorter` | `true` | Allowed to assign the Sorter job. |
| `workorders.job.farmhand` | `true` | Allowed to assign the Farmhand job. |

Job permissions default to **everyone**. To gate jobs (e.g. by rank), set `gating.require-permission: true` and assign the `workorders.job.*` nodes in your permissions plugin. Example with LuckPerms:

```
/lp group member permission set workorders.job.janitor true
/lp group member permission set workorders.job.restocker true
/lp group veteran permission set workorders.job.courier true
```

---

## Configuration

Full `config.yml` reference. Every value below has a sensible default; edit and run `/wo reload`.

### `settings`

| Key | Default | Meaning |
| --- | --- | --- |
| `service-interval-ticks` | `10` | How often the work scheduler fires (10 = twice per second). |
| `slices` | `4` | Golems are processed in this many rotating groups (1/N per fire) to spread load. |
| `active-radius` | `64.0` | A golem only works when a player is within this many blocks of it or its targets; otherwise it hibernates. |
| `range-cap` | `48.0` | Max distance between a golem and a container it can be bound to (hard-capped at 64). |
| `move-speed` | `1.0` | Pathfinding speed multiplier when walking to a target. |
| `pull-batch` | `16` | Items carried per trip (vanilla copper-golem cadence is 16). |
| `path-fail-grace` | `3` | Work cycles with no reachable path before a route is marked "stuck". |
| `bind-session-ttl-ticks` | `600` | How long the two-tap "set source then destination" flow stays open (600 = 30s). |

### `ownership`

| Key | Default | Meaning |
| --- | --- | --- |
| `trusted-only-retask` | `true` | Only the golem's owner (or `workorders.admin`) may re-task it. |
| `wax-anyone` | `true` | Anyone may wax a golem with honeycomb. If false, owner/admin only. |
| `whistle-enabled` | `true` | Enable the whistle-to-locate feature. |
| `whistle-item` | `COPPER_INGOT` | Item held (sneak + right-click air) to glow your nearby golems. |
| `whistle-radius` | `48.0` | Whistle range in blocks. |
| `whistle-glow-seconds` | `4` | How long golems glow after a whistle. |
| `consume-role-item` | `false` | If true, assigning a job consumes the held item; if false, it's only copied onto the golem as a label. |

### `native-ai`

| Key | Default | Meaning |
| --- | --- | --- |
| `suppress-native-sort` | `auto` | `auto` = suppress the native sorting goal only if a removable one exists at runtime, else coexist. `off` = always coexist. |

### `gating`

| Key | Default | Meaning |
| --- | --- | --- |
| `require-permission` | `true` | Require `workorders.job.<role>` to assign that job. Set false to let anyone use every job. |

### `work-orders`

Each job's Work Order is a **shaped recipe** — this is the cost. Edit the shape + ingredients freely.

```yaml
work-orders:
  courier:
    shape: [" C ", "CPC", "CTC"]   # a space = empty cell
    ingredients: { C: COPPER_INGOT, P: PAPER, T: HOPPER }
    model: 0                        # optional custom_model_data (0 = none)
  stoker:
    ingredients: { C: COPPER_INGOT, P: PAPER, T: FURNACE }
  restocker:
    ingredients: { C: COPPER_INGOT, P: PAPER, T: BARREL }
  janitor:
    ingredients: { C: COPPER_INGOT, P: PAPER, T: BRUSH }
  sorter:
    ingredients: { C: COPPER_INGOT, P: PAPER, T: COMPARATOR }
  farmhand:
    ingredients: { C: COPPER_INGOT, P: PAPER, T: IRON_HOE }
```

### Per-job &amp; feedback

| Key | Default | Meaning |
| --- | --- | --- |
| `restocker.default-threshold` | `64` | "Keep topped up to N" when a Restocker has no explicit threshold. |
| `janitor.radius` | `6` | How far (blocks) a Janitor vacuums dropped items (capped at 16). |
| `farmhand.radius` | `4` | How far (blocks) a Farmhand tends crops (capped at 8). |
| `oxidation.efficiency-scales-with-oxidation` | `false` | If true, golems work slower as they oxidise (before freezing). |
| `feedback.intensity` | `normal` | `off` / `subtle` / `normal` / `lively` — scales particles &amp; sounds. |
| `feedback.sounds` | `true` | Play feedback sounds. |
| `feedback.actionbar` | `true` | Show action-bar narration. |
| `feedback.frozen-glow` | `true` | Glow a fully-oxidised (frozen) golem so you can find it. |
| `feedback.sluggish-fx` | `true` | Particles/sounds as a golem oxidises and slows. |
| `feedback.bring-flower` | `true` | A golem occasionally brings its owner a flower after a big batch. |
| `debug` | `false` | Verbose logging. |

All player-facing strings live in **`messages.yml`** (MiniMessage formatting) — fully re-brandable and translatable.

---

## Under the hood

The technical details, for server admins and the curious.

- **Built on the first-class Copper Golem API.** Work Orders uses only the stable Bukkit/Paper API (`CopperGolem`, `Pathfinder`, inventory APIs) — no NMS, no reflection into server internals — so it stays portable across server forks.
- **Coexists with native sorting.** A copper golem's vanilla item-sorting behaviour is **brain-internal and not removable** through the public Goal API (only `minecraft:has_rider` is exposed). Work Orders is designed to operate in a non-overlapping envelope and **coexist** with it; `suppress-native-sort: auto` simply finds nothing to remove and coexists. Use `/wo spike` to inspect a golem's exposed goals.
- **Never dupes, never voids.** The only place an in-transit item lives is the golem's main hand. Pulls are atomic (remove-from-source + place-in-hand in one synchronous step); deposits use leftover-returning inventory merges with correct furnace/brewing slot semantics. If a destination is full the golem carries the cargo back or idles holding it — items are never dropped on the ground or deleted. On death, cargo and the job item drop and are recoverable.
- **Cheap and player-gated.** A slice-and-stagger scheduler processes a fraction of tracked golems each tick, and a distance gate skips any golem with no player within `active-radius`. Golems with no nearby player **hibernate** — which doubles as the economy guard (no offline/AFK item flow).
- **Survives restarts.** Each managed golem's state (job, route, filter, owner) is stored in its **persistent data container**, so it survives chunk unloads, server restarts, and even spawn-egg capture.
- **Integrations are optional.** PlaceholderAPI and Vault are soft-depends; the plugin runs fully without them. Vault, if present, enables optional money-cost gating.
- **No public developer API in v1.** The plugin exposes no events or hooks for other plugins yet.

---

## Compatibility

- **Minecraft:** 26.1.x
- **Server:** Purpur, Paper (Spigot / Bukkit also supported)
- **Side:** server-only — no client mod required (works for Java and Bedrock-via-Geyser players)
- **Java:** 21+
- **Requires:** Copper Golems (Minecraft's Copper Age update or later)

---

## Building from source

```bash
git clone https://github.com/FranciscoContreras/work-orders.git
cd work-orders
./gradlew build
# → build/libs/WorkOrders-1.0.0.jar
```

Requires a **JDK 21+**. The build pulls the Paper API from Maven Central + the PaperMC repo; no other setup needed.

---

## Issues &amp; contributing

Found a bug or have an idea? **[Open an issue](../../issues).** Please include your server version, the plugin version, and steps to reproduce.

This project is source-available but **not** open-source — see the license below before reusing any code.

---

## License

**All Rights Reserved.** © Machina. See [`LICENSE`](LICENSE).

You may run the official, unmodified binary on your own server. All other rights — copying, modifying, redistributing, or selling — are reserved.

Built by **[Machina](https://wearemachina.com)**.
