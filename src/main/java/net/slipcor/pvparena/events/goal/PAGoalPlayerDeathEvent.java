package net.slipcor.pvparena.events.goal;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PADeathInfo;
import net.slipcor.pvparena.loadables.ArenaGoal;

import static java.util.Optional.ofNullable;


public class PAGoalPlayerDeathEvent extends PAGoalTriggerEvent {

    private final ArenaPlayer arenaPlayer;
    private final ArenaPlayer killer;
    private final boolean respawning;

    /**
     * create an arena goal event
     *
     * @param arena  the arena where the event is happening in
     * @param arenaPlayer arenaPlayer
     * @param deathInfo information about death to find killer
     * @param respawning does player respawn
     * @param goal the goal triggering the event
     */
    public PAGoalPlayerDeathEvent(final Arena arena, final ArenaGoal goal, final ArenaPlayer arenaPlayer, final PADeathInfo deathInfo, final boolean respawning) {
        super(arena, goal);
        this.arenaPlayer = arenaPlayer;
        this.respawning = respawning;
        this.killer = ofNullable(deathInfo.getKiller())
                .filter(killer -> !killer.getName().equals(arenaPlayer.getName()))
                .map(ArenaPlayer::fromPlayer)
                .orElse(null);
    }

    @Override
    public boolean refreshScoreboard() {
        return true;
    }

    public ArenaPlayer getArenaPlayer() {
        return this.arenaPlayer;
    }

    public ArenaPlayer getKiller() {
        return this.killer;
    }

    public boolean isRespawning() {
        return this.respawning;
    }

    @Override
    public ArenaPlayer getTriggerPlayer() {
        return this.killer;
    }
}
