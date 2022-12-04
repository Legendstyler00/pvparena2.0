package net.slipcor.pvparena.config;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        int compareValue = getConfigGroupPriority(o1) - getConfigGroupPriority(o2);
        return (compareValue != 0) ? compareValue : o1.compareTo(o2);
    }

    private static int getConfigGroupPriority(String object) {
        try {
            List<String> groupOrder = Stream.of(
                    "configversion",
                    "uuid",
                    "cmds",
                    "general",
                    "goal",
                    "uses",
                    "perms",
                    "damage",
                    "msg",
                    "chat",
                    "player",
                    "items",
                    "time",
                    "ready",
                    "join",
                    "tp",
                    "protection",
                    "block",
                    "arenaregion",
                    "spawns",
                    "blocks",
                    "teams",
                    "flagColors",
                    "mods",
                    "modules",
                    "classchests",
                    "classitems"
            ).collect(Collectors.toList());
            int index = groupOrder.indexOf(object.toLowerCase());
            return (index == -1) ? 999 : index;
        } catch (ClassCastException | NullPointerException ignored) {
            return 999;
        }
    }
}
