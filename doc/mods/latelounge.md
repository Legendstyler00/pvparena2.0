# LateLounge

## Description

Let people announce a join to an arena, yet keep playing until the minimum player amount is reached.

## Installation

Installation of this module can be done in a normal way. You'll find installation process in [modules page](../modules.md#installing-modules) of the doc.


## How to use it?

This module has no particular command. After it being enabled, players are automatically enqueued with LateLounge when
they join the arena using join commands. Once a player is in the joining queue, they can't join another arena.

Run `/pa leave` to leave the joining queue.

The minimum number of players can be changed in [configuration](../configuration.md) at path `ready.minPlayers`.
