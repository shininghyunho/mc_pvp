package kr.utila.pvp.managers;

import kr.utila.pvp.config.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.User;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class UserManager extends Manager {

    private static final File DIRECTORY = new File(Main.getInstance().getDataFolder(), "users");
    private static final UserManager INSTANCE = new UserManager();

    private final Map<String, User> USERS = new HashMap<>();

    private UserManager() {
        super(Main.getInstance());
    }

    public static UserManager getInstance() {
        return INSTANCE;
    }

    public User get(Player player) {
        return get(player.getUniqueId().toString());
    }

    public User get(String uuid) {
        return USERS.get(uuid);
    }

    public boolean exists(Player player) {
        return USERS.containsKey(player.getUniqueId().toString());
    }

    public void register(Player player) {
        USERS.put(player.getUniqueId().toString(), new User(player.getUniqueId().toString(), 0, 0, new ArrayList<>(), null, null, null));
        USERS.get(player.getUniqueId().toString()).write();
    }

    public void getRanking(Player player) {
        List<User> sorted = USERS.values().stream().sorted(Comparator.comparing(User::getTotalStraight).reversed()).collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i++) {
            int finalI = i;
            try {
                Lang.send(player, Lang.RANK_FORMAT, s -> {
                    return s.replaceAll("%rank%", (finalI + 1) + "").
                            replaceAll("%player%", Bukkit.getOfflinePlayer(UUID.fromString(sorted.get(finalI).getUUID())).getName()).
                            replaceAll("%straight%", sorted.get(finalI).getTotalStraight() + "");
                });
                Lang.send(player, Lang.RANK_FORMAT, s -> {
                    return s.replaceAll("%rank%", (finalI + 1) + "").
                            replaceAll("%player%", Bukkit.getOfflinePlayer(UUID.fromString(sorted.get(finalI).getUUID())).getName()).
                            replaceAll("%straight%", sorted.get(finalI).getTotalStraight() + "");
                });
            } catch (Exception e) {
                Lang.send(player, Lang.RANK_FORMAT, s -> {
                    return s.replaceAll("%rank%", (finalI + 1) + "").replaceAll("%player%", "N/A").replaceAll("%straight%", "N/A");
                });
            }
        }
    }

    @Override
    public void load() throws Exception {
        for (Player player : Bukkit.getOnlinePlayers()) {
            register(player);
        }
        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
            return;
        }
        if (DIRECTORY.listFiles() == null) {
            return;
        }
        for (File file : DIRECTORY.listFiles()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            String uuid = yamlConfiguration.getString("uuid");
            int totalStraight = yamlConfiguration.getInt("totalStraight");
            int straight = yamlConfiguration.getInt("straight");
            List<Integer> acquiredRewards = yamlConfiguration.getIntegerList("acquiredRewards");
            String currentPVP = null;
            if (yamlConfiguration.contains("currentPVP")) {
                currentPVP = yamlConfiguration.getString("currentPVP");
            }
            List<ItemStack> items = null;
            if (yamlConfiguration.contains("inventoryContents")) {
                items = new ArrayList<>();
                for (String a : yamlConfiguration.getConfigurationSection("inventoryContents").getKeys(false)) {
                    items.add(yamlConfiguration.getItemStack("inventoryContents." + a));
                }
            }
            LocationDTO beforeLocation = null;
            if (yamlConfiguration.contains("beforeLocation")) {
                beforeLocation = LocationDTO.readYAML(yamlConfiguration.getConfigurationSection("beforeLocation"));
            }
            USERS.put(uuid, new User(uuid, totalStraight, straight, acquiredRewards, currentPVP, items == null ? null : items.toArray(new ItemStack[items.size()]), beforeLocation));
        }
    }

    public void saveAll() {
        for (User user : USERS.values()) {
            user.write();
        }
    }
}
