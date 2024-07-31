package kr.utila.pvp;

import kr.utila.pvp.commands.PVPAdminCommand;
import kr.utila.pvp.commands.PVPCommand;
import kr.utila.pvp.libraries.SimpleInventoryHolder;
import kr.utila.pvp.listeners.MainListener;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.managers.pvp.RewardManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    public static NamespacedKey key;

    private static Main instance;

    public static Main getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        key = new NamespacedKey(this, "pvp");

        prepareLibraries();
        registerEvents();
        executeCommands();
        load();

        GameTimer.run();
    }

    @Override
    public void onDisable() {
        RegionManager.getInstance().saveAll();
        UserManager.getInstance().saveAll();
    }

    private void prepareLibraries() {
        this.getServer().getPluginManager().registerEvents(new SimpleInventoryHolder.InventoryHolderHandler(), this);
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvents(new MainListener(), this);
    }

    private void executeCommands() {
        PVPAdminCommand.register();
        PVPCommand.register();
    }

    private void load() {
        Config.load();
        Lang.load();
        try {
            RewardManager.getInstance().load();
            RegionManager.getInstance().load();
            UserManager.getInstance().load();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
