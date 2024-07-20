package kr.utila.pvp.config;

import kr.utila.pvp.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final Main PLUGIN = Main.getInstance();

    public static int START_COOLTIME;
    public static int WAITING_PLAYER_SECOND;
    public static int GAME_TIME;
    public static int GAME_RESET_TIME;
    public static int GAME_RESTART_TIME;
    public static String BOSS_BAR_TIMER_TITLE;

    public static void load() {
        PLUGIN.saveDefaultConfig();
        PLUGIN.reloadConfig();

        var config = PLUGIN.getConfig();
        START_COOLTIME = config.getInt("START_COOLTIME");
        WAITING_PLAYER_SECOND = config.getInt("WAITING_PLAYER_SECOND");
        GAME_TIME = config.getInt("GAME_TIME");
        GAME_RESET_TIME = config.getInt("GAME_RESET_TIME");
        GAME_RESTART_TIME = config.getInt("GAME_RESTART_TIME");
        BOSS_BAR_TIMER_TITLE = config.getString("BOSS_BAR_TIMER_TITLE");
        ClickMessage.load(config);
    }

    public static class ClickMessage {
        // variable map
        public static final Map<String, String> clickMessageMap = new HashMap<>();
        // click command map
        public static final Map<String, String> clickCommandMap = new HashMap<>() {{
            put("accept_yes", "/pvp 수락");
            put("accept_no", "/pvp 거절");
            put("retry_yes", "/pvp 다시하기");
            put("retry_no", "/pvp 그만하기");
        }};

        private static void load(FileConfiguration config) {
            var clickMessage = config.getConfigurationSection("CLICK_MESSAGE");
            if (clickMessage == null) {
                return;
            }
            for (String key : clickMessage.getKeys(false)) {
                clickMessageMap.put(key, clickMessage.getString(key));
            }
        }
    }
}
