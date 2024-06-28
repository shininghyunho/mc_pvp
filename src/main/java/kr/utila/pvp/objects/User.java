package kr.utila.pvp.objects;

import kr.utila.pvp.Main;
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
    private String uuid;
    private int totalStraight;
    private int straight;
    private List<Integer> acquiredRewards;
    private String currentPVP;
    private ItemStack[] inventoryContents;
    private LocationDTO beforeLocation;

    public User(String uuid, int totalStraight, int straight, List<Integer> acquiredRewards, String currentPVP, ItemStack[] inventoryContents, LocationDTO beforeLocation) {
        this.uuid = uuid;
        this.totalStraight = totalStraight;
        this.straight = straight;
        this.acquiredRewards = acquiredRewards;
        this.currentPVP = currentPVP;
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
        yamlConfiguration.set("currentPVP", currentPVP);
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
        currentPVP = name;
        inventoryContents = player.getInventory().getContents();
        beforeLocation = LocationDTO.toLocationDTO(player.getLocation());
    }

    public void quit() {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
        currentPVP = null;
        if (!player.isOnline()) {
            return;
        }
        Player parsedPlayer = (Player) player;
        parsedPlayer.teleport(beforeLocation.toLocation());
        parsedPlayer.getInventory().setContents(inventoryContents);
        inventoryContents = null;
        beforeLocation = null;
    }

    public int getTotalStraight() {
        return totalStraight;
    }

    public void setTotalStraight(int totalStraight) {
        this.totalStraight = totalStraight;
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

    public void setAcquiredRewards(List<Integer> acquiredRewards) {
        this.acquiredRewards = acquiredRewards;
    }

    public String getCurrentPVP() {
        return currentPVP;
    }

    public void setCurrentPVP(String currentPVP) {
        this.currentPVP = currentPVP;
    }

    public ItemStack[] getInventoryContents() {
        return inventoryContents;
    }

    public void setInventoryContents(ItemStack[] inventoryContents) {
        this.inventoryContents = inventoryContents;
    }

    public LocationDTO getBeforeLocation() {
        return beforeLocation;
    }

    public void setBeforeLocation(LocationDTO beforeLocation) {
        this.beforeLocation = beforeLocation;
    }
}
