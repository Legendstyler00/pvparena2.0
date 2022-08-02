package net.slipcor.pvparena.statistics.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import org.bukkit.Bukkit;

import javax.persistence.Transient;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents an object storing all player-related data, which can load and save it.
 */
@DatabaseTable(tableName = "pvparena_statistics")
public class PlayerArenaStats {

    @DatabaseField(generatedId = true)
    private Long id;

    @DatabaseField(columnName = "player_uuid", uniqueCombo = true, canBeNull = false, index = true)
    private String playerUuid;

    @DatabaseField(columnName = "arena_uuid", uniqueCombo = true, canBeNull = false, index = true)
    private String arenaUuid;

    @DatabaseField(canBeNull = false)
    private Long losses;

    @DatabaseField(canBeNull = false)
    private Long wins;

    @DatabaseField(canBeNull = false)
    private Long kills;

    @DatabaseField(canBeNull = false)
    private Long deaths;

    @DatabaseField(canBeNull = false)
    private Long damage;

    @DatabaseField(columnName = "max_damage", canBeNull = false)
    private Long maxDamage;

    @DatabaseField(columnName = "damage_taken", canBeNull = false)
    private Long damageTaken;

    @DatabaseField(columnName = "max_damage_taken", canBeNull = false)
    private Long maxDamageTaken;

    private void setBlankValues() {
        this.damage = 0L;
        this.damageTaken = 0L;
        this.deaths = 0L;
        this.wins = 0L;
        this.losses = 0L;
        this.kills = 0L;
        this.maxDamage = 0L;
        this.maxDamageTaken = 0L;
    }

    public PlayerArenaStats() {
        this.setBlankValues();
    }

    public PlayerArenaStats(Long id, String playerUuid, Long losses, Long wins, Long kills, Long deaths, Long damage, Long maxDamage, Long damageTaken, Long maxDamageTaken) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.losses = losses;
        this.wins = wins;
        this.kills = kills;
        this.deaths = deaths;
        this.damage = damage;
        this.maxDamage = maxDamage;
        this.damageTaken = damageTaken;
        this.maxDamageTaken = maxDamageTaken;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlayerUuid() {
        return this.playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public String getArenaUuid() {
        return this.arenaUuid;
    }

    public void setArenaUuid(String arenaUuid) {
        this.arenaUuid = arenaUuid;
    }

    public Long getLosses() {
        return this.losses;
    }

    public void setLosses(Long losses) {
        this.losses = losses;
    }

    public void incLosses() {
        this.losses += 1;
    }

    public Long getWins() {
        return this.wins;
    }

    public void setWins(Long wins) {
        this.wins = wins;
    }

    public void incWins() {
        this.wins += 1;
    }

    public Long getKills() {
        return this.kills;
    }

    public void setKills(Long kills) {
        this.kills = kills;
    }

    public void incKills() {
        this.kills += 1;
    }

    public Long getDeaths() {
        return this.deaths;
    }

    public void setDeaths(Long deaths) {
        this.deaths = deaths;
    }

    public void incDeaths() {
        this.deaths += 1;
    }

    public Long getDamage() {
        return this.damage;
    }

    public void setDamage(Long damage) {
        this.damage = damage;
    }

    public void addDamage(Long damage) {
        this.damage += damage;
    }

    public Long getMaxDamage() {
        return this.maxDamage;
    }

    public void setMaxDamage(Long maxDamage) {
        this.maxDamage = maxDamage;
    }

    public void addMaxDamage(Long maxDamage) {
        this.maxDamage += maxDamage;
    }

    public Long getDamageTaken() {
        return this.damageTaken;
    }

    public void setDamageTaken(Long damageTaken) {
        this.damageTaken = damageTaken;
    }

    public void addDamageTake(Long damageTake) {
        this.damageTaken += damageTake;
    }

    public Long getMaxDamageTaken() {
        return this.maxDamageTaken;
    }

    public void setMaxDamageTaken(Long maxDamageTaken) {
        this.maxDamageTaken = maxDamageTaken;
    }

    public void addMaxDamageTake(Long maxDamageTake) {
        this.maxDamageTaken += maxDamageTake;
    }

    @Transient
    public String getPlayerName() {
        if(this.playerUuid != null) {
            return Bukkit.getOfflinePlayer(UUID.fromString(this.playerUuid)).getName();
        }
        return null;
    }

    public void mergeWithDiff(PlayerArenaStats diffStats) {
        this.deaths += diffStats.getDeaths();
        this.kills += diffStats.getKills();
        this.wins += diffStats.getWins();
        this.losses += diffStats.getLosses();
        this.damage += diffStats.getDamage();
        this.damageTaken += diffStats.getDamageTaken();
        this.maxDamage = Math.max(this.maxDamage, diffStats.getMaxDamage());
        this.maxDamageTaken = Math.max(this.maxDamageTaken, diffStats.getMaxDamageTaken());
    }

    public void clearValues() {
        this.setBlankValues();
    }

    public Long getValueByStatType(StatEntry statType) {
        switch (statType) {
            case DEATHS:
                return this.deaths;
            case KILLS:
                return this.kills;
            case WINS:
                return this.wins;
            case LOSSES:
                return this.losses;
            case DAMAGE:
                return this.damage;
            case DAMAGE_TAKEN:
                return this.damageTaken;
            case MAX_DAMAGE:
                return this.maxDamage;
            case MAX_DAMAGE_TAKEN:
                return this.maxDamageTaken;
            default:
                return 0L;
        }
    }

    public Map<StatEntry, Long> getMap() {
        Map<StatEntry, Long> map = new HashMap<>();
        map.put(StatEntry.DEATHS, this.deaths);
        map.put(StatEntry.KILLS, this.kills);
        map.put(StatEntry.WINS, this.wins);
        map.put(StatEntry.LOSSES, this.losses);
        map.put(StatEntry.DAMAGE, this.damage);
        map.put(StatEntry.DAMAGE_TAKEN, this.damageTaken);
        map.put(StatEntry.MAX_DAMAGE, this.maxDamage);
        map.put(StatEntry.MAX_DAMAGE_TAKEN, this.maxDamageTaken);
        return map;
    }

    public static PlayerArenaStats fromSumQueryResult(String[] columnNames, String[] resultColumns) {
        return new PlayerArenaStats(
                Long.parseLong(resultColumns[0]),
                resultColumns[1],
                Long.parseLong(resultColumns[2]),
                Long.parseLong(resultColumns[3]),
                Long.parseLong(resultColumns[4]),
                Long.parseLong(resultColumns[5]),
                Long.parseLong(resultColumns[6]),
                Long.parseLong(resultColumns[7]),
                Long.parseLong(resultColumns[8]),
                Long.parseLong(resultColumns[9])
        );
    }
}
