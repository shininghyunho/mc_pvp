package kr.utila.pvp;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Config {
    private static final Main PLUGIN = Main.getInstance();
    /**
     * 경기 리셋 시간
     */
    public static int MATCH_INITIALIZED_SECOND;
    /**
     * 경기 시작 대기 시간
     */
    public static int MATCH_WAITING_SECOND;
    /**
     * 경기 시간
     */
    public static int MATCH_SECOND;
    /**
     * 탈주한 유저 대기 시간
     */
    public static int WAITING_FOR_LEFT_USER_SECOND;

    public static void load() {
        PLUGIN.saveDefaultConfig();
        PLUGIN.reloadConfig();
        var config = PLUGIN.getConfig();
        loadValues(config);
        ClickableValue.load(config);
    }

    private static void loadValues(FileConfiguration config) {
        MATCH_INITIALIZED_SECOND = getNonNullValue(config, "MATCH_INITIALIZED_SECOND", 5);
        MATCH_WAITING_SECOND = getNonNullValue(config, "MATCH_WAITING_SECOND", 5);
        MATCH_SECOND = getNonNullValue(config, "MATCH_SECOND", 60);
        WAITING_FOR_LEFT_USER_SECOND = getNonNullValue(config, "WAITING_FOR_LEFT_USER_SECOND", 15);
    }

    public static class ClickableValue {
        public static final Map<String,String> buttonNameMap = new HashMap<>();
        public static final Map<String,String> commandMap = new HashMap<>() {{
           put("ACCEPT_YES","/pvp 수락");
           put("ACCEPT_NO","/pvp 거절");
           put("REPLAY_YES","/pvp 다시하기");
           put("REPLAY_NO","/pvp 그만하기");
        }};

        private static void load(FileConfiguration config) {
            Optional.ofNullable(config.getConfigurationSection("CLICK_MESSAGE"))
                    .ifPresent(clickable -> clickable.getKeys(false).forEach(key -> buttonNameMap.put(key, clickable.getString(key))));
        }
    }

    private static int getNonNullValue(FileConfiguration config, String path, int defaultValue) {
        return config.contains(path) ? config.getInt(path) : defaultValue;
    }
}
