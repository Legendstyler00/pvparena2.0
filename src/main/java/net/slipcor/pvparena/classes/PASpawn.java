package net.slipcor.pvparena.classes;

import net.slipcor.pvparena.PVPArena;
import net.slipcor.pvparena.arena.Arena;
import net.slipcor.pvparena.core.Config;
import net.slipcor.pvparena.exceptions.GameplayException;
import net.slipcor.pvparena.managers.SpawnManager;
import org.bukkit.util.Vector;

import java.util.Objects;

import static java.util.Optional.ofNullable;

public class PASpawn {

    public static final String FIGHT = "fight";
    public static final String SPECTATOR = "spectator";
    public static final String OLD = "old";
    public static final String EXIT = "exit";
    public static final String PA_SPAWN_FORMAT = "%s%s%s";

    private final PALocation location;
    private final String name;
    private final String teamName;
    private final String className;
    private Vector offset;

    public PASpawn(final PALocation loc, final String name, final String teamName, final String className) {
        this.location = ofNullable(loc).map(PALocation::simplifyCoordinates).orElse(null);
        this.name = name;
        this.teamName = teamName;
        this.className = className;
    }

    /**
     * Deserialize PaSpawn from a config node
     *
     * format:
     * (team_)<name>(_class): world,x,y,z,yaw,pitch
     * </p>
     * spectator: event,3408,76,135,175.4559326171875,5.699995517730713
     * red_spawn: event,3459,62,104,-90.00845336914062,-1.650019884109497
     * red_spawn_pyro: event,3459,62,104,-90.00845336914062,-1.650019884109497
     *
     * @param spawnNode config node name
     * @param location location
     *
     * @return PaSpawn
     */
    public static PASpawn deserialize(String spawnNode, String location, Arena arena) {
        try {
            String[] parsedArgs = SpawnManager.parseSpawnNameFromConfig(arena, spawnNode);
            String spawnName = parsedArgs[0];
            String teamName = parsedArgs[1];
            String className = parsedArgs[2];
            return new PASpawn(Config.parseLocation(location), spawnName, teamName, className);
        } catch (GameplayException e) {
            PVPArena.getInstance().getLogger().severe(
                    String.format("[%s] Error while parsing spawn name %s: %s",
                            arena.getName(),
                            spawnNode,
                            e.getMessage()
                    ));
        } catch (ArrayIndexOutOfBoundsException e) {
            PVPArena.getInstance().getLogger().severe(
                    String.format("[%s] Error while parsing spawn name %s: format is not correct, it should be (teamName_)[spawnName](_className)",
                            arena.getName(),
                            spawnNode
                    ));
        }
        return null;
    }

    /**
     * Return PaSpawn full name (like in config file)
     *
     * format:
     * (team_)<name>(_class)
     *
     * @return String of spawn serialized
     */
    public String getFullName(){
            return (String.format(PA_SPAWN_FORMAT,
                    ofNullable(this.teamName).map(tname -> tname + '_').orElse(""),
                    this.name,
                    ofNullable(this.className).map(cname -> cname + '_').orElse("")
            ));
    }

    public String getPrettyName(){
        return (String.join(" ",
                ofNullable(this.teamName).orElse(""),
                this.name,
                ofNullable(this.className).orElse("")
        ).trim());
    }

    @Override
    public boolean equals(final Object o) {
        if (o instanceof PASpawn) {
            final PASpawn other = (PASpawn) o;
            return this.name.equals(other.name)
                    && this.location.equals(other.location)
                    && Objects.equals(this.teamName, other.teamName)
                    && Objects.equals(this.className, other.className);
        }
        return false;
    }

    public PALocation getPALocation() {
        return this.location;
    }

    public PALocation getPALocationWithOffset() {
        return this.location.add(PVPArena.getInstance().getSpawnOffset().toVector());
    }

    public String getName() {
        return this.name;
    }

    public String getTeamName() {
        return this.teamName;
    }

    public String getClassName() {
        return this.className;
    }

    public Vector getOffset() {
        return this.offset;
    }

    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    public boolean hasTeamName() {
        return this.teamName != null;
    }

    public boolean hasClassName() {
        return this.className != null;
    }

    @Override
    public String toString() {
        return "PASpawn{" +
                "location=" + this.location +
                ", spawnName='" + this.name + '\'' +
                ", teamName='" + this.teamName + '\'' +
                ", className='" + this.className + '\'' +
                ", offset=" + this.offset +
                '}';
    }
}
