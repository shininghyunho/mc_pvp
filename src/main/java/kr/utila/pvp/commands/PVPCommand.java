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
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.logging.Logger;

public class PVPCommand {
    // logger
    private static final Logger logger = Logger.getLogger(PVPCommand.class.getName());
    /**
     * <초대받은 플레이어, 초대한 플레이어> 매핑
     */
    public static Map<UUID, UUID> inviterMap = new HashMap<>();
    /**
     * <초대한 플레이어, 초대받은 플레이어> 매핑
     */
    public static Map<UUID, UUID> inviteeMap = new HashMap<>();
    /**
     * inviter 의 초대 자동 삭제 타이머
     */
    private static final Map<UUID, BukkitTask> inviterExpireTimer = new HashMap<>();

    // PVP 자동 삭제 시간
    public static final int PVP_EXPIRE_TIME = 20;

    private static final List<String> commands = List.of("신청", "수락", "거절", "다시하기", "그만하기", "랭킹", "보상");
    public static void register() {
        new SimpleCommandBuilder("pvp")
                .aliases("피브이피","PVP")
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

                    String op = args[0];
                    switch (op) {
                        case "신청" -> {
                            Player inviter = player;
                            String inviteeName = args[1];
                            Player invitee = Bukkit.getPlayer(inviteeName);
                            if (!isValidInvite(inviter, invitee)) break;

                            // 초대가 성공적이므로 초대 메시지를 보냄
                            sendInviteMessage(args, invitee);
                            // 자기 자신에게 안내 메시지를 보냄
                            Lang.send(inviter, Lang.SEND_INVITATION, s -> s.replaceAll("%player%", inviteeName));

                            invite(inviter, invitee);
                            setExpireInviteTimer(inviter,invitee);
                        }
                        case "거절" -> {
                            Player invitee = player;
                            // 초대 된 적이 없다면 break
                            if(!isInvitee(invitee)) {
                                invitee.sendMessage("§c초대받지 않은 플레이어입니다.");
                                break;
                            }

                            Player inviter = getInviter(invitee);
                            if(inviter == null) break;
                            Lang.send(invitee, Lang.REJECT_REQUEST_RECEIVER, s -> s.replaceAll("%player%", inviter.getName()));
                            Lang.send(inviter, Lang.REJECT_REQUEST_SENDER, s -> s.replaceAll("%player%", invitee.getName()));
                            
                            deleteInvite(inviter,invitee);
                        }
                        case "수락" -> {
                            Player invitee = player;
                            // 초대된 플레이어 목록에서 없다면 break
                            if(!isInvitee(invitee)) {
                                invitee.sendMessage("§c초대받지 않은 플레이어입니다.");
                                break;
                            }
                            // 상대가 존재하지 않을 때
                            Player inviter = getInviter(invitee);
                            if(inviter == null) {
                                Lang.send(invitee, Lang.PLAYER_NOT_FOUND);
                                break;
                            }
                            
                            // 준비된 경기장이 없을 때
                            if (!RegionManager.getInstance().hasAvailableSpace()) {
                                // TEST
                                // 모든 경기장 정보 출력
                                RegionManager.getInstance().getAllRegions().forEach((region) -> {
                                    // name print
                                    logger.info("name : "+region.getName());
                                    // gameStatus print
                                    logger.info("gameStatus : "+region.getGameStatus());
                                });
                                Lang.send(invitee, Lang.NON_AVAILABLE_PLACE);
                                break;
                            }

                            // 경기장 준비
                            readyPvpRegion(invitee);

                            // 초대 데이터 삭제
                            deleteInvite(getInviter(invitee), invitee);
                        }
                        case "다시하기" -> {
                            PVPRegion pvpRegion = getPvpRegion(player);
                            if(pvpRegion == null) break;
                            // 다시하기 상태가 아닐 때 종료
                            if(!pvpRegion.getGameStatus().equals(GameStatus.MATCH_REPLAY_REQUESTED)) break;

                            // 다시하기 요청 수락
                            pvpRegion.replayAccept(player);
                        }
                        case "그만하기" -> {
                            // 경기 중이 아니면 종료
                            PVPRegion pvpRegion = getPvpRegion(player);
                            if(pvpRegion == null) break;

                            // 상대가 나가서 매치가 일시정지된 경우
                            if(pvpRegion.getGameStatus().equals(GameStatus.PAUSED)) {
                                pvpRegion.getOpponent(player).ifPresent(pvpRegion::cancelGameByLogout);
                            }
                            // 상대가 다시하기를 요청한 경우
                            else if(pvpRegion.getGameStatus().equals(GameStatus.MATCH_REPLAY_REQUESTED)) {
                                pvpRegion.cancelGameByReject(player);
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
        Lang.sendClickableCommand(invitee, Lang.ACCEPT_INVITATION);
        StringBuilder message = new StringBuilder();
        for (int i = 2; i < args.length; i++) message.append(args[i]).append(" ");
        if(!message.isEmpty()) invitee.sendMessage("§7[보낸요청] " + message);
    }

    private static boolean isValidInvite(Player inviter, Player invitee) {
        // 상대 플레이어가 없을 때
        if(invitee == null) {
            Lang.send(inviter, Lang.PLAYER_NOT_FOUND);
            return false;
        }
        // 자기 자신을 초대할 때
        if(inviter.equals(invitee)) {
            Lang.send(inviter, Lang.CANNOT_INVITE_SELF);
            return false;
        }
        // 준비된 경기장이 없을 때
        if (!RegionManager.getInstance().hasAvailableSpace()) {
            Lang.send(inviter, Lang.NON_AVAILABLE_PLACE);
            return false;
        }
        // 플레이어가 이미 PVP 중일 때
        if (UserManager.getInstance().get(invitee).getPVPName() != null) {
            Lang.send(inviter, Lang.ALREADY_PVP);
            return false;
        }
        // 상대가 이미 초대되었을 때
        if (isInvitee(invitee)) {
            Lang.send(inviter, Lang.ALREADY_INVITING);
            return false;
        }
        // 플레이어가 이미 초대를 보냈을 때
        if(isInviter(inviter)) {
            Lang.send(inviter, Lang.ALREADY_PVP_SELF);
            return false;
        }
        // 초대가 불가능 할 때
        if(!canInvite(inviter,invitee)) {
            Lang.send(inviter, Lang.ALREADY_INVITING);
            return false;
        }
        return true;
    }

    private static void invite(Player inviter,Player invitee) {
        if(!canInvite(inviter,invitee)) return;

        var inviterId = inviter.getUniqueId();
        var inviteeId = invitee.getUniqueId();
        logger.info("inviterId : "+inviterId+" inviteeId : "+inviteeId);
        inviterMap.put(inviteeId, inviterId);
        inviteeMap.put(inviterId, inviteeId);
    }

    // can invite invitee
    private static boolean canInvite(Player inviter,Player invitee) {
        if(inviter == null || invitee == null) return false;
        return !inviterMap.containsKey(invitee.getUniqueId()) && !inviteeMap.containsKey(inviter.getUniqueId());
    }
    // is inviter
    private static boolean isInviter(Player player) {
        return inviteeMap.containsKey(player.getUniqueId());
    }
    // is invitee
    private static boolean isInvitee(Player player) {
        return inviterMap.containsKey(player.getUniqueId());
    }
    // get inviter
    private static Player getInviter(Player invitee) {
        if(!isInvitee(invitee)) return null;
        return Bukkit.getPlayer(inviterMap.get(invitee.getUniqueId()));
    }
    // delete invitee
    private static void deleteInvitee(Player invitee) {
        if(invitee == null) return;
        logger.info("deleteInvitee : "+invitee.getUniqueId());
        if(!isInvitee(invitee)) return;
        inviterMap.remove(invitee.getUniqueId());
    }
    // delete inviter
    private static void deleteInviter(Player inviter) {
        if(inviter == null) return;
        logger.info("deleteInviter : "+inviter.getUniqueId());
        if(!isInviter(inviter)) return;
        inviteeMap.remove(inviter.getUniqueId());
    }
    // delete invite
    private static void deleteInvite(Player inviter,Player invitee) {
        deleteInviter(inviter);
        deleteInvitee(invitee);
        deleteInviterExpireTimer(inviter);
    }
    private static void readyPvpRegion(Player invitee) {
        PVPRegion pvpRegion = RegionManager.getInstance().getAvailableRegion();
        if(pvpRegion == null) return;

        Player inviter = getInviter(invitee);
        if(inviter == null) return;

        pvpRegion.start(inviter,invitee);
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
    private static void setExpireInviteTimer(Player inviter, Player invitee) {
        var expireTimer = new BukkitRunnable() {
            @Override
            public void run() {
                deleteInvite(inviter, invitee);
                // inviter 에게서 온 초대가 만료되었습니다.
                invitee.sendMessage("§c%s님의 초대가 만료되었습니다.".replaceAll("%s", inviter.getName()));
                // invitee 에게 보낸 초대가 만료되었습니다.
                inviter.sendMessage("§c%s님에게 보낸 초대가 만료되었습니다.".replaceAll("%s", invitee.getName()));
            }
        }.runTaskLater(Main.getInstance(), PVP_EXPIRE_TIME * 20);
        deleteInviterExpireTimer(inviter);
        inviterExpireTimer.put(inviter.getUniqueId(), expireTimer);
    }
    // clear invite
    public static void clearInvite() {
        logger.info("clear invite info");
        inviterMap.clear();
        inviteeMap.clear();
    }
    private static void deleteInviterExpireTimer(Player inviter) {
        if(inviter == null) return;
        if(inviterExpireTimer.containsKey(inviter.getUniqueId())) {
            inviterExpireTimer.get(inviter.getUniqueId()).cancel();
            inviterExpireTimer.remove(inviter.getUniqueId());
        }
    }
}
