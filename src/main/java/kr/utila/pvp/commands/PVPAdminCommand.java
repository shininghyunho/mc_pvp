package kr.utila.pvp.commands;

import kr.utila.pvp.Config;
import kr.utila.pvp.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.guis.ItemSettingGUI;
import kr.utila.pvp.guis.StraightRewardSettingGUI;
import kr.utila.pvp.libraries.SimpleCommandBuilder;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.managers.pvp.RewardManager;
import kr.utila.pvp.objects.LocationDTO;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PVPAdminCommand {

    public static Map<String, LocationDTO[]> posSettingData = new HashMap<>();

    private static final RegionManager manager = RegionManager.getInstance();

    public static void register() {
        new SimpleCommandBuilder("admin-pvp")
                .aliases("관리자-pvp")
                .permission("op")
                .commandExecutor((sender, command, label, args) -> {
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        if (args.length == 0) {
                            player.sendMessage("/admin-pvp 경기장생성 [이름]");
                            player.sendMessage("/admin-pvp 진영설정 [경기장 이름] [레드/블루]");
                            player.sendMessage("/admin-pvp 아이템설정 [경기장 이름] [레드/블루]");
                            player.sendMessage("/admin-pvp 연승보상설정 [n] (n연승 보상)");
                            player.sendMessage("/admin-pvp 구역설정아이템");
                            player.sendMessage("/admin-pvp 삭제");
                            player.sendMessage("/admin-pvp reload");
                            return false;
                        }
                        switch (args[0]) {
                            case "경기장생성" -> {
                                var regionName = args[1];
                                var pos = posSettingData.get(player.getUniqueId().toString());
                                if (!isValidRegionCreate(args, player,pos)) return false;

                                // 경기장 생성
                                manager.create(regionName, pos[0], pos[1]);
                                player.sendMessage("§a생성 완료");
                                posSettingData.remove(player.getUniqueId().toString());
                                return false;
                            }
                            case "진영설정" -> {
                                if (!isValidRegion(args, player)) return false;
                                var startingLocation = LocationDTO.toLocationDTO(player.getLocation());
                                var regionName = args[1];
                                var team = TeamType.getTeam(args[2]);
                                manager.setTeamLocation(regionName, team, startingLocation);
                                player.sendMessage("§a설정 완료");
                                return false;
                            }
                            case "아이템설정" -> {
                                if (!isValidRegion(args, player)) return false;
                                ItemSettingGUI.open(player, args[1], TeamType.getTeam(args[2]));
                                return false;
                            }
                            case "연승보상설정" -> {
                                if (args.length == 1) {
                                    player.sendMessage("§cn연승을 입력해주세요");
                                    return false;
                                }
                                StraightRewardSettingGUI.open(player, Integer.parseInt(args[1]));
                                return false;
                            }
                            case "구역설정아이템" -> {
                                ItemStack itemStack = new ItemStack(Material.PAPER);
                                ItemMeta itemMeta = itemStack.getItemMeta();
                                itemMeta.setDisplayName("§f구역 설정용 종이");
                                itemMeta.getPersistentDataContainer().set(Main.key, PersistentDataType.STRING, "region-paper");
                                itemMeta.setLore(List.of("§7pos1 - 좌클릭", "§7pos2 - 우클릭"));
                                itemStack.setItemMeta(itemMeta);
                                player.getInventory().addItem(itemStack);
                                return false;
                            }
                            case "삭제" -> {
                                if (args.length == 1) {
                                    player.sendMessage("§c경기장 이름을 입력해주세요");
                                    return false;
                                }
                                manager.delete(args[1]);
                                player.sendMessage("§a삭제 완료");
                                return false;
                            }
                            case "reload" -> {
                                try {
                                    manager.saveAll();
                                    manager.load();
                                    RewardManager.getInstance().load();
                                    Config.load();
                                    Lang.load();
                                    player.sendMessage("§a리로드 완료");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return false;
                            }
                        }
                    }
                    return false;
                })
                .tabCompleter((sender, command, label, args) -> {
                    switch (args.length) {
                        case 1 -> {
                            return List.of("경기장생성", "진영설정", "아이템설정", "연승보상설정", "구역설정아이템", "삭제", "reload");
                        }
                        case 2 -> {
                            if (args[0].equals("진영설정") || args[0].equals("아이템설정") || args[0].equals("삭제")) {
                                return manager.getAllRegionNames();
                            }
                        }
                        case 3 -> {
                            if (args[0].equals("진영설정") || args[0].equals("아이템설정")) {
                                return List.of("레드", "블루");
                            }
                        }
                    }
                    return null;
                })
                .register();
    }

    private static boolean isValidRegion(String[] args, Player player) {
        if (args.length == 1) {
            player.sendMessage("§c경기장 이름을 입력해주세요");
            return false;
        }
        if (args.length == 2) {
            player.sendMessage("§c팀 이름을 입력해주세요");
            return false;
        }
        if (!manager.exists(args[1])) {
            player.sendMessage("§c존재하지 않는 경기장입니다");
            return false;
        }
        return true;
    }

    private static boolean isValidRegionCreate(String[] args, Player player,LocationDTO[] pos) {
        if (!posSettingData.containsKey(player.getUniqueId().toString())) {
            player.sendMessage("§cpos1과 pos2를 설정 후 입력해주세요");
            return false;
        }
        if (pos[0] == null || pos[1] == null) {
            player.sendMessage("§cpos1과 pos2를 설정 후 입력해주세요");
            return false;
        }
        if (args.length == 1) {
            player.sendMessage("§c경기장 이름을 입력해주세요");
            return false;
        }
        var regionName = args[1];
        if (manager.exists(regionName)) {
            player.sendMessage("§c이미 존재하는 경기장입니다");
            return false;
        }
        return true;
    }

}
