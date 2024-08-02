package kr.utila.pvp.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.logging.Logger;

public class BossBarEntity {
    Logger logger = Logger.getLogger(BossBarEntity.class.getName());
    private final BossBar bossBar;

    public BossBarEntity(List<Player> players, String title, int second, int totalSecond, BarColor barColor, BarStyle barStyle) {
        this.bossBar = Bukkit.createBossBar(title, barColor, barStyle);
        this.bossBar.setVisible(false);
        players.forEach(bossBar::addPlayer);
        set(second, totalSecond, title);
    }

    // clear
    public void clear() {
        bossBar.setVisible(false);
        bossBar.removeAll();
    }

    // set progress
    private void setProgress(double progress) {
        // validate
        if(progress < 0 || progress > 1) progress = 0;
        bossBar.setProgress(progress);
    }

    // set title
    private void setTitle(String title) {
        if(title == null) title = "Default Title";
        bossBar.setTitle(title);
    }

    // set
    private void set(int second, int totalSecond, String title) {
        double progress = (double) second / totalSecond;
        setProgress(progress);
        setTitle(title.replaceAll("%second%", String.valueOf(second)));

        // visible
        bossBar.setVisible(true);
    }
}
