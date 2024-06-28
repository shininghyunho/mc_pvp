package kr.utila.pvp.utils;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InventoryUtil {

    public static int getEmptySpaceOfInventory(Player player) {
        int i = 0;
        for (ItemStack itemStack : player.getInventory().getStorageContents()) {
            if (itemStack == null) {
                i++;
            }
        }
        return i;
    }

}
