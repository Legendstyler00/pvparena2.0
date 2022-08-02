package net.slipcor.pvparena.statistics.dao;

import com.j256.ormlite.dao.Dao;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;
import net.slipcor.pvparena.statistics.model.StatEntry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public interface PlayerArenaStatsDao extends Dao<PlayerArenaStats, Long> {
    Optional<PlayerArenaStats> findByPlayerAndArena(Player player, Arena arena);

    Optional<PlayerArenaStats> findStatsSumByPlayer(@NotNull Player player);

    List<PlayerArenaStats> findBestStatByArena(StatEntry entryName, Arena arena, Long limit);

    List<PlayerArenaStats> findBestStat(StatEntry entryName, Long limit);

    void save(@NotNull PlayerArenaStats playerArenaStats);
}
