package kr.utila.pvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.List;

public class BossBarEntity {
    private final BossBar bossBar;

    public BossBarEntity(List<Player> playes) {
        this.bossBar = Bukkit.createBossBar("Time Left: ", BarColor.GREEN, BarStyle.SOLID);
        playes.forEach(bossBar::addPlayer);
    }

    // start
    public void start() {
        bossBar.setVisible(true);
    }

    // stop
    public void stop() {
        bossBar.setVisible(false);
        bossBar.getPlayers().forEach(bossBar::removePlayer);
    }

    // pause
    public void pause() {
        bossBar.setVisible(false);
    }

    public void update(int remainSecond, int totalSecond) {
        double progress = (double) remainSecond / totalSecond;
        setProgress(progress);
        setTitle("Time Left: " + remainSecond);
    }
    // set progress
    private void setProgress(double progress) {
        bossBar.setProgress(progress);
    }

    // set title
    private void setTitle(String title) {
        bossBar.setTitle(title);
    }

    // clear
    public void clear() {
        bossBar.removeAll();
    }
}
