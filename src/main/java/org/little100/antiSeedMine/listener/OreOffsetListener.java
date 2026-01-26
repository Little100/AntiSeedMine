package org.little100.antiSeedMine.listener;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.little100.antiSeedMine.AntiSeedMine;
import org.little100.antiSeedMine.config.BlockManager;
import org.little100.antiSeedMine.config.ConfigManager;

import java.io.File;
import java.util.*;

public class OreOffsetListener implements Listener {
    
    private final AntiSeedMine plugin;
    private final ConfigManager configManager;
    private final BlockManager blockManager;
    
    // 缓存世界创建时间
    private final Map<String, Long> worldCreationTimeCache = new HashMap<>();
    
    // 6个方向的偏移量
    private static final int[][] DIRECTIONS = {
        {1, 0, 0}, {-1, 0, 0},
        {0, 1, 0}, {0, -1, 0},
        {0, 0, 1}, {0, 0, -1}
    };

    public OreOffsetListener(AntiSeedMine plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.blockManager = plugin.getBlockManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        
        if (!configManager.isWorldEnabled(world.getName())) {
            return;
        }
        
        long timestamp = getTimestamp(world);
        processChunk(chunk, timestamp);
    }

    // 获取时间戳
    private long getTimestamp(World world) {
        switch (configManager.getTimestampSource()) {
            case WORLD_CREATION:
                return getWorldCreationTime(world);
            case SERVER_START:
                return plugin.getServerStartTime();
            case CUSTOM:
                long custom = configManager.getCustomTimestamp();
                return custom == 0 ? System.currentTimeMillis() : custom;
            default:
                return getWorldCreationTime(world);
        }
    }

    // 获取世界创建时间
    private long getWorldCreationTime(World world) {
        String worldName = world.getName();
        
        if (worldCreationTimeCache.containsKey(worldName)) {
            return worldCreationTimeCache.get(worldName);
        }
        
        long creationTime;
        File worldFolder = world.getWorldFolder();
        
        if (worldFolder != null && worldFolder.exists()) {
            File levelDat = new File(worldFolder, "level.dat");
            if (levelDat.exists()) {
                creationTime = levelDat.lastModified();
            } else {
                creationTime = worldFolder.lastModified();
            }
        } else {
            creationTime = world.getSeed() ^ (worldName.hashCode() * 31L);
        }
        
        worldCreationTimeCache.put(worldName, creationTime);
        
        if (configManager.isDebug()) {
            plugin.getLogger().info("世界 " + worldName + " 的时间戳: " + creationTime);
        }
        
        return creationTime;
    }

    // 处理区块中的矿物
    private void processChunk(Chunk chunk, long timestamp) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();
        
        int minY = getWorldMinHeight(world);
        int maxY = world.getMaxHeight();

        Map<WorldPos, Material> oreMap = new HashMap<>();
        
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material material = block.getType();
                    
                    if (blockManager.isOre(material)) {
                        int worldX = chunkX * 16 + x;
                        int worldZ = chunkZ * 16 + z;
                        oreMap.put(new WorldPos(worldX, y, worldZ), material);
                    }
                }
            }
        }
        
        if (oreMap.isEmpty()) {
            return;
        }
        
        // 检测矿物簇
        List<OreCluster> clusters = detectOreClusters(oreMap);
        
        // 创建基于区块位置和时间戳的随机数生成器
        long seed = timestamp;
        seed = seed * 31 + chunkX;
        seed = seed * 31 + chunkZ;
        Random random = new Random(seed);
        
        int offsetXMin = configManager.getOffsetXMin();
        int offsetXMax = configManager.getOffsetXMax();
        int offsetZMin = configManager.getOffsetZMin();
        int offsetZMax = configManager.getOffsetZMax();
        int offsetYMin = configManager.getOffsetYMin();
        int offsetYMax = configManager.getOffsetYMax();
        
        // 处理每个矿物簇
        for (OreCluster cluster : clusters) {
            // 为整个矿物簇计算统一偏移量
            int offsetX = calculateOffset(random, offsetXMin, offsetXMax);
            int offsetZ = calculateOffset(random, offsetZMin, offsetZMax);
            int offsetY = calculateOffset(random, offsetYMin, offsetYMax);
            
            // 收集移动操作
            List<MoveOperation> operations = new ArrayList<>();
            boolean canMove = true;
            
            for (WorldPos pos : cluster.blocks) {
                int newX = pos.x + offsetX;
                int newY = pos.y + offsetY;
                int newZ = pos.z + offsetZ;
                
                // 确保Y坐标在有效范围内
                newY = Math.max(minY, Math.min(maxY - 1, newY));
                
                WorldPos newPos = new WorldPos(newX, newY, newZ);

                if (cluster.blocks.contains(newPos)) {
                    operations.add(new MoveOperation(pos, newPos, oreMap.get(pos)));
                    continue;
                }
                
                // 检查目标区块是否已加载
                int targetChunkX = newX >> 4;
                int targetChunkZ = newZ >> 4;
                
                if (!world.isChunkLoaded(targetChunkX, targetChunkZ)) {
                    if (!world.loadChunk(targetChunkX, targetChunkZ, false)) {
                        // 无法加载跳过
                        canMove = false;
                        break;
                    }
                }
                
                // 检查新位置是否可替换
                Block newBlock = world.getBlockAt(newX, newY, newZ);
                if (!isReplaceableBlock(newBlock.getType())) {
                    canMove = false;
                    break;
                }
                
                operations.add(new MoveOperation(pos, newPos, oreMap.get(pos)));
            }
            
            // 执行移动
            if (canMove && !operations.isEmpty()) {
                // 先清除原位置
                for (MoveOperation op : operations) {
                    Block originalBlock = world.getBlockAt(op.from.x, op.from.y, op.from.z);
                    Material replacement = getReplacementMaterial(op.material, op.from.y, minY);
                    originalBlock.setType(replacement, false);
                }
                
                // 再设置新位置
                for (MoveOperation op : operations) {
                    Block newBlock = world.getBlockAt(op.to.x, op.to.y, op.to.z);
                    newBlock.setType(op.material, false);
                }
            }
            // 如果无法移动保持原位
        }
    }

    // 使用BFS检测矿物簇
    private List<OreCluster> detectOreClusters(Map<WorldPos, Material> oreMap) {
        List<OreCluster> clusters = new ArrayList<>();
        Set<WorldPos> visited = new HashSet<>();
        
        for (Map.Entry<WorldPos, Material> entry : oreMap.entrySet()) {
            WorldPos startPos = entry.getKey();
            
            if (visited.contains(startPos)) {
                continue;
            }
            
            Material material = entry.getValue();
            OreCluster cluster = new OreCluster(material);
            Queue<WorldPos> queue = new LinkedList<>();
            queue.add(startPos);
            visited.add(startPos);
            
            while (!queue.isEmpty()) {
                WorldPos current = queue.poll();
                cluster.blocks.add(current);
                
                // 检查6个相邻方向
                for (int[] dir : DIRECTIONS) {
                    int nx = current.x + dir[0];
                    int ny = current.y + dir[1];
                    int nz = current.z + dir[2];
                    
                    WorldPos neighbor = new WorldPos(nx, ny, nz);
                    
                    if (!visited.contains(neighbor) && oreMap.containsKey(neighbor)) {
                        Material neighborMaterial = oreMap.get(neighbor);
                        if (isSameOreType(material, neighborMaterial)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
            
            clusters.add(cluster);
        }
        
        return clusters;
    }

    // 检查两种材料是否是同类型矿物
    private boolean isSameOreType(Material mat1, Material mat2) {
        if (mat1 == mat2) {
            return true;
        }
        
        String name1 = mat1.name();
        String name2 = mat2.name();
        
        String base1 = name1.replace("DEEPSLATE_", "");
        String base2 = name2.replace("DEEPSLATE_", "");
        
        return base1.equals(base2);
    }

    // 获取世界最低高度
    private int getWorldMinHeight(World world) {
        try {
            return world.getMinHeight();
        } catch (NoSuchMethodError e) {
            return 0;
        }
    }

    // 计算偏移量
    private int calculateOffset(Random random, int min, int max) {
        int offset = min + random.nextInt(max - min + 1);
        if (random.nextBoolean()) {
            offset = -offset;
        }
        return offset;
    }

    // 获取替换材料
    private Material getReplacementMaterial(Material oreMaterial, int y, int minY) {
        String materialName = oreMaterial.name();
        
        if (materialName.startsWith("DEEPSLATE_")) {
            try {
                return Material.valueOf("DEEPSLATE");
            } catch (IllegalArgumentException e) {
                return Material.STONE;
            }
        }
        
        if (materialName.startsWith("NETHER_") || materialName.equals("ANCIENT_DEBRIS")) {
            return Material.NETHERRACK;
        }
        
        if (minY < 0 && y < 0) {
            try {
                return Material.valueOf("DEEPSLATE");
            } catch (IllegalArgumentException e) {
                return Material.STONE;
            }
        }
        
        return Material.STONE;
    }

    // 检查方块是否可被替换
    private boolean isReplaceableBlock(Material material) {
        switch (material) {
            case STONE:
            case GRANITE:
            case DIORITE:
            case ANDESITE:
            case NETHERRACK:
                return true;
            default:
                String name = material.name();
                return name.equals("DEEPSLATE") || name.equals("TUFF") || name.equals("CALCITE");
        }
    }

    // 世界坐标位置
    private static class WorldPos {
        final int x, y, z;
        
        WorldPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WorldPos pos = (WorldPos) o;
            return x == pos.x && y == pos.y && z == pos.z;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(x, y, z);
        }
    }

    // 矿物簇
    private static class OreCluster {
        final Material material;
        final Set<WorldPos> blocks = new HashSet<>();
        
        OreCluster(Material material) {
            this.material = material;
        }
    }
    
    // 移动操作
    private static class MoveOperation {
        final WorldPos from, to;
        final Material material;
        
        MoveOperation(WorldPos from, WorldPos to, Material material) {
            this.from = from;
            this.to = to;
            this.material = material;
        }
    }
}
