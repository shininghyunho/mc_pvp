package kr.utila.pvp.listeners;

import kr.utila.pvp.Main;
import kr.utila.pvp.commands.PVPAdminCommand;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.objects.region.GameStatus;
import kr.utila.pvp.objects.region.PVPRegion;
import org.bukkit.block.Block;
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
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class MainListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UserManager userManager = UserManager.getInstance();
        if(!userManager.exists(player)) userManager.register(player);
        User user = userManager.get(player);

        // 탈주한 플레이어 처리
        if(user.isEscapingUser()) {
            user.quit();
            return;
        }

        // PVP 중이면 재접속 처리
        getPVPRegion(player).ifPresent(region -> {
            if(region.getGameStatus().equals(GameStatus.PAUSED)) region.resume();
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        getPVPRegion(player).ifPresent(pvpRegion -> {
            // 게임중이면 일시정지
            if(pvpRegion.getGameStatus().isInGame()) pvpRegion.pause();
            // 이미 일시정지 이면 취소
            else if(pvpRegion.getGameStatus().equals(GameStatus.PAUSED)) pvpRegion.cancelByReject(player);
            else pvpRegion.waitPlayer(player);
        });
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        // 맨손 일때만 처리
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        Player player = event.getPlayer();
        ItemStack hand = player.getItemInHand();
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) return;
        if (!meta.getPersistentDataContainer().has(Main.key, PersistentDataType.STRING)) return;

        event.setCancelled(true);
        String uuid = player.getUniqueId().toString();
        if (!PVPAdminCommand.posSettingData.containsKey(uuid)) {
            PVPAdminCommand.posSettingData.put(uuid, new LocationDTO[2]);
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        switch (action) {
            case LEFT_CLICK_BLOCK -> {
                PVPAdminCommand.posSettingData.get(uuid)[0] = LocationDTO.toLocationDTO(clickedBlock.getLocation());
                player.sendMessage("pos1 설정 완료");
            }
            case RIGHT_CLICK_BLOCK -> {
                PVPAdminCommand.posSettingData.get(uuid)[1] = LocationDTO.toLocationDTO(clickedBlock.getLocation());
                player.sendMessage("pos2 설정 완료");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player player) getPVPRegion(player).ifPresent(pvpRegion -> {
            if (player.getHealth() <= event.getDamage()) getPVPRegion(player).ifPresent(PVPRegion::endMatch);
        });
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        User user = UserManager.getInstance().get(player);

        // 일시정지 상태에서 이동 방지
        getPVPRegion(user).ifPresent(pvpRegion -> {
            if(pvpRegion.getGameStatus().isNotMovable()) event.setCancelled(true);
        });
    }

    private Optional<PVPRegion> getPVPRegion(User user) {
        return Optional.ofNullable(user)
                .map(User::getPVPName)
                .map(RegionManager.getInstance()::get);
    }
    private Optional<PVPRegion> getPVPRegion(Player player) {
        return getPVPRegion(UserManager.getInstance().get(player));
    }
}
