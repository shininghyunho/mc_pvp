package kr.utila.pvp;

import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.region.PVPRegion;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GameTimer {

    public static void run() {
        new BukkitRunnable() {
            @Override
            public void run() {
                RegionManager regionManager = RegionManager.getInstance();
                for (String name : regionManager.getAllRegions()) {
                    PVPRegion pvpRegion = regionManager.get(name);
                    if (pvpRegion.isDelaying()) {
                        continue;
                    }
                    if (pvpRegion.isGaming()) {
                        int remainSecond = pvpRegion.remainSecond - 1;
                        pvpRegion.remainSecond = remainSecond;
                        if (remainSecond % 10 == 0 || remainSecond <= 10) {
                            for (String uuid : pvpRegion.regionPlayerUniqueIdMap.values()) {
                                Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                                Lang.send(player, Lang.BROADCAST_REMAIN_COUNT, s -> s.replaceAll("%second%", remainSecond + ""));
                            }
                        }
                        if (remainSecond == 0) {
                            Player player1, player2;
                            player1 = Bukkit.getPlayer(UUID.fromString(pvpRegion.regionPlayerUniqueIdMap.get(TeamType.RED)));
                            player2 = Bukkit.getPlayer(UUID.fromString(pvpRegion.regionPlayerUniqueIdMap.get(TeamType.BLUE)));
                            if (player1.getHealth() == player2.getHealth()) {
                                pvpRegion.askRestartWhenDraw();
                            } else if (player1.getHealth() > player2.getHealth()) {
                                pvpRegion.askRestartWhenNotDraw(player1, player2);
                            } else {
                                pvpRegion.askRestartWhenNotDraw(player2, player1);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 20, 20);
    }

}
