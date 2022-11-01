package net.slipcor.pvparena.managers;

import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.classes.PABlockLocation;
import net.slipcor.pvparena.commands.PAG_Join;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

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

    public void checkPlayerLocation(Player player, PABlockLocation pLoc) {
        ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

        if(arenaPlayer.getArena() == null) {
            this.joinRegionsCache.stream()
                    .filter(rg -> rg.getShape().contains(pLoc))
                    .findFirst()
                    .filter(rg -> !rg.getArena().isLocked() && rg.getArena().getConfig().getBoolean(Config.CFG.JOIN_FORCE))
                    .filter(rg -> !rg.getArena().isFightInProgress() || (rg.getArena().isFightInProgress() && rg.getArena().getGoal().allowsJoinInBattle()))
                    .ifPresent(joinRegion -> {
                        final PAG_Join cmd = new PAG_Join();
                        cmd.commit(joinRegion.getArena(), player, new String[]{joinRegion.getRegionName().replace("-join", "")});
                    });

        } else if(!arenaPlayer.isTeleporting()) {

            if (arenaPlayer.getStatus() == FIGHT) {
                this.handleFightingPlayerMove(arenaPlayer, pLoc);

            } else if (arenaPlayer.getStatus() == READY || arenaPlayer.getStatus() == LOUNGE) {
                this.handleEscapeLoungeRegions(arenaPlayer, pLoc);

            } else if (arenaPlayer.isSpectating()) {
                this.handleEscapeWatchRegions(arenaPlayer, pLoc);
            }
        }
    }

    public void handleFightingPlayerMove(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        Arena arena = arenaPlayer.getArena();

        if (arena.isFightInProgress()) {
            boolean escaping = this.isEscapingBattleRegions(arenaPlayer, pLoc);

            if (!escaping) {
                arena.getRegions().stream()
                        .filter(rg -> rg.getType() == RegionType.BATTLE || rg.getType() == RegionType.CUSTOM)
                        .forEach(rg -> rg.handleRegionFlags(arenaPlayer, pLoc));
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

    private boolean isEscapingBattleRegions(ArenaPlayer arenaPlayer, PABlockLocation pLoc) {
        Arena arena = arenaPlayer.getArena();
        Set<ArenaRegion> regions = arena.getRegionsByType(RegionType.BATTLE);
        boolean isInRegion = regions.isEmpty() || regions.stream().anyMatch(rg -> rg.getShape().contains(pLoc));

        if (!isInRegion) {

            Player player = arenaPlayer.getPlayer();
            debug(player, "escaping BATTLE, loc : {}", pLoc);
            Arena.pmsg(player, Language.MSG.NOTICE_YOU_ESCAPED);

            if (arena.getConfig().getBoolean(Config.CFG.GENERAL_LEAVEDEATH)) {
                player.setLastDamageCause(new EntityDamageEvent(player, EntityDamageEvent.DamageCause.CUSTOM, 1004.0));
                player.damage(1000);
            } else {
                arena.playerLeave(player, Config.CFG.TP_EXIT, false, false, false);
            }

            return true;
        }
        return false;
    }
}
