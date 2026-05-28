package org.little100.antiSeedMine.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;

    private TimestampSource timestampSource;
    private long customTimestamp;

    private int offsetXMin;
    private int offsetXMax;
    private int offsetZMin;
    private int offsetZMax;
    private int offsetYMin;
    private int offsetYMax;

    private boolean enableAllWorlds;
    private List<String> enabledWorlds;

    private boolean aggressiveVerify;
    private boolean recheckLoadedChunks;
    private boolean debug;

    private int maxChunksPerTick;
    private int processedChunkCacheSize;

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

        String sourceStr = config.getString("timestamp.source", "WORLD_CREATION");
        try {
            timestampSource = TimestampSource.valueOf(sourceStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的时间戳来源: " + sourceStr + "，已使用默认值 WORLD_CREATION");
            timestampSource = TimestampSource.WORLD_CREATION;
        }
        customTimestamp = config.getLong("timestamp.custom-value", 0);

        offsetXMin = config.getInt("offset.x.min", 5);
        offsetXMax = config.getInt("offset.x.max", 10);
        offsetZMin = config.getInt("offset.z.min", 5);
        offsetZMax = config.getInt("offset.z.max", 10);
        offsetYMin = config.getInt("offset.y.min", 1);
        offsetYMax = config.getInt("offset.y.max", 2);

        enableAllWorlds = config.getBoolean("worlds.enable-all", true);
        enabledWorlds = config.getStringList("worlds.enabled-worlds");

        aggressiveVerify = config.getBoolean("aggressive-verify", true);
        recheckLoadedChunks = config.getBoolean("recheck-loaded-chunks", false);
        debug = config.getBoolean("debug", false);

        maxChunksPerTick = Math.max(1, config.getInt("performance.max-chunks-per-tick", 2));
        processedChunkCacheSize = Math.max(1024, config.getInt("performance.processed-chunk-cache-size", 100000));

        if (debug) {
            plugin.getLogger().info("配置加载完成:");
            plugin.getLogger().info("  时间戳来源: " + timestampSource);
            plugin.getLogger().info("  X 轴偏移范围: " + offsetXMin + " ~ " + offsetXMax);
            plugin.getLogger().info("  Z 轴偏移范围: " + offsetZMin + " ~ " + offsetZMax);
            plugin.getLogger().info("  Y 轴偏移范围: " + offsetYMin + " ~ " + offsetYMax);
            plugin.getLogger().info("  二次验证: " + formatBoolean(aggressiveVerify));
            plugin.getLogger().info("  重扫已加载区块: " + formatBoolean(recheckLoadedChunks));
            plugin.getLogger().info("  每 tick 最大处理区块数: " + maxChunksPerTick);
            plugin.getLogger().info("  已处理区块缓存上限: " + processedChunkCacheSize);
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

    public List<String> getEnabledWorlds() {
        return enabledWorlds;
    }

    public boolean isAggressiveVerify() {
        return aggressiveVerify;
    }

    public boolean isRecheckLoadedChunks() {
        return recheckLoadedChunks;
    }

    public boolean isDebug() {
        return debug;
    }

    public int getMaxChunksPerTick() {
        return maxChunksPerTick;
    }

    public int getProcessedChunkCacheSize() {
        return processedChunkCacheSize;
    }

    public boolean isWorldEnabled(String worldName) {
        if (enableAllWorlds) {
            return true;
        }
        return enabledWorlds.contains(worldName);
    }

    private String formatBoolean(boolean value) {
        return value ? "开启" : "关闭";
    }
}
