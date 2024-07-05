package kr.utila.pvp.managers.pvp;

import kr.utila.pvp.Main;
import kr.utila.pvp.managers.Manager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.region.PVPRegion;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

public class RegionManager extends Manager {

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
        PVPRegion pvpRegion = PVP_REGIONS.get(name);
        if (!pvpRegion.getRegionData().containsKey(team)) {
            pvpRegion.getRegionData().put(team, new PVPRegion.TeamRegion(team, startingLocation, null));
            return;
        }
        pvpRegion.getRegionData().get(team).setStartingLocation(startingLocation);
        pvpRegion.write();
    }

    public void setTeamStartItem(String name, TeamType team, List<ItemStack> itemStacks) {
        PVPRegion pvpRegion = PVP_REGIONS.get(name);
        if (!pvpRegion.getRegionData().containsKey(team)) {
            pvpRegion.getRegionData().put(team, new PVPRegion.TeamRegion(team, null, itemStacks));
            return;
        }
        pvpRegion.getRegionData().get(team).setItemPackage(itemStacks);
        pvpRegion.write();
    }

    public List<ItemStack> getTeamStartItem(String name, TeamType team) {
        PVPRegion pvpRegion = PVP_REGIONS.get(name);
        if (!pvpRegion.getRegionData().containsKey(team)) {
            return new ArrayList<>();
        }
        return pvpRegion.getRegionData().get(team).getItemPackage();
    }

    public void delete(String name) {
        File FILE = new File(DIRECTORY, name + ".yml");
        FILE.delete();
        PVP_REGIONS.remove(name);
    }

    public boolean hasAvailableSpace() {
        for (PVPRegion pvpRegion : PVP_REGIONS.values()) {
            if (!pvpRegion.isGaming()) {
                return true;
            }
        }
        return false;
    }

    public PVPRegion getAvailableRegion() {
        List<PVPRegion> regions = new ArrayList<>();
        for (PVPRegion pvpRegion : PVP_REGIONS.values()) {
            if (!pvpRegion.isGaming()) {
                regions.add(pvpRegion);
            }
        }
        return regions.get(new Random().nextInt(regions.size()));
    }

    public List<String> getAllRegions() {
        return PVP_REGIONS.keySet().stream().toList();
    }

    public void saveAll() {
        for (PVPRegion pvpRegion : PVP_REGIONS.values()) {
            pvpRegion.write();
        }
    }

    @Override
    public void load() throws Exception {
        PVP_REGIONS.clear();
        if (!DIRECTORY.exists()) {
            DIRECTORY.mkdirs();
            return;
        }
        if (DIRECTORY.listFiles() == null) {
            return;
        }
        for (File file : DIRECTORY.listFiles()) {
            YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
            String name = yamlConfiguration.getString("name");
            // load commands
            List<String> commands = yamlConfiguration.contains("commands")
                    ? yamlConfiguration.getStringList("commands")
                    : new ArrayList<>(List.of("tellraw @p \"경기장 초기화 예시\""));

            LocationDTO pos1 = LocationDTO.readYAML(yamlConfiguration.getConfigurationSection("pos1"));
            LocationDTO pos2 = LocationDTO.readYAML(yamlConfiguration.getConfigurationSection("pos2"));
            Map<TeamType, PVPRegion.TeamRegion> regionData = new HashMap<>();
            if (yamlConfiguration.contains("regionData")) {
                ConfigurationSection teamRegionSection = yamlConfiguration.getConfigurationSection("regionData");
                for (String team : teamRegionSection.getKeys(false)) {
                    TeamType teamType = TeamType.valueOf(team);
                    LocationDTO startingLocation = null;
                    if (teamRegionSection.contains(team + ".startingLocation")) {
                        startingLocation = LocationDTO.readYAML(teamRegionSection.getConfigurationSection(team + ".startingLocation"));
                    }
                    List<ItemStack> itemPackage = null;
                    if (teamRegionSection.contains(team + ".items")) {
                        itemPackage = new ArrayList<>();
                        for (String key : teamRegionSection.getConfigurationSection(team + ".items").getKeys(false)) {
                            itemPackage.add(teamRegionSection.getItemStack(team + ".items." + key));
                        }
                    }
                    regionData.put(teamType, new PVPRegion.TeamRegion(teamType, startingLocation, itemPackage));
                }
            }
            Map<TeamType, String> regionPlayer = new HashMap<>();
            int remainSecond = 0;
            boolean gaming = false;
            if (yamlConfiguration.contains("regionPlayer")) {
                gaming = true;
                ConfigurationSection gamingSection = yamlConfiguration.getConfigurationSection("regionPlayer");
                for (String team : gamingSection.getKeys(false)) {
                    regionPlayer.put(TeamType.valueOf(team), gamingSection.getString(team));
                }
                remainSecond = yamlConfiguration.getInt("remainSecond");
            }
            // PVP_REGIONS 에 추가
            PVP_REGIONS.put(name, new PVPRegion(name, pos1, pos2, regionData, regionPlayer, gaming, remainSecond, commands));
        }
    }
}
