package kr.utila.pvp.objects.region;

import kr.utila.pvp.Config;
import kr.utila.pvp.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RewardManager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.objects.Writable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class PVPRegion implements Writable {
    Logger logger = Logger.getLogger("PVPRegion");

    private static final File DIRECTORY = new File(Main.getInstance().getDataFolder(), "pvp_regions");

    private String name;
    private LocationDTO pos1;
    private LocationDTO pos2;
    private Map<TeamType, TeamRegion> regionData;
    private Map<TeamType, String> regionPlayer;
    private boolean gaming;
    private boolean delaying;
    private boolean retry;
    private Map<TeamType, Boolean> acceptingData;
    private int remainSecond;
    private List<String> commands;

    public PVPRegion(String name, LocationDTO pos1, LocationDTO pos2) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.regionData = new HashMap<>();
        this.regionPlayer = new HashMap<>();
    }

    public PVPRegion(String name, LocationDTO pos1, LocationDTO pos2, Map<TeamType, TeamRegion> regionData, Map<TeamType, String> regionPlayer, boolean gaming, int remainSecond
    ,List<String> commands) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.regionData = regionData;
        this.regionPlayer = regionPlayer;
        this.gaming = gaming;
        this.remainSecond = remainSecond;
        this.commands = commands;
    }

    public void setTeamToPlayer(TeamType teamType, Player player) {
        regionPlayer.put(teamType, player.getUniqueId().toString());
    }

    public TeamType getTeam(Player player) {
        if (regionPlayer.get(TeamType.RED).equals(player.getUniqueId().toString())) {
            return TeamType.RED;
        }
        return TeamType.BLUE;
    }

    public void restart() {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<TeamType, String> entry : regionPlayer.entrySet()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue()));
            if (!offlinePlayer.isOnline()) {
                cancel(offlinePlayer);
                return;
            }
            TeamRegion teamRegion = regionData.get(entry.getKey());
            Player player = offlinePlayer.getPlayer();
            player.getInventory().clear();
            if (teamRegion.getItemPackage() != null) {
                for (ItemStack itemStack : teamRegion.getItemPackage()) {
                    player.getInventory().addItem(itemStack);
                }
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.teleport(teamRegion.getStartingLocation().toLocation());
            players.add(player);
        }
        delaying = true;
        gaming = true;
        remainSecond = Config.GAME_TIME;
        int index = 0;
        for (int i = Config.START_COOLTIME; i >= 0; i--) {
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gaming) {
                        return;
                    }
                    if (finalI == 0) {
                        delaying = false;
                        for (Player player : players) {
                            Lang.send(player, Lang.START_GAME, s -> {
                                return s;
                            });
                        }
                    } else {
                        for (Player player : players) {
                            Lang.send(player, Lang.WAITING_STARTING_GAME, s -> s.replaceAll("%seconds%", finalI + ""));
                        }
                    }
                }
            }.runTaskLater(Main.getInstance(), 20 * index);
            index++;
        }
    }

    public void start() {
        List<Player> players = new ArrayList<>();
        for (Map.Entry<TeamType, String> entry : regionPlayer.entrySet()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(entry.getValue()));
            if (!offlinePlayer.isOnline()) {
                cancel(offlinePlayer);
                return;
            }
            User user = UserManager.getInstance().get(entry.getValue());
            user.start(name);
            TeamRegion teamRegion = regionData.get(entry.getKey());
            Player player = offlinePlayer.getPlayer();
            player.getInventory().clear();
            if (teamRegion.getItemPackage() != null) {
                for (ItemStack itemStack : teamRegion.getItemPackage()) {
                    player.getInventory().addItem(itemStack);
                }
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.teleport(teamRegion.getStartingLocation().toLocation());
            players.add(player);
        }
        delaying = true;
        gaming = true;
        remainSecond = Config.GAME_TIME;
        int index = 0;
        for (int i = Config.START_COOLTIME; i >= 0; i--) {
            int finalI = i;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!gaming) {
                        return;
                    }
                    if (finalI == 0) {
                        delaying = false;
                        for (Player player : players) {
                            Lang.send(player, Lang.START_GAME, s -> {
                                return s;
                            });
                        }
                    } else {
                        for (Player player : players) {
                            Lang.send(player, Lang.WAITING_STARTING_GAME, s -> s.replaceAll("%seconds%", finalI + ""));
                        }
                    }
                }
            }.runTaskLater(Main.getInstance(), 20 * index);
            index++;
        }
    }

    public void waitPlayer(Player quitPlayer) {
        delaying = true;
        TeamType joining;
        if (regionPlayer.get(TeamType.RED).equals(quitPlayer.getUniqueId().toString())) {
            joining = TeamType.BLUE;
        } else {
            joining = TeamType.RED;
        }
        Player onlinePlayer = Bukkit.getPlayer(UUID.fromString(regionPlayer.get(joining)));
        onlinePlayer.teleport(regionData.get(joining).getStartingLocation().toLocation());

        int index = 0;
        for (int i = Config.WAITING_PLAYER_SECOND; i >= 0; i--) {
            int finalI = i;

            if (finalI == 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (delaying && !quitPlayer.isOnline()) {
                            gaming = false;
                            delaying = false;

                            User onlineUser = UserManager.getInstance().get(onlinePlayer);
                            User offlineUser = UserManager.getInstance().get(quitPlayer);

                            onlineUser.setStraight(onlineUser.getStraight() + 1);
                            offlineUser.setStraight(0);
                            Lang.send(onlinePlayer, Lang.FINISH_WINNER, s -> s);
                            if (RewardManager.getInstance().get(onlineUser.getStraight()).size() != 0 && !onlineUser.getAcquiredRewards().contains(onlineUser.getStraight())) {
                                Lang.send(onlinePlayer, Lang.AVAILABLE_TO_GET_REWARD, s -> s);
                                return;
                            }

                            onlineUser.quit();
                            offlineUser.quit();

                            regionPlayer.clear();
                        }
                    }
                }.runTaskLater(Main.getInstance(), 20 * index);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Lang.send(onlinePlayer, Lang.WAITING_PLAYER, s -> {
                            return s.replaceAll("%player%", quitPlayer.getName())
                                    .replaceAll("%second%", finalI + "");
                        });
                    }
                }.runTaskLater(Main.getInstance(), 20 * index);
                index++;
            }
        }
    }

    public void askRestart(Player winner, Player loser) {
        delaying = true;
        if (regionPlayer.get(TeamType.BLUE).equals(winner.getUniqueId().toString())) {
            winner.teleport(regionData.get(TeamType.BLUE).getStartingLocation().toLocation());
            loser.teleport(regionData.get(TeamType.RED).getStartingLocation().toLocation());
        } else {
            winner.teleport(regionData.get(TeamType.RED).getStartingLocation().toLocation());
            loser.teleport(regionData.get(TeamType.BLUE).getStartingLocation().toLocation());
        }
        UserManager userManager = UserManager.getInstance();
        User winUser = userManager.get(winner);
        User loseUser = userManager.get(loser);
        winUser.setStraight(winUser.getStraight() + 1);
        loseUser.setStraight(0);
        Lang.send(winner, Lang.FINISH_WINNER, s -> s);
        Lang.send(loser, Lang.FINISH_LOSER, s -> s);
        if (RewardManager.getInstance().get(winUser.getStraight()).size() != 0 && !winUser.getAcquiredRewards().contains(winUser.getStraight())) {
            Lang.send(winner, Lang.AVAILABLE_TO_GET_REWARD, s -> s);
            return;
        }
        Lang.send(winner, Lang.ASK_RETRY, s -> s);
        Lang.send(loser, Lang.ASK_RETRY, s -> s);
        retry = true;
        acceptingData = new HashMap<>();
        acceptingData.put(TeamType.RED, false);
        acceptingData.put(TeamType.BLUE, false);
    }

    public void askRestart() {
        delaying = true;
        for (String uuid : regionPlayer.values()) {
            Player player = Bukkit.getPlayer(UUID.fromString(uuid));

            if (regionPlayer.get(TeamType.BLUE).equals(player.getUniqueId().toString())) {
                player.teleport(regionData.get(TeamType.BLUE).getStartingLocation().toLocation());
            } else {
                player.teleport(regionData.get(TeamType.RED).getStartingLocation().toLocation());
            }
            Lang.send(player, Lang.DRAW, s -> s);
            Lang.send(player, Lang.ASK_RETRY, s -> s);
        }
        retry = true;
        acceptingData = new HashMap<>();
        acceptingData.put(TeamType.RED, false);
        acceptingData.put(TeamType.BLUE, false);
    }

    public void cancel(OfflinePlayer quitPlayer) {
        gaming = false;
        delaying = false;
        Player onlinePlayer;
        OfflinePlayer offlinePlayer = quitPlayer;
        if (regionPlayer.get(TeamType.RED).equals(quitPlayer.getUniqueId().toString())) {
            onlinePlayer = Bukkit.getPlayer(UUID.fromString(regionPlayer.get(TeamType.BLUE)));
        } else {
            onlinePlayer = Bukkit.getPlayer(UUID.fromString(regionPlayer.get(TeamType.RED)));
        }
        Lang.send(onlinePlayer, Lang.PVP_CANCEL_BY_LOGOUT, s -> s.replaceAll("%player%", offlinePlayer.getName()));
        UserManager.getInstance().get(quitPlayer.getUniqueId().toString()).quit();
        UserManager.getInstance().get(onlinePlayer).quit();
        regionPlayer.clear();
    }

    public void cancel(Player rejectingPlayer) {
        gaming = false;
        delaying = false;
        for (String uuid : regionPlayer.values()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            if (offlinePlayer.isOnline()) {
                Lang.send(offlinePlayer.getPlayer(), Lang.REJECT_RETRY, s -> s.replaceAll("%player%", rejectingPlayer.getName()));
            }
            User user = UserManager.getInstance().get(uuid);
            user.quit();
        }
        regionPlayer.clear();
    }

    @Override
    public void write() {
        final File PATH = new File(DIRECTORY, name + ".yml");
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(PATH);
        yamlConfiguration.set("name", name);
        pos1.writeYAML(yamlConfiguration.createSection("pos1"));
        pos2.writeYAML(yamlConfiguration.createSection("pos2"));
        ConfigurationSection teamRegionSection = yamlConfiguration.createSection("regionData");
        for (TeamType teamType : regionData.keySet()) {
            TeamRegion teamRegion = regionData.get(teamType);
            teamRegionSection.set(teamType.name() + ".teamType", teamType.name());
            LocationDTO startingLocation = teamRegion.getStartingLocation();
            if (startingLocation != null) {
                startingLocation.writeYAML(teamRegionSection.createSection(teamType.name() + ".startingLocation"));
            }
            List<ItemStack> itemPackage = teamRegion.getItemPackage();
            if (itemPackage != null) {
                for (int i = 0; i < itemPackage.size(); i++) {
                    teamRegionSection.set(teamType.name() + ".items." + i, itemPackage.get(i));
                }
            }
        }
        if (gaming) {
            ConfigurationSection gamingSection = yamlConfiguration.createSection("regionPlayer");
            for (Map.Entry<TeamType, String> entry : regionPlayer.entrySet()) {
                gamingSection.set(entry.getKey().name(), entry.getValue());
            }
            yamlConfiguration.set("remainSecond", remainSecond);
        }
        // 재시작, 종료시  실행될 명령어
        // commands 설정값이 없으면 새로 생성
        String commands="commands";
        if(!yamlConfiguration.contains(commands)) {
            yamlConfiguration.createSection(commands);
        }
        try {
            yamlConfiguration.save(PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // commands 실행
    public void executeCommands() {
        for (String command : commands) {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                // 오류 로그 발생, 해당 Region 의 이름과 함께 출력
                logger.warning("명령어 오류 발생. Region: "+name+" Command: "+command+" Error: "+e.getMessage());
            }
        }
    }

    public int getRemainSecond() {
        return remainSecond;
    }

    public void setRemainSecond(int remainSecond) {
        this.remainSecond = remainSecond;
    }

    public boolean isRetry() {
        return retry;
    }

    public void setRetry(boolean retry) {
        this.retry = retry;
    }

    public Map<TeamType, Boolean> getAcceptingData() {
        return acceptingData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocationDTO getPos1() {
        return pos1;
    }

    public void setPos1(LocationDTO pos1) {
        this.pos1 = pos1;
    }

    public LocationDTO getPos2() {
        return pos2;
    }

    public void setPos2(LocationDTO pos2) {
        this.pos2 = pos2;
    }

    public Map<TeamType, TeamRegion> getRegionData() {
        return regionData;
    }

    public void setRegionData(Map<TeamType, TeamRegion> regionData) {
        this.regionData = regionData;
    }

    public Map<TeamType, String> getRegionPlayer() {
        return regionPlayer;
    }

    public void setRegionPlayer(Map<TeamType, String> regionPlayer) {
        this.regionPlayer = regionPlayer;
    }

    public boolean isGaming() {
        return gaming;
    }

    public void setGaming(boolean gaming) {
        this.gaming = gaming;
    }

    public boolean isDelaying() {
        return delaying;
    }

    public void setDelaying(boolean delaying) {
        this.delaying = delaying;
    }

    public static class TeamRegion {
        private TeamType teamType;
        private LocationDTO startingLocation;
        private List<ItemStack> itemPackage;

        public TeamRegion(TeamType teamType, LocationDTO startingLocation, List<ItemStack> itemPackage) {
            this.teamType = teamType;
            this.startingLocation = startingLocation;
            this.itemPackage = itemPackage;
        }

        public TeamType getTeamType() {
            return teamType;
        }

        public void setTeamType(TeamType teamType) {
            this.teamType = teamType;
        }

        public LocationDTO getStartingLocation() {
            return startingLocation;
        }

        public void setStartingLocation(LocationDTO startingLocation) {
            this.startingLocation = startingLocation;
        }

        public List<ItemStack> getItemPackage() {
            return itemPackage;
        }

        public void setItemPackage(List<ItemStack> itemPackage) {
            this.itemPackage = itemPackage;
        }
    }

}
