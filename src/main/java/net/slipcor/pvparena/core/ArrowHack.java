package net.slipcor.pvparena.core;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public final class ArrowHack {
    public static void processArrowHack(Player player) {
        try {
            // Spigot API >= 1.16.2
            Method setArrowCount = player.getClass().getMethod("setArrowsInBody", int.class);
            setArrowCount.invoke(player, 0);
        } catch (ReflectiveOperationException ignored) {

        }
    }
}
