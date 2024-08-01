package kr.utila.pvp.utils;

import kr.utila.pvp.Config;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.List;

public class BossBarEntity {
    private final BossBar bossBar;

    // BarColor, BarStyle 을 인자로 받는 생성자 추가
    public BossBarEntity(List<Player> players, BarColor barColor, BarStyle barStyle) {
        this.bossBar = Bukkit.createBossBar("BossBarTimer", barColor, barStyle);
        players.forEach(bossBar::addPlayer);
    }

    public BossBarEntity(List<Player> players) {
        this(players, BarColor.GREEN, BarStyle.SOLID);
    }

    // start
    public void start() {
        bossBar.setVisible(true);
    }

    // pause
    public void pause() {
        bossBar.setVisible(false);
    }

    public void update(int remainSecond, int totalSecond, String title) {
        double progress = (double) remainSecond / totalSecond;
        setProgress(progress);
        setTitle(title.replaceAll("%SECOND%", String.valueOf(remainSecond)));
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
        bossBar.setVisible(false);
        bossBar.removeAll();
    }
}
