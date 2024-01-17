package net.slipcor.pvparena.commands;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.arena.ArenaPlayer;
import net.slipcor.pvparena.arena.PlayerStatus;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Help.HELP;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringParser;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.exceptions.GameplayExceptionNotice;
import net.slipcor.pvparena.managers.TeamManager;
import net.slipcor.pvparena.regions.ArenaRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * <pre>PVP Arena READY Command class</pre>
 * <p/>
 * A command to ready up inside the arena
 *
 * @author slipcor
 * @version v0.10.0
 */

public class PAI_Ready extends AbstractArenaCommand {

    public PAI_Ready() {
        super(new String[]{"pvparena.cmds.ready"});
    }

    @Override
    public void commit(final Arena arena, final CommandSender sender, final String[] args) {
        if (!this.hasPerms(sender, arena)) {
            return;
        }

        if (!argCountValid(sender, arena, args, new Integer[]{0, 1})) {
            return;
        }

        if (!(sender instanceof Player)) {
            Arena.pmsg(sender, MSG.ERROR_ONLY_PLAYERS);
            return;
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer((Player) sender);

        if (!arena.hasPlayer(aPlayer.getPlayer())) {

            arena.msg(sender, MSG.ERROR_NOT_IN_ARENA);
            return;
        }

        if (args.length < 1) {
            try {
                if(arena.isFightInProgress()) {
                    checkReadyRequirementsDuringFight(arena, aPlayer);
                } else {
                    checkReadyRequirementsBeforeFight(arena, aPlayer);
                }
            } catch (GameplayException e) {
                arena.msg(sender, e.getMessage());
            }
        } else {
            // /pa ready list
            final Set<String> names = new HashSet<>();

            for (ArenaPlayer player : arena.getEveryone()) {
                if (player.getStatus() == PlayerStatus.LOUNGE) {
                    names.add("&7" + player.getName() + "&r");
                } else if (player.getStatus() == PlayerStatus.READY) {
                    names.add("&a" + player.getName() + "&r");
                }
            }
            arena.msg(sender, MSG.READY_LIST, StringParser.joinSet(names, ", "));
        }
    }

    private static void checkReadyRequirementsBeforeFight(Arena arena, ArenaPlayer aPlayer) throws GameplayException {
        if (aPlayer.getStatus() != PlayerStatus.LOUNGE) {
            return;
        }

        if (aPlayer.getArenaClass() == null) {
            throw new GameplayException(MSG.ERROR_READY_NOCLASS);
        }

        arena.msg(aPlayer.getPlayer(), MSG.READY_DONE);

        // Replace placeholders except for the player name
        String readyMessage = Language.parseWithPlaceholder(aPlayer.getPlayer(), MSG.PLAYER_READY);

        // Now replace the player name placeholder with the colored name
        String teamColor = aPlayer.getArenaTeam() != null ? aPlayer.getArenaTeam().getColor().toString() : "";
        String coloredPlayerName = teamColor + PlaceholderAPI.setPlaceholders(aPlayer.getPlayer(), "%haonick_name%") + ChatColor.RESET;
        readyMessage = readyMessage.replace("%haonick_name%", coloredPlayerName);

        String finalReadyMessage = readyMessage;
        arena.getEveryone().stream()
                .map(ArenaPlayer::getPlayer)
                .forEach(p -> arena.msg(p, finalReadyMessage));

        // Set player status to READY
        aPlayer.setStatus(PlayerStatus.READY);


        if (arena.getConfig().getBoolean(CFG.USES_EVENTEAMS) && !TeamManager.checkEven(arena)) {
            // even teams desired, not done => announce
            throw new GameplayExceptionNotice(MSG.NOTICE_WAITING_EQUAL);
        }

        if (!ArenaRegion.checkRegions(arena)) {
            throw new GameplayExceptionNotice(MSG.NOTICE_WAITING_FOR_ARENA);
        }

        final String error = arena.ready();

        if (error == null) {
            arena.start();
        } else if (error.isEmpty()) {
            arena.countDown();
        } else {
            throw new GameplayException(error);
        }
    }



    public static void checkReadyRequirementsDuringFight(Arena arena, ArenaPlayer aPlayer) throws GameplayException {
        if (aPlayer.getStatus() != PlayerStatus.LOUNGE) {
            return;
        }

        if (aPlayer.getArenaClass() == null) {
            throw new GameplayException(MSG.ERROR_READY_NOCLASS);
        }

        if (arena.getConfig().getBoolean(CFG.USES_EVENTEAMS) && !TeamManager.checkEven(arena)) {
            // even teams desired, not done => announce
            throw new GameplayExceptionNotice(MSG.NOTICE_WAITING_EQUAL);
        }

        arena.msg(aPlayer.getPlayer(), MSG.READY_DONE);
        aPlayer.setStatus(PlayerStatus.READY);

        final String error = arena.ready();

        if (error == null) {
            arena.addPlayerDuringMatch(aPlayer);
        } else {
            throw new GameplayException(error);
        }
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public void displayHelp(final CommandSender sender) {
        Arena.pmsg(sender, HELP.READY);
    }

    @Override
    public List<String> getMain() {
        return Collections.singletonList("ready");
    }

    @Override
    public List<String> getShort() {
        return Collections.singletonList("-r");
    }

    @Override
    public CommandTree<String> getSubs(final Arena arena) {
        return new CommandTree<>(null);
    }
}
