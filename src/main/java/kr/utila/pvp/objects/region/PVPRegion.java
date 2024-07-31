package kr.utila.pvp.objects.region;

import kr.utila.pvp.Config;
import kr.utila.pvp.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RewardManager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.objects.Writable;
import kr.utila.pvp.utils.BossBarEntity;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class PVPRegion implements Writable {
    Logger logger = Logger.getLogger("PVPRegion");
    private static final String COMMANDS="commands";
    private static final String GAME_STATUS = "gameStatus";
    private static final String DEFAULT_GAME_STATUS = GameStatus.NOT_STARTED.name();

    private static final File DIRECTORY = new File(Main.getInstance().getDataFolder(), "pvp_regions");

    private final String name;
    private final LocationDTO pos1;
    private final LocationDTO pos2;
    public final Map<TeamType, TeamRegion> teamRegionMap;
    public  final Map<TeamType, String> regionPlayerUniqueIdMap;
    private GameStatus gameStatus;
    public Map<TeamType, Boolean> isAcceptedMap;
    public int remainSecond;
    private final List<String> commands;
    public BossBarEntity bossBarEntity;
    private GameStatus priorGameStatus;

    private int second = 0;

    public PVPRegion(String name, LocationDTO pos1, LocationDTO pos2) {
        this(name, pos1, pos2, new HashMap<>(), new HashMap<>(), GameStatus.NOT_STARTED, 0, new ArrayList<>());
    }
    public PVPRegion(String name, LocationDTO pos1, LocationDTO pos2, Map<TeamType, TeamRegion> teamRegionMap, Map<TeamType, String> regionPlayerUniqueIdMap, GameStatus gameStatus, int remainSecond, List<String> commands) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.teamRegionMap = teamRegionMap;
        this.regionPlayerUniqueIdMap = regionPlayerUniqueIdMap;
        this.gameStatus = gameStatus;
        this.remainSecond = remainSecond;
        this.commands = commands;
    }
    public GameStatus getGameStatus() {
        return gameStatus;
    }
    public void setTeamToPlayer(TeamType teamType, Player player) {
        regionPlayerUniqueIdMap.put(teamType, player.getUniqueId().toString());
    }
    public Optional<TeamType> getTeam(@NotNull Player player) {
        if (!containsPlayer(player)) return Optional.empty();
        return Optional.of(regionPlayerUniqueIdMap.get(TeamType.RED).equals(player.getUniqueId().toString())
                ? TeamType.RED
                : TeamType.BLUE);
    }
    // 해당 region 에 플레이어가 있는지 확인
    public boolean containsPlayer(Player player) {
        return regionPlayerUniqueIdMap.containsValue(player.getUniqueId().toString());
    }
    public void restart() {
        prepareMatch(false);
    }
    public void start() {
        prepareMatch(true);
    }
    public void waitPlayer(Player quitPlayer) {
        pauseMatch();
        getOpponent(quitPlayer).ifPresent(opponent -> {
            // 상대 플레이어를 스타팅 지역으로 이동
            teleportToStartingLocation(opponent);
            // 나간 플레이어가 다시 접속할 때까지 대기
            waitPlayerTimer(opponent, quitPlayer);
        });
    }
    public void pause() {
        pauseMatch();
    }
    public void resume() {
        // 플레이어들에게 경기 재개 알림 메시지 전송
        getPlayers().forEach(player -> Lang.send(player, Lang.RESTART, s -> s.replaceAll("%player%", player.getName())));
        resumeMatch();
    }
    public void endMatch() {
        // 게임중이 아니었다면 종료
        if (!gameStatus.equals(GameStatus.MATCH_IN_PROGRESS)) return;
        gameStatus = GameStatus.MATCH_REPLAY_REQUESTED;

        Player player1 = Bukkit.getPlayer(UUID.fromString(regionPlayerUniqueIdMap.get(TeamType.RED)));
        Player player2 = Bukkit.getPlayer(UUID.fromString(regionPlayerUniqueIdMap.get(TeamType.BLUE)));

        if (player1 == null || player2 == null) {
            // 플레이어를 찾을 수 없으면 게임 취소
            cancelGame();
            return;
        }

        double player1Health = player1.getHealth();
        double player2Health = player2.getHealth();

        // 승부 결정
        if (player1Health > player2Health) {
            askRestartWhenNotDraw(player1, player2);
        } else if (player2Health > player1Health) {
            askRestartWhenNotDraw(player2, player1);
        } else {
            askRestartWhenDraw();
        }
    }
    public void askRestartWhenNotDraw(Player winner, Player loser) {
        prepareMatchReplayRequest();
        // 승자, 패자 플레이어를 스타팅 지역으로 이동
        teleportToStartingLocation(winner);
        teleportToStartingLocation(loser);
        // 보상 지급
        giveReward(winner, loser);

        Lang.send(winner, Lang.FINISH_WINNER);
        Lang.send(loser, Lang.FINISH_LOSER);
        Lang.send(winner, Lang.ASK_RETRY);
        Lang.send(loser, Lang.ASK_RETRY);
    }
    private void askRestartWhenDraw() {
        prepareMatchReplayRequest();
        // 플레이어들을 스타팅 지역으로 이동
        regionPlayerUniqueIdMap.values().forEach(uuid -> {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));
            teleportToStartingLocation(player);

            Lang.send(player, Lang.DRAW);
            Lang.send(player, Lang.ASK_RETRY);
        });
    }
    public void cancelByLogout(OfflinePlayer offlinePlayer) {
        prepareCancel();

        if (offlinePlayer.getPlayer() == null) return;
        getOpponent(offlinePlayer.getPlayer()).ifPresent(opponent -> {
            // 상대방이 온라인이면 quit()
            UserManager.getInstance().get(opponent).quit();
            Lang.send(opponent, Lang.PVP_CANCEL_BY_LOGOUT, s -> s.replaceAll("%player%", Objects.toString(offlinePlayer.getName(), "")));
        });
    }
    public void cancelByReject(Player rejectingPlayer) {
        prepareCancel();

        regionPlayerUniqueIdMap.values().forEach(uuid -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            // 온라인 플레이어면 quit()
            if(offlinePlayer.getPlayer() != null) UserManager.getInstance().get(offlinePlayer.getPlayer()).quit();
            // 오프라인 플레이어면 거절 메시지 전송
            if (offlinePlayer.isOnline()) Lang.send(offlinePlayer.getPlayer(), Lang.REJECT_RETRY, s -> s.replaceAll("%player%", rejectingPlayer.getName()));
        });
    }
    public void cancelGame() {
        prepareCancel();

        regionPlayerUniqueIdMap.values().forEach(uuid -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            // 온라인 플레이어면 quit()
            if(offlinePlayer.getPlayer() != null) UserManager.getInstance().get(offlinePlayer.getPlayer()).quit();
        });
    }
    @Override
    public void write() {
        final File PATH = new File(DIRECTORY, name + ".yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(PATH);
        // name
        yamlConfiguration.set("name", name);
        // pos
        pos1.writeYAML(yamlConfiguration.createSection("pos1"));
        pos2.writeYAML(yamlConfiguration.createSection("pos2"));
        // teamRegion
        ConfigurationSection teamRegionSection = yamlConfiguration.createSection("regionData");
        teamRegionMap.forEach((teamType, teamRegion) -> {
            teamRegionSection.set(teamType.name() + ".teamType", teamType.name());
            LocationDTO startingLocation = teamRegion.startingLocation;
            if (startingLocation != null) startingLocation.writeYAML(teamRegionSection.createSection(teamType.name() + ".startingLocation"));

            List<ItemStack> itemPackage = teamRegion.itemPackage;
            if (itemPackage != null) for(int i = 0; i < itemPackage.size(); i++) teamRegionSection.set(teamType.name() + ".items." + i, itemPackage.get(i));
        });
        // regionPlayer
        if(gameStatus.isInGame()) {
            ConfigurationSection gamingSection = yamlConfiguration.createSection("regionPlayer");
            regionPlayerUniqueIdMap.forEach((teamType, uuid) -> gamingSection.set(teamType.name(), uuid));
            yamlConfiguration.set("remainSecond", remainSecond);
        }
        // commands
        if(!yamlConfiguration.contains(COMMANDS)) yamlConfiguration.set(COMMANDS, new ArrayList<>(List.of("tellraw @p \"경기장 초기화 예시\"")));
        // gameStatus
        if(!yamlConfiguration.contains(GAME_STATUS)) yamlConfiguration.set(GAME_STATUS, DEFAULT_GAME_STATUS);

        // save
        try {
            yamlConfiguration.save(PATH);
        } catch (Exception e) {
            logger.warning("파일 저장 오류 발생. Region: "+name+" Error: "+e.getMessage());
        }
    }

    public void executeCommands() {
        commands.forEach(command -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                // 오류 로그 발생, 해당 Region 의 이름과 함께 출력
                logger.warning("명령어 오류 발생. Region: "+name+" Command: "+command+" Error: "+e.getMessage());
            }
        });
    }
    public Optional<BossBarEntity> getBossBarEntity() {
        return Optional.ofNullable(bossBarEntity);
    }

    // Team 설정이 되었는지 여부
    public boolean isTeamSet() {
        return teamRegionMap.size() == 2;
    }

    // inner class
    public static class TeamRegion {
        public LocationDTO startingLocation;
        public List<ItemStack> itemPackage;

        public TeamRegion(LocationDTO startingLocation, List<ItemStack> itemPackage) {
            this.startingLocation = startingLocation;
            this.itemPackage = itemPackage;
        }
    }

    // private methods
    private void prepareMatch(boolean isNewGame) {
        // 경기에 참가하는 플레이어 목록
        List<Player> players = getReadyPlayers(isNewGame);
        if (players == null) return;
        // 경기 초기화
        initializeMatch(players);
    }
    private void initializeMatch(@NotNull List<Player> players) {
        gameStatus = GameStatus.MATCH_INITIALIZED;
        executeCommands();
        initializeMatchTimer(players);
    }
    private void initializeMatchTimer(@NotNull List<Player> players) {
        second = Config.MATCH_RESET_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(gameStatus.equals(GameStatus.PAUSED)) return;

                if (second-- <= 0) {
                    players.forEach(player -> Lang.send(player, Lang.RESET_GAME));
                    // 경기 대기
                    waitMatch(players);
                    cancel();
                }
                else players.forEach(player -> Lang.send(player, Lang.WAITING_RESET_GAME, s -> s.replaceAll("%seconds%", second + "")));
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    private void waitMatch(@NotNull List<Player> players) {
        gameStatus = GameStatus.MATCH_WAITING;
        waitMatchTimer(players);
    }
    private void waitMatchTimer(@NotNull List<Player> players) {
        second = Config.MATCH_WAIT_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(gameStatus.equals(GameStatus.PAUSED)) return;

                if (second-- <= 0) {
                    players.forEach(player -> Lang.send(player, Lang.START_GAME));
                    startMatch();
                    cancel();
                }
                else players.forEach(player -> Lang.send(player, Lang.WAITING_STARTING_GAME, s -> s.replaceAll("%seconds%", second + "")));
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    /**
     * 나간 플레이어를 대기시키는 타이머 <br><br>
     * 나간 플레이어가 다시 접속하면 gameStatus 를 MATCH_IN_PROGRESS 로 변경 후 경기 재개 <br><br>
     * 대기시간이 초과하면 gameStatus 를 NOT_STARTED 로 변경 후 종료
     */
    private void waitPlayerTimer(Player waitPlayer, Player quitPlayer) {
        // 나간 플레이어가 다시 접속할 때까지 대기
        second = Config.WAITING_FOR_LEFT_USER_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                // 나갔던 유저가 다시 접속하면 종료
                if (quitPlayer.isOnline()) cancel();
                // 대기해도 안들어오면 경기 종료
                if (second-- <= 0)  {
                    giveReward(waitPlayer, quitPlayer);
                    cancelGame();
                    cancel();
                }
                // 대기중인 플레이어에게 메시지 전송
                else Lang.send(waitPlayer, Lang.WAITING_PLAYER, s -> s.replaceAll("%player%", quitPlayer.getName()).replaceAll("%second%", second + ""));
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    /**
     * 보상 지급
     * @param winPlayer 승리한 플레이어
     * @param losePlayer 패배한 플레이어
     */
    private void giveReward(Player winPlayer, Player losePlayer) {
        User winUser = UserManager.getInstance().get(winPlayer);
        User loseUser = UserManager.getInstance().get(losePlayer);
        winUser.setStraight(winUser.getStraight() + 1);
        loseUser.setStraight(0);
        // 보상 지급을 할 수 있으면 보상 지급
        if (!RewardManager.getInstance().get(winUser.getStraight()).isEmpty() && !winUser.getAcquiredRewards().contains(winUser.getStraight())) {
            Lang.send(winPlayer, Lang.AVAILABLE_TO_GET_REWARD);
        }
    }
    /**
     * 게임 재시작 요청 초기화<br><br>
     * 게임 상태를 MATCH_REPLAY_REQUESTED 로 변경하고 게임 수락 여부 초기화
     */
    private void prepareMatchReplayRequest() {
        gameStatus = GameStatus.MATCH_REPLAY_REQUESTED;
        // 게임 수락 여부 초기화
        isAcceptedMap = new HashMap<>();
    }
    /**
     * 시작 지역으로 이동
     * @param player 플레이어
     */
    private void teleportToStartingLocation(Player player) {
        if(player == null) return;
        getTeam(player).ifPresent(teamType -> player.teleport(teamRegionMap.get(teamType).startingLocation.toLocation()));
    }
    /**
     * 상대방 플레이어를 반환
     * @param player 플레이어
     */
    public Optional<Player> getOpponent(@NotNull Player player) {
        return getTeam(player)
                .map(teamType -> Bukkit.getPlayer(UUID.fromString(regionPlayerUniqueIdMap.get(teamType))));
    }
    /**
     * 게임 취소 준비, 게임 상태를 NOT_STARTED 로 변경하고 보스바 타이머를 중지
     */
    private void prepareCancel() {
        gameStatus = GameStatus.NOT_STARTED;
        bossBarEntity.stop();
    }
    /**
     * 플레이어들을 준비시키고 플레이어 목록을 반환
     */
    private List<Player> getReadyPlayers(boolean isNewGame) {
        // 경기에 참가하는 플레이어 목록
        List<Player> players = new ArrayList<>();
        // 플레이어들 초기화
        for (TeamType teamType: regionPlayerUniqueIdMap.keySet()) {
            String uuid = regionPlayerUniqueIdMap.get(teamType);
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            // 오프라인 플레이어면 취소
            if (!offlinePlayer.isOnline()) {
                cancelByLogout(offlinePlayer);
                return null;
            }
            // 새게임이면 User.start() 호출
            User user = UserManager.getInstance().get(uuid);
            if (isNewGame) user.start(name);

            TeamRegion teamRegion = teamRegionMap.get(teamType);
            Player player = offlinePlayer.getPlayer();
            if(player == null) continue;
            // 인벤토리 초기화
            player.getInventory().clear();
            // 아이템 지급
            if (teamRegion.itemPackage != null) teamRegion.itemPackage.forEach(itemStack -> player.getInventory().addItem(itemStack));
            // 체력, 포만도, 위치 초기화
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            teleportToStartingLocation(player);
            // 플레이어 추가
            players.add(player);
        }
        return players;
    }
    private List<Player> getPlayers() {
        return regionPlayerUniqueIdMap.values().stream()
                .map(UUID::fromString)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 경기 시작
     */
    private void startMatch() {
        remainSecond = Config.MATCH_SECOND;
        gameStatus = GameStatus.MATCH_IN_PROGRESS;
        setBossBarEntity();
    }
    private void setBossBarEntity() {
        bossBarEntity = new BossBarEntity(getPlayers());
        bossBarEntity.start();
    }
    private void resumeMatch() {
        bossBarEntity.start();
        gameStatus = priorGameStatus;
    }
    private void pauseMatch() {
        bossBarEntity.pause();
        priorGameStatus = gameStatus;
        gameStatus = GameStatus.PAUSED;
    }
}
