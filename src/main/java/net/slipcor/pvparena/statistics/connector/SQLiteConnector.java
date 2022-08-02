package net.slipcor.pvparena.statistics.connector;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import net.slipcor.pvparena.PVPArena;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Connects to and uses a SQLite database
 */
public class SQLiteConnector extends DatabaseConnector {
    private final static String FILE_NAME = "database.sqlite";

    /**
     * Creates a new SQLite instance
     */
    public SQLiteConnector() {
        super();
    }

    @Override
    public JdbcPooledConnectionSource openConnection() {
        Plugin plugin = PVPArena.getInstance();

        final File file = new File(plugin.getDataFolder(), FILE_NAME);
        if (!(file.exists())) {
            try {
                if(!file.createNewFile()){
                    PVPArena.getInstance().getLogger().severe( "Can't create sqLite file: " + file.getPath());
                    return null;
                }
            } catch (final IOException e) {
                PVPArena.getInstance().getLogger().severe( "Unable to create SqLite database file: " + e.getMessage());
            }
        }

        JdbcPooledConnectionSource connection = null;
        try {
            // pooled connection source
            String jdbcUri = String.format("jdbc:sqlite:%s/%s", plugin.getDataFolder().toPath(), FILE_NAME);
            connection = new JdbcPooledConnectionSource(jdbcUri);
            connection.setMaxConnectionAgeMillis(MAX_CONNECTION_AGE_MILLIS);

        } catch (SQLException e) {
            PVPArena.getInstance().getLogger().severe( "Can't open Sqlite connection: " + e.getMessage());
        }
        return connection;
    }
}
