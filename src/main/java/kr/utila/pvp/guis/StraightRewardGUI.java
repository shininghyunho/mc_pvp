package kr.utila.pvp.guis;

import kr.utila.pvp.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.libraries.SimpleInventoryHolder;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RewardManager;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.utils.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class StraightRewardGUI implements SimpleInventoryHolder {

    private Inventory inventory;

    public static void open(Player player) {
        player.openInventory(new StraightRewardGUI(player).getInventory());
    }


    public StraightRewardGUI(Player player) {
        this.inventory = Bukkit.createInventory(this, 9 * 6, "[ 연승 보상 ]");
        User user = UserManager.getInstance().get(player);
        RewardManager rewardManager = RewardManager.getInstance();
        int slot = 0;
        for (int i = 1; i <= user.getTotalStraight(); i++) {
            if (rewardManager.exists(i) && !user.getAcquiredRewards().contains(i)) {
                ItemStack itemStack = new ItemStack(Material.CHEST);
                ItemMeta itemMeta = itemStack.getItemMeta();
                itemMeta.setDisplayName("§f[ §e" + i + "연승 보상 §f]");
                itemMeta.setLore(List.of("§7클릭 시 획득합니다"));
                itemMeta.getPersistentDataContainer().set(Main.key, PersistentDataType.INTEGER, i);
                itemStack.setItemMeta(itemMeta);
                inventory.setItem(slot, itemStack);
                slot++;
            }
        }
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void executeEvent(InventoryEvent inventoryEvent, EventType type) {
        switch (type) {
            case CLICK -> {
                InventoryClickEvent event = (InventoryClickEvent) inventoryEvent;
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null) {
                    return;
                }
                if (!clicked.hasItemMeta()) {
                    return;
                }
                if (!clicked.getItemMeta().getPersistentDataContainer().has(Main.key, PersistentDataType.INTEGER)) {
                    return;
                }
                Player player = (Player) event.getWhoClicked();
                int straightIndex = clicked.getItemMeta().getPersistentDataContainer().get(Main.key, PersistentDataType.INTEGER);
                List<ItemStack> reward = RewardManager.getInstance().get(straightIndex);
                if (reward.size() > InventoryUtil.getEmptySpaceOfInventory(player)) {
                    Lang.send(player, Lang.NEED_TO_EMPTY_SPACE, s -> s.replaceAll("%n", reward.size() + ""));
                    return;
                }
                User user = UserManager.getInstance().get(player);
                user.getAcquiredRewards().add(straightIndex);
                for (ItemStack itemStack : reward) {
                    player.getInventory().addItem(itemStack);
                }
                inventory.clear();
                open(player);
            }
        }
    }

}
