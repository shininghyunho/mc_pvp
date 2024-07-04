package kr.utila.pvp.commands;

import kr.utila.pvp.Main;
import kr.utila.pvp.libraries.SimpleCommandBuilder;
import kr.utila.pvp.utils.BossBarTimer;
import org.bukkit.entity.Player;

public class BossBarCommand {
    static BossBarTimer timer = new BossBarTimer(Main.getInstance(), 60, 60);
    // register() method
    public static void register() {
        new SimpleCommandBuilder("bossbar")
                .aliases("보스바")
                .commandExecutor((sender, command, label, args) -> {
                    if(!(sender instanceof Player)) {
                        return false;
                    }
                    if(args[0].equals("start")) {
                       timer.addPlayer((Player) sender);
                       timer.start();
                   }
                   else if(args[0].equals("stop")) {
                       timer.stop();
                   }
                       // Start boss bar
                    return false;
                })
                .tabCompleter((sender, command, alias, args) -> null)
                .register();
    }
}
