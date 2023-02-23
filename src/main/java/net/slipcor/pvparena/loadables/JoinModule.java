package net.slipcor.pvparena.loadables;

import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.ArenaTeam;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Language;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * <pre>
 * Arena Module class
 * </pre>
 * <p/>
 * The framework for adding modules to an arena
 *
 * @author slipcor
 */

public abstract class JoinModule extends ArenaModule {
    protected JoinModule(final String name) {
        super(name);
    }

    public ModuleType getType() {
        return ModuleType.JOIN;
    }

    public boolean overridesStart() { return false; }

    public void commitStart() { throw new IllegalStateException(this.name); }

    protected void initPlayerState(ArenaPlayer arenaPlayer) {
        // Important: clear inventory before setting player state to deal with armor modifiers (like health)
        Player player = arenaPlayer.getPlayer();
        ArenaPlayer.backupAndClearInventory(this.arena, player);
        arenaPlayer.createState(player);
        arenaPlayer.dump();


        if (arenaPlayer.getArenaTeam() != null && arenaPlayer.getArenaClass() == null) {
            String autoClassCfg = this.arena.getConfig().getDefinedString(Config.CFG.READY_AUTOCLASS);
            if (autoClassCfg != null) {
                this.arena.getAutoClass(autoClassCfg, arenaPlayer.getArenaTeam()).ifPresent(autoClass ->
                        this.arena.chooseClass(player, null, autoClass)
                );
            }
        }
    }
    protected void broadcastJoinMessages(Player player, ArenaTeam arenaTeam) {
        if (this.arena.isFreeForAll()) {
            this.arena.msg(player,
                    Language.parse(this.arena, Config.CFG.MSG_YOUJOINED,
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(Config.CFG.READY_MAXPLAYERS))
                    ));
            this.arena.broadcastExcept(
                    player,
                    Language.parse(this.arena, Config.CFG.MSG_PLAYERJOINED,
                            player.getName(),
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(Config.CFG.READY_MAXPLAYERS))
                    ));
        } else {

            this.arena.msg(player,
                    Language.parse(this.arena, Config.CFG.MSG_YOUJOINEDTEAM,
                            arenaTeam.getColoredName() + ChatColor.COLOR_CHAR + 'r',
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(Config.CFG.READY_MAXPLAYERS))
                    ));

            this.arena.broadcastExcept(
                    player,
                    Language.parse(this.arena, Config.CFG.MSG_PLAYERJOINEDTEAM,
                            player.getName(),
                            arenaTeam.getColoredName() + ChatColor.COLOR_CHAR + 'r',
                            Integer.toString(arenaTeam.getTeamMembers().size()),
                            Integer.toString(this.arena.getConfig().getInt(Config.CFG.READY_MAXPLAYERS))
                    ));
        }
    }

}
