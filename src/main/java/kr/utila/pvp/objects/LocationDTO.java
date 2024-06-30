package kr.utila.pvp.objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class LocationDTO {

    private String world;

    private double x;
    private double y;
    private double z;
    private float pitch;
    private float yaw;

    public LocationDTO(String world, double x, double y, double z, float pitch, float yaw) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static LocationDTO toLocationDTO(Location location) {
        return new LocationDTO(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw());
    }

    public static LocationDTO readYAML(ConfigurationSection section) {
        return new LocationDTO(section.getString("world"), section.getDouble("x"), section.getDouble("y"), section.getDouble("z"),
                (float) section.getDouble("pitch"), (float) section.getDouble("yaw"));
    }

    public Location toLocation() {
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    public void writeYAML(ConfigurationSection section) {
        section.set("world", world);
        section.set("x", x);
        section.set("y", y);
        section.set("z", z);
    }
}
