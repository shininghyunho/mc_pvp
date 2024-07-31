package kr.utila.pvp;

public class Config {
    private static final Main PLUGIN = Main.getInstance();
    /**
     * 경기 리셋 시간
     */
    public static int MATCH_RESET_SECOND;
    /**
     * 경기 시작 대기 시간
     */
    public static int MATCH_WAIT_SECOND;
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
        MATCH_RESET_SECOND = PLUGIN.getConfig().getInt("MATCH_RESET_SECOND");
        MATCH_WAIT_SECOND = PLUGIN.getConfig().getInt("MATCH_WAIT_SECOND");
        MATCH_SECOND = PLUGIN.getConfig().getInt("MATCH_SECOND");
        WAITING_FOR_LEFT_USER_SECOND = PLUGIN.getConfig().getInt("WAITING_FOR_LEFT_USER_SECOND");
    }
}
