package com.dazzhub.skywars.Utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class locUtils
{
    public static String locToString(Location l) {
        World world = l.getWorld();
        if (world == null) return null;

        return world.getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ() + ":" + l.getYaw() + ":" + l.getPitch();
    }

    public static Location stringToLoc(String s) {
        try {
            World world = Bukkit.getWorld(s.split(":")[0]);
            double x = Double.parseDouble(s.split(":")[1]);
            double y = Double.parseDouble(s.split(":")[2]);
            double z = Double.parseDouble(s.split(":")[3]);
            float p = Float.parseFloat(s.split(":")[4]);
            float y2 = Float.parseFloat(s.split(":")[5]);
            return new Location(world, x + 0.5, y, z + 0.5, p, y2);
        }
        catch (Exception ex) {
            return null;
        }
    }
}
