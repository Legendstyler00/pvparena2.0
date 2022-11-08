package net.slipcor.pvparena.modules;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.loadables.ModuleType;
import org.bukkit.entity.Player;

/**
 * <pre>
 * Arena Module class "QuickLounge"
 * </pre>
 * <p/>
 * Enables joining to lounge with an auto-start. Autoclass is required.
 *
 * @author Eredrim
 */
public class QuickLounge extends StandardLounge {

    private static final int PRIORITY = 1;

    public QuickLounge() {
        this.name = "QuickLounge";
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
    public ModuleType getType() {
        return ModuleType.JOIN;
    }

    @Override
    public boolean handleJoin(Player player) throws GameplayException {
        if(this.arena.getConfig().getDefinedString(CFG.READY_AUTOCLASS) == null) {
            throw new GameplayException(Language.parse(MSG.ERROR_CLASS_NEEDS_AUTOCLASS, this.name));
        }

        return super.handleJoin(player);
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
