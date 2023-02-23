# BattlefieldJoin

## About

This mod directly teleports joining players to the battlefield. Match starts when required minimum of players 
(`ready.minPlayers` setting) is reached.
After game startup, external players still have time to join fight.

> ℹ This mod is not compatible with `uses.evenTeams` setting. It must be set to `false`.

## Installation

This module is bundled with PVPArena and doesn't need to be installed.

## Config settings

- joinDuration \- duration (in seconds) to join the arena after game startup. Set to 0 to remove limit.

## Setup

Configure [autoclass](../faq.md#is-it-possible-to-automatically-affect-a-class-to-all-players-or-to-a-specific-team) 
for your arena.  
Then, just enable `join.allowDuringMatch` setting with [/pa set](../commands/set.md) command or in your arena config file.

> **🚩 Tips:**  
> To really enjoy this mod, you can start the game immediately. To do that :
> - Set `ready.minPlayers` setting to 2
> - Set `time.startCountDown` to 0
> 
> With this configuration, the two first players will auto-start the game and the other will be able to join it during
> `joinDuration`.




