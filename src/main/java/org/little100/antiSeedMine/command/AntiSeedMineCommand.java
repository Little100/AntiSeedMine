package org.little100.antiSeedMine.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.ChatColor;
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

        switch (args[0].toLowerCase()) {
            case "reload":
                return handleReload(sender);
            case "info":
                return handleInfo(sender);
            case "rescan":
                return handleRescan(sender);
            case "help":
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("antiseedmine.reload")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }

        plugin.reload();
        sender.sendMessage(ChatColor.GREEN + "[AntiSeedMine] 配置已重新加载。");
        return true;
    }

    private boolean handleInfo(CommandSender sender) {
        if (!sender.hasPermission("antiseedmine.info")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "========== AntiSeedMine 信息 ==========");
        sender.sendMessage(ChatColor.YELLOW + "版本: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "已加载矿物数: " + ChatColor.WHITE + plugin.getBlockManager().getOreCount());
        sender.sendMessage(ChatColor.YELLOW + "时间戳来源: " + ChatColor.WHITE + plugin.getConfigManager().getTimestampSource());
        sender.sendMessage(ChatColor.YELLOW + "X 轴偏移范围: " + ChatColor.WHITE + plugin.getConfigManager().getOffsetXMin() + " ~ " + plugin.getConfigManager().getOffsetXMax());
        sender.sendMessage(ChatColor.YELLOW + "Z 轴偏移范围: " + ChatColor.WHITE + plugin.getConfigManager().getOffsetZMin() + " ~ " + plugin.getConfigManager().getOffsetZMax());
        sender.sendMessage(ChatColor.YELLOW + "Y 轴偏移范围: " + ChatColor.WHITE + plugin.getConfigManager().getOffsetYMin() + " ~ " + plugin.getConfigManager().getOffsetYMax());
        sender.sendMessage(ChatColor.YELLOW + "二次验证: " + ChatColor.WHITE + (plugin.getConfigManager().isAggressiveVerify() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
        sender.sendMessage(ChatColor.YELLOW + "重扫已加载区块: " + ChatColor.WHITE + (plugin.getConfigManager().isRecheckLoadedChunks() ? ChatColor.GREEN + "开启" : ChatColor.RED + "关闭"));
        sender.sendMessage(ChatColor.YELLOW + "每 tick 最大处理区块数: " + ChatColor.WHITE + plugin.getConfigManager().getMaxChunksPerTick());
        sender.sendMessage(ChatColor.YELLOW + "已处理区块缓存上限: " + ChatColor.WHITE + plugin.getConfigManager().getProcessedChunkCacheSize());
        sender.sendMessage(ChatColor.YELLOW + "调试模式: " + ChatColor.WHITE + (plugin.getConfigManager().isDebug() ? "开启" : "关闭"));
        sender.sendMessage(ChatColor.GOLD + "=======================================");
        return true;
    }

    private boolean handleRescan(CommandSender sender) {
        if (!sender.hasPermission("antiseedmine.rescan")) {
            sender.sendMessage(ChatColor.RED + "你没有权限执行此命令。");
            return true;
        }

        int chunks = plugin.getOreOffsetListener().rescanAllChunks();
        sender.sendMessage(ChatColor.GREEN + "[AntiSeedMine] 已将 " + chunks + " 个已加载区块加入矿物偏移扫描队列。");
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "========== AntiSeedMine 帮助 ==========");
        sender.sendMessage(ChatColor.YELLOW + "/asm reload " + ChatColor.GRAY + "- 重新加载配置");
        sender.sendMessage(ChatColor.YELLOW + "/asm info " + ChatColor.GRAY + "- 查看插件信息");
        sender.sendMessage(ChatColor.YELLOW + "/asm rescan " + ChatColor.GRAY + "- 重新扫描已加载区块");
        sender.sendMessage(ChatColor.YELLOW + "/asm help " + ChatColor.GRAY + "- 显示帮助");
        sender.sendMessage(ChatColor.GOLD + "=======================================");
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
            if (sender.hasPermission("antiseedmine.rescan")) {
                completions.add("rescan");
            }
            completions.add("help");

            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
