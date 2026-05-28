# Work Orders

**Put your copper golems to work.** Couriers, stokers, restockers, janitors, sorters, and farmhands — assigned the way it should be: you hand a golem a job and point it at a chest. No commands to memorize, no menus to fight.

Copper golems already tidy items into copper chests in vanilla. **Work Orders** gives them real jobs and lets them move items *anywhere* you tell them — while keeping the cozy, hands-on feel of the base game.

📖 **Full guide & how-tos:** [Work Orders on the Sunday Market wiki](https://sundaymarket.wearemachina.com/wiki/work-orders)

## Features

- **Six jobs**, each assigned by a craftable **Work Order** item — no commands required.
- **Tap-to-route:** tap a container to set the pickup, tap another to set the drop-off. That's the whole setup.
- **Vanilla at heart:** golems walk, carry one stack at a time, and visibly wear the tool of their trade. They look like they belong.
- **Safe with your stuff:** every transfer is a real move — items are **never duplicated and never voided**. Destination full? The golem just holds the cargo.
- **Performance-friendly:** golems only work when a player is nearby; otherwise they hibernate. No offline item flow, no wasted CPU.
- **Yours to command:** only the golem's owner (or trusted friends) can re-task it.

## How it works

1. **Craft** the Work Order for the job you want (recipes below).
2. **Right-click** a copper golem while holding it — the golem takes the job and picks up its tool.
3. **Tap a container** to set the pickup, then **tap another** for the drop-off. It gets to work.

## The jobs

| Job | Tool | What it does |
|---|:---:|---|
| **Courier** | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/hopper_outside.png" alt="Hopper" width="40"> | Hauls items from one container to another. |
| **Stoker** | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/furnace" alt="Furnace" width="40"> | Feeds furnaces — fuel to the fuel slot, ore to the input. |
| **Restocker** | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/barrel" alt="Barrel" width="40"> | Keeps a container topped up to a set amount. |
| **Janitor** | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/brush" alt="Brush" width="40"> | Vacuums up dropped items nearby and banks them. |
| **Sorter** | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/comparator" alt="Comparator" width="40"> | Splits items across containers by what each already holds. |
| **Farmhand** | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/iron_hoe" alt="Iron Hoe" width="40"> | Harvests and replants crops in a small radius. |

## Recipes

Every Work Order uses the same frame — **copper ingots around a sheet of paper** — with the **job's tool** in the bottom-center slot. Craft the one you want, then hand it to a golem.

**Example — Courier:**

| | | |
|:-:|:-:|:-:|
| | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/copper_ingot" alt="Copper Ingot" width="40"> | |
| <img src="https://sundaymarket.wearemachina.com/api/mc/texture/copper_ingot" alt="Copper Ingot" width="40"> | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/paper" alt="Paper" width="40"> | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/copper_ingot" alt="Copper Ingot" width="40"> |
| <img src="https://sundaymarket.wearemachina.com/api/mc/texture/copper_ingot" alt="Copper Ingot" width="40"> | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/hopper_outside.png" alt="Hopper" width="40"> | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/copper_ingot" alt="Copper Ingot" width="40"> |

→ **Work Order: Courier**

For the other five jobs, keep the copper-and-paper frame and just **swap the bottom-center tool**:

| Job | Bottom-center tool |
|---|---|
| Courier | <img src="https://cdn.jsdelivr.net/gh/misode/mcmeta@assets/assets/minecraft/textures/block/hopper_outside.png" alt="Hopper" width="36"> Hopper |
| Stoker | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/furnace" alt="Furnace" width="36"> Furnace |
| Restocker | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/barrel" alt="Barrel" width="36"> Barrel |
| Janitor | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/brush" alt="Brush" width="36"> Brush |
| Sorter | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/comparator" alt="Comparator" width="36"> Comparator |
| Farmhand | <img src="https://sundaymarket.wearemachina.com/api/mc/texture/iron_hoe" alt="Iron Hoe" width="36"> Iron Hoe |

> Recipes are fully configurable — server owners can change the shape, ingredients, and cost in `config.yml`.

## Caring for your golems

- **Wax** with a honeycomb to stop oxidation (just like vanilla).
- **Whistle** — sneak + right-click the air holding a copper ingot to make your nearby golems glow.
- **Check on one** — right-click it empty-handed to see its current job and route.
- **Follow / stay** — sneak + right-click empty-handed to toggle it following you.

## Compatibility

- **Minecraft 26.1.x** · Purpur / Paper / Spigot / Bukkit
- **Server-side only** — no client mod required.
- Requires Copper Golems (Minecraft's Copper Age update or later).
- Optional integrations: PlaceholderAPI, Vault.

---

Built by **[Machina](https://wearemachina.com)** ✦ All Rights Reserved.
