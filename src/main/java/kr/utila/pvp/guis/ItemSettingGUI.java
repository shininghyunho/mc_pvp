package kr.utila.pvp.guis;

import kr.utila.pvp.libraries.SimpleInventoryHolder;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ItemSettingGUI implements SimpleInventoryHolder {

    private Inventory inventory;
    private String name;
    private TeamType teamType;

    public static void open(Player player, String name, TeamType teamType) {
        player.openInventory(new ItemSettingGUI(name, teamType).getInventory());
    }

    public ItemSettingGUI(String name, TeamType teamType) {
        this.inventory = Bukkit.createInventory(this, 9 * 6, "시작 아이템 설정");
        this.name = name;
        this.teamType = teamType;
        List<ItemStack> items = RegionManager.getInstance().getTeamStartItem(name, teamType);
        if (items == null) {
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            inventory.setItem(i, items.get(i));
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
                RegionManager.getInstance().setTeamStartItem(name, teamType, contents);
                event.getPlayer().sendMessage("§a설정 완료");
            }
        }
    }
}
