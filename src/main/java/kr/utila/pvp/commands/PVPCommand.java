package kr.utila.pvp.commands;

import kr.utila.pvp.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.guis.StraightRewardGUI;
import kr.utila.pvp.libraries.SimpleCommandBuilder;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.objects.region.PVPRegion;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PVPCommand {

    public static Map<String, String> inviteData = new HashMap<>();

    public static void register() {
        new SimpleCommandBuilder("pvp")
                .aliases("피브이피")
                .commandExecutor((sender, command, label, args) -> {
                    if (sender instanceof Player player) {
                        // 메시지 없이 명령어만 입력했을 때 사용법 출력
                        if (args.length == 0) {
                            player.sendMessage("/PVP 신청 [닉네임] [메세지]");
                            player.sendMessage("/PVP 수락");
                            player.sendMessage("/PVP 거절");
                            player.sendMessage("/PVP 다시하기");
                            player.sendMessage("/PVP 그만하기");
                            player.sendMessage("/PVP 랭킹");
                            player.sendMessage("/PVP 보상");
                            return false;
                        }
                        // validate command
                        if(args.length < 2) return false;

                        String op = args[0];
                        switch (op) {
                            case "신청" -> {
                                // 준비된 경기장이 없을 때
                                if (!RegionManager.getInstance().hasAvailableSpace()) {
                                    Lang.send(player, Lang.NON_AVAILABLE_PLACE, s -> s);
                                    return false;
                                }
                                String opponentName = args[1];
                                Player opponent = Bukkit.getPlayer(opponentName);
                                // 플레이어 명이 잘못되었을 때
                                if(opponent == null) return false;
                                // 플레이어가 이미 PVP 중일 때
                                if (UserManager.getInstance().get(opponent).getCurrentPVP() != null) {
                                    Lang.send(player, Lang.ALREADY_PVP, s -> s);
                                    return false;
                                }
                                // 플레이어가 이미 초대되었을 때
                                if (inviteData.containsKey(opponent.getUniqueId().toString())) {
                                    Lang.send(player, Lang.ALREADY_INVITING, s -> s);
                                    return false;
                                }
                                // 플레이어가 이미 초대를 보냈을 때
                                if(UserManager.getInstance().get(player).getCurrentPVP() != null) {
                                    Lang.send(player, Lang.ALREADY_PVP_SELF, s -> s);
                                    return false;
                                }
                                // 초대가 성공적이므로 초대 메시지를 보냄
                                StringBuilder message = new StringBuilder();
                                for (int i = 2; i < args.length; i++) message.append(args[i]).append(" ");
                                opponent.sendMessage(message.toString());
                                opponent.sendTitle(message.toString(), "", 10, 40, 10);
                                Lang.sendClickableCommand(opponent, Lang.ACCEPT_INVITATION);
                                player.sendMessage("§7[보낸요청] " + message);

                                // 초대 데이터 저장
                                inviteData.put(opponent.getUniqueId().toString(), player.getUniqueId().toString());

                                // 기다리다가 초대 취소
                                int inviteCancelTime = 30;
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        if (inviteData.containsKey(opponent.getUniqueId().toString())) {
                                            if (inviteData.get(opponent.getUniqueId().toString()).equals(player.getUniqueId().toString())) {
                                                inviteData.remove(opponent.getUniqueId().toString());
                                            }
                                        }
                                    }
                                }.runTaskLater(Main.getInstance(), 20 * inviteCancelTime);
                                return false;
                            }
                            case "거절" -> {
                                if (inviteData.containsKey(player.getUniqueId().toString())) {
                                    Player invitationSender = Bukkit.getPlayer(UUID.fromString(inviteData.get(player.getUniqueId().toString())));
                                    inviteData.remove(player.getUniqueId().toString());
                                    Lang.send(player, Lang.REJECT_REQUEST_RECEIVER, s -> s.replaceAll("%player%", invitationSender.getName()));
                                    Lang.send(invitationSender, Lang.REJECT_REQUEST_SENDER, s -> s.replaceAll("%player%", player.getName()));
                                }
                                return false;
                            }
                            case "수락" -> {
                                if (inviteData.containsKey(player.getUniqueId().toString())) {
                                    if (!RegionManager.getInstance().hasAvailableSpace()) {
                                        Lang.send(player, Lang.NON_AVAILABLE_PLACE, s -> s);
                                        return false;
                                    }
                                    PVPRegion pvpRegion = RegionManager.getInstance().getAvailableRegion();
                                    pvpRegion.regionPlayerUniqueIdMap.clear();
                                    pvpRegion.setTeamToPlayer(TeamType.RED, player);
                                    pvpRegion.setTeamToPlayer(TeamType.BLUE, Bukkit.getPlayer(UUID.fromString(inviteData.get(player.getUniqueId().toString()))));
                                    pvpRegion.start();
                                    inviteData.remove(player.getUniqueId().toString());
                                    return false;
                                }
                                return false;
                            }
                            case "다시하기" -> {
                                User user = UserManager.getInstance().get(player);
                                if (user.getCurrentPVP() == null) {
                                    return false;
                                }
                                PVPRegion pvpRegion = RegionManager.getInstance().get(user.getCurrentPVP());
                                if (!pvpRegion.isRetry()) {
                                    return false;
                                }
                                pvpRegion.isAcceptedMap.put(pvpRegion.getTeam(player), true);
                                Lang.send(player, Lang.ACCEPT_RETRY, s -> s.replaceAll("%player%", player.getName()));
                                for (boolean isAccepted : pvpRegion.isAcceptedMap.values()) if (!isAccepted) return false;
                                pvpRegion.restart();
                                return false;
                            }
                            case "그만하기" -> {
                                User user = UserManager.getInstance().get(player);
                                if (user.getCurrentPVP() == null) {
                                    return false;
                                }
                                PVPRegion pvpRegion = RegionManager.getInstance().get(user.getCurrentPVP());
                                if (!pvpRegion.isRetry()) {
                                    if (pvpRegion.isDelaying()) {
                                        OfflinePlayer offlinePlayer;
                                        TeamType teamType = pvpRegion.getTeam(player);
                                        if (teamType == TeamType.BLUE) {
                                            offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(pvpRegion.regionPlayerUniqueIdMap.get(TeamType.RED)));
                                        } else {
                                            offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(pvpRegion.regionPlayerUniqueIdMap.get(TeamType.BLUE)));
                                        }
                                        pvpRegion.cancelByLogout(offlinePlayer);
                                        return false;
                                    }
                                    return false;
                                }
                                pvpRegion.cancelByReject(player);
                                return false;
                            }
                            case "보상" -> {
                                StraightRewardGUI.open(player);
                                return false;
                            }
                            case "랭킹" -> {
                                UserManager.getInstance().getRanking(player);
                                return false;
                            }
                        }
                        return false;
                    }
                    return false;
                })
                .tabCompleter((sender, command, label, args) -> {
                    switch (args.length) {
                        case 1 -> {
                            return List.of("신청", "수락", "거절", "다시하기", "그만하기", "랭킹", "보상");
                        }
                        case 2 -> {
                            if (args[0].equals("신청")) {
                                List<String> players = new ArrayList<>();
                                for (Player online : Bukkit.getServer().getOnlinePlayers()) {
                                    players.add(online.getName());
                                }
                                return players;
                            }
                        }
                    }
                    return null;
                })
                .register();
    }

}
