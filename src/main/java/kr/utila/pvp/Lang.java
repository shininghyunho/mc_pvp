package kr.utila.pvp;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class Lang {

    private static final File FILE = new File(Main.getInstance().getDataFolder(), "lang.yml");

    public static LangFormat PVP_CANCEL_BY_LOGOUT;
    public static LangFormat REJECT_REQUEST_RECEIVER;
    public static LangFormat REJECT_REQUEST_SENDER;
    public static LangFormat FINISH_WINNER;
    public static LangFormat FINISH_LOSER;
    public static LangFormat ACCEPT_INVITATION;
    public static LangFormat ASK_RETRY;
    public static LangFormat REJECT_RETRY;
    public static LangFormat ACCEPT_RETRY;
    public static LangFormat WAITING_PLAYER;
    public static LangFormat BROADCAST_REMAIN_COUNT;
    public static LangFormat DRAW;
    public static LangFormat RESTART;
    public static LangFormat AVAILABLE_TO_GET_REWARD;
    public static LangFormat NEED_TO_EMPTY_SPACE;
    public static LangFormat RANK_FORMAT;
    // START
    public static LangFormat START_GAME;
    public static LangFormat WAITING_STARTING_GAME;
    // RESET
    public static LangFormat RESET_GAME;
    public static LangFormat WAITING_RESET_GAME;
    public static LangFormat NON_AVAILABLE_PLACE;
    public static LangFormat ALREADY_PVP;
    public static LangFormat ALREADY_INVITING;
    public static LangFormat ALREADY_PVP_SELF;

    private static final Map<String,String> commandMap = new HashMap<>();
    // 커맨드 메시지
    private static final Map<String,String> commandButton = new HashMap<>();

    public Lang() {
        initCommand();
    }

    public static void load() {
        Main.getInstance().saveResource("lang.yml", false);
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(FILE);
        PVP_CANCEL_BY_LOGOUT = LangFormat.get(yamlConfiguration.getConfigurationSection("PVP_CANCEL_BY_LOGOUT"));
        REJECT_REQUEST_RECEIVER = LangFormat.get(yamlConfiguration.getConfigurationSection("REJECT_REQUEST_RECEIVER"));
        REJECT_REQUEST_SENDER = LangFormat.get(yamlConfiguration.getConfigurationSection("REJECT_REQUEST_SENDER"));
        FINISH_WINNER = LangFormat.get(yamlConfiguration.getConfigurationSection("FINISH_WINNER"));
        FINISH_LOSER = LangFormat.get(yamlConfiguration.getConfigurationSection("FINISH_LOSER"));
        ACCEPT_INVITATION = LangFormat.get(yamlConfiguration.getConfigurationSection("ACCEPT_INVITATION"));
        ASK_RETRY = LangFormat.get(yamlConfiguration.getConfigurationSection("ASK_RETRY"));
        REJECT_RETRY = LangFormat.get(yamlConfiguration.getConfigurationSection("REJECT_RETRY"));
        ACCEPT_RETRY = LangFormat.get(yamlConfiguration.getConfigurationSection("ACCEPT_RETRY"));
        WAITING_PLAYER = LangFormat.get(yamlConfiguration.getConfigurationSection("WAITING_PLAYER"));
        BROADCAST_REMAIN_COUNT = LangFormat.get(yamlConfiguration.getConfigurationSection("BROADCAST_REMAIN_COUNT"));
        DRAW = LangFormat.get(yamlConfiguration.getConfigurationSection("DRAW"));
        RESTART = LangFormat.get(yamlConfiguration.getConfigurationSection("RESTART"));
        AVAILABLE_TO_GET_REWARD = LangFormat.get(yamlConfiguration.getConfigurationSection("AVAILABLE_TO_GET_REWARD"));
        NEED_TO_EMPTY_SPACE = LangFormat.get(yamlConfiguration.getConfigurationSection("NEED_TO_EMPTY_SPACE"));
        RANK_FORMAT = LangFormat.get(yamlConfiguration.getConfigurationSection("RANK_FORMAT"));
        START_GAME = LangFormat.get(yamlConfiguration.getConfigurationSection("START_GAME"));
        WAITING_STARTING_GAME = LangFormat.get(yamlConfiguration.getConfigurationSection("WAITING_STARTING_GAME"));
        RESET_GAME = LangFormat.get(yamlConfiguration.getConfigurationSection("RESET_GAME"));
        WAITING_RESET_GAME = LangFormat.get(yamlConfiguration.getConfigurationSection("WAITING_RESET_GAME"));
        NON_AVAILABLE_PLACE = LangFormat.get(yamlConfiguration.getConfigurationSection("NON_AVAILABLE_PLACE"));
        ALREADY_PVP = LangFormat.get(yamlConfiguration.getConfigurationSection("ALREADY_PVP"));
        ALREADY_INVITING = LangFormat.get(yamlConfiguration.getConfigurationSection("ALREADY_INVITING"));
        ALREADY_PVP_SELF = LangFormat.get(yamlConfiguration.getConfigurationSection("ALREADY_PVP_SELF"));
    }

    public static void send(Player player, LangFormat langFormat, Function<String, String> filter) {
        for (String text : langFormat.text) {
            player.sendMessage(filter.apply(text));
        }
        String title = "";
        if (!langFormat.title.equals("")) {
            title = langFormat.title;
        }
        String subtitle = "";
        if (!langFormat.subtitle.equals("")) {
            subtitle = langFormat.subtitle;
        }
        if (!title.equals("") || !subtitle.equals("")) {
            player.sendTitle(filter.apply(title), filter.apply(subtitle), 10, 40, 10);
        }
    }

    public static void sendClickableCommand(Player player, LangFormat langFormat) {
        for (String message : langFormat.text) {
            TextComponent component = null;
            for(String key:commandMap.keySet()) {
                if(!message.contains(key)) continue;
                component = new TextComponent(message.replace(key, commandButton.get(key)));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, commandMap.get(key)));
                break;
            }
            player.spigot().sendMessage(component != null
                    ? component
                    : new TextComponent(message));
        }
    }

    public static class LangFormat {
        private List<String> text;
        private String title;
        private String subtitle;

        public LangFormat(List<String> text, String title, String subtitle) {
            this.text = text;
            this.title = title;
            this.subtitle = subtitle;
        }

        public static LangFormat get(ConfigurationSection section) {
            List<String> text = section.getStringList("text");
            String title = section.getString("title");
            String subtitle = section.getString("subtitle");
            return new LangFormat(text, title, subtitle);
        }
    }

    // init command
    private static void initCommand() {
        initCommandParam("%accept_yes%", "수락");
        initCommandParam("%accept_no%", "거절");
        // retry yes
        initCommandParam("%retry_yes%", "다시하기");
        initCommandParam("%retry_no%", "그만하기");
    }
    private static void initCommandParam(String param, String message) {
        commandMap.put(param,"/pvp "+message);
        commandButton.put(param, message+"버튼");
    }
}
