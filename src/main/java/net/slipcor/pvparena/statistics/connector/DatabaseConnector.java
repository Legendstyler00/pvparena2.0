package net.slipcor.pvparena.statistics.connector;

import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.logger.Level;
import com.j256.ormlite.logger.Logger;
import com.j256.ormlite.table.TableUtils;
import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.config.Debugger;
import net.slipcor.pvparena.statistics.model.PlayerArenaStats;

import java.sql.SQLException;

/**
 * Abstract Database class, serves as a base for any connection method (MySQL,
 * SQLite, etc.)
 */
public abstract class DatabaseConnector {
    // only keep the connections open for 5 minutes
    protected static final int MAX_CONNECTION_AGE_MILLIS = 300_000;
    protected JdbcConnectionSource connectionSource;

    protected DatabaseConnector() {}

    public JdbcConnectionSource getConnection() {
        if (this.connectionSource == null) {
            this.connectionSource = this.openConnection();
        }
        return this.connectionSource;
    }

    protected abstract JdbcConnectionSource openConnection();

    public void closeConnection() {
        if (this.connectionSource != null) {
            try {
                this.connectionSource.close();
            } catch (final Exception e) {
                PVPArena.getInstance().getLogger().severe("There was an exception when closing database connection: " + e.getMessage());
            }
        }
        this.connectionSource = null;
    }

    public void initDatabase() {
        if (!Debugger.isActive()) {
            // Mute ORMLite Logger except for errors
            Logger.setGlobalLogLevel(Level.ERROR);
        }

        try {
            TableUtils.createTableIfNotExists(this.connectionSource, PlayerArenaStats.class);
        } catch (SQLException exception) {
            PVPArena.getInstance().getLogger().severe("There was an exception creating database table: " + exception.getMessage());
        }
    }

}
