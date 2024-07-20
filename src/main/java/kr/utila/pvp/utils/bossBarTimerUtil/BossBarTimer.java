package kr.utila.pvp.utils.bossBarTimerUtil;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BossBarTimer {
    private final JavaPlugin plugin;
    private final BossBar bossBar;
    private final long totalTime;
    private long timeLeft;
    private final Set<Player> players;
    private BukkitTask timer = null;

    public BossBarTimer(JavaPlugin plugin, long totalTime) {
        this.plugin = plugin;
        this.totalTime = totalTime;
        this.timeLeft = totalTime;
        this.bossBar = Bukkit.createBossBar(getUpdatedBossBarTitle(), BarColor.GREEN, BarStyle.SOLID);
        this.players = new HashSet<>();
    }

    public void addPlayer(Player player) {
        if(player==null) return;
        players.add(player);
        bossBar.addPlayer(player);
    }

    public void start() {
        if(timer != null) {
            return;
        }

        prepareTimer();
        timer = new BukkitRunnable() {
            @Override
            public void run() {
                updateBossBar();
                if (timeLeft <= 0) {
                    stop();
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void stop() {
        if(timer == null) {
            return;
        }
        bossBar.setVisible(false);
        bossBar.removeAll();
        timer.cancel();
        players.clear();
    }

    private void prepareTimer() {
        timeLeft = totalTime;
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);
    }

    private void updateBossBar() {
        timeLeft--;
        bossBar.setProgress((double) timeLeft / totalTime);
        bossBar.setTitle(getUpdatedBossBarTitle());
    }

    private String getUpdatedBossBarTitle() {
        return kr.utila.pvp.config.Config.BOSS_BAR_TIMER_TITLE.replace("%second%", String.valueOf(timeLeft));
    }
}