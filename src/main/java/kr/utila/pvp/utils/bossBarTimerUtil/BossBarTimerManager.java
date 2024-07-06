package kr.utila.pvp.utils.bossBarTimerUtil;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class BossBarTimerManager {
    private static final Map<String, BossBarTimer> timers = new HashMap<>();

    // get or create timer
    public static BossBarTimer getTimer(String name, JavaPlugin plugin, long totalTime) {
        if(timers.containsKey(name)) {
            return timers.get(name);
        }
        BossBarTimer timer = new BossBarTimer(plugin, totalTime);
        timers.put(name, timer);
        return timer;
    }

    public static void removeTimer(String name) {
        BossBarTimer timer = timers.get(name);
        if(timer != null) {
            timer.stop();
        }
        timers.remove(name);
    }
}
