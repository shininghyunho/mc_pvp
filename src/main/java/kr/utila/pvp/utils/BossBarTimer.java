package kr.utila.pvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Set;

public class BossBarTimer {
    private final JavaPlugin plugin;
    private final BossBar bossBar;
    private final long totalTime;
    private long timeLeft;
    private final Set<Player> players;
    private BukkitTask timer = null;

    public BossBarTimer(JavaPlugin plugin, long totalTime, long timeLeft) {
        this.plugin = plugin;
        this.totalTime = totalTime;
        this.timeLeft = timeLeft;
        this.bossBar = Bukkit.createBossBar("Time Left: " + timeLeft, BarColor.GREEN, BarStyle.SOLID);
        this.players = new HashSet<>();
    }

    public void addPlayer(Player player) {
        players.add(player);
        bossBar.addPlayer(player);
    }

    public void removePlayer(Player player) {
        players.remove(player);
        bossBar.removePlayer(player);
    }

    public void start() {
        if(timer != null) {
            return;
        }

        bossBar.setProgress((double) timeLeft / totalTime);
        bossBar.setVisible(true);
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

    private void updateBossBar() {
        timeLeft--;
        bossBar.setProgress((double) timeLeft / totalTime);
        bossBar.setTitle("Time Left: " + timeLeft);
    }

    public void stop() {
        if(timer == null) {
            return;
        }
        bossBar.setVisible(false);
        timer.cancel();
    }
}