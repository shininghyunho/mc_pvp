package kr.utila.pvp.commands;

import kr.utila.pvp.Lang;
import kr.utila.pvp.Main;
import kr.utila.pvp.guis.StraightRewardGUI;
import kr.utila.pvp.libraries.SimpleCommandBuilder;
import kr.utila.pvp.managers.UserManager;
import kr.utila.pvp.managers.pvp.RegionManager;
import kr.utila.pvp.objects.User;
import kr.utila.pvp.objects.region.GameStatus;
import kr.utila.pvp.objects.region.PVPRegion;
import kr.utila.pvp.objects.region.TeamType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class PVPCommand {
    /**
     * <초대받은 플레이어, 초대한 플레이어> 매핑
     */
    public static Map<String, String> inviterMap = new HashMap<>();
    /**
     * <초대한 플레이어, 초대받은 플레이어> 매핑
     */
    public static Map<String, String> inviteeMap = new HashMap<>();

    // PVP 자동 삭제 시간
    public static final int PVP_EXPIRE_TIME = 20;

    private static final List<String> commands = List.of("신청", "수락", "거절", "다시하기", "그만하기", "랭킹", "보상");
    public static void register() {
        new SimpleCommandBuilder("pvp")
                .aliases("피브이피")
                .commandExecutor((sender, command, label, args) -> {
                    if(!(sender instanceof Player player)) return false;

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
                            Player inviter = player;
                            String inviteeName = args[1];
                            Player invitee = Bukkit.getPlayer(inviteeName);
                            if(invitee == null) return false;
                            if (!isValidInvite(inviter, invitee)) break;

                            // 초대가 성공적이므로 초대 메시지를 보냄
                            sendInviteMessage(args, invitee);

                            // 초대 데이터 저장
                            invite(inviter, invitee);

                            expireInviteTimer(inviter,invitee);
                        }
                        case "거절" -> {
                            Player invitee = player;
                            // 초대 된 적이 없다면 break
                            if(!isInvitee(invitee)) break;

                            Player inviter = getInviter(invitee);
                            if(inviter == null) break;
                            Lang.send(invitee, Lang.REJECT_REQUEST_RECEIVER, s -> s.replaceAll("%player%", inviter.getName()));
                            Lang.send(inviter, Lang.REJECT_REQUEST_SENDER, s -> s.replaceAll("%player%", invitee.getName()));
                            
                            deleteInvite(inviter,invitee);
                        }
                        case "수락" -> {
                            Player invitee = player;
                            // 초대된 플레이어 목록에서 없다면 break
                            if(!isInvitee(invitee)) break;
                            
                            // 준비된 경기장이 없을 때
                            if (!RegionManager.getInstance().hasAvailableSpace()) {
                                Lang.send(invitee, Lang.NON_AVAILABLE_PLACE, s -> s);
                                break;
                            }

                            // 경기장 준비
                            readyPvpRegion(invitee);

                            // 초대 데이터 삭제
                            deleteInvite(invitee, getInviter(invitee));
                        }
                        case "다시하기" -> {
                            PVPRegion pvpRegion = getPvpRegion(player);
                            if(pvpRegion == null) break;
                            // 다시하기 상태가 아닐 때 종료
                            if(!pvpRegion.getGameStatus().equals(GameStatus.MATCH_REPLAY_REQUESTED)) break;

                            // 다시하기 요청 수락
                            // 다시하기 요청 수락
                            Optional<TeamType> teamTypeOptional = pvpRegion.getTeam(player);
                            if (teamTypeOptional.isEmpty()) return false;
                            teamTypeOptional.ifPresent(teamType -> {
                                pvpRegion.isAcceptedMap.put(teamType, true);
                            });
                            // 다른 플레이어가 아직 다시하기 요청을 수락하지 않았을 때 종료
                            for (boolean isAccepted : pvpRegion.isAcceptedMap.values()) if (!isAccepted) return false;

                            // 다시하기 진행
                            Lang.send(player, Lang.ACCEPT_RETRY, s -> s.replaceAll("%player%", player.getName()));
                            pvpRegion.restart();
                        }
                        case "그만하기" -> {
                            // 경기 중이 아니면 종료
                            PVPRegion pvpRegion = getPvpRegion(player);
                            if(pvpRegion == null) break;

                            // 상대가 나가서 매치가 일시정지된 경우
                            if(pvpRegion.getGameStatus().equals(GameStatus.PAUSED)) {
                                pvpRegion.getOpponent(player).ifPresent(pvpRegion::cancelByLogout);
                            }
                            // 상대가 다시하기를 요청한 경우
                            else if(pvpRegion.getGameStatus().equals(GameStatus.MATCH_REPLAY_REQUESTED)) {
                                pvpRegion.cancelByReject(player);
                            }
                            // 그 외의 경우는 종료 안됨
                        }
                        case "보상" -> {
                            if(isInGame(player))  break;
                            StraightRewardGUI.open(player);
                        }
                        case "랭킹" -> {
                            if(isInGame(player)) break;
                            UserManager.getInstance().getRanking(player);
                        }
                    }
                    return false;
                })
                .tabCompleter((sender, command, label, args) -> {
                    switch (args.length) {
                        case 1 -> {
                            return commands;
                        }
                        case 2 -> {
                            if(args[0].equals("신청")) return Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
                        }
                    }
                    return null;
                })
                .register();
    }

    private static void sendInviteMessage(String[] args, Player invitee) {
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) message.append(args[i]).append(" ");
        invitee.sendMessage(message.toString());
        invitee.sendTitle(message.toString(), "", 10, 40, 10);
        Lang.sendClickableCommand(invitee, Lang.ACCEPT_INVITATION);
        invitee.sendMessage("§7[보낸요청] " + message);
    }

    private static boolean isValidInvite(Player inviter, Player invitee) {
        // 준비된 경기장이 없을 때
        if (!RegionManager.getInstance().hasAvailableSpace()) {
            Lang.send(inviter, Lang.NON_AVAILABLE_PLACE, s -> s);
            return false;
        }
        // 플레이어가 이미 PVP 중일 때
        if (UserManager.getInstance().get(invitee).getPVPName() != null) {
            Lang.send(inviter, Lang.ALREADY_PVP, s -> s);
            return false;
        }
        // 상대가 이미 초대되었을 때
        if (isInvitee(invitee)) {
            Lang.send(inviter, Lang.ALREADY_INVITING, s -> s);
            return false;
        }
        // 플레이어가 이미 초대를 보냈을 때
        if(isInviter(inviter)) {
            Lang.send(inviter, Lang.ALREADY_PVP_SELF, s -> s);
            return false;
        }
        return true;
    }

    private static void invite(Player inviter,Player invitee) {
        if(!canInvite(inviter,invitee)) return;
        inviterMap.put(invitee.getUniqueId().toString(),inviter.getUniqueId().toString());
        inviteeMap.put(inviter.getUniqueId().toString(),invitee.getUniqueId().toString());
    }

    // can invite invitee
    private static boolean canInvite(Player inviter,Player invitee) {
        return !inviterMap.containsKey(invitee.getUniqueId().toString()) && !inviteeMap.containsKey(inviter.getUniqueId().toString());
    }
    // is inviter
    private static boolean isInviter(Player player) {
        return inviteeMap.containsKey(player.getUniqueId().toString());
    }
    // is invitee
    private static boolean isInvitee(Player player) {
        return inviterMap.containsKey(player.getUniqueId().toString());
    }
    // get inviter
    private static Player getInviter(Player invitee) {
        if(!isInvitee(invitee)) return null;
        return Bukkit.getPlayer(UUID.fromString(inviterMap.get(invitee.getUniqueId().toString())));
    }
    // delete invitee
    private static void deleteInvitee(Player invitee) {
        if(!isInvitee(invitee)) return;
        inviterMap.remove(invitee.getUniqueId().toString());
    }
    // delete inviter
    private static void deleteInviter(Player inviter) {
        if(!isInviter(inviter)) return;
        inviteeMap.remove(inviter.getUniqueId().toString());
    }
    // delete invite
    private static void deleteInvite(Player inviter,Player invitee) {
        deleteInviter(inviter);
        deleteInvitee(invitee);
    }
    private static void readyPvpRegion(Player invitee) {
        PVPRegion pvpRegion = RegionManager.getInstance().getAvailableRegion();
        pvpRegion.regionPlayerUniqueIdMap.clear();
        Player inviter = getInviter(invitee);
        if(inviter == null) return;
        pvpRegion.setTeamToPlayer(TeamType.RED, invitee);
        pvpRegion.setTeamToPlayer(TeamType.BLUE, inviter);
        pvpRegion.start();
    }
    private static PVPRegion getPvpRegion(Player player) {
        // 경기중이 아니면 종료
        User user = UserManager.getInstance().get(player);
        if (user.getPVPName() == null) return null;

        return RegionManager.getInstance().get(user.getPVPName());
    }
    private static boolean isInGame(Player player) {
        return UserManager.getInstance().get(player).getPVPName() != null;
    }
    private static void expireInviteTimer(Player inviter, Player invitee) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!isInGame(inviter)) {
                    deleteInvite(inviter, invitee);
                    // TODO : 초대가 만료되었음을 알리는 메시지
                }
            }
        }.runTaskLater(Main.getInstance(), PVP_EXPIRE_TIME);
    }
}
