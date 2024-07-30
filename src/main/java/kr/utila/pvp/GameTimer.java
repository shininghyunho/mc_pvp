package kr.utila.pvp;

import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.region.GameStatus;
import kr.utila.pvp.objects.region.PVPRegion;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.UUID;

public class GameTimer {
    public static void run() {
        new BukkitRunnable() {
            @Override
            public void run() {
                RegionManager.getInstance().getAllRegions().stream()
                        .filter(Objects::nonNull)
                        .filter(pvpRegion -> pvpRegion.getGameStatus().equals(GameStatus.MATCH_IN_PROGRESS))
                        .forEach(pvpRegion -> {
                            if (pvpRegion.remainSecond--<=0) pvpRegion.endMatch();
                            else if (pvpRegion.remainSecond % 10 == 0) {
                                pvpRegion.regionPlayerUniqueIdMap.values().stream()
                                        .map(UUID::fromString)
                                        .map(Bukkit::getPlayer)
                                        .forEach(player -> {
                                            // 팀별로 남은 시간 알림
                                            Lang.send(player, Lang.BROADCAST_REMAIN_COUNT, s -> s.replaceAll("%second%", pvpRegion.remainSecond + ""));
                                            // boss bar 업데이트
                                            pvpRegion.getBossBarEntity().ifPresent(bossBarEntity -> bossBarEntity.update(pvpRegion.remainSecond,Config.GAME_TIME));
                                        });
                            }
                        });
            }
        }.runTaskTimer(Main.getInstance(), 20, 20);
    }
}
