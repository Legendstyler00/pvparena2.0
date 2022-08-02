package net.slipcor.pvparena.statistics.connector;

import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import net.slipcor.pvparena.PVPArena;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.SQLException;

/**
 * Connects to and uses a MySQL database
 */
public class MySqlConnector extends DatabaseConnector {
    private final String user;
    private final String database;
    private final String password;
    private final String port;
    private final String hostname;
    private final String ssl;

    public MySqlConnector(ConfigurationSection mysqlConfig) {
        super();
        this.hostname = mysqlConfig.getString("hostname");
        this.port = mysqlConfig.getString("port");
        this.database = mysqlConfig.getString("database");
        this.ssl = mysqlConfig.getString("ssl");
        this.user = mysqlConfig.getString("user");
        this.password = mysqlConfig.getString("password");
    }

    @Override
    public JdbcPooledConnectionSource openConnection() {

        JdbcPooledConnectionSource connectionSource = null;
        try {
            // pooled connection source
            connectionSource = new JdbcPooledConnectionSource(this.buildJdbcString(), this.user, this.password);

            connectionSource.setMaxConnectionAgeMillis(MAX_CONNECTION_AGE_MILLIS);
            // change the check-every milliseconds from 30 seconds to 60
            // connectionSource.setCheckConnectionsEveryMillis(60 * 1000);
            // for extra protection, enable the testing of connections
            // right before they are handed to the user
            // connectionSource.setTestBeforeGet(true);

        } catch (SQLException e) {
            PVPArena.getInstance().getLogger().warning("Can't open Mysql connection: " + e.getMessage());
        }
        return connectionSource;
    }

    private String buildJdbcString() {
        return String.format("jdbc:mysql://%s:%s/%s?useSSL=%s", this.hostname, this.port, this.database, this.ssl);
    }
}
