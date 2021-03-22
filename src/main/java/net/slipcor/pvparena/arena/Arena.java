package net.slipcor.pvparena.arena;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.classes.PABlock;
import net.slipcor.pvparena.classes.PAClassSign;
import net.slipcor.pvparena.classes.PALocation;
import net.slipcor.pvparena.classes.PASpawn;
import net.slipcor.pvparena.core.ArrowHack;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.core.Config.CFG;
import net.slipcor.pvparena.core.Language;
import net.slipcor.pvparena.core.Language.MSG;
import net.slipcor.pvparena.core.StringUtils;
import net.slipcor.pvparena.events.*;
import net.slipcor.pvparena.loadables.ArenaGoal;
import net.slipcor.pvparena.loadables.ArenaModule;
import net.slipcor.pvparena.loadables.ArenaModuleManager;
import net.slipcor.pvparena.managers.*;
import net.slipcor.pvparena.regions.ArenaRegion;
import net.slipcor.pvparena.regions.RegionType;
import net.slipcor.pvparena.runnables.StartRunnable;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static net.slipcor.pvparena.config.Debugger.debug;

/**
 * <pre>
 * Arena class
 * </pre>
 * <p/>
 * contains >general< arena methods and variables
 *
 * @author slipcor
 * @version v0.10.2
 */

public class Arena {

    public static final String OLD_TP = "old";
    private final Set<ArenaClass> classes = new HashSet<>();
    private final Set<ArenaModule> mods = new HashSet<>();
    private final Set<ArenaRegion> regions = new HashSet<>();
    private final Set<PAClassSign> signs = new HashSet<>();
    private final Set<ArenaTeam> teams = new HashSet<>();
    private final Set<String> playedPlayers = new HashSet<>();

    private final Set<PABlock> blocks = new HashSet<>();
    private final Set<PASpawn> spawns = new HashSet<>();

    private final Map<Player, UUID> entities = new HashMap<>();

    private final String name;
    private String prefix = "PVP Arena";
    private String owner = "%server%";

    // arena status
    private boolean fightInProgress;
    private boolean locked;
    private boolean free;
    private final boolean valid;
    private int startCount;

    private ArenaGoal goal;

    // Runnable IDs
    public BukkitRunnable endRunner;
    public BukkitRunnable pvpRunner;
    public BukkitRunnable realEndRunner;
    public BukkitRunnable startRunner;

    private boolean gaveRewards;

    private final Config cfg;
    private YamlConfiguration language = new YamlConfiguration();
    private long startTime;
    private ArenaScoreboard scoreboard = null;

    private ArenaTimer timer;

    public Arena(final String name) {
        this.name = name;

        debug(this, "loading Arena " + name);
        File file = new File(String.format("%s/arenas/%s.yml", PVPArena.getInstance().getDataFolder().getPath(), name));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        this.cfg = new Config(file);
        this.valid = ConfigurationManager.configParse(this, this.cfg);
        if (this.valid) {
            StatisticsManager.loadStatistics(this);
            SpawnManager.loadSpawns(this, this.cfg);

            final String langName = this.cfg.getDefinedString(CFG.GENERAL_LANG);
            if (langName == null) {
                return;
            }

            final File langFile = new File(PVPArena.getInstance().getDataFolder(), langName);
            this.language = new YamlConfiguration();
            try {
                this.language.load(langFile);
            } catch (final InvalidConfigurationException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Config getConfig() {
        return this.cfg;
    }

    public Set<PABlock> getBlocks() {
        return this.blocks;
    }

    public ArenaClass getClass(final String className) {
        return this.classes.stream()
                .filter(ac -> ac.getName().equalsIgnoreCase(className))
                .findAny()
                .orElse(null);
    }

    public Set<ArenaClass> getClasses() {
        return this.classes;
    }

    public Player getEntityOwner(final Entity entity) {
        return this.entities.entrySet().stream()
                .filter(entry -> entry.getValue().equals(entity.getUniqueId()))
                .findAny()
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * hand over everyone being part of the arena
     */
    public Set<ArenaPlayer> getEveryone() {
        return ArenaPlayer.getAllArenaPlayers().stream()
                .filter(ap -> this.equals(ap.getArena()))
                .collect(Collectors.toSet());
    }

    public boolean isFightInProgress() {
        return this.fightInProgress;
    }

    public Set<ArenaPlayer> getFighters() {
        return this.teams.stream().flatMap(team -> team.getTeamMembers().stream()).collect(Collectors.toSet());
    }

    public boolean isFreeForAll() {
        return this.free;
    }

    public void setFree(final boolean isFree) {
        this.free = isFree;
        if (this.free && this.cfg.getUnsafe("teams.free") == null) {
            this.teams.clear();
            this.teams.add(new ArenaTeam("free", "WHITE"));
        } else if (this.free) {
            this.teams.clear();
            this.teams.add(new ArenaTeam("free", (String) this.cfg
                    .getUnsafe("teams.free")));
        }
        this.cfg.set(CFG.GENERAL_TYPE, isFree ? "free" : "none");
        this.cfg.save();
    }

    public ArenaScoreboard getScoreboard() {
        if(this.scoreboard == null) {
            this.scoreboard = new ArenaScoreboard(this);
        }
        return this.scoreboard;
    }

    public ArenaGoal getGoal() {
        return this.goal;
    }

    public void setGoal(ArenaGoal goal, boolean updateConfig) {
        goal.setArena(this);
        this.goal = goal;
        if (updateConfig) {
            this.cfg.set(CFG.GENERAL_GOAL, this.goal.getName());
            this.cfg.save();
        }
    }

    public Set<ArenaModule> getMods() {
        return this.mods;
    }

    public boolean hasMod(String modName) {
        return this.mods.stream().anyMatch(m -> m.getName().equalsIgnoreCase(modName));
    }

    public void addModule(ArenaModule module, boolean updateConfig) {
        module.setArena(this);
        this.mods.add(module);

        if (updateConfig) {
            this.updateModsInCfg();
        }
    }

    public void removeModule(String moduleName) {
        this.mods.removeIf(mod -> mod.getName().equalsIgnoreCase(moduleName));
        this.updateModsInCfg();
    }

    private void updateModsInCfg() {
        final List<String> list = this.mods.stream().map(ArenaModule::getName).collect(Collectors.toList());
        this.cfg.set(CFG.LISTS_MODS, list);
        this.cfg.save();
    }

    public YamlConfiguration getLanguage() {
        return this.language;
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(final boolean locked) {
        this.locked = locked;
    }

    public String getName() {
        return this.name;
    }

    public String getOwner() {
        return this.owner;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public Set<String> getPlayedPlayers() {
        return this.playedPlayers;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix;
    }

    public Material getReadyBlock() {
        try {
            return this.cfg.getMaterial(CFG.READY_BLOCK, Material.STICK);
        } catch (final Exception e) {
            Language.logWarn(MSG.ERROR_MAT_NOT_FOUND, "ready block");
        }
        return Material.IRON_BLOCK;
    }

    public ArenaRegion getRegion(final String name) {
        return this.regions.stream()
                .filter(region -> region.getRegionName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    public Set<ArenaRegion> getRegions() {
        return this.regions;
    }

    public Set<ArenaRegion> getRegionsByType(final RegionType regionType) {
        return this.regions.stream()
                .filter(rg -> rg.getType() == regionType)
                .collect(Collectors.toSet());
    }

    public Set<PAClassSign> getSigns() {
        return this.signs;
    }

    public Set<PASpawn> getSpawns() {
        return this.spawns;
    }

    public ArenaTeam getTeam(final String name) {
        return this.teams.stream()
                .filter(team -> team.getName().equalsIgnoreCase(name))
                .findAny()
                .orElse(null);
    }

    public Set<ArenaTeam> getTeams() {
        return this.teams;
    }

    public Set<ArenaTeam> getNotEmptyTeams() {
        return this.teams.stream()
                .filter(ArenaTeam::isNotEmpty)
                .collect(Collectors.toSet());
    }

    public Set<String> getTeamNames() {
        return this.teams.stream().map(ArenaTeam::getName).collect(Collectors.toSet());
    }

    public Set<String> getTeamNamesColored() {
        return this.teams.stream().map(ArenaTeam::getColoredName).collect(Collectors.toSet());
    }

    public int getPlayedSeconds() {
        final int seconds = (int) (System.currentTimeMillis() - this.startTime);
        return seconds / 1000;
    }

    public void setStartingTime() {
        this.startTime = System.currentTimeMillis();
    }

    public boolean isValid() {
        return this.valid;
    }

    public World getWorld() {
        return this.getRegionsByType(RegionType.BATTLE).stream()
                .findAny()
                .map(rg -> Bukkit.getWorld(rg.getWorldName()))
                .orElse(this.spawns.stream()
                        .filter(sp -> sp.getName().contains("spawn"))
                        .findAny()
                        .map(sp -> sp.getLocation().toLocation().getWorld())
                        .orElse(Bukkit.getWorlds().get(0))
                );
    }

    public void addClass(String className, ItemStack[] items, ItemStack offHand, ItemStack[] armors) {
        if (this.getClass(className) != null) {
            this.removeClass(className);
        }

        this.classes.add(new ArenaClass(className, items, offHand, armors));
    }

    public void addEntity(final Player player, final Entity entity) {
        this.entities.put(player, entity.getUniqueId());
    }

    public void addRegion(final ArenaRegion region) {
        this.regions.add(region);
        debug(this, "adding region: " + region.getRegionName());
    }

    public void broadcast(final String msg) {
        debug(this, "@all: " + msg);
        final Set<ArenaPlayer> players = this.getEveryone();
        for (final ArenaPlayer arenaPlayer : players) {
            if (arenaPlayer.getArena() == null || !arenaPlayer.getArena().equals(this)) {
                continue;
            }
            this.msg(arenaPlayer.getPlayer(), msg);
        }
    }

    /**
     * send a message to every player, prefix player name and ChatColor
     *
     * @param msg    the message to send
     * @param color  the color to use
     * @param player the player to prefix
     */
    public void broadcastColored(final String msg, final ChatColor color, final Player player) {
        final String sColor = this.cfg.getBoolean(CFG.CHAT_COLORNICK) ? color.toString() : "";
        synchronized (this) {
            this.broadcast(sColor + player.getName() + ChatColor.WHITE + ": " + msg.replace("&", "%%&%%"));
        }
    }

    /**
     * send a message to every player except the given one
     *
     * @param sender the player to exclude
     * @param msg    the message to send
     */
    public void broadcastExcept(final CommandSender sender, final String msg) {
        debug(this, sender, "@all/" + sender.getName() + ": " + msg);
        final Set<ArenaPlayer> players = this.getEveryone();
        for (final ArenaPlayer arenaPlayer : players) {
            if (this.equals(arenaPlayer.getArena()) && !arenaPlayer.getName().equals(sender.getName())) {
                this.msg(arenaPlayer.getPlayer(), msg);
            }
        }
    }

    public void chooseClass(final Player player, final Sign sign, final String className) {

        debug(this, player, "choosing player class");

        debug(this, player, "checking class perms");
        if (this.cfg.getBoolean(CFG.PERMS_EXPLICITCLASS) && !player.hasPermission("pvparena.class." + className)) {
            this.msg(player, Language.parse(this, MSG.ERROR_NOPERM_CLASS, className));
            return; // class permission desired and failed =>
            // announce and OUT
        }

        if (sign != null) {
            if (this.cfg.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) {
                PAClassSign.remove(this.signs, player);
                final Block block = sign.getBlock();
                PAClassSign classSign = PAClassSign.used(block.getLocation(), this.signs);
                if (classSign == null) {
                    classSign = new PAClassSign(block.getLocation());
                    this.signs.add(classSign);
                }
                if (!classSign.add(player)) {
                    this.msg(player, Language.parse(this, MSG.ERROR_CLASS_FULL, className));
                    return;
                }
            }

            if (ArenaModuleManager.cannotSelectClass(this, player, className)) {
                return;
            }
            if (this.startRunner != null) {
                ArenaPlayer.fromPlayer(player).setStatus(PlayerStatus.READY);
            }
        }
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (aPlayer.getArena() == null) {

            PVPArena.getInstance().getLogger().warning(String.format("failed to set class %s to player %s", className, player.getName()));
        } else if (!ArenaModuleManager.cannotSelectClass(this, player, className)) {

            aPlayer.setArenaClass(className);
            if (aPlayer.getArenaClass() != null) {
                if ("custom".equalsIgnoreCase(className)) {
                    // if custom, give stuff back
                    ArenaPlayer.reloadInventory(this, player, false);
                } else {
                    InventoryManager.clearInventory(player);
                    ArenaPlayer.givePlayerFightItems(this, player);
                }
            }
            return;
        }
        InventoryManager.clearInventory(player);
    }

    public void clearRegions() {
        this.regions.forEach(ArenaRegion::reset);
    }

    /**
     * initiate the arena start countdown
     */
    public void countDown() {
        if (this.startRunner != null || this.fightInProgress) {

            if (!this.cfg.getBoolean(CFG.READY_ENFORCECOUNTDOWN) && this.getClass(this.cfg.getString(CFG.READY_AUTOCLASS)) == null && !this.fightInProgress) {
                this.startRunner.cancel();
                this.startRunner = null;
                this.broadcast(Language.parse(this, MSG.TIMER_COUNTDOWN_INTERRUPTED));
            }
            return;
        }

        new StartRunnable(this, this.cfg.getInt(CFG.TIME_STARTCOUNTDOWN));
    }

    /**
     * count all players being ready
     *
     * @return the number of ready players
     */
    public int countReadyPlayers() {
        long sum = this.teams.stream()
                .flatMap(team -> team.getTeamMembers().stream())
                .filter(p -> p.getStatus() == PlayerStatus.READY)
                .count();
        debug(this, "counting ready players: " + sum);
        return (int) sum;
    }



    /**
     * give customized rewards to players
     *
     * @param player the player to give the reward
     */
    public void giveRewards(final Player player) {
        if (this.gaveRewards) {
            return;
        }

        debug(this, player, "giving rewards to " + player.getName());

        ArenaModuleManager.giveRewards(this, player);
        ItemStack[] items = this.cfg.getItems(CFG.ITEMS_REWARDS);

        final boolean isRandom = this.cfg.getBoolean(CFG.ITEMS_RANDOM);
        final Random rRandom = new Random();

        final PAWinEvent dEvent = new PAWinEvent(this, player, items);
        Bukkit.getPluginManager().callEvent(dEvent);
        items = dEvent.getItems();

        debug(this, player, "start " + this.startCount + " - minplayers: " + this.cfg.getInt(CFG.ITEMS_MINPLAYERS));

        if (items == null || items.length < 1
                || this.cfg.getInt(CFG.ITEMS_MINPLAYERS) > this.startCount) {
            return;
        }

        final int randomItem = rRandom.nextInt(items.length);

        for (int i = 0; i < items.length; ++i) {
            if (items[i] == null) {
                continue;
            }
            final ItemStack stack = items[i];
            if (stack == null) {
                PVPArena.getInstance().getLogger().warning(
                        "unrecognized item: " + items[i]);
                continue;
            }
            if (isRandom && i != randomItem) {
                continue;
            }
            try {
                player.getInventory().setItem(
                        player.getInventory().firstEmpty(), stack);
            } catch (final Exception e) {
                this.msg(player, Language.parse(this, MSG.ERROR_INVENTORY_FULL));
                return;
            }
        }
    }

    public boolean hasEntity(final Entity entity) {
        return this.entities.containsValue(entity.getUniqueId());
    }

    public boolean hasAlreadyPlayed(final String playerName) {
        return this.playedPlayers.contains(playerName);
    }

    public void hasNotPlayed(final ArenaPlayer player) {
        if (this.cfg.getBoolean(CFG.JOIN_ONLYIFHASPLAYED)) {
            return;
        }
        this.playedPlayers.remove(player.getName());
    }

    public boolean hasPlayer(final Player player) {
        for (final ArenaTeam team : this.teams) {
            if (team.hasPlayer(player)) {
                return true;
            }
        }
        return this.equals(ArenaPlayer.fromPlayer(player).getArena());
    }

    public void increasePlayerCount() {
        this.startCount++;
    }

    public void markPlayedPlayer(final String playerName) {
        this.playedPlayers.add(playerName);
    }

    public void msg(final CommandSender sender, final String[] msg) {
        for (final String string : msg) {
            this.msg(sender, string);
        }
    }

    public void msg(final CommandSender sender, final String msg) {
        if (sender != null && StringUtils.isBlank(msg)) {
            debug(this, '@' + sender.getName() + ": " + msg);
            sender.sendMessage(Language.parse(this, MSG.MESSAGES_GENERAL, this.prefix, msg));
        }
    }

    /**
     * return an understandable representation of a player's death cause
     *
     * @param player  the dying player
     * @param cause   the cause
     * @param damager an eventual damager entity
     * @return a colored string
     */
    public String parseDeathCause(final Player player, final DamageCause cause,
                                  final Entity damager) {

        if (cause == null) {
            return Language.parse(this, MSG.DEATHCAUSE_CUSTOM);
        }

        debug(this, player, "return a damage name for : " + cause.toString());

        debug(this, player, "damager: " + damager);

        ArenaPlayer aPlayer = null;
        ArenaTeam team = null;
        if (damager instanceof Player) {
            aPlayer = ArenaPlayer.fromPlayer(damager.getName());
            team = aPlayer.getArenaTeam();
        }

        final EntityDamageEvent lastDamageCause = player.getLastDamageCause();

        Entity eventDamager = ((EntityDamageByEntityEvent) lastDamageCause).getDamager();
        switch (cause) {
            case ENTITY_ATTACK:
            case ENTITY_SWEEP_ATTACK:
                if (damager instanceof Player && team != null) {
                    return team.colorizePlayer(aPlayer.getPlayer()) + ChatColor.YELLOW;
                }

                try {
                    debug(this, player, "last damager: " + eventDamager.getType());
                    return Language.parse(this, MSG.getByName("DEATHCAUSE_" + eventDamager.getType().name()));
                } catch (final Exception e) {
                    return Language.parse(this, MSG.DEATHCAUSE_CUSTOM);
                }
            case ENTITY_EXPLOSION:
                try {
                    debug(this, player, "last damager: " + eventDamager.getType());
                    return Language.parse(this, MSG.getByName("DEATHCAUSE_" + eventDamager.getType().name()));
                } catch (final Exception e) {
                    return Language.parse(this, MSG.DEATHCAUSE_ENTITY_EXPLOSION);
                }
            case PROJECTILE:
                if (damager instanceof Player && team != null) {
                    return team.colorizePlayer(aPlayer.getPlayer()) + ChatColor.YELLOW;
                }
                try {
                    ProjectileSource source = ((Projectile) eventDamager).getShooter();
                    LivingEntity lEntity = (LivingEntity) source;

                    debug(this, player, "last damager: " + lEntity.getType());

                    return Language.parse(this, MSG.getByName("DEATHCAUSE_" + lEntity.getType().name()));
                } catch (final Exception e) {

                    return Language.parse(this, MSG.DEATHCAUSE_PROJECTILE);
                }
            default:
                break;
        }
        MSG string = MSG.getByName("DEATHCAUSE_" + cause.toString());
        if (string == null) {
            PVPArena.getInstance().getLogger().warning("Unknown cause: " + cause.toString());
            string = MSG.DEATHCAUSE_VOID;
        }
        return Language.parse(this, string);
    }

    public static void pmsg(final CommandSender sender, final String msg) {
        if (sender != null && StringUtils.isBlank(msg)) {
            debug(sender, "@{} : {}", sender.getName(), msg);
            sender.sendMessage(Language.parse(MSG.MESSAGES_GENERAL, PVPArena.getInstance().getConfig().getString("globalPrefix", "PVP Arena"), msg));
        }
    }

    /**
     * a player leaves from the arena
     *
     * @param player the leaving player
     */
    public void playerLeave(final Player player, final CFG location, final boolean silent,
                            final boolean force, final boolean soft) {
        if (player == null) {
            return;
        }

        this.goal.parseLeave(player);

        if (!this.fightInProgress) {
            this.startCount--;
            this.playedPlayers.remove(player.getName());
        }
        debug(this, player, "fully removing player from arena");
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (!silent) {

            final ArenaTeam team = aPlayer.getArenaTeam();
            if (team == null) {

                this.broadcastExcept(
                        player,
                        Language.parse(this, MSG.FIGHT_PLAYER_LEFT, player.getName()
                                + ChatColor.YELLOW));
            } else {
                ArenaModuleManager.parsePlayerLeave(this, player, team);

                this.broadcastExcept(
                        player,
                        Language.parse(this, MSG.FIGHT_PLAYER_LEFT,
                                team.colorizePlayer(player) + ChatColor.YELLOW));
            }
            this.msg(player, Language.parse(this, MSG.NOTICE_YOU_LEFT));
        }

        this.removePlayer(player, this.cfg.getString(location), soft, force);

        if (!this.cfg.getBoolean(CFG.READY_ENFORCECOUNTDOWN) && this.startRunner != null && this.cfg.getInt(CFG.READY_MINPLAYERS) > 0 &&
                this.getFighters().size() <= this.cfg.getInt(CFG.READY_MINPLAYERS)) {
            this.startRunner.cancel();
            this.broadcast(Language.parse(this, MSG.TIMER_COUNTDOWN_INTERRUPTED));
            this.startRunner = null;
        }

        if (this.fightInProgress) {
            ArenaManager.checkAndCommit(this, force);
        }

        aPlayer.reset();
    }

    /**
     * check if an arena is ready
     *
     * @return null if ok, error message otherwise
     */
    public String ready() {
        debug(this, "ready check !!");

        final int players = TeamManager.countPlayersInTeams(this);
        if (players < 2) {
            return Language.parse(this, MSG.ERROR_READY_1_ALONE);
        }
        if (players < this.cfg.getInt(CFG.READY_MINPLAYERS)) {
            return Language.parse(this, MSG.ERROR_READY_4_MISSING_PLAYERS);
        }

        if (this.cfg.getBoolean(CFG.READY_CHECKEACHPLAYER)) {
            for (final ArenaTeam team : this.teams) {
                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    if (ap.getStatus() != PlayerStatus.READY) {
                        return Language.parse(this, MSG.ERROR_READY_0_ONE_PLAYER_NOT_READY);
                    }
                }
            }
        }

        if (!this.free) {
            final Set<String> activeTeams = new HashSet<>();

            for (final ArenaTeam team : this.teams) {
                for (final ArenaPlayer ap : team.getTeamMembers()) {
                    if (!this.cfg.getBoolean(CFG.READY_CHECKEACHTEAM) || ap.getStatus() == PlayerStatus.READY) {
                        activeTeams.add(team.getName());
                        break;
                    }
                }
            }

            if (this.cfg.getBoolean(CFG.USES_EVENTEAMS) && !TeamManager.checkEven(this)) {
                return Language.parse(this, MSG.NOTICE_WAITING_EQUAL);
            }

            if (activeTeams.size() < 2) {
                return Language.parse(this, MSG.ERROR_READY_2_TEAM_ALONE);
            }
        }

        for (final ArenaTeam team : this.teams) {
            for (final ArenaPlayer p : team.getTeamMembers()) {
                debug(this, p.getPlayer(), "checking class: " + p.getPlayer().getName());

                if (p.getArenaClass() == null) {
                    debug(this, p.getPlayer(), "player has no class");

                    String autoClass = this.cfg.getDefinedString(CFG.READY_AUTOCLASS);
                    if (this.cfg.getBoolean(CFG.USES_PLAYERCLASSES) && this.getClass(p.getName()) != null) {
                        autoClass = p.getName();
                    }
                    if (autoClass != null && this.getClass(autoClass) != null) {
                        this.selectClass(p, autoClass);
                    } else {
                        // player no class!
                        PVPArena.getInstance().getLogger().warning("Player no class: " + p.getPlayer());
                        return Language.parse(this, MSG.ERROR_READY_5_ONE_PLAYER_NO_CLASS);
                    }
                }
            }
        }
        final int readyPlayers = this.countReadyPlayers();

        if (players > readyPlayers) {
            final double ratio = this.cfg.getDouble(CFG.READY_NEEDEDRATIO);
            debug(this, "ratio: " + ratio);
            if (ratio > 0) {
                double aRatio = ((double) readyPlayers) / players;
                if (aRatio >= ratio) {
                    return "";
                }
            }
            return Language.parse(this, MSG.ERROR_READY_0_ONE_PLAYER_NOT_READY);
        }
        return this.cfg.getBoolean(CFG.READY_ENFORCECOUNTDOWN) ? "" : null;
    }

    /**
     * call event when a player is exiting from an arena (by plugin)
     *
     * @param player the player to remove
     */
    public void callExitEvent(final Player player) {
        final PAExitEvent exitEvent = new PAExitEvent(this, player);
        Bukkit.getPluginManager().callEvent(exitEvent);
    }

    /**
     * call event when a player is leaving an arena (on his own)
     *
     * @param player the player to remove
     */
    public void callLeaveEvent(final Player player) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final PALeaveEvent event = new PALeaveEvent(this, player, aPlayer.getStatus() == PlayerStatus.FIGHT);
        Bukkit.getPluginManager().callEvent(event);
    }

    public void removeClass(final String string) {
        this.classes.removeIf(ac -> ac.getName().equals(string));
    }

    public void removeEntity(final Entity entity) {
        this.entities.values().removeIf(uuid -> uuid.equals(entity.getUniqueId()));
    }

    /**
     * remove a player from the arena
     *
     * @param player the player to reset
     * @param tploc  the coord string to teleport the player to
     */
    public void removePlayer(final Player player, final String tploc, final boolean soft,
                             final boolean force) {
        debug(player, "removing player {}, soft: {}, tp to {}", player.getName(), soft, tploc);
        this.resetPlayer(player, tploc, soft, force);

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        if (!soft && aPlayer.getArenaTeam() != null) {
            aPlayer.getArenaTeam().remove(aPlayer);
        }

        this.callExitEvent(player);
        if (this.cfg.getBoolean(CFG.USES_CLASSSIGNSDISPLAY)) {
            PAClassSign.remove(this.signs, player);
        }

        player.setNoDamageTicks(60);
    }

    /**
     * reset an arena
     *
     * @param force enforce it
     */
    public void resetPlayers(final boolean force) {
        debug(this, "resetting player manager");
        final Set<ArenaPlayer> players = new HashSet<>();
        for (final ArenaTeam team : this.teams) {
            for (final ArenaPlayer arenaPlayer : team.getTeamMembers()) {
                debug(this, arenaPlayer.getPlayer(), "player: " + arenaPlayer.getName());
                if (arenaPlayer.getArena() == null || !arenaPlayer.getArena().equals(this)) {
                    debug(this, arenaPlayer.getPlayer(), "> skipped");
                } else {
                    debug(this, arenaPlayer.getPlayer(), "> added");
                    players.add(arenaPlayer);
                }
            }
        }

        // pre-parsing for "whole team winning"
        for (final ArenaPlayer arenaPlayer : players) {
            if (arenaPlayer.getStatus() != null && arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                if (!force && arenaPlayer.getStatus() == PlayerStatus.FIGHT
                        && this.fightInProgress && !this.gaveRewards && !this.free && this.cfg.getBoolean(CFG.USES_TEAMREWARDS)) {
                    players.removeAll(arenaPlayer.getArenaTeam().getTeamMembers());
                    this.giveRewardsLater(arenaPlayer.getArenaTeam()); // this removes the players from the arena
                    break;
                }
            }
        }

        for (final ArenaPlayer arenaPlayer : players) {

            arenaPlayer.debugPrint();
            if (arenaPlayer.getStatus() != null && arenaPlayer.getStatus() == PlayerStatus.FIGHT) {
                // TODO enhance wannabe-smart exploit fix for people that
                // spam join and leave the arena to make one of them win
                final Player player = arenaPlayer.getPlayer();
                if (!force) {
                    arenaPlayer.addWins();
                }
                this.callExitEvent(player);
                this.resetPlayer(player, this.cfg.getString(CFG.TP_WIN, OLD_TP),
                        false, force);
                if (!force && arenaPlayer.getStatus() == PlayerStatus.FIGHT && this.fightInProgress && !this.gaveRewards) {
                    // if we are remaining, give reward!
                    this.giveRewards(player);
                }
            } else if (arenaPlayer.getStatus() == PlayerStatus.DEAD || arenaPlayer.getStatus() == PlayerStatus.LOST) {

                final PALoseEvent loseEvent = new PALoseEvent(this, arenaPlayer.getPlayer());
                Bukkit.getPluginManager().callEvent(loseEvent);

                final Player player = arenaPlayer.getPlayer();
                if (!force) {
                    arenaPlayer.addLosses();
                }
                this.callExitEvent(player);
                this.resetPlayer(player, this.cfg.getString(CFG.TP_LOSE, OLD_TP), false, force);
            } else {
                this.callExitEvent(arenaPlayer.getPlayer());
                this.resetPlayer(arenaPlayer.getPlayer(), this.cfg.getString(CFG.TP_LOSE, OLD_TP), false, force);
            }

            arenaPlayer.reset();
        }
        for (final ArenaPlayer player : ArenaPlayer.getAllArenaPlayers()) {
            if (this.equals(player.getArena()) && player.getStatus() == PlayerStatus.WATCH) {

                this.callExitEvent(player.getPlayer());
                this.resetPlayer(player.getPlayer(), this.cfg.getString(CFG.TP_EXIT, OLD_TP), false, force);
                player.setArena(null);
                player.reset();
            }
        }
    }

    private void giveRewardsLater(final ArenaTeam arenaTeam) {
        debug("Giving rewards to the whole team!");
        if (arenaTeam == null) {
            debug("team is null");
            return; // this one failed. try next time...
        }

        final Set<ArenaPlayer> players = new HashSet<>(arenaTeam.getTeamMembers());

        players.forEach(ap -> {
            ap.addWins();
            this.callExitEvent(ap.getPlayer());
            this.resetPlayer(ap.getPlayer(), this.cfg.getString(CFG.TP_WIN, OLD_TP), false, false);
            ap.reset();
        });

        debug("Giving rewards to team " + arenaTeam.getName() + '!');

        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
            players.forEach(ap -> {
                debug("Giving rewards to " + ap.getPlayer().getName() + '!');
                try {
                    Arena.this.giveRewards(ap.getPlayer());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            });
            Arena.this.gaveRewards = true;
        }, 1L);

    }

    /**
     * reset an arena
     */
    public void reset(boolean force) {

        final PAEndEvent event = new PAEndEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        debug(this, "resetting arena; force: " + force);
        for (final PAClassSign as : this.signs) {
            as.clear();
        }
        this.signs.clear();
        this.playedPlayers.clear();
        this.resetPlayers(force);
        this.setFightInProgress(false);

        ofNullable(this.endRunner).ifPresent(BukkitRunnable::cancel);
        ofNullable(this.realEndRunner).ifPresent(BukkitRunnable::cancel);
        ofNullable(this.pvpRunner).ifPresent(BukkitRunnable::cancel);
        this.endRunner = null;
        this.realEndRunner = null;
        this.pvpRunner = null;
        ofNullable(this.timer).ifPresent(timer -> {
            timer.stop();
            this.timer = null;
        });

        ArenaModuleManager.reset(this, force);
        ArenaManager.advance(Arena.this);
        this.clearRegions();
        this.goal.reset(force);

        StatisticsManager.save();

        try {
            Bukkit.getScheduler().scheduleSyncDelayedTask(PVPArena.getInstance(), () -> {
                Arena.this.playedPlayers.clear();
                Arena.this.startCount = 0;
            }, 30L);
        } catch (Exception ignored) {
        }
        this.scoreboard = null;
    }

    /**
     * reset a player to his pre-join values
     *
     * @param player      the player to reset
     * @param destination the teleport location
     * @param soft        if location should be preserved (another tp incoming)
     */
    private void resetPlayer(final Player player, final String destination, final boolean soft,
                             final boolean force) {
        if (player == null) {
            return;
        }
        debug(player, "resetting player, soft: {}", soft);

        try {
            new ArrowHack(player);
        } catch (Exception ignored) {
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        ofNullable(aPlayer.getState()).ifPresent(playerState -> playerState.unload(soft));

        this.getScoreboard().reset(player, force, soft);

        ArenaModuleManager.resetPlayer(this, player, soft, force);

        if (!soft && (!aPlayer.hasCustomClass() || this.cfg.getBoolean(CFG.GENERAL_CUSTOMRETURNSGEAR))) {
            ArenaPlayer.reloadInventory(this, player, true);
        }

        this.teleportPlayerAfterReset(destination, soft, force, aPlayer);
    }

    private void teleportPlayerAfterReset(final String destination, final boolean soft, final boolean force, final ArenaPlayer aPlayer) {
        final Player player = aPlayer.getPlayer();
        class RunLater implements Runnable {

            @Override
            public void run() {
                debug(Arena.this, player, "string = " + destination);
                aPlayer.setTelePass(true);

                int noDamageTicks = Arena.this.cfg.getInt(CFG.TIME_TELEPORTPROTECT) * 20;
                if (OLD_TP.equalsIgnoreCase(destination)) {
                    debug(Arena.this, player, "tping to old");
                    if (aPlayer.getSavedLocation() != null) {
                        debug(Arena.this, player, "location is fine");
                        final PALocation loc = aPlayer.getSavedLocation();
                        player.teleport(loc.toLocation());
                        player.setNoDamageTicks(noDamageTicks);
                        aPlayer.setTeleporting(false);
                    }
                } else {
                    Vector offset = Arena.this.cfg.getOffset(destination);
                    PALocation loc = SpawnManager.getSpawnByExactName(Arena.this, destination);
                    if (loc == null) {
                        new Exception("RESET Spawn null: " + Arena.this.getName() + "->" + destination).printStackTrace();
                    } else {
                        player.teleport(loc.toLocation().add(offset));
                        aPlayer.setTelePass(false);
                        aPlayer.setTeleporting(false);
                    }
                    player.setNoDamageTicks(noDamageTicks);
                }
                if (soft || !force) {
                    StatisticsManager.update(Arena.this, aPlayer);
                }
                if (!soft) {
                    aPlayer.setLocation(null);
                    aPlayer.clearFlyState();
                }
            }
        }

        final RunLater runLater = new RunLater();

        aPlayer.setTeleporting(true);
        if (this.cfg.getInt(CFG.TIME_RESETDELAY) > 0 && !force) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), runLater, this.cfg.getInt(CFG.TIME_RESETDELAY) * 20L);
        } else if (PVPArena.getInstance().isShuttingDown()) {
            runLater.run();
        } else {
            // Waiting two ticks in order to avoid player death bug
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), runLater, 2);
        }
    }

    /**
     * reset player variables
     *
     * @param player the player to access
     */
    public void unKillPlayer(final Player player, final DamageCause cause, final Entity damager) {

        debug(this, player, "respawning player " + player.getName());
        double iHealth = this.cfg.getInt(CFG.PLAYER_HEALTH, -1);

        if (iHealth < 1) {
            iHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        }

        PlayerState.playersetHealth(player, iHealth);
        player.setFoodLevel(this.cfg.getInt(CFG.PLAYER_FOODLEVEL, 20));
        player.setSaturation(this.cfg.getInt(CFG.PLAYER_SATURATION, 20));
        player.setExhaustion((float) this.cfg.getDouble(CFG.PLAYER_EXHAUSTION, 0.0));
        player.setVelocity(new Vector());
        player.setFallDistance(0);

        if (this.cfg.getBoolean(CFG.PLAYER_DROPSEXP)) {
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
        }

        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);
        final ArenaTeam team = aPlayer.getArenaTeam();

        if (team == null) {
            return;
        }

        PlayerState.removeEffects(player);

        if (aPlayer.getNextArenaClass() != null) {
            InventoryManager.clearInventory(aPlayer.getPlayer());
            aPlayer.setArenaClass(aPlayer.getNextArenaClass());
            if (aPlayer.getArenaClass() != null) {
                ArenaPlayer.givePlayerFightItems(this, aPlayer.getPlayer());
                aPlayer.setMayDropInventory(true);
            }
            aPlayer.setNextArenaClass(null);
        }

        ArenaModuleManager.parseRespawn(this, player, team, cause, damager);
        player.setFireTicks(0);
        try {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                if (player.getFireTicks() > 0) {
                    player.setFireTicks(0);
                }
            }, 5L);
        } catch (Exception ignored) {
        }
        player.setNoDamageTicks(this.cfg.getInt(CFG.TIME_TELEPORTPROTECT) * 20);
    }

    public void selectClass(final ArenaPlayer aPlayer, final String cName) {
        if (ArenaModuleManager.cannotSelectClass(this, aPlayer.getPlayer(), cName)) {
            return;
        }
        for (final ArenaClass c : this.classes) {
            if (c.getName().equalsIgnoreCase(cName)) {
                aPlayer.setArenaClass(c);
                if (aPlayer.getArenaClass() != null) {
                    aPlayer.setArena(this);
                    aPlayer.createState(aPlayer.getPlayer());
                    InventoryManager.clearInventory(aPlayer.getPlayer());
                    c.equip(aPlayer.getPlayer());
                    this.msg(aPlayer.getPlayer(), Language.parse(this, MSG.CLASS_PREVIEW, c.getName()));
                }
                return;
            }
        }
        this.msg(aPlayer.getPlayer(), Language.parse(this, MSG.ERROR_CLASS_NOT_FOUND, cName));
    }

    public void setFightInProgress(final boolean fightInProgress) {
        this.fightInProgress = fightInProgress;
        debug(this, "fighting : " + fightInProgress);
    }

    public void spawnSet(final String node, final PALocation paLocation) {
        final String string = Config.parseToString(paLocation);

        // the following conversion is needed because otherwise the arena will add
        // too much offset until the next restart, where the location is loaded based
        // on the BLOCK position of the given location plus the player orientation
        final PALocation location = Config.parseLocation(string);

        this.cfg.setManually("spawns." + node, string);
        this.cfg.save();
        this.addSpawn(new PASpawn(location, node));
    }

    public void spawnUnset(final String node) {
        this.cfg.setManually("spawns." + node, null);
        this.cfg.save();
    }

    public void start() {
        this.start(false);
    }

    /**
     * initiate the arena start
     */
    public void start(final boolean forceStart) {
        debug(this, "start()");
        if (this.getConfig().getBoolean(CFG.USES_SCOREBOARD)) {
            if (this.isFightInProgress()) {
                this.getScoreboard().show();
            }
        }
        this.gaveRewards = false;
        this.startRunner = null;
        if (this.fightInProgress) {
            debug(this, "already in progress! OUT!");
            return;
        }
        int sum = 0;
        for (final ArenaTeam team : this.teams) {
            for (final ArenaPlayer ap : team.getTeamMembers()) {
                if (forceStart) {
                    ap.setStatus(PlayerStatus.READY);
                }
                if (ap.getStatus() == PlayerStatus.LOUNGE || ap.getStatus() == PlayerStatus.READY) {
                    sum++;
                }
            }
        }
        debug(this, "sum == " + sum);
        final String error = this.ready();

        boolean overRide = false;

        if (forceStart) {
            overRide = error == null ||
                    error.contains(Language.parse(MSG.ERROR_READY_1_ALONE)) ||
                    error.contains(Language.parse(MSG.ERROR_READY_2_TEAM_ALONE)) ||
                    error.contains(Language.parse(MSG.ERROR_READY_3_TEAM_MISSING_PLAYERS)) ||
                    error.contains(Language.parse(MSG.ERROR_READY_4_MISSING_PLAYERS));
        }

        if (overRide || StringUtils.isBlank(error)) {
            final Boolean handle = WorkflowManager.handleStart(this, null, forceStart);

            if (overRide || Boolean.TRUE.equals(handle)) {
                debug(this, "START!");
                this.setFightInProgress(true);

                this.timer = new ArenaTimer(this);
                this.timer.start();

                if (this.getConfig().getBoolean(CFG.USES_SCOREBOARD)) {
                    this.getScoreboard().show();
                }

            } else {

                // false
                PVPArena.getInstance().getLogger().info("START aborted by event cancel");
                //reset(true);
            }
        } else {
            // false
            this.broadcast(Language.parse(MSG.ERROR_ERROR, error));
            //reset(true);
        }
    }

    public void stop(final boolean force) {
        for (final ArenaPlayer p : this.getFighters()) {
            this.playerLeave(p.getPlayer(), CFG.TP_EXIT, true, force, false);
        }
        this.reset(force);
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        Arena arena = (Arena) o;
        return this.name.equals(arena.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    public void tpPlayerToCoordName(ArenaPlayer player, String place) {
        Location destination = this.prepareTeleportation(player, place);
        this.teleportPlayer(place, player, destination);
        this.execPostTeleportationFixes(player);
    }

    /**
     * teleport a given player to the given coord string
     *
     * @param player the player to teleport
     * @param place  the coord string
     */
    public void tpPlayerToCoordNameForJoin(final ArenaPlayer player, final String place, boolean async) {
        Location destination = this.prepareTeleportation(player, place);
        int delay = async ? 2 : 0;
        Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
            this.teleportPlayer(place, player, destination);
            this.getScoreboard().setupPlayer(player);
        }, delay);
        this.execPostTeleportationFixes(player);
    }

    private Location prepareTeleportation(ArenaPlayer aPlayer, String place) {
        Player player = aPlayer.getPlayer();
        debug(this, player, "teleporting " + player + " to coord " + place);

        if (player.isInsideVehicle()) {
            player.getVehicle().eject();
        }

        ArenaModuleManager.tpPlayerToCoordName(this, player, place);

        if ("spectator".equals(place)) {
            if (this.getFighters().contains(aPlayer)) {
                aPlayer.setStatus(PlayerStatus.LOST);
            } else {
                aPlayer.setStatus(PlayerStatus.WATCH);
            }
        }
        PALocation loc = SpawnManager.getSpawnByExactName(this, place);
        if (OLD_TP.equals(place)) {
            loc = aPlayer.getSavedLocation();
        }
        if (loc == null) {
            throw new RuntimeException("TP Spawn null: " + this.name + "->" + place);
        }

        debug("raw location: {}", loc);

        Vector offset = this.cfg.getOffset(place);
        debug("offset location: {}", offset);

        aPlayer.setTeleporting(true);
        aPlayer.setTelePass(true);
        return loc.toLocation().add(offset);
    }

    private void execPostTeleportationFixes(ArenaPlayer aPlayer) {
        if (this.cfg.getBoolean(CFG.PLAYER_REMOVEARROWS)) {
            try {
                new ArrowHack(aPlayer.getPlayer());
            } catch (final Exception ignored) {
            }
        }

        if (this.cfg.getBoolean(CFG.USES_INVISIBILITYFIX) && Arrays.asList(PlayerStatus.FIGHT, PlayerStatus.LOUNGE).contains(aPlayer.getStatus())) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () ->
                Arena.this.getFighters()
                        .forEach(ap -> ap.getPlayer().showPlayer(PVPArena.getInstance(), aPlayer.getPlayer()))
            , 5L);
        }

        if (!this.cfg.getBoolean(CFG.PERMS_FLY)) {
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                aPlayer.getPlayer().setAllowFlight(false);
                aPlayer.getPlayer().setFlying(false);
            }, 5L);
        }
    }

    private void teleportPlayer(String place, final ArenaPlayer arenaPlayer, Location location) {
        Player player = arenaPlayer.getPlayer();
        player.teleport(location);
        int noDamageTicks = this.cfg.getInt(CFG.TIME_TELEPORTPROTECT) * 20;
        player.setNoDamageTicks(noDamageTicks);
        if (place.contains("lounge")) {
            debug(this, "setting TelePass later!");
            Bukkit.getScheduler().runTaskLater(PVPArena.getInstance(), () -> {
                arenaPlayer.setTelePass(false);
                arenaPlayer.setTeleporting(false);
            }, noDamageTicks);

        } else {
            debug(this, "setting TelePass now!");
            arenaPlayer.setTelePass(false);
            arenaPlayer.setTeleporting(false);
        }
    }

    /**
     * last resort to put a player into an arena (when no goal/module wants to)
     *
     * @param player the player to put
     * @param team   the arena team to put into
     * @return true if joining successful
     */
    public boolean tryJoin(final Player player, final ArenaTeam team) {
        final ArenaPlayer aPlayer = ArenaPlayer.fromPlayer(player);

        debug(this, player, "trying to join player " + player.getName());

        final String clear = this.cfg.getString(CFG.PLAYER_CLEARINVENTORY);

        if ("ALL".equals(clear) || clear.contains(player.getGameMode().name())) {
            player.getInventory().clear();
            ArenaPlayer.backupAndClearInventory(this, player);
            aPlayer.dump();
        }

        final PAJoinEvent event = new PAJoinEvent(this, player, false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            debug("! Join event cancelled by a plugin !");
            return false;
        }

        if (aPlayer.getStatus() == PlayerStatus.NULL) {
            // joining DIRECTLY - save loc !!
            aPlayer.setLocation(new PALocation(player.getLocation()));
        } else {
            // should not happen; just make sure it does not. If noone reports this
            // for some time, we can remove this check. It should never happen
            // anything different. Just saying.
            PVPArena.getInstance().getLogger().warning("Status not null for tryJoin: " + player.getName());
        }

        if (aPlayer.getArenaClass() == null) {
            String autoClass = this.cfg.getDefinedString(CFG.READY_AUTOCLASS);
            if(this.cfg.getBoolean(CFG.USES_PLAYERCLASSES) && this.getClass(player.getName()) != null) {
                autoClass = player.getName();
            }

            if (autoClass != null && autoClass.contains(":") && autoClass.contains(";")) {
                final String[] definitions = autoClass.split(";");
                autoClass = definitions[definitions.length - 1]; // set default

                final Map<String, ArenaClass> classes = new HashMap<>();

                for (final String definition : definitions) {
                    if (!definition.contains(":")) {
                        continue;
                    }
                    final String[] var = definition.split(":");
                    final ArenaClass aClass = this.getClass(var[1]);
                    if (aClass != null) {
                        classes.put(var[0], aClass);
                    }
                }

                if (classes.containsKey(team.getName())) {
                    autoClass = classes.get(team.getName()).getName();
                }
            }

            if (autoClass != null && this.getClass(autoClass) == null) {
                this.msg(player, Language.parse(this, MSG.ERROR_CLASS_NOT_FOUND,
                        "autoClass"));
                return false;
            }
        }

        aPlayer.setArena(this);
        team.add(aPlayer);
        aPlayer.setStatus(PlayerStatus.FIGHT);

        final Set<PASpawn> spawns = new HashSet<>();
        if (this.cfg.getBoolean(CFG.GENERAL_CLASSSPAWN)) {
            String arenaClass = this.getConfig().getDefinedString(CFG.READY_AUTOCLASS);
            if(this.getConfig().getBoolean(CFG.USES_PLAYERCLASSES) && this.getClass(player.getName()) != null) {
                arenaClass = player.getName();
            }
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, team.getName() + arenaClass + "spawn"));
        } else if (this.free) {
            if ("free".equals(team.getName())) {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, "spawn"));
            } else {
                spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, team.getName()));
            }
        } else {
            spawns.addAll(SpawnManager.getPASpawnsStartingWith(this, team.getName() + "spawn"));
        }

        int pos = new Random().nextInt(spawns.size());

        for (final PASpawn spawn : spawns) {
            if (--pos < 0) {
                this.tpPlayerToCoordName(aPlayer, spawn.getName());
                break;
            }
        }

        if (aPlayer.getState() == null) {

            final Arena arena = aPlayer.getArena();


            aPlayer.createState(player);
            ArenaPlayer.backupAndClearInventory(arena, player);
            aPlayer.dump();


            if (aPlayer.getArenaTeam() != null && aPlayer.getArenaClass() == null) {
                String autoClass = arena.cfg.getDefinedString(CFG.READY_AUTOCLASS);
                if (arena.cfg.getBoolean(CFG.USES_PLAYERCLASSES) && arena.getClass(player.getName()) != null) {
                    autoClass = player.getName();
                }
                if (autoClass != null && arena.getClass(autoClass) != null) {
                    arena.chooseClass(player, null, autoClass);
                }
            }
        }
        return true;
    }

    public static void pmsg(final CommandSender sender, final String[] msgs) {
        for (final String s : msgs) {
            pmsg(sender, s);
        }
    }

    public void addBlock(final PABlock paBlock) {
        this.blocks.removeIf(block -> block.getName().equals(paBlock.getName()));
        this.blocks.add(paBlock);
    }

    public void addSpawn(final PASpawn paSpawn) {
        this.spawns.removeIf(spawn -> spawn.getName().equals(paSpawn.getName()));
        this.spawns.add(paSpawn);
    }
}
