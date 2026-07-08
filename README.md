# ToolPRO — a Meteor Client addon

ToolPRO is a [Meteor Client](https://meteorclient.com/) addon focused on crystal PvP (cPvP).
It adds an automatic **D-Tap** combo, configurable **pearl** mechanics, a **Pearl Anchor** module and
an improved **Auto Totem**.

> **Version:** Minecraft `1.21.4` · Meteor Client `1.21.4-SNAPSHOT` · Java `21`

> ⚠️ **Fair play:** Automation like this is against the rules of most public servers and of
> Minecraft's EULA in many contexts. Only use it where it is explicitly allowed (private servers,
> testing worlds, friends). You are responsible for complying with the rules of any server you join.

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft `1.21.4`.
2. Download Meteor Client for `1.21.4` from [meteorclient.com](https://meteorclient.com/) and place the jar
   in your `.minecraft/mods` folder.
3. Place `toolpro-1.0.0.jar` (see [Building](#building) or the release page) in the same
   `mods` folder, next to Meteor Client.
4. Launch the game. The modules appear in the Meteor GUI (default `Right Shift`) under the
   **ToolPRO** category.

Because it is a normal Fabric mod jar it can also be distributed through and installed from
[Modrinth](https://modrinth.com/) once uploaded there.

---

## Modules

### Auto D-Tap (`auto-d-tap`)
Hits a nearby opponent to launch them, then places (and optionally breaks) end crystals while they
are airborne. If the pearl option is enabled and ender pearls are in your hotbar, a pearl is thrown
near the target before the crystals go down.

| Setting | Group | Default | Description |
| --- | --- | --- | --- |
| `target-range` | General | `5.0` | Max distance to look for a target. |
| `target-priority` | General | `LowestHealth` | How the target is chosen when several are in range. |
| `hit-delay` | General | `2` | Ticks between launch attacks. |
| `require-airborne` | General | `true` | Only crystal the target once they leave the ground. |
| `swing` | General | `true` | Render a hand swing for attacks/placements/pearls. |
| `place-delay` | Crystal | `1` | Ticks between crystal placements. |
| `auto-break` | Crystal | `true` | Break placed crystals that are within reach of the target. |
| `break-delay` | Crystal | `0` | Ticks between crystal breaks. |
| `search-radius` | Crystal | `4.0` | Horizontal radius around the target to look for a crystal base. |
| `min-damage` | Crystal | `4.0` | Minimum damage a placement must deal to the target. |
| `max-self-damage` | Crystal | `8.0` | Never place crystals that would deal more than this to you. |
| `swap-back` | Crystal | `true` | Switch back to the previous slot after placing. |
| `throw-pearls` | Pearl | `false` | **The configurable pearl mechanic.** When on and pearls are in the hotbar, throw one near the target while airborne before crystalling. |
| `pearl-delay` | Pearl | `30` | Minimum ticks between pearl throws. |
| `airborne-only` | Pearl | `true` | Only throw pearls while the target is off the ground. |
| `rotate` | Rotation | `true` | Rotate towards the target and placements. |

The best crystal base is chosen using Meteor's own damage math (`DamageUtils.crystalDamage`) so the
placement that hurts the target the most — while respecting `max-self-damage` — is picked.

### Pearl Anchor (`pearl-anchor`)
When ender pearls are in the hotbar, this module pearls you into the **deepest, most protected hole**
nearby to maximise your defensive positioning. It scans the surrounding area for holes surrounded by
blast-resistant blocks (obsidian / bedrock) and throws a pearl into the deepest one.

| Setting | Default | Description |
| --- | --- | --- |
| `radius` | `3.0` | Horizontal radius to scan for holes. |
| `depth` | `4` | How many blocks below your feet to look for a hole bottom. |
| `require-walls` | `true` | Only consider spots enclosed by blast-resistant blocks on all four sides. |
| `delay` | `20` | Minimum ticks between pearl throws. |
| `rotate` | `true` | Rotate down into the hole before throwing. |
| `swing` | `true` | Render a hand swing when throwing. |
| `swap-back` | `true` | Switch back to the previous slot after throwing. |

> Pearl trajectories are physics-based, so landing is best-effort: the module aims straight down into
> the chosen hole. Keep `radius` small for the most reliable drop.

### Better Auto Totem (`better-auto-totem`)
An improved Auto Totem. On top of "keep a totem in your offhand" it predicts incoming explosion/fall
damage, reacts the **same tick a totem pops**, works without any inventory screen open, and shows the
number of totems left next to the module name.

| Setting | Default | Description |
| --- | --- | --- |
| `mode` | `Smart` | `Smart` only holds when needed, `Strict` always holds. |
| `health` | `12` | Equip when effective health drops to/below this. |
| `predict-explosion` | `true` | Equip when a nearby crystal/bed/anchor could kill you. |
| `predict-fall` | `true` | Equip when fall damage could kill you. |
| `elytra` | `true` | Always hold a totem while gliding. |
| `delay` | `0` | Ticks between totem swaps. |

---

## Building

Requires JDK 21.

```bash
./gradlew build
```

The compiled addon jar is written to `build/libs/toolpro-1.0.0.jar`. Copy it into your
`.minecraft/mods` folder alongside Meteor Client.

### Project layout

```
src/main/java/com/toolpro
├── ToolPro.java        # addon entrypoint, registers the category + modules
├── modules
│   ├── AutoDTap.java           # feature 1 + 2 (auto d-tap + configurable pearl)
│   ├── PearlAnchor.java        # feature 3 (pearl into deepest hole)
│   └── BetterAutoTotem.java    # feature 4 (enhanced auto totem)
└── util
    └── CombatUtils.java        # shared packet helpers (attack / place / use item)
```

## License

Released under the CC0 license (inherited from the Meteor addon template). See `LICENSE`.
