package kr.utila.pvp;

public class Config {

    private static final Main PLUGIN = Main.getInstance();

    public static int START_COOLTIME;
    public static int WAITING_PLAYER_SECOND;
    public static int GAME_TIME;

    public static void load() {
        PLUGIN.saveDefaultConfig();
        PLUGIN.reloadConfig();
        START_COOLTIME = PLUGIN.getConfig().getInt("START_COOLTIME");
        WAITING_PLAYER_SECOND = PLUGIN.getConfig().getInt("WAITING_PLAYER_SECOND");
        GAME_TIME = PLUGIN.getConfig().getInt("GAME_TIME");
    }

}
