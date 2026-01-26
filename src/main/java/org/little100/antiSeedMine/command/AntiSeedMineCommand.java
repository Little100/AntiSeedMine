package org.little100.antiSeedMine.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.little100.antiSeedMine.AntiSeedMine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AntiSeedMineCommand implements CommandExecutor, TabCompleter {
    
    private final AntiSeedMine plugin;

    public AntiSeedMineCommand(AntiSeedMine plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("antiseedmine.reload")) {
            sender.sendMessage("§c你没有权限执行此命令!");
            return true;
        }
        
        plugin.reload();
        sender.sendMessage("§a[AntiSeedMine] 配置已重新加载!");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("antiseedmine.info")) {
            sender.sendMessage("§c你没有权限执行此命令!");
            return true;
        }
        
        sender.sendMessage("§6========== AntiSeedMine 信息 ==========");
        sender.sendMessage("§e版本: §f" + plugin.getDescription().getVersion());
        sender.sendMessage("§e已加载矿物数量: §f" + plugin.getBlockManager().getOreCount());
        sender.sendMessage("§e时间戳来源: §f" + plugin.getConfigManager().getTimestampSource());
        sender.sendMessage("§eX偏移范围: §f" + plugin.getConfigManager().getOffsetXMin() + " ~ " + plugin.getConfigManager().getOffsetXMax());
        sender.sendMessage("§eZ偏移范围: §f" + plugin.getConfigManager().getOffsetZMin() + " ~ " + plugin.getConfigManager().getOffsetZMax());
        sender.sendMessage("§eY偏移范围: §f" + plugin.getConfigManager().getOffsetYMin() + " ~ " + plugin.getConfigManager().getOffsetYMax());
        sender.sendMessage("§e调试模式: §f" + (plugin.getConfigManager().isDebug() ? "开启" : "关闭"));
        sender.sendMessage("§6=========================================");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6========== AntiSeedMine 帮助 ==========");
        sender.sendMessage("§e/asm reload §7- 重新加载配置");
        sender.sendMessage("§e/asm info §7- 查看插件信息");
        sender.sendMessage("§e/asm help §7- 显示此帮助");
        sender.sendMessage("§6=========================================");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            if (sender.hasPermission("antiseedmine.reload")) {
                completions.add("reload");
            }
            if (sender.hasPermission("antiseedmine.info")) {
                completions.add("info");
            }
            completions.add("help");
            
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
