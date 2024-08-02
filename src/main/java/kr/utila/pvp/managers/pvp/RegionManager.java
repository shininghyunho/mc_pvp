package kr.utila.pvp.managers.pvp;

import kr.utila.pvp.Main;
import kr.utila.pvp.managers.Manager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.region.GameStatus;
import kr.utila.pvp.objects.region.PVPRegion;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class RegionManager extends Manager {
    Logger logger = Logger.getLogger(RegionManager.class.getName());

    private static final File DIRECTORY = new File(Main.getInstance().getDataFolder(), "pvp_regions");

    private static final RegionManager INSTANCE = new RegionManager();
    private final Map<String, PVPRegion> PVP_REGIONS = new HashMap<>();

    public static RegionManager getInstance() {
        return INSTANCE;
    }

    private RegionManager() {
        super(Main.getInstance());
    }

    public boolean exists(String name) {
        return PVP_REGIONS.containsKey(name);
    }

    public PVPRegion get(String name) {
        return PVP_REGIONS.get(name);
    }

    public void create(String name, LocationDTO pos1, LocationDTO pos2) {
        PVP_REGIONS.put(name, new PVPRegion(name, pos1, pos2));
        PVP_REGIONS.get(name).write();
    }

    public void setTeamLocation(String name, TeamType team, LocationDTO startingLocation) {
        var pvpRegion = PVP_REGIONS.get(name);
        if(pvpRegion == null) return;

        var teamRegion = pvpRegion.teamRegionMap.get(team);
        if(teamRegion == null) pvpRegion.teamRegionMap.put(team, new PVPRegion.TeamRegion(startingLocation, null));
        else teamRegion.startingLocation = startingLocation;

        pvpRegion.write();
    }

    public void setTeamStartItem(String name, TeamType team, List<ItemStack> itemStacks) {
        PVPRegion pvpRegion = PVP_REGIONS.get(name);
        if(pvpRegion == null) return;

        var teamRegion = pvpRegion.teamRegionMap.get(team);
        if(teamRegion == null) pvpRegion.teamRegionMap.put(team, new PVPRegion.TeamRegion(null, itemStacks));
        else teamRegion.itemPackage = itemStacks;

        pvpRegion.write();
    }

    public List<ItemStack> getTeamStartItem(String name, TeamType team) {
        PVPRegion pvpRegion = PVP_REGIONS.get(name);
        if (!pvpRegion.teamRegionMap.containsKey(team)) {
            return new ArrayList<>();
        }
        return pvpRegion.teamRegionMap.get(team).itemPackage;
    }

    public void delete(String name) {
        try {
            File FILE = new File(DIRECTORY, name + ".yml");
            FILE.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        PVP_REGIONS.remove(name);
    }

    public boolean hasAvailableSpace() {
        return PVP_REGIONS.values().stream()
                .anyMatch(pvpRegion -> pvpRegion.isTeamSet() && !pvpRegion.getGameStatus().isInGame());
    }

    public PVPRegion getAvailableRegion() {
        List<PVPRegion> regions = new ArrayList<>();
        for (PVPRegion pvpRegion : PVP_REGIONS.values()) if (!pvpRegion.getGameStatus().isInGame()) regions.add(pvpRegion);
        return regions.get(new Random().nextInt(regions.size()));
    }

    public List<String> getAllRegionNames() {
        return PVP_REGIONS.keySet().stream().toList();
    }

    public List<PVPRegion> getAllRegions() {
        return new ArrayList<>(PVP_REGIONS.values());
    }

    public void saveAll() {
        for (PVPRegion pvpRegion : PVP_REGIONS.values()) {
            pvpRegion.write();
        }
    }

    @Override
    public void load() {
        PVP_REGIONS.clear();

        if(DIRECTORY.mkdir()) return;
        Optional.ofNullable(DIRECTORY.listFiles()).ifPresent(files -> {
            Arrays.stream(files).forEach(file -> {
                var yaml = YamlConfiguration.loadConfiguration(file);
                // name
                var name = yaml.contains("name")
                        ? yaml.getString("name")
                        : file.getName().replace(".yml", "");
                // commands
                var commands = yaml.contains("commands")
                        ? yaml.getStringList("commands")
                        : new ArrayList<>(List.of("tellraw @p \"경기장 초기화 예시\""));
                // pos
                var pos1Section = yaml.getConfigurationSection("pos1");
                var pos2Section = yaml.getConfigurationSection("pos2");
                if(pos1Section == null || pos2Section == null) return;
                var pos1 = LocationDTO.readYAML(pos1Section);
                var pos2 = LocationDTO.readYAML(pos2Section);

                // teamRegions
                var teamRegions = new HashMap<TeamType, PVPRegion.TeamRegion>();
                if (yaml.contains("teamRegions")) {
                    var teamRegionSection = yaml.getConfigurationSection("teamRegions");
                    if (teamRegionSection != null) {
                        teamRegionSection.getKeys(false).forEach(team -> {
                            var teamType = TeamType.valueOf(team);
                            var startingLocation = teamRegionSection.contains(team + ".startingLocation")
                                    ? LocationDTO.readYAML(Objects.requireNonNull(teamRegionSection.getConfigurationSection(team + ".startingLocation")))
                                    : null;
                            var itemPackage = teamRegionSection.contains(team + ".items")
                                    ? teamRegionSection.getStringList(team + ".items").stream().map(yaml::getItemStack).toList()
                                    : null;
                            teamRegions.put(teamType, new PVPRegion.TeamRegion(startingLocation, itemPackage));
                        });
                    }
                }
                // game status
                var gameStatus = yaml.contains("gameStatus")
                        ? GameStatus.valueOf(yaml.getString("gameStatus"))
                        : GameStatus.NOT_STARTED;
                // regionPlayer
                var regionPlayerMap = new HashMap<TeamType, UUID>();
                if (yaml.contains("regionPlayer")) {
                    var regionPlayerSection = yaml.getConfigurationSection("regionPlayer");
                    if (regionPlayerSection != null) regionPlayerSection.getKeys(false).forEach(team -> {
                        if(regionPlayerSection.getString(team)!=null) {
                            regionPlayerMap.put(TeamType.valueOf(team), UUID.fromString(Objects.requireNonNull(regionPlayerSection.getString(team))));
                        }
                    });
                }
                // PVP_REGIONS 에 추가
                PVP_REGIONS.put(name, new PVPRegion(name, pos1, pos2, teamRegions, regionPlayerMap, gameStatus, commands));
            });
        });
    }
}
