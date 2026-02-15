package org.little100.antiSeedMine;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.little100.antiSeedMine.command.AntiSeedMineCommand;
import org.little100.antiSeedMine.config.BlockManager;
import org.little100.antiSeedMine.config.ConfigManager;
import org.little100.antiSeedMine.listener.OreOffsetListener;
import org.little100.antiSeedMine.util.ServerUtils;

public final class AntiSeedMine extends JavaPlugin {
    
    private static AntiSeedMine instance;
    
    private ConfigManager configManager;
    private BlockManager blockManager;
    private OreOffsetListener oreOffsetListener;
    private long serverStartTime;
    
    @Override
    public void onEnable() {
        instance = this;
        serverStartTime = System.currentTimeMillis();
        
        // 输出服务器类型信息
        getLogger().info("检测到服务器类型: " + ServerUtils.getServerType());
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化矿物管理器
        blockManager = new BlockManager(this);
        blockManager.loadBlocks();
        
        // 注册事件监听器
        oreOffsetListener = new OreOffsetListener(this);
        getServer().getPluginManager().registerEvents(oreOffsetListener, this);
        
        // 注册命令
        AntiSeedMineCommand commandExecutor = new AntiSeedMineCommand(this);
        PluginCommand command = getCommand("antiseedmine");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
        
        getLogger().info("AntiSeedMine 已启用!");
        getLogger().info("已加载 " + blockManager.getOreCount() + " 种矿物进行偏移处理");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("AntiSeedMine 已禁用!");
    }

    public static AntiSeedMine getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BlockManager getBlockManager() {
        return blockManager;
    }

    public long getServerStartTime() {
        return serverStartTime;
    }

    public OreOffsetListener getOreOffsetListener() {
        return oreOffsetListener;
    }

    public void reload() {
        configManager.reloadConfig();
        blockManager.loadBlocks();
        if (oreOffsetListener != null) {
            oreOffsetListener.clearCache();
        }
        getLogger().info("配置已重新加载!");
    }
}
