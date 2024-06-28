package kr.utila.pvp.guis;

import kr.utila.pvp.libraries.SimpleInventoryHolder;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.managers.pvp.RewardManager;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class StraightRewardSettingGUI implements SimpleInventoryHolder {

    private Inventory inventory;

    private int straight;

    public static void open(Player player, int straight) {
        player.openInventory(new StraightRewardSettingGUI(straight).getInventory());
    }

    public StraightRewardSettingGUI(int straight) {
        this.inventory = Bukkit.createInventory(this, 9 * 6, "연승 보상 설정");
        this.straight = straight;
        int i = 0;
        for (ItemStack itemStack : RewardManager.getInstance().get(straight)) {
            this.inventory.setItem(i, itemStack);
        }
    }


    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void executeEvent(InventoryEvent inventoryEvent, EventType type) {
        switch (type) {
            case CLOSE -> {
                InventoryCloseEvent event = (InventoryCloseEvent) inventoryEvent;
                List<ItemStack> contents = new ArrayList<>();
                for (ItemStack itemStack : inventory.getContents()) {
                    if (itemStack == null) {
                        continue;
                    }
                    contents.add(itemStack);
                }
                RewardManager.getInstance().set(straight, contents);
                event.getPlayer().sendMessage("§a설정 완료");
            }
        }
    }
}
