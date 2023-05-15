package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Utils;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import java.util.Set;
import java.util.stream.Collectors;

import static net.slipcor.pvparena.arena.PlayerStatus.*;
import static net.slipcor.pvparena.config.Debugger.debug;
import static net.slipcor.pvparena.config.Debugger.trace;

/**
 * Class to manage player movements inside regions
 */
public final class RegionManager {
    private static RegionManager instance;
    private Set<ArenaRegion> joinRegionsCache;

    private RegionManager() {
        this.reloadCache();
    }

    public static RegionManager getInstance() {
        if(instance == null){
            synchronized (RegionManager.class) {
                if(instance == null){
                    instance = new RegionManager();
                }
            }
        }
        return instance;
    }

    public void reloadCache() {
        this.joinRegionsCache = ArenaManager.getArenas().stream()
                .flatMap(arena -> arena.getRegions().stream().filter(rg -> rg.getType() == RegionType.JOIN))
                .collect(Collectors.toSet());
    }

    public void checkPlayerLocation(Player player, PABlockLocation locTo, PlayerMoveEvent event) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if(arenaPlayer.getArena() == null) {
            this.joinRegionsCache.stream()
                    .filter(rg -> rg.getShape().contains(locTo))
                    .findFirst()
                    .filter(rg -> !rg.getArena().isLocked() && rg.getArena().getConfig().getBoolean(Config.CFG.JOIN_FORCE))
                    .filter(rg -> !rg.getArena().isFightInProgress() || (rg.getArena().isFightInProgress() && rg.getArena().getGoal().allowsJoinInBattle()))
                    .ifPresent(joinRegion -> {
                        final PAG_Join cmd = new PAG_Join();
                        cmd.commit(joinRegion.getArena(), player, new String[]{joinRegion.getRegionName().replace("-join", "")});
                    });

        } else if(!arenaPlayer.isTeleporting()) {

            if (arenaPlayer.getStatus() == FIGHT) {
                this.handleFightingPlayerMove(arenaPlayer, locTo, event);

            } else if (arenaPlayer.getStatus() == READY || arenaPlayer.getStatus() == LOUNGE) {
                this.handleEscapeLoungeRegions(arenaPlayer, locTo);

            } else if (arenaPlayer.isSpectating()) {
                this.handleEscapeWatchRegions(arenaPlayer, locTo);
            }
        }
    }

    public void handleFightingPlayerMove(ArenaPlayer arenaPlayer, PABlockLocation locTo, PlayerMoveEvent event) {
        Arena arena = arenaPlayer.getArena();

        if (arena.isFightInProgress()) {
            boolean escaping = this.isEscapingBattleRegions(arenaPlayer, locTo);

            if (escaping) {
                Player player = arenaPlayer.getPlayer();
                debug(player, "escaping BATTLE, loc : {}", locTo);

                boolean hasBeenRollback = this.tryRollbackPosition(event);
                if(!hasBeenRollback) {
                    Arena.pmsg(player, Language.MSG.NOTICE_YOU_ESCAPED);
                    if (arena.getConfig().getBoolean(Config.CFG.GENERAL_LEAVEDEATH)) {
                        player.setLastDamageCause(new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, 1004.0));
                        player.damage(1000);
                    } else {
                        arena.playerLeave(player, Config.CFG.TP_EXIT, false, false, false);
                    }
                }
            } else {
                arena.getRegions().stream()
                        .filter(rg -> rg.getType() == RegionType.BATTLE || rg.getType() == RegionType.CUSTOM)
                        .forEach(rg -> rg.handleRegionFlags(arenaPlayer, locTo));
            }
        }
    }

    private void handleEscapeLoungeRegions(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        trace("LOUNGE region move check");
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.LOUNGE);
        boolean isInRegion = regions.isEmpty() || regions.stream().anyMatch(rg -> rg.getShape().contains(pLoc));

        if (!isInRegion) {
            debug(arenaPlayer, "escaping LOUNGE, loc : {}", pLoc);
            Arena.pmsg(arenaPlayer.getPlayer(), Language.MSG.NOTICE_YOU_ESCAPED);
            arena.playerLeave(arenaPlayer.getPlayer(), Config.CFG.TP_EXIT, false, false, false);
        }
    }

    private void handleEscapeWatchRegions(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        trace("WATCH region move check");
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.WATCH);
        boolean isInRegion = regions.isEmpty() || regions.stream().anyMatch(rg -> rg.getShape().contains(pLoc));

        if (!isInRegion) {
            debug(arenaPlayer, "escaping WATCH, loc : {}", pLoc);
            Arena.pmsg(arenaPlayer.getPlayer(), Language.MSG.NOTICE_YOU_ESCAPED);
            arena.playerLeave(arenaPlayer.getPlayer(), Config.CFG.TP_EXIT, false, false, false);
        }
    }

    private boolean isEscapingBattleRegions(ArenaPlayer arenaPlayer, PABlockLocation locTo) {
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);
        return !regions.isEmpty() && regions.stream().noneMatch(rg -> rg.getShape().contains(locTo));
    }

    private boolean tryRollbackPosition(final PlayerMoveEvent event) {
        if(event instanceof PlayerTeleportEvent || event.getFrom().getBlock().isLiquid()) {
            trace(event.getPlayer(), "escaping - cancel movement");
            event.setCancelled(true);
            return true;
        }

        // Checking if player can be rollback on the floor (solid block under previous position)
        final Location locFrom = event.getFrom();
        for(int i = 1; i <= 10; i++) {
            Location maybeFloor = locFrom.clone().subtract(new Vector(0, i, 0));
            if(maybeFloor.getBlock().getType().isSolid()) {
                Location rollbackLoc = Utils.getCenteredLocation(locFrom);
                rollbackLoc.setPitch(locFrom.getPitch());
                rollbackLoc.setYaw(locFrom.getYaw());
                event.getPlayer().teleport(rollbackLoc);
                trace(event.getPlayer(), "escaping - rollback to location : {}", rollbackLoc);
                Arena.pmsg(event.getPlayer(), Language.MSG.NOTICE_ARENA_BOUNDS);
                return true;
            }
        }

        return false;
    }
}
