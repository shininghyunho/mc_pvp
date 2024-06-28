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
                        int remainSecond = pvpRegion.getRemainSecond() - 1;
                        pvpRegion.setRemainSecond(remainSecond);
                        if (remainSecond % 10 == 0 || remainSecond <= 10) {
                            for (String uuid : pvpRegion.getRegionPlayer().values()) {
                                Player player = Bukkit.getPlayer(UUID.fromString(uuid));
                                Lang.send(player, Lang.BROADCAST_REMAIN_COUNT, s -> s.replaceAll("%second%", remainSecond + ""));
                            }
                        }
                        if (remainSecond == 0) {
                            Player player1, player2;
                            player1 = Bukkit.getPlayer(UUID.fromString(pvpRegion.getRegionPlayer().get(TeamType.RED)));
                            player2 = Bukkit.getPlayer(UUID.fromString(pvpRegion.getRegionPlayer().get(TeamType.BLUE)));
                            if (player1.getHealth() == player2.getHealth()) {
                                pvpRegion.askRestart();
                            } else if (player1.getHealth() > player2.getHealth()) {
                                pvpRegion.askRestart(player1, player2);
                            } else {
                                pvpRegion.askRestart(player2, player1);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(), 20, 20);
    }

}
