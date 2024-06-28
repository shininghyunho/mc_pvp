package kr.utila.pvp.libraries;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

public class SimpleCommandBuilder {

    private String name;
    private String[] aliases;
    private CommandExecutor commandExecutor;
    private TabCompleter tabCompleter;
    private String permission;

    public SimpleCommandBuilder(String name) {
        this.name = name;
    }

    public SimpleCommandBuilder aliases(String... aliases) {
        this.aliases = aliases;
        return this;
    }

    public SimpleCommandBuilder commandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        return this;
    }

    public SimpleCommandBuilder tabCompleter(TabCompleter tabCompleter) {
        this.tabCompleter = tabCompleter;
        return this;
    }

    public SimpleCommandBuilder permission(String permission) {
        this.permission = permission;
        return this;
    }

    public void register() {
        BukkitCommand bukkitCommand = new BukkitCommand(this.name) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                return commandExecutor.onCommand(sender, this, commandLabel, args);
            }

            @Override
            public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                return tabCompleter.onTabComplete(sender, this, alias, args);
            }
        };
        bukkitCommand.setAliases(Arrays.asList(this.aliases));
        if (this.permission != null) {
            bukkitCommand.setPermission(this.permission);
        }
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            commandMap.register(this.name, bukkitCommand);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
