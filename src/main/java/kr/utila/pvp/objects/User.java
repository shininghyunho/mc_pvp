package kr.utila.pvp.objects;

import kr.utila.pvp.Main;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.region.GameStatus;
import kr.utila.pvp.objects.region.PVPRegion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class User implements Writable {
    private static final File DIRECTORY = new File(Main.getInstance().getDataFolder(), "users");
    private final String uuid;
    private int totalStraight;
    private int straight;
    private final List<Integer> acquiredRewards;
    private String PVPName;
    private ItemStack[] inventoryContents;
    private LocationDTO beforeLocation;

    public User(String uuid, int totalStraight, int straight, List<Integer> acquiredRewards, String PVPName, ItemStack[] inventoryContents, LocationDTO beforeLocation) {
        this.uuid = uuid;
        this.totalStraight = totalStraight;
        this.straight = straight;
        this.acquiredRewards = acquiredRewards;
        this.PVPName = PVPName;
        this.inventoryContents = inventoryContents;
        this.beforeLocation = beforeLocation;
    }

    @Override
    public void write() {
        final File FILE = new File(DIRECTORY, uuid + ".yml");
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        yamlConfiguration.set("uuid", uuid);
        yamlConfiguration.set("totalStraight", totalStraight);
        yamlConfiguration.set("straight", straight);
        yamlConfiguration.set("acquiredRewards", acquiredRewards);
        yamlConfiguration.set("PVPName", PVPName);
        if (inventoryContents != null) {
            for (int i = 0; i < inventoryContents.length; i++) {
                yamlConfiguration.set("inventoryContents." + i, inventoryContents[i]);
            }
        }
        if (beforeLocation != null) {
            beforeLocation.writeYAML(yamlConfiguration.createSection("beforeLocation"));
        }
        try {
            yamlConfiguration.save(FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(String name) {
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        if(player == null) return;
        PVPName = name;
        inventoryContents = player.getInventory().getContents();
        beforeLocation = LocationDTO.toLocationDTO(player.getLocation());
    }

    public void quit() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        if (!player.isOnline()) return;

        Player parsedPlayer = (Player) player;
        if(beforeLocation != null) parsedPlayer.teleport(beforeLocation.toLocation());
        parsedPlayer.getInventory().setContents(inventoryContents);

        PVPName = null;
        inventoryContents = null;
        beforeLocation = null;
    }

    public int getTotalStraight() {
        return totalStraight;
    }

    public String getUUID() {
        return uuid;
    }

    public int getStraight() {
        return straight;
    }

    public void setStraight(int straight) {
        this.straight = straight;
        if (totalStraight < straight) {
            totalStraight = straight;
        }
    }

    public List<Integer> getAcquiredRewards() {
        return acquiredRewards;
    }
    public String getPVPName() {
        return PVPName;
    }
    public LocationDTO getBeforeLocation() {
        return beforeLocation;
    }

    // 탈주했다가 경기가 끝난후 재섭했는지 여부
    public boolean isEscapingUser() {
        Player player = Bukkit.getPlayer(UUID.fromString(uuid));
        if(player == null) return false;

        PVPRegion pvpRegion = RegionManager.getInstance().get(PVPName);
        if(pvpRegion == null) return true;

        // 경기중 맴버중 포함되어있는지 여부
        for(Player regionPlayer : pvpRegion.getPlayers()) if(regionPlayer.getUniqueId().equals(player.getUniqueId())) return false;

        return true;
    }
}
