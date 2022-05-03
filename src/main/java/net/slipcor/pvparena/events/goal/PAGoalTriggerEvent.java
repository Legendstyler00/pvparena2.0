package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.loadables.ArenaGoal;


public abstract class PAGoalTriggerEvent extends PAGoalEvent {

    PAGoalTriggerEvent(Arena arena, ArenaGoal goal) {
        super(arena, goal);
    }

    public abstract ArenaPlayer getTriggerPlayer();
}
