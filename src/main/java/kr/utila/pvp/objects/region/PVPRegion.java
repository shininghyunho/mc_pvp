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
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

// 해당 region 에 플레이어가 있는지 확인
// Team 설정이 되었는지 여부
// get name
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
    public  final Map<TeamType, UUID> regionPlayerUUIDMap;
    public final Map<UUID, Vector> playerDirectionMap = new HashMap<>();
    private GameStatus gameStatus;
    private final Set<UUID> replayAcceptPlayers = new HashSet<>();
    private final List<String> commands;
    public BossBarEntity matchBossBar;
    public BossBarEntity matchReplayCancelBossBar;
    public BossBarEntity matchPausedBossBar;
    private GameStatus priorGameStatus;

    private int matchSecond = 0;
    private int pauseSecond = 0;

    public PVPRegion(String name, LocationDTO pos1, LocationDTO pos2) {
        this(name, pos1, pos2, new HashMap<>(), new HashMap<>(), GameStatus.NOT_STARTED, new ArrayList<>());
    }
    public PVPRegion(String name, LocationDTO pos1, LocationDTO pos2, Map<TeamType, TeamRegion> teamRegionMap, Map<TeamType, UUID> regionPlayerUUIDMap, GameStatus gameStatus, List<String> commands) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.teamRegionMap = teamRegionMap;
        this.regionPlayerUUIDMap = regionPlayerUUIDMap;
        this.gameStatus = gameStatus;
        this.commands = commands;
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
    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public Optional<TeamType> getTeam(@NotNull Player player) {
        if (!containsPlayer(player)) return Optional.empty();
        return Optional.of(regionPlayerUUIDMap.get(TeamType.RED).equals(player.getUniqueId())
                ? TeamType.RED
                : TeamType.BLUE);
    }
    public boolean containsPlayer(Player player) {
        return regionPlayerUUIDMap.containsValue(player.getUniqueId());
    }
    public void restart() {
        prepareMatch();
    }
    public void start(Player player1, Player player2) {
        addPlayers(player1, player2);
        setDirection();
        getPlayers().forEach(player -> UserManager.getInstance().get(player).start(name));
        prepareMatch();
    }
    public void waitPlayer(Player quitPlayer) {
        pauseMatch();
        getOpponent(quitPlayer).ifPresent(opponent -> {
            // 상대 플레이어를 스타팅 지역으로 이동
            teleportToStartingLocation(opponent);
            // 나간 플레이어가 다시 접속할 때까지 대기
            waitLeftPlayerTimer(opponent, quitPlayer);
        });
    }
    public void resume() {
        // 플레이어들에게 경기 재개 알림 메시지 전송
        getPlayers().forEach(player -> Lang.send(player, Lang.RESUME));
        resumeMatch();
    }
    public void endMatch() {
        // 게임중이 아니었다면 종료
        if (!gameStatus.equals(GameStatus.MATCH_IN_PROGRESS)) return;
        gameStatus = GameStatus.MATCH_REPLAY_REQUESTED;

        Player player1 = Bukkit.getPlayer(regionPlayerUUIDMap.get(TeamType.RED));
        Player player2 = Bukkit.getPlayer(regionPlayerUUIDMap.get(TeamType.BLUE));

        if (player1 == null || player2 == null) {
            // 플레이어를 찾을 수 없으면 게임 취소
            cancelGame();
            return;
        }

        // boss bar 삭제
        getMatchBossBar().ifPresent(BossBarEntity::clear);

        double player1Health = player1.getHealth();
        double player2Health = player2.getHealth();

        // 승부 결정
        if (player1Health > player2Health) {
            replayWhenNotDraw(player1, player2);
        } else if (player2Health > player1Health) {
            replayWhenNotDraw(player2, player1);
        } else {
            replayWhenDraw();
        }
    }
    public void replayWhenNotDraw(Player winner, Player loser) {
        prepareMatchReplayRequest();
        // 승자, 패자 플레이어를 스타팅 지역으로 이동
        teleportToStartingLocation(winner);
        teleportToStartingLocation(loser);
        // 보상 지급
        giveReward(winner, loser);

        Lang.send(winner, Lang.FINISH_WINNER);
        Lang.send(loser, Lang.FINISH_LOSER);
        Lang.sendClickableCommand(winner, Lang.ASK_REPLAY);
        Lang.sendClickableCommand(loser, Lang.ASK_REPLAY);
    }
    private void replayWhenDraw() {
        prepareMatchReplayRequest();
        // 플레이어들을 스타팅 지역으로 이동
        regionPlayerUUIDMap.values().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            teleportToStartingLocation(player);

            Lang.send(player, Lang.DRAW);
            Lang.sendClickableCommand(player, Lang.ASK_REPLAY);
        });
    }
    public void cancelGameByLogout(OfflinePlayer offlinePlayer) {
        prepareCancel();

        if (offlinePlayer.getPlayer() == null) return;
        getOpponent(offlinePlayer.getPlayer()).ifPresent(opponent -> {
            // 상대방이 온라인이면 quit()
            UserManager.getInstance().get(opponent).quit();
            Lang.send(opponent, Lang.PVP_CANCEL_BY_LOGOUT, s -> s.replaceAll("%player%", Objects.toString(offlinePlayer.getName(), "")));
        });
    }
    public void cancelGameByReject(Player rejectingPlayer) {
        prepareCancel();

        regionPlayerUUIDMap.values().forEach(uuid -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            // 온라인 플레이어면 quit()
            if(offlinePlayer.getPlayer() != null) UserManager.getInstance().get(offlinePlayer.getPlayer()).quit();
            // 오프라인 플레이어면 거절 메시지 전송
            if (offlinePlayer.isOnline()) Lang.send(offlinePlayer.getPlayer(), Lang.REJECT_RETRY, s -> s.replaceAll("%player%", rejectingPlayer.getName()));
        });
    }
    public void cancelGame() {
        prepareCancel();

        getPlayers().forEach(player -> {
                    Lang.send(player, Lang.GAME_TERMINATED);
                    UserManager.getInstance().get(player).quit();
                });
    }
    @Override
    public void write() {
        if(name == null) return;
        final File PATH = new File(DIRECTORY, name + ".yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(PATH);
        // name
        yamlConfiguration.set("name", name);
        // pos
        pos1.writeYAML(yamlConfiguration.createSection("pos1"));
        pos2.writeYAML(yamlConfiguration.createSection("pos2"));
        // teamRegion
        var teamRegionSection = yamlConfiguration.createSection("teamRegions");
        for(TeamType teamType : teamRegionMap.keySet()) {
            var teamRegion = teamRegionMap.get(teamType);
            if(teamRegion == null) continue;

            teamRegionSection.set(teamType.name() + ".teamType", teamType.name());
            // startingLocation
            var startingLocation = teamRegion.startingLocation;
            if (startingLocation != null) startingLocation.writeYAML(teamRegionSection.createSection(teamType.name() + ".startingLocation"));
            // items
            var itemPackage = teamRegion.itemPackage;
            if (itemPackage != null) for(int i = 0; i < itemPackage.size(); i++) teamRegionSection.set(teamType.name() + ".items." + i, itemPackage.get(i));
        }
        // regionPlayer
        if(gameStatus.isInGame()) {
            ConfigurationSection gamingSection = yamlConfiguration.createSection("regionPlayer");
            regionPlayerUUIDMap.forEach((teamType, uuid) -> gamingSection.set(teamType.name(), uuid));
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
    public Optional<BossBarEntity> getMatchBossBar() {
        return Optional.ofNullable(matchBossBar);
    }
    public Optional<BossBarEntity> getMatchReplayCancelBossBar() {
        return Optional.ofNullable(matchReplayCancelBossBar);
    }
    public Optional<BossBarEntity> getMatchPausedBossBar() {
        return Optional.ofNullable(matchPausedBossBar);
    }
    public void replayAccept(Player player) {
        replayAcceptPlayers.add(player.getUniqueId());
        if (replayAcceptPlayers.size() == 2) {
            replayAcceptPlayers.clear();
            Lang.send(player, Lang.ACCEPT_RETRY, s -> s.replaceAll("%player%", player.getName()));
            restart();
        }
    }
    public boolean isTeamSet() {
        return teamRegionMap.size() == 2;
    }
    public String getName() {
        return name;
    }
    // private methods
    private void prepareMatch() {
        setPlayersReady();
        // 경기 초기화
        initializeMatch();
        // matchReplayCancelBossBar 초기화
        getMatchReplayCancelBossBar().ifPresent(BossBarEntity::clear);
    }
    private void initializeMatch() {
        gameStatus = GameStatus.MATCH_INITIALIZED;
        replayAcceptPlayers.clear();
        executeCommands();
        initializeMatchTimer();
    }
    private void initializeMatchTimer() {
        matchSecond = Config.MATCH_INITIALIZED_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(gameStatus.equals(GameStatus.PAUSED)) return;
                getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
                if(!gameStatus.equals(GameStatus.MATCH_INITIALIZED)) {
                    cancel();
                }

                if (matchSecond-- <= 0) {
                    getPlayers().forEach(player -> Lang.send(player, Lang.RESET_GAME));
                    // 경기 대기
                    waitMatch();
                    cancel();
                }
                else getPlayers().forEach(player -> Lang.send(player, Lang.WAITING_RESET_GAME, s -> s.replaceAll("%seconds%", matchSecond + "")));
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    private void waitMatch() {
        gameStatus = GameStatus.MATCH_WAITING;
        waitMatchTimer();
    }
    private void waitMatchTimer() {
        matchSecond = Config.MATCH_WAITING_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(gameStatus.equals(GameStatus.PAUSED)) return;
                getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
                if(!gameStatus.equals(GameStatus.MATCH_WAITING)) {
                    cancel();
                }

                if (matchSecond-- <= 0) {
                    getPlayers().forEach(player -> Lang.send(player, Lang.START_GAME));
                    startMatch();
                    cancel();
                }
                else getPlayers().forEach(player -> Lang.send(player, Lang.WAITING_STARTING_GAME, s -> s.replaceAll("%seconds%", matchSecond + "")));
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    /**
     * 경기 시작
     */
    private void startMatch() {
        gameStatus = GameStatus.MATCH_IN_PROGRESS;
        startMatchTimer();
    }
    private void startMatchTimer() {
        matchSecond = Config.MATCH_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(gameStatus.equals(GameStatus.PAUSED)) return;
                getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
                if(!gameStatus.equals(GameStatus.MATCH_IN_PROGRESS)) {
                    getMatchBossBar().ifPresent(BossBarEntity::clear);
                    cancel();
                }
                matchSecond--;
                setMatchBossBar(Config.MATCH_IN_PROGRESS_BOSSBAR_TITLE.replaceAll("%second%",String.valueOf(matchSecond)), matchSecond);
                if (matchSecond <= 0) {
                    endMatch();
                    getMatchBossBar().ifPresent(BossBarEntity::clear);
                    cancel();
                }
                else if(matchSecond % 10 == 0) getPlayers().forEach(player -> Lang.send(player, Lang.BROADCAST_REMAIN_COUNT, s -> s.replaceAll("%second%", matchSecond + "")));
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    private void replayMatchCancelTimer() {
        matchSecond = Config.MATCH_REPLAY_CANCEL_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(gameStatus.equals(GameStatus.PAUSED)) return;
                getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
                if(!gameStatus.equals(GameStatus.MATCH_REPLAY_REQUESTED)) {
                    getMatchReplayCancelBossBar().ifPresent(BossBarEntity::clear);
                    cancel();
                }
                matchSecond--;
                setMatchReplayCancelBossBar(Config.MATCH_REPLAY_CANCEL_BOSSBAR_TITLE.replaceAll("%second%",String.valueOf(matchSecond)), matchSecond);
                if (matchSecond <= 0) {
                    cancelGame();
                    getMatchReplayCancelBossBar().ifPresent(BossBarEntity::clear);
                    cancel();
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
    /**
     * 나간 플레이어를 대기시키는 타이머 <br><br>
     * 나간 플레이어가 다시 접속하면 gameStatus 를 MATCH_IN_PROGRESS 로 변경 후 경기 재개 <br><br>
     * 대기시간이 초과하면 gameStatus 를 NOT_STARTED 로 변경 후 종료
     */
    private void waitLeftPlayerTimer(Player waitPlayer, Player quitPlayer) {
        // 나간 플레이어가 다시 접속할 때까지 대기
        pauseSecond = Config.WAITING_FOR_LEFT_USER_SECOND;
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!gameStatus.equals(GameStatus.PAUSED)) {
                    getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
                    cancel();
                }

                // 대기해도 안들어오면 경기 종료
                pauseSecond--;
                setMatchPausedBossBar(Config.WAITING_FOR_LEFT_USER_BOSSBAR_TITLE.replaceAll("%second%",String.valueOf(pauseSecond)), pauseSecond);
                if(pauseSecond <= 0)  {
                    giveReward(waitPlayer, quitPlayer);
                    cancelGame();
                    getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
                    cancel();
                }
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
        replayAcceptPlayers.clear();
        // 매치 재시작 취소 타이머 시작
        replayMatchCancelTimer();
    }
    /**
     * 시작 지역으로 이동
     * @param player 플레이어
     */
    private void teleportToStartingLocation(Player player) {
        if(player == null) return;
        getTeam(player).ifPresent(teamType -> player.teleport(teamRegionMap.get(teamType).startingLocation.toLocation()));
        // 시야값 설정
        var vector = playerDirectionMap.get(player.getUniqueId());
        if(vector != null) player.getLocation().setDirection(vector);
    }
    /**
     * 상대방 플레이어를 반환
     * @param player 플레이어
     */
    public Optional<Player> getOpponent(@NotNull Player player) {
        return getTeam(player)
                .map(teamType -> Bukkit.getPlayer(regionPlayerUUIDMap.get(teamType)));
    }
    /**
     * 게임 취소 준비, 게임 상태를 NOT_STARTED 로 변경하고 보스바 타이머를 중지
     */
    private void prepareCancel() {
        gameStatus = GameStatus.NOT_STARTED;
        getMatchBossBar().ifPresent(BossBarEntity::clear);
        // 상태 변수 초기화
        regionPlayerUUIDMap.clear();
        replayAcceptPlayers.clear();
    }
    /**
     * 플레이어들을 준비시키고 플레이어 목록을 반환
     */
    private void setPlayersReady() {
        // 플레이어들 초기화
        for(Player player : getPlayers()) {
            if(player == null) continue;
            // 인벤토리 초기화
            player.getInventory().clear();
            // 아이템 지급
            var teamType = getTeam(player).orElse(null);
            if(teamType == null) continue;
            var teamRegion = teamRegionMap.get(teamType);
            if (teamRegion.itemPackage != null) teamRegion.itemPackage.forEach(itemStack -> player.getInventory().addItem(itemStack));
            // 체력, 포만도, 위치 초기화
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            // 플레이어 위치 초기화
            teleportToStartingLocation(player);
        }
    }
    public List<Player> getPlayers() {
        return regionPlayerUUIDMap.values().stream()
                .filter(Objects::nonNull)
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }
    private void resumeMatch() {
        getMatchPausedBossBar().ifPresent(BossBarEntity::clear);
        gameStatus = priorGameStatus;
    }
    private void pauseMatch() {
        clearBossBar();
        priorGameStatus = gameStatus;
        gameStatus = GameStatus.PAUSED;
    }
    private void clearBossBar() {
        switch (gameStatus) {
            case MATCH_IN_PROGRESS -> getMatchBossBar().ifPresent(BossBarEntity::clear);
            case MATCH_REPLAY_REQUESTED -> getMatchReplayCancelBossBar().ifPresent(BossBarEntity::clear);
        }
    }
    private void setMatchBossBar(String title, int second) {
        if(matchBossBar != null) matchBossBar.clear();
        if(!gameStatus.equals(GameStatus.MATCH_IN_PROGRESS)) return;
        matchBossBar = new BossBarEntity(getPlayers(), title, second, Config.MATCH_SECOND, BarColor.GREEN, BarStyle.SOLID);
    }
    private void setMatchReplayCancelBossBar(String title, int second) {
        if(matchReplayCancelBossBar != null) matchReplayCancelBossBar.clear();
        if(!gameStatus.equals(GameStatus.MATCH_REPLAY_REQUESTED)) return;
        matchReplayCancelBossBar = new BossBarEntity(getPlayers(), title, second, Config.MATCH_REPLAY_CANCEL_SECOND, BarColor.YELLOW, BarStyle.SOLID);
    }
    private void setMatchPausedBossBar(String title, int second) {
        if(matchPausedBossBar != null) matchPausedBossBar.clear();
        if(!gameStatus.equals(GameStatus.PAUSED)) return;
        matchPausedBossBar = new BossBarEntity(getPlayers(), title, second, Config.WAITING_FOR_LEFT_USER_SECOND, BarColor.WHITE, BarStyle.SOLID);
    }
    private void setDirection() {
        // set direction map
        getPlayers().forEach(player -> playerDirectionMap.put(player.getUniqueId(), player.getLocation().getDirection()));
    }
    private void addPlayers(Player player1,Player player2) {
        // 랜덤으로 팀 선정
        List<TeamType> teamTypes = new ArrayList<>() {{
            add(TeamType.RED);
            add(TeamType.BLUE);
        }};
        Collections.shuffle(teamTypes);
        regionPlayerUUIDMap.put(teamTypes.get(0), player1.getUniqueId());
        regionPlayerUUIDMap.put(teamTypes.get(1), player2.getUniqueId());
    }
}
