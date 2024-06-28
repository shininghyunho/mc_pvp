package kr.utila.pvp.utils;

import org.bukkit.Location;

public class LocationUtil {

    public static boolean isMoved(Location before, Location after) {
        if ((before.getX() != after.getX()) ||
                (before.getY() != after.getY()) ||
                (before.getZ() != after.getZ())) {
            return true;
        }
        return false;
    }

}
