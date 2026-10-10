# HerdStead

HerdStead gives wild animals a real population. Instead of vanilla's endless
respawning, each stretch of land holds a limited number of animals. Hunt them
too hard and the area thins out. Leave them alone and herds slowly recover.

Requires Minecraft 26.1.2, Fabric Loader 0.19.5 or newer, Java 25 or newer, and
Fabric API.

## How it works

**Zones.** When you walk into land that isn't part of a zone yet, HerdStead maps
out the connected area of the same biome around you and makes it a zone. Bigger
zones hold more animals. Mapping is spread over several ticks, so even a huge
zone doesn't stall the game.

**Population limits.** Every zone has a maximum for each species, based on its
biome and size. A plains zone supports lots of cows. A desert zone supports
camels and rabbits but no cows.

**Herds.** New zones are seeded with herds rather than scattered animals. Herds
spawn as their chunks load, stay away from the zone's edge, and never appear
right next to a player. Livestock (cows, sheep, pigs, chickens) spawn before
everything else.

**Hunting and recovery.** Every wild animal you kill counts against its zone.
The more you take, the slower and less likely recovery becomes. Left alone,
that pressure fades over time and new young appear near existing herds. If a
species is wiped out of a zone, a small founding herd eventually returns.

**Predators are not hunters.** Animals killed by wolves, foxes and other mobs
don't count as over-hunting. Sheep flee from wolves, chickens from foxes and
ocelots, and rabbits from foxes.

**Farm animals are separate.** Feeding an animal its breeding item makes it
domestic. Domestic animals leave the wild population and never count against
the zone. Breed as many as you like.

**Vanilla spawning is replaced.** Natural and world-generation spawns of the
managed species are blocked, so the zone limits actually mean something.
Animals in villages are left alone and simply join the local wild population.

### Managed species

Cow, sheep, pig, chicken, rabbit, horse, donkey, llama, panda, goat, camel,
armadillo and mooshroom.

## Configuration

Both files are created in your `config` folder the first time you launch the
game. Changes apply the next time the game starts. When a change affects
population limits, existing zones are updated automatically on world load.
Animals already in the world are never removed. Only herds still waiting to
spawn are trimmed.

### `config/herdstead.json`

| Setting | Default | What it does |
|---|---|---|
| `chunksPerBasePopulation` | 256 | Zone size the base populations are written for. Lower it for more animals per area, raise it for fewer. |
| `maxRecruitRolls` | 40 | Cap on how many recovery attempts a zone gets per interval. |
| `recruitmentIntervalTicks` | 24000 | How often a depleted zone tries to recover (24000 ticks is one Minecraft day). |
| `herdSizeRatio` | 0.15 | Typical herd size as a fraction of a zone's maximum. |
| `debugLogging` | false | Prints a status line for every zone and warns about slow ticks. |
| `species` | all true | Turn individual species on or off. |

Setting a species to `false` stops it spawning in new and existing zones.
Turning it back on later brings it back if that species is completely gone.

### `config/herdstead-species.json`

Per-species settings: `herdMin` and `herdMax` (herd size), `tier` (spawn order,
0 first), `ground` (blocks the species may spawn on), and `biomes` (the
population each biome supports). Biomes can be written as `plains` or
`minecraft:plains`.

If something in the file is wrong, such as an unknown block name, HerdStead
logs a warning naming the species and the bad value, then uses the built-in
setting for that entry. If the file can't be read at all, built-in settings are
used for that run and the file is left untouched so you can fix it.

## Reading the debug log

With `debugLogging` on, each zone prints a line like this:

```
Zone (119,260): pig=14+7/35(d1) cow=19+0/30
```

That reads as `species=alive+waiting/maximum`. Here 14 pigs are alive, 7 more
are planned herds that haven't spawned yet, and the zone's maximum is 35.
`(d1)` means one point of hunting pressure.

## Compatibility with other mods

HerdStead has not been tested with other mods, so treat this section as what
to expect rather than a guarantee.

**Biome and terrain mods.** Populations are set per biome. Mods that reshape
the land but keep the vanilla biomes work as normal. Biomes added by another
mod have no population entry, so no managed animals will live there until you
add one. Open `config/herdstead-species.json`, find the species, and add the
biome to its `biomes` list using its full name, for example
`"terralith:alpine_highlands": 20`. The number is the population that biome
supports.

**Animal mods.** Only the 13 species listed above are managed. Animals added
by other mods are left completely alone. Because HerdStead replaces natural
spawning of the managed species, another mod's natural spawns of those same
animals will probably be blocked.

If you find a conflict, please report it along with the other mod's name and
version.

## Reporting bugs

Bugs, conflicts and suggestions go on the
[issue tracker](https://github.com/mavethedave09-sudo/herdstead/issues). It
helps a lot if you include:

- the HerdStead, Minecraft and Fabric Loader versions
- the other mods you have installed
- what you expected to happen and what happened instead
- your `logs/latest.log` (and `config/herdstead-species.json` if it is about
  species or biome settings)

If something seems slow, set `debugLogging` to `true` in
`config/herdstead.json` and include the log from that run.

## License

MIT. See the `LICENSE` file.
