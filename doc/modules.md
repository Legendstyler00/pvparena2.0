# PVP Arena Modules

## About

PvPArena modules are ways to enhance your arenas. They could modify a lot of things like configuration, fights, classes 
or spectating...

To install arena mods, please check [documentation below](#installing-modules). The `/pa modules` command allows for 
managing your arena mods (download, install, remove, etc), please check [documentation](commands/modules.md) to get 
more information.

## List of PVP Arena Mods

Hook into many different aspects of the game!

| Mod                                                | Description                                                                                             | Type     | Status |
|----------------------------------------------------|---------------------------------------------------------------------------------------------------------|----------|--------|
| [AfterMatch](mods/aftermatch.md)                   | could also be called "Sudden Death"                                                                     |          | ✔      |
| [Announcements](mods/announcements.md)             | announce events happening                                                                               |          | ✔      |
| [ArenaMaps](mods/arenamaps.md)                     | never lose yourself ever again!                                                                         |          | ✔      |
| [BanKick](mods/bankick.md)                         | secure your arenas!                                                                                     |          | ✔      |
| [BattlefieldJoin](mods/battlefieldjoin.md)         | join directly your battlefield without using the lounge                                                 | JOIN     | ✔      |
| [BattlefieldManager](mods/battlefieldmanager.md)   | manage your battlefield                                                                                 |          | ⚠      |
| [BetterClasses](mods/betterclasses.md)             | add potion effects and more to specific classes                                                         |          | ☑      |
| [BetterGears](mods/bettergears.md)                 | give team colored leather                                                                               |          | ☑      |
| [BetterFight](mods/betterfight.md)                 | kill streaks and one-hit-kill items!                                                                    |          | ⚠      |
| [BetterKillstreaks](mods/betterkillstreaks.md)     | even more detailed kill streaks!                                                                        |          | ⚠      |
| [BlockDissolve](mods/blockdissolve.md)             | dissolve blocks under fighting players                                                                  |          | ☑      |
| [BlockRestore](mods/blockrestore.md)               | restore the battlefield                                                                                 |          | ✔      |
| [ChestFiller](mods/chestfiller.md)                 | fill battlefield chests with customizable content!                                                      |          | ☑      |
| [Duel](mods/duel.md)                               | duel someone!                                                                                           |          | ⚠      |
| [EventActions](mods/eventactions.md)               | do stuff when stuff happens                                                                             |          | ⚠      |
| [Factions](mods/factions.md)                       | fix pvp not working                                                                                     |          | ⚠      |
| [FixInventoryLoss](mods/fixinventoryloss.md)       | prevent loss by gamemode / inventory check                                                              |          | ⚠      |
| [FlySpectate](mods/flyspectate.md)                 | have players spectating a fight in fly mode                                                             | SPECTATE | ✔      |
| [Items](mods/items.md)                             | spawn (random) items                                                                                    |          | ⚠      |
| [LateLounge](mods/latelounge.md)                   | keep playing until enough ppl are joining                                                               |          | ☑      |
| [MatchResultStats](mods/matchresultstats.md)       | keep stats of player games, who won, who lost?                                                          |          | ⚠      |
| [PlayerFinder](mods/playerfinder.md)               | allow players to find others with a compass                                                             |          | ☑      |
| [Points](mods/points.md)                           | allow to restrict certain classes to require players to fight for better classes                        |          | ⚠      |
| [PowerUps](mods/powerups.md)                       | spawn items giving special powers                                                                       |          | ☑      |
| [Projectiles](mods/projectiles.md)                 | add knockback to throwable items (snowballs, eggs, etc)                                                 |          | ☑      |
| [QuickLounge](mods/quicklounge.md)                 | join a lounge which starts the game automatically                                                       | JOIN     | ✔      |
| [RealSpectate](mods/realspectate.md)               | spectate the game, CounterStrike style!                                                                 |          | ✔      |
| [RedstoneTriggers](mods/redstonetriggers.md)       | add win/lose triggered by redstone                                                                      | SPECTATE | ⚠      |
| [RespawnRelay](mods/respawnrelay.md)               | add a relay for respawning players                                                                      |          | ✔      |
| [SinglePlayerSupport](mods/singleplayersupport.md) | Allow players to use an arena on their own!                                                             |          | ⚠      |
| [Skins](mods/skins.md)                             | add custom skins to teams/classes                                                                       |          | ❌      |
| [SpecialJoin](mods/specialjoin.md)                 | join via buttons, levers, etc                                                                           |          | ⚠      |
| [Spectate](mods/spectate.md)                       | use the new 1.8 SPECTATOR mode to allow flying and POV spectating                                       | SPECTATE | ✔      |
| StandardLounge                                     | Default lounge module                                                                                   | JOIN     | ✔      |
| StandardSpectate                                   | Default spectate module (survival mode)                                                                 | SPECTATE | ✔      |
| [Squads](mods/squads.md)                           | add squads to the game, basically only showing players belonging together apart from teams and classes. |          | ☑      |
| [StartFreeze](mods/startfreeze.md)                 | freeze players at start                                                                                 |          | ⚠      |
| [TeamSizeRestrict](mods/teamsizerestrict.md)       | a small mod to restrict the size of specific teams                                                      |          | ⚠      |
| [Titles](mods/titles.md)                           | send messages to players as the "title" command would do                                                |          | ☑      |
| [TempPerms](mods/tempperms.md)                     | add temporary perms                                                                                     |          | ☑      |
| [Turrets](mods/turrets.md)                         | add turrets where players fire projectiles                                                              |          | ⚠      |
| [Vault](mods/vault.md)                             | add economy                                                                                             |          | ☑      |
| [Walls](mods/walls.md)                             | define wall regions to simulate "The Walls"                                                             |          | ⚠      |
| [WarmupJoin](mods/warmupjoin.md)                   | ???                                                                                                     | JOIN     | ⚠      |
| [WorldEdit](mods/worldedit.md)                     | backup/restore regions                                                                                  |          | ☑      |
| [WorldGuard](mods/worldguard.md)                   | import region definitions from WorldGuard                                                               |          | ⚠      |

**Key :** ✔ Adapated and fully tested for 2.0 | ☑ Fully works with previous PVPArena version, but not tested with 2.0 | ⚠ Legacy modules, not tested for a while | ❌ Temporarily unavailable

> ℹ **NB:** StandardLounge, StandardSpectate, QuickLounge, BattlefieldJoin and WarmupJoin are bundled in the plugin. Consequently, they're missing from the modules pack.

### Why are there different statuses?

PVP Arena exists since 2011 and Minecraft servers evolution make modules follow-up complicated. The objective of 2.0 
version will be to make a great check-up of all of them and fix all eventual issues.

Anyway, don't hesitate to test legacy modules by yourself, a big part of them work normally or have trivial issues. Obviously
if you encounter one, you can [report it](https://github.com/Eredrim/pvparena/issues) 😉

### What happened to ArenaBoards, AutoSneak, AutoVote or BattlefieldGuard?
These modules either used discontinued features (like arena rounds) or their features have been integrated in core of
PVPArena plugin.

## Installing modules

### Download the module pack

> ℹ This has to be done only once
 
Use the [`/pa modules download`](commands/modules.md) command to download the release version of modules. If you want to
install a dev build version, download the zip archive directly on our [discord](https://discord.gg/a8NhSsXKVQ) 
and deflate it in the `/files` directory of pvparena.

After this step, if you type [`/pa modules list`](commands/modules.md), you will show the list of all installable 
modules.

### Installing a module

> ℹ This has to be done for each module you want to install

Modules aren't loaded by default, a quick installation is required. 
Type [`/pa modules install [moduleName]`](commands/modules.md) to install one of them.


### Enable a module for an arena

> ℹ This has to be done for each arena

Last step: your module is installed and you want to use it in some of your arenas. 
Type [`/pa [arena] !tm [moduleName]`](commands/togglemod.md) to enable it in your arena.