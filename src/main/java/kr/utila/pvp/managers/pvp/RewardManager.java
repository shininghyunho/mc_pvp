package kr.utila.pvp.managers.pvp;

import kr.utila.pvp.Main;
import kr.utila.pvp.managers.Manager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RewardManager extends Manager {

    private static final File DIRECTORY = new File(Main.getInstance().getDataFolder(), "rewards");
    private static final RewardManager INSTANCE = new RewardManager();

    private final Map<Integer, List<ItemStack>> STRAIGHT_REWARDS = new HashMap<>();

    private RewardManager() {
        super(Main.getInstance());
    }

    public static RewardManager getInstance() {
        return INSTANCE;
    }

    public void set(int straight, List<ItemStack> rewards) {
        STRAIGHT_REWARDS.put(straight, rewards);
        save(straight);
    }

    public List<ItemStack> get(int straight) {
        List<ItemStack> items = new ArrayList<>();
        if (STRAIGHT_REWARDS.containsKey(straight)) {
            items.addAll(STRAIGHT_REWARDS.get(straight));
        }
        return items;
    }

    public boolean exists(int straight) {
        return STRAIGHT_REWARDS.containsKey(straight);
    }

    public void save(int straight) {
        final File FILE = new File(DIRECTORY, straight + ".yml");
        YamlConfiguration yamlConfiguration = new YamlConfiguration();
        int i = 0;
        for (ItemStack itemStack : STRAIGHT_REWARDS.get(straight)) {
            yamlConfiguration.set(i + "", itemStack);
            i++;
        }
        try {
            yamlConfiguration.save(FILE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void load() throws Exception {
        STRAIGHT_REWARDS.clear();
        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
            return;
        }
        if (DIRECTORY.listFiles() == null) {
            return;
        }
        for (File file : DIRECTORY.listFiles()) {
            int straight = Integer.parseInt(file.getName().replaceAll(".yml", ""));
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            List<ItemStack> items = new ArrayList<>();
            for (String key : yamlConfiguration.getKeys(false)) {
                items.add(yamlConfiguration.getItemStack(key));
            }
            STRAIGHT_REWARDS.put(straight, items);
        }
    }
}
