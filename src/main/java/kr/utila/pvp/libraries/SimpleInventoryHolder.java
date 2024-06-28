package kr.utila.pvp.libraries;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface SimpleInventoryHolder extends InventoryHolder {

    void executeEvent(InventoryEvent inventoryEvent, EventType type);

    enum EventType {
        CLICK, DRAG, OPEN, CLOSE
    }

    class InventoryHolderHandler implements Listener {
        @EventHandler
        public void onInventoryClick(InventoryClickEvent event) {
            Inventory inventory = event.getInventory();
            if (isHolder(inventory)) {
                executeEvent(inventory.getHolder(), event, EventType.CLICK);
            }
        }

        @EventHandler
        public void onInventoryDrag(InventoryDragEvent event) {
            Inventory inventory = event.getInventory();
            if (isHolder(inventory)) {
                executeEvent(inventory.getHolder(), event, EventType.DRAG);
            }
        }

        @EventHandler
        public void onInventoryOpen(InventoryOpenEvent event) {
            Inventory inventory = event.getInventory();
            if (isHolder(inventory)) {
                executeEvent(inventory.getHolder(), event, EventType.OPEN);
            }
        }

        @EventHandler
        public void onInventoryClick(InventoryCloseEvent event) {
            Inventory inventory = event.getInventory();
            if (isHolder(inventory)) {
                executeEvent(inventory.getHolder(), event, EventType.CLOSE);
            }
        }

        private boolean isHolder(Inventory inventory) {
            if (inventory.getHolder() == null) {
                return false;
            }
            if (!(inventory.getHolder() instanceof SimpleInventoryHolder)) {
                return false;
            }
            return true;
        }

        private void executeEvent(InventoryHolder holder, InventoryEvent event, EventType type) {
            ((SimpleInventoryHolder) holder).executeEvent(event, type);
        }
    }
}
