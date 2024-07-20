package kr.utila.pvp.config;

import kr.utila.pvp.Main;
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
    public static LangFormat PVP_CANCEL_BY_LOGOUT;
    public static LangFormat REJECT_REQUEST_RECEIVER;
    public static LangFormat REJECT_REQUEST_SENDER;
    public static LangFormat FINISH_WINNER;
    public static LangFormat FINISH_LOSER;
    public static LangFormat ACCEPT_INVITATION;
    public static LangFormat ASK_RETRY_INFO;
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

    public static void load() {
        // Lang 로딩
        String fileName = "lang.yml";
        Main.getInstance().saveResource(fileName, false);
        initLangFormat(YamlConfiguration.loadConfiguration(new File(Main.getInstance().getDataFolder(), fileName)));
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
            for(String key: Config.ClickMessage.clickCommandMap.keySet()) {
                if(!message.contains(key)) continue;
                String clickMessage = Config.ClickMessage.clickMessageMap.get(key);
                var key_par= "%" + key + "%";
                component = new TextComponent(TextComponent.fromLegacyText(message.replace(key_par, clickMessage == null ? "" : clickMessage)));
                component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Config.ClickMessage.clickCommandMap.get(key)));
                break;
            }
            player.spigot().sendMessage(component != null
                    ? component
                    : new TextComponent(TextComponent.fromLegacyText(message)));
        }
    }

    private static void initLangFormat(YamlConfiguration yamlConfiguration) {
        if(yamlConfiguration == null) return;

        PVP_CANCEL_BY_LOGOUT = LangFormat.get(yamlConfiguration.getConfigurationSection("PVP_CANCEL_BY_LOGOUT"));
        REJECT_REQUEST_RECEIVER = LangFormat.get(yamlConfiguration.getConfigurationSection("REJECT_REQUEST_RECEIVER"));
        REJECT_REQUEST_SENDER = LangFormat.get(yamlConfiguration.getConfigurationSection("REJECT_REQUEST_SENDER"));
        FINISH_WINNER = LangFormat.get(yamlConfiguration.getConfigurationSection("FINISH_WINNER"));
        FINISH_LOSER = LangFormat.get(yamlConfiguration.getConfigurationSection("FINISH_LOSER"));
        ACCEPT_INVITATION = LangFormat.get(yamlConfiguration.getConfigurationSection("ACCEPT_INVITATION"));
        ASK_RETRY_INFO = LangFormat.get(yamlConfiguration.getConfigurationSection("ASK_RETRY_INFO"));
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
            if(section == null) return null;

            List<String> text = section.getStringList("text");
            String title = section.getString("title");
            String subtitle = section.getString("subtitle");
            return new LangFormat(text, title, subtitle);
        }
    }
}
