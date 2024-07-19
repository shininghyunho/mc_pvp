package kr.utila.pvp.listeners;

import kr.utila.pvp.config.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.commands.PVPAdminCommand;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.objects.region.PVPRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class MainListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UserManager userManager = UserManager.getInstance();
        if (!userManager.exists(player)) {
            userManager.register(player);
        }
        User user = userManager.get(player);
        if (user.getCurrentPVP() == null && user.getBeforeLocation() != null) {
            user.quit();
            return;
        }
        if (user.getCurrentPVP() != null && RegionManager.getInstance().get(user.getCurrentPVP()).isDelaying()) {
            PVPRegion pvpRegion = RegionManager.getInstance().get(user.getCurrentPVP());
            for (String member : pvpRegion.getRegionPlayer().values()) {
                Player p = Bukkit.getPlayer(UUID.fromString(member));
                Lang.send(p, Lang.RESTART, s -> s.replaceAll("%player%", player.getName()));
            }
            RegionManager.getInstance().get(user.getCurrentPVP()).setDelaying(false);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        User user = UserManager.getInstance().get(player);
        if (user.getCurrentPVP() != null) {
            PVPRegion pvpRegion = RegionManager.getInstance().get(user.getCurrentPVP());
            if (pvpRegion.isDelaying()) {
                pvpRegion.cancel(player);
            } else {
                pvpRegion.waitPlayer(player);
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();
        if (hand == null) {
            return;
        }
        if (!hand.hasItemMeta()) {
            return;
        }
        if (!hand.getItemMeta().getPersistentDataContainer().has(Main.key, PersistentDataType.STRING)) {
            return;
        }
        event.setCancelled(true);
        String uuid = player.getUniqueId().toString();
        if (!PVPAdminCommand.posSettingData.containsKey(uuid)) {
            PVPAdminCommand.posSettingData.put(uuid, new LocationDTO[2]);
        }
        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                PVPAdminCommand.posSettingData.get(uuid)[0] = LocationDTO.toLocationDTO(event.getClickedBlock().getLocation());
                player.sendMessage("pos1 설정 완료");
            }
            case RIGHT_CLICK_BLOCK -> {
                PVPAdminCommand.posSettingData.get(uuid)[1] = LocationDTO.toLocationDTO(event.getClickedBlock().getLocation());
                player.sendMessage("pos2 설정 완료");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            User user = UserManager.getInstance().get(player);
            if (user.getCurrentPVP() != null) {
                if (player.getHealth() <= event.getDamage()) {
                    event.setCancelled(true);
                    RegionManager.getInstance().get(user.getCurrentPVP()).askRestart((Player) event.getDamager(), player);
                }
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        User user = UserManager.getInstance().get(player);
        if (user.getCurrentPVP() == null) {
            return;
        }
        if (RegionManager.getInstance().get(user.getCurrentPVP()).isDelaying()) {
            event.setCancelled(true);
        }
    }

}
