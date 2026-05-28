package org.little100.antiSeedMine.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BlockManager {

    private final JavaPlugin plugin;
    private final Set<Material> oreBlocks;
    private File blockFile;
    private FileConfiguration blockConfig;

    public BlockManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.oreBlocks = new HashSet<>();
    }

    public void loadBlocks() {
        saveDefaultBlockConfig();
        reloadBlockConfig();

        List<String> oreList = blockConfig.getStringList("ores");
        oreBlocks.clear();

        int loaded = 0;
        int skipped = 0;

        for (String oreName : oreList) {
            try {
                Material material = Material.valueOf(oreName.toUpperCase());
                oreBlocks.add(material);
                loaded++;
            } catch (IllegalArgumentException e) {
                skipped++;
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("跳过当前版本不支持的矿物: " + oreName);
                }
            }
        }

        plugin.getLogger().info("已加载 " + loaded + " 种矿物，跳过 " + skipped + " 个不支持的条目。");
    }

    private void saveDefaultBlockConfig() {
        blockFile = new File(plugin.getDataFolder(), "block.yml");
        if (!blockFile.exists()) {
            plugin.saveResource("block.yml", false);
        }
    }

    public void reloadBlockConfig() {
        if (blockFile == null) {
            blockFile = new File(plugin.getDataFolder(), "block.yml");
        }
        blockConfig = YamlConfiguration.loadConfiguration(blockFile);

        InputStream defaultStream = plugin.getResource("block.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            blockConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getBlockConfig() {
        if (blockConfig == null) {
            reloadBlockConfig();
        }
        return blockConfig;
    }

    public void saveBlockConfig() {
        if (blockConfig == null || blockFile == null) {
            return;
        }
        try {
            getBlockConfig().save(blockFile);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存 block.yml: " + e.getMessage());
        }
    }

    public boolean isOre(Material material) {
        return oreBlocks.contains(material);
    }

    public Set<Material> getOreBlocks() {
        return new HashSet<>(oreBlocks);
    }

    public int getOreCount() {
        return oreBlocks.size();
    }
}
