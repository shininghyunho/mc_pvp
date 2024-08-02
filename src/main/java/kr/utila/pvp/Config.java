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
    /**
     * 매치 재시작 취소 시간
     */
    public static int MATCH_REPLAY_CANCEL_SECOND;
    /**
     * 매치 시간 보스바 타이틀
     */
    public static String MATCH_IN_PROGRESS_BOSSBAR_TITLE;
    /**
     * 매치 재시작 취소 보스바 타이틀
     */
    public static String MATCH_REPLAY_CANCEL_BOSSBAR_TITLE;
    // WAITING_FOR_LEFT_USER_BOSSBAR_TITLE
    /**
     * 탈주한 유저 대기 보스바 타이틀
     */
    public static String WAITING_FOR_LEFT_USER_BOSSBAR_TITLE;

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
        MATCH_REPLAY_CANCEL_SECOND = getNonNullValue(config, "MATCH_REPLAY_CANCEL_SECOND", 10);
        MATCH_IN_PROGRESS_BOSSBAR_TITLE = getNonNullValue(config, "MATCH_BOSSBAR_TITLE", "%second% 초 남았습니다.");
        MATCH_REPLAY_CANCEL_BOSSBAR_TITLE = getNonNullValue(config, "MATCH_REPLAY_CANCEL_BOSSBAR_TITLE", "%second% 초 후 경기가 종료됩니다.");
        WAITING_FOR_LEFT_USER_BOSSBAR_TITLE = getNonNullValue(config, "WAITING_FOR_LEFT_USER_BOSSBAR_TITLE", "나간 유저 대기 중 남은 시간 : %second% 초");
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

    private static <T> T getNonNullValue(FileConfiguration config, String path, T defaultValue) {
        return Optional.ofNullable(config.get(path))
                .map(value -> (T) value)
                .orElse(defaultValue);
    }
}
