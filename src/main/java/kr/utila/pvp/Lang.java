package kr.utila.pvp;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.function.Function;

public class Lang {

    private static final File FILE = new File(Main.getInstance().getDataFolder(), "lang.yml");

    public static LangFormat PVP_CANCEL_BY_LOGOUT;
    public static LangFormat REJECT_REQUEST_RECEIVER;
    public static LangFormat REJECT_REQUEST_SENDER;
    public static LangFormat FINISH_WINNER;
    public static LangFormat FINISH_LOSER;
    public static LangFormat ACCEPT_INVITATION;
    public static LangFormat ASK_REPLAY;
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
    // UNFOUNDED_PLAYER
    public static LangFormat PLAYER_NOT_FOUND;
    // CANNOT_INVITE_SELF
    public static LangFormat CANNOT_INVITE_SELF;

    public static void load() {
        Main.getInstance().saveResource("lang.yml", false);
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(FILE);
        initLangFormat(yamlConfiguration);
    }

    public static void send(Player player, LangFormat langFormat) {
        send(player, langFormat, s -> s);
    }

    public static void send(Player player, LangFormat langFormat, Function<String, String> filter) {
        for (String text : langFormat.text) {
            player.sendMessage(filter.apply(text));
        }
        String title = "";
        if (!langFormat.title.isEmpty()) {
            title = langFormat.title;
        }
        String subtitle = "";
        if (!langFormat.subtitle.isEmpty()) {
            subtitle = langFormat.subtitle;
        }
        if (!title.isEmpty() || !subtitle.isEmpty()) {
            player.sendTitle(filter.apply(title), filter.apply(subtitle), 10, 40, 10);
        }
    }

    public static void sendClickableCommand(Player player, LangFormat langFormat) {
        for(String message : langFormat.text) {
            for(String clickable : Config.ClickableValue.buttonNameMap.keySet()) {
                // 메시지에 clickable 이 포함되어 있다면
                if(message.contains(clickable)) {
                    var buttonName = Config.ClickableValue.buttonNameMap.get(clickable);
                    var clickablePath = "%" + clickable + "%";
                    // 메시지에 clickable 을 버튼 이름으로 바꿈
                    var component = new TextComponent(TextComponent.fromLegacyText(message.replaceAll(clickablePath,buttonName == null ? "버튼" : buttonName)));
                    // 클릭 이벤트로 설정
                    component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, Config.ClickableValue.commandMap.get(clickable)));
                    // 메시지 전송
                    player.spigot().sendMessage(component);
                    break;
                }
            }
        }
    }

    public static class LangFormat {
        private final List<String> text;
        private final String title;
        private final String subtitle;

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

    private static void initLangFormat(YamlConfiguration yamlConfiguration) {
        if (yamlConfiguration == null) return;

        PVP_CANCEL_BY_LOGOUT = getNotNullLangFormat(yamlConfiguration, "PVP_CANCEL_BY_LOGOUT");
        REJECT_REQUEST_RECEIVER = getNotNullLangFormat(yamlConfiguration, "REJECT_REQUEST_RECEIVER");
        REJECT_REQUEST_SENDER = getNotNullLangFormat(yamlConfiguration, "REJECT_REQUEST_SENDER");
        FINISH_WINNER = getNotNullLangFormat(yamlConfiguration, "FINISH_WINNER");
        FINISH_LOSER = getNotNullLangFormat(yamlConfiguration, "FINISH_LOSER");
        ACCEPT_INVITATION = getNotNullLangFormat(yamlConfiguration, "ACCEPT_INVITATION");
        ASK_REPLAY = getNotNullLangFormat(yamlConfiguration, "ASK_REPLAY");
        REJECT_RETRY = getNotNullLangFormat(yamlConfiguration, "REJECT_RETRY");
        ACCEPT_RETRY = getNotNullLangFormat(yamlConfiguration, "ACCEPT_RETRY");
        WAITING_PLAYER = getNotNullLangFormat(yamlConfiguration, "WAITING_PLAYER");
        BROADCAST_REMAIN_COUNT = getNotNullLangFormat(yamlConfiguration, "BROADCAST_REMAIN_COUNT");
        DRAW = getNotNullLangFormat(yamlConfiguration, "DRAW");
        RESTART = getNotNullLangFormat(yamlConfiguration, "RESTART");
        AVAILABLE_TO_GET_REWARD = getNotNullLangFormat(yamlConfiguration, "AVAILABLE_TO_GET_REWARD");
        NEED_TO_EMPTY_SPACE = getNotNullLangFormat(yamlConfiguration, "NEED_TO_EMPTY_SPACE");
        RANK_FORMAT = getNotNullLangFormat(yamlConfiguration, "RANK_FORMAT");
        START_GAME = getNotNullLangFormat(yamlConfiguration, "START_GAME");
        WAITING_STARTING_GAME = getNotNullLangFormat(yamlConfiguration, "WAITING_STARTING_GAME");
        RESET_GAME = getNotNullLangFormat(yamlConfiguration, "RESET_GAME");
        WAITING_RESET_GAME = getNotNullLangFormat(yamlConfiguration, "WAITING_RESET_GAME");
        NON_AVAILABLE_PLACE = getNotNullLangFormat(yamlConfiguration, "NON_AVAILABLE_PLACE");
        ALREADY_PVP = getNotNullLangFormat(yamlConfiguration, "ALREADY_PVP");
        ALREADY_INVITING = getNotNullLangFormat(yamlConfiguration, "ALREADY_INVITING");
        ALREADY_PVP_SELF = getNotNullLangFormat(yamlConfiguration, "ALREADY_PVP_SELF");
        PLAYER_NOT_FOUND = getNotNullLangFormat(yamlConfiguration, "UNFOUNDED_PLAYER");
        CANNOT_INVITE_SELF = getNotNullLangFormat(yamlConfiguration, "CANNOT_INVITE_SELF");
    }

    public static LangFormat getNotNullLangFormat(YamlConfiguration yamlConfiguration,String path) {
        var section = yamlConfiguration.getConfigurationSection(path);
        return section == null ? new LangFormat(List.of("해당 언어 포멧을 찾을 수 없습니다. lang.yml 파일을 확인하세요."), "", "") : LangFormat.get(section);
    }
}
