package org.little100.antiSeedMine.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    
    // 时间戳设置
    private TimestampSource timestampSource;
    private long customTimestamp;
    
    // 偏移范围设置
    private int offsetXMin;
    private int offsetXMax;
    private int offsetZMin;
    private int offsetZMax;
    private int offsetYMin;
    private int offsetYMax;
    
    // 世界设置
    private boolean enableAllWorlds;
    private java.util.List<String> enabledWorlds;
    
    // 调试模式
    private boolean debug;

    public enum TimestampSource {
        WORLD_CREATION,
        SERVER_START,
        CUSTOM
    }

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();
        
        // 加载时间戳设置
        String sourceStr = config.getString("timestamp.source", "WORLD_CREATION");
        try {
            timestampSource = TimestampSource.valueOf(sourceStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的时间戳来源: " + sourceStr + ", 使用默认值 WORLD_CREATION");
            timestampSource = TimestampSource.WORLD_CREATION;
        }
        customTimestamp = config.getLong("timestamp.custom-value", 0);
        
        // 加载偏移范围设置
        offsetXMin = config.getInt("offset.x.min", 5);
        offsetXMax = config.getInt("offset.x.max", 10);
        offsetZMin = config.getInt("offset.z.min", 5);
        offsetZMax = config.getInt("offset.z.max", 10);
        offsetYMin = config.getInt("offset.y.min", 1);
        offsetYMax = config.getInt("offset.y.max", 2);
        
        // 加载世界设置
        enableAllWorlds = config.getBoolean("worlds.enable-all", true);
        enabledWorlds = config.getStringList("worlds.enabled-worlds");
        
        // 加载调试模式
        debug = config.getBoolean("debug", false);
        
        if (debug) {
            plugin.getLogger().info("配置加载完成:");
            plugin.getLogger().info("  时间戳来源: " + timestampSource);
            plugin.getLogger().info("  X偏移范围: " + offsetXMin + " ~ " + offsetXMax);
            plugin.getLogger().info("  Z偏移范围: " + offsetZMin + " ~ " + offsetZMax);
            plugin.getLogger().info("  Y偏移范围: " + offsetYMin + " ~ " + offsetYMax);
        }
    }

    public void reloadConfig() {
        loadConfig();
    }

    public TimestampSource getTimestampSource() {
        return timestampSource;
    }
    
    public long getCustomTimestamp() {
        return customTimestamp;
    }
    
    public int getOffsetXMin() {
        return offsetXMin;
    }
    
    public int getOffsetXMax() {
        return offsetXMax;
    }
    
    public int getOffsetZMin() {
        return offsetZMin;
    }
    
    public int getOffsetZMax() {
        return offsetZMax;
    }
    
    public int getOffsetYMin() {
        return offsetYMin;
    }
    
    public int getOffsetYMax() {
        return offsetYMax;
    }
    
    public boolean isEnableAllWorlds() {
        return enableAllWorlds;
    }
    
    public java.util.List<String> getEnabledWorlds() {
        return enabledWorlds;
    }
    
    public boolean isDebug() {
        return debug;
    }

    public boolean isWorldEnabled(String worldName) {
        if (enableAllWorlds) {
            return true;
        }
        return enabledWorlds.contains(worldName);
    }
}
