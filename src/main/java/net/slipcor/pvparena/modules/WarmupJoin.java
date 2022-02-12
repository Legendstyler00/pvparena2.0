package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.managers.ArenaManager;
import net.slipcor.pvparena.managers.PermissionManager;
import net.slipcor.pvparena.runnables.ArenaWarmupRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>Arena Module class "WarmupJoin"</pre>
 * <p/>
 * Enables a warmup countdown before joining the arena
 *
 * @author slipcor
 */

public class WarmupJoin extends ArenaModule {

    private static final int PRIORITY = 3;

    private Set<ArenaPlayer> playerSet;

    private boolean announced = false;

    public WarmupJoin() {
        super("WarmupJoin");
    }

    public static boolean didNotAnnounceYet(Arena arena) {
        for (ArenaModule mod : arena.getMods()) {
            if (mod instanceof WarmupJoin) {
                return !((WarmupJoin) mod).announced;
            }
        }
        return true;
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
    public boolean handleJoin(Player player) throws GameplayException {
        if (this.arena.isLocked() && !PermissionManager.hasAdminPerm(player) && !PermissionManager.hasBuilderPerm(player, this.arena)) {
            throw new GameplayException(Language.parse(MSG.ERROR_DISABLED));
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        if (this.getPlayerSet().contains(aPlayer)) {
            return false;
        }

        if (aPlayer.getArena() != null) {
            debug(aPlayer.getArena(), player, this.getName());
            throw new GameplayException(Language.parse(
                    MSG.ERROR_ARENA_ALREADY_PART_OF, ArenaManager.getIndirectArenaName(aPlayer.getArena())));
        }
        this.getPlayerSet().add(aPlayer);

        return true;
    }

    @Override
    public void commitJoin(final Player player, final ArenaTeam team) {
        new ArenaWarmupRunnable(this.arena, ArenaPlayer.fromPlayer(player), team.getName(), false, this.arena.getConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
        this.announced = true;
    }

    @Override
    public void commitSpectate(final Player player) {
        new ArenaWarmupRunnable(this.arena, ArenaPlayer.fromPlayer(player), null, true, this.arena.getConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
    }

    @Override
    public void displayInfo(final CommandSender sender) {
        sender.sendMessage("seconds: " +
                this.arena.getConfig().getInt(CFG.TIME_WARMUPCOUNTDOWN));
    }

    private Set<ArenaPlayer> getPlayerSet() {
        if (this.playerSet == null) {
            this.playerSet = new HashSet<>();
        }
        return this.playerSet;
    }

    @Override
    public void reset(final boolean force) {
        this.getPlayerSet().clear();
        this.announced = false;
    }

    @Override
    public void parsePlayerLeave(final Player player, final ArenaTeam team) {
        this.getPlayerSet().remove(ArenaPlayer.fromPlayer(player));
    }
}
