package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.commands.PAI_Ready;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.JoinModule;
import net.slipcor.pvparena.managers.SpawnManager;
import net.slipcor.pvparena.managers.TeleportManager;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena Module class "QuickLounge"
 * </pre>
 * <p/>
 * Enables direct joining to battlefield with an auto-start. Autoclass is required. JoinInBattle is required.
 *
 * @author Eredrim
 */
public class BattlefieldJoin extends JoinModule {

    private static final int PRIORITY = 1;

    private LocalDateTime arenaStartTime;

    public BattlefieldJoin() {
        super("BattlefieldJoin");
    }

    @Override
    public String version() {
        return PVPArena.getInstance().getDescription().getVersion();
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean overridesStart() { return true; }

    @Override
    public boolean handleJoin(Player player) throws GameplayException {
        if(this.arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS) == null) {
            throw new GameplayException(Language.parse(MSG.ERROR_REQ_NEEDS_AUTOCLASS, this.name));
        }

        if(!this.arena.getGoal().allowsJoinInBattle()) {
            throw new GameplayException(Language.parse(MSG.ERROR_REQ_NEEDS_JOINDURINGMATCHGOAL, this.name));
        }

        if(this.arena.getConfig().getBoolean(CFG.USES_EVENTEAMS)) {
            throw new GameplayException(Language.parse(MSG.ERROR_REQ_INCOMPATIBLESETTING, this.name, CFG.USES_EVENTEAMS.getNode()));
        }

        return true;
    }

    @Override
    public void commitJoin(Player player, ArenaTeam arenaTeam) {
        final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);
        arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));
        arenaPlayer.setArena(this.arena);
        arenaTeam.add(arenaPlayer);
        arenaPlayer.setStatus(PlayerStatus.LOUNGE);

        TeleportManager.teleportPlayerToSpawnForJoin(this.arena, arenaPlayer, SpawnManager.selectSpawnsForPlayer(this.arena, arenaPlayer, "fight"), true);

        this.broadcastJoinMessages(player, arenaTeam);

        if (arenaPlayer.getState() == null) {
            this.initPlayerState(arenaPlayer);
        } else {
            PVPArena.getInstance().getLogger().warning("Player has a state while joining: " + arenaPlayer.getName());
        }
    }

    @Override
    public void commitJoinDuringMatch(Player player, ArenaTeam arenaTeam) {
        final int joinDuration = this.arena.getConfig().getInt(CFG.MODULES_BATTLEFIELDJOIN_JOINDURATION, 0);
        LocalDateTime now = LocalDateTime.now();
        long joinDiffSeconds = ChronoUnit.SECONDS.between(this.arenaStartTime, now);
        boolean isRejoinAllowed = this.arena.getConfig().getBoolean(CFG.JOIN_ALLOW_REJOIN) && this.arena.hasAlreadyPlayed(player.getName());

        if(joinDuration <= 0 || joinDiffSeconds <= joinDuration || isRejoinAllowed) {
            final ArenaPlayer arenaPlayer = ArenaPlayer.fromPlayer(player);

            if (arenaPlayer.getState() == null) {
                arenaPlayer.setLocation(new PALocation(arenaPlayer.getPlayer().getLocation()));
                arenaPlayer.setArena(this.arena);
                arenaTeam.add(arenaPlayer);

                this.initPlayerState(arenaPlayer);
                arenaPlayer.setStatus(PlayerStatus.LOUNGE);

                try {
                    PAI_Ready.checkReadyRequirementsDuringFight(this.arena, arenaPlayer);
                    this.broadcastJoinMessages(player, arenaTeam);
                } catch (GameplayException e) {
                    this.arena.msg(player, e.getMessage());
                    arenaPlayer.reset();
                }
            } else {
                PVPArena.getInstance().getLogger().warning("Player has a state while joining: " + arenaPlayer.getName());
            }
        } else {
            this.arena.msg(player, MSG.ERROR_FIGHT_IN_PROGRESS);
        }
    }

    @Override
    public void commitStart() {
        // Players are already on the battlefield
        debug("hooking commitStart in BattlefieldJoin");
        this.arena.getTeams().forEach(team -> {
            team.getTeamMembers().forEach(arenaPlayer -> arenaPlayer.setStatus(PlayerStatus.FIGHT));
        });

        final int joinDuration = this.arena.getConfig().getInt(CFG.MODULES_BATTLEFIELDJOIN_JOINDURATION, 0);
        this.arenaStartTime = LocalDateTime.now();
        if(joinDuration > 0) {
            this.arena.broadcast(Language.parse(MSG.MODULE_BATTLEFIELDJOIN_REMAININGTIME, joinDuration));
        }
    }

    @Override
    public void parseJoin(final Player player, final ArenaTeam team) {
        // Auto starting countdown when first player joins
        if(this.arena.getFighters().size() >= this.arena.getConfig().getInt(CFG.READY_MINPLAYERS)) {
            if (this.arena.startRunner == null) {
                this.arena.countDown();
            }
        }
    }
}
