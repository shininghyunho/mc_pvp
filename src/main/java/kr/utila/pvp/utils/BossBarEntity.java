package kr.utila.pvp.utils;

import kr.utila.pvp.Config;
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

    // BarColor, BarStyle 을 인자로 받는 생성자 추가
    public BossBarEntity(List<Player> players, String title, BarColor barColor, BarStyle barStyle) {
        this.bossBar = Bukkit.createBossBar(title, barColor, barStyle);
        players.forEach(bossBar::addPlayer);
    }

    public BossBarEntity(List<Player> players) {
        this(players, "타이머",BarColor.GREEN, BarStyle.SOLID);
    }

    // start
    public void start() {
        logger.info("BossBarEntity start : " + bossBar.getTitle());
        bossBar.setVisible(true);
    }

    // pause
    public void pause() {
        bossBar.setVisible(false);
    }

    public void update(int second, int totalSecond, String title) {
        logger.info("BossBarEntity update : " + bossBar.getTitle());
        // print all players
        bossBar.getPlayers().forEach(player -> logger.info(player.getName()));
        // visible
        bossBar.setVisible(true);

        double progress = (double) second / totalSecond;
        setProgress(progress);
        setTitle(title.replaceAll("%second%", String.valueOf(second)));
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

    // clear
    public void clear() {
        bossBar.setVisible(false);
        bossBar.removeAll();
    }
}
