package org.little100.antiSeedMine.listener;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.scheduler.BukkitTask;
import org.little100.antiSeedMine.AntiSeedMine;
import org.little100.antiSeedMine.config.BlockManager;
import org.little100.antiSeedMine.config.ConfigManager;
import org.little100.antiSeedMine.util.ServerUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class OreOffsetListener implements Listener {

    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private static final long COORD_MASK = 0x3FFFFFFL;
    private static final long Y_MASK = 0xFFFL;

    private final AntiSeedMine plugin;
    private final ConfigManager configManager;
    private final BlockManager blockManager;
    private final Map<String, Long> worldCreationTimeCache = new HashMap<>();
    private final Map<Long, Boolean> processedChunks = new LinkedHashMap<>(1024, 0.75F, true);
    private final Set<Long> queuedChunks = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<ChunkProcessRequest> processQueue = new ConcurrentLinkedQueue<>();

    private BukkitTask bukkitQueueTask;
    private Object foliaQueueTask;

    public OreOffsetListener(AntiSeedMine plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.blockManager = plugin.getBlockManager();
        startQueueWorker();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        enqueueNewChunk(event.getChunk());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.isNewChunk() || configManager.isRecheckLoadedChunks()) {
            enqueueNewChunk(event.getChunk());
        }
    }

    private void enqueueNewChunk(Chunk chunk) {
        World world = chunk.getWorld();
        if (!configManager.isWorldEnabled(world.getName())) {
            return;
        }

        long chunkKey = getChunkKey(world, chunk.getX(), chunk.getZ());
        if (!markProcessedIfNew(chunkKey)) {
            return;
        }

        enqueueChunkProcess(chunk, getTimestamp(world), false);
    }

    private void enqueueChunkProcess(Chunk chunk, long timestamp, boolean verifyPass) {
        long chunkKey = getChunkKey(chunk.getWorld(), chunk.getX(), chunk.getZ());
        if (!verifyPass && !queuedChunks.add(chunkKey)) {
            return;
        }

        processQueue.add(new ChunkProcessRequest(
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ(),
                timestamp,
                verifyPass,
                chunkKey
        ));
    }

    private void startQueueWorker() {
        if (ServerUtils.isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method runAtFixedRate = scheduler.getClass().getMethod(
                        "runAtFixedRate",
                        org.bukkit.plugin.Plugin.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                foliaQueueTask = runAtFixedRate.invoke(
                        scheduler,
                        plugin,
                        (Consumer<Object>) task -> drainProcessQueue(),
                        1L,
                        1L
                );
            } catch (Throwable e) {
                plugin.getLogger().warning("无法启动 Folia 队列处理器: " + e.getMessage());
            }
        } else {
            bukkitQueueTask = Bukkit.getScheduler().runTaskTimer(plugin, this::drainProcessQueue, 1L, 1L);
        }
    }

    private void drainProcessQueue() {
        int max = configManager.getMaxChunksPerTick();
        for (int i = 0; i < max; i++) {
            ChunkProcessRequest request = processQueue.poll();
            if (request == null) {
                return;
            }

            if (!request.verifyPass) {
                queuedChunks.remove(request.chunkKey);
            }

            World world = Bukkit.getWorld(request.worldName);
            if (world == null || !world.isChunkLoaded(request.chunkX, request.chunkZ)) {
                continue;
            }

            executeOnCorrectThread(world, request.chunkX, request.chunkZ, () -> processQueuedChunk(request));
        }
    }

    private void processQueuedChunk(ChunkProcessRequest request) {
        World world = Bukkit.getWorld(request.worldName);
        if (world == null || !world.isChunkLoaded(request.chunkX, request.chunkZ)) {
            return;
        }

        Chunk chunk = world.getChunkAt(request.chunkX, request.chunkZ);
        int movedCount = processChunk(chunk, request.timestamp);

        if (!request.verifyPass && configManager.isAggressiveVerify() && movedCount > 0) {
            scheduleVerifyPass(chunk, request.timestamp);
        }
    }

    private void scheduleVerifyPass(Chunk chunk, long timestamp) {
        Runnable enqueueVerify = () -> enqueueChunkProcess(chunk, timestamp, true);

        if (ServerUtils.isFolia()) {
            try {
                Object scheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method runDelayed = scheduler.getClass().getMethod(
                        "runDelayed",
                        org.bukkit.plugin.Plugin.class,
                        World.class,
                        int.class,
                        int.class,
                        Consumer.class,
                        long.class
                );
                runDelayed.invoke(
                        scheduler,
                        plugin,
                        chunk.getWorld(),
                        chunk.getX(),
                        chunk.getZ(),
                        (Consumer<Object>) task -> enqueueVerify.run(),
                        1L
                );
            } catch (Throwable e) {
                executeOnCorrectThread(chunk.getWorld(), chunk.getX(), chunk.getZ(), enqueueVerify);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, enqueueVerify, 1L);
        }
    }

    private void executeOnCorrectThread(World world, int chunkX, int chunkZ, Runnable task) {
        if (ServerUtils.isFolia()) {
            try {
                Object regionScheduler = Bukkit.class.getMethod("getRegionScheduler").invoke(null);
                Method execute = regionScheduler.getClass().getMethod(
                        "execute",
                        org.bukkit.plugin.Plugin.class,
                        World.class,
                        int.class,
                        int.class,
                        Runnable.class
                );
                execute.invoke(regionScheduler, plugin, world, chunkX, chunkZ, task);
            } catch (Throwable e) {
                plugin.getLogger().warning("无法调度区域线程任务: " + e.getMessage());
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    public int rescanAllChunks() {
        int totalChunks = 0;

        for (World world : Bukkit.getWorlds()) {
            if (!configManager.isWorldEnabled(world.getName())) {
                continue;
            }

            long timestamp = getTimestamp(world);
            for (Chunk chunk : world.getLoadedChunks()) {
                enqueueChunkProcess(chunk, timestamp, true);
                totalChunks++;
            }
        }

        return totalChunks;
    }

    private boolean markProcessedIfNew(long chunkKey) {
        synchronized (processedChunks) {
            if (processedChunks.containsKey(chunkKey)) {
                return false;
            }

            processedChunks.put(chunkKey, Boolean.TRUE);
            trimProcessedChunkCache();
            return true;
        }
    }

    private void trimProcessedChunkCache() {
        int maxSize = configManager.getProcessedChunkCacheSize();
        while (processedChunks.size() > maxSize) {
            Long oldest = processedChunks.keySet().iterator().next();
            processedChunks.remove(oldest);
        }
    }

    private long getChunkKey(World world, int chunkX, int chunkZ) {
        long worldHash = world.getName().hashCode() & 0xFFFFFFFFL;
        return (worldHash << 32) ^ (((long) chunkX) * 73428767L) ^ (((long) chunkZ) * 91227153L);
    }

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

    private long getWorldCreationTime(World world) {
        String worldName = world.getName();

        if (worldCreationTimeCache.containsKey(worldName)) {
            return worldCreationTimeCache.get(worldName);
        }

        long creationTime;
        File worldFolder = world.getWorldFolder();

        if (worldFolder != null && worldFolder.exists()) {
            File levelDat = new File(worldFolder, "level.dat");
            creationTime = levelDat.exists() ? levelDat.lastModified() : worldFolder.lastModified();
        } else {
            creationTime = world.getSeed() ^ (worldName.hashCode() * 31L);
        }

        worldCreationTimeCache.put(worldName, creationTime);

        if (configManager.isDebug()) {
            plugin.getLogger().info("世界 " + worldName + " 的时间戳: " + creationTime);
        }

        return creationTime;
    }

    private int processChunk(Chunk chunk, long timestamp) {
        World world = chunk.getWorld();
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        int minY = getWorldMinHeight(world);
        int maxY = world.getMaxHeight();

        Map<Long, Material> oreMap = new HashMap<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {
                    Block block = chunk.getBlock(x, y, z);
                    Material material = block.getType();

                    if (blockManager.isOre(material)) {
                        int worldX = chunkX * 16 + x;
                        int worldZ = chunkZ * 16 + z;
                        oreMap.put(packBlockPos(worldX, y, worldZ), material);
                    }
                }
            }
        }

        if (oreMap.isEmpty()) {
            return 0;
        }

        List<OreCluster> clusters = detectOreClusters(oreMap);

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

        int movedClusters = 0;

        for (OreCluster cluster : clusters) {
            int offsetX = calculateOffset(random, offsetXMin, offsetXMax);
            int offsetZ = calculateOffset(random, offsetZMin, offsetZMax);
            int offsetY = calculateOffset(random, offsetYMin, offsetYMax);

            List<MoveOperation> operations = new ArrayList<>();
            boolean canMove = true;

            for (long pos : cluster.blocks) {
                int newX = getBlockX(pos) + offsetX;
                int newY = Math.max(minY, Math.min(maxY - 1, getBlockY(pos) + offsetY));
                int newZ = getBlockZ(pos) + offsetZ;
                long newPos = packBlockPos(newX, newY, newZ);

                if (cluster.blocks.contains(newPos)) {
                    operations.add(new MoveOperation(pos, newPos, oreMap.get(pos)));
                    continue;
                }

                int targetChunkX = newX >> 4;
                int targetChunkZ = newZ >> 4;

                if (!world.isChunkLoaded(targetChunkX, targetChunkZ)) {
                    canMove = false;
                    break;
                }

                Block newBlock = world.getBlockAt(newX, newY, newZ);
                if (!isReplaceableBlock(newBlock.getType())) {
                    canMove = false;
                    break;
                }

                operations.add(new MoveOperation(pos, newPos, oreMap.get(pos)));
            }

            if (canMove && !operations.isEmpty()) {
                for (MoveOperation op : operations) {
                    Block originalBlock = world.getBlockAt(getBlockX(op.from), getBlockY(op.from), getBlockZ(op.from));
                    Material replacement = getReplacementMaterial(op.material, getBlockY(op.from), minY);
                    originalBlock.setType(replacement, false);
                }

                for (MoveOperation op : operations) {
                    Block newBlock = world.getBlockAt(getBlockX(op.to), getBlockY(op.to), getBlockZ(op.to));
                    newBlock.setType(op.material, false);
                }

                movedClusters++;
            }
        }

        if (configManager.isDebug() && movedClusters > 0) {
            plugin.getLogger().info("区块 (" + chunkX + ", " + chunkZ + ") 已偏移 " + movedClusters + "/" + clusters.size() + " 个矿物簇");
        }

        return movedClusters;
    }

    private List<OreCluster> detectOreClusters(Map<Long, Material> oreMap) {
        List<OreCluster> clusters = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        for (Map.Entry<Long, Material> entry : oreMap.entrySet()) {
            long startPos = entry.getKey();

            if (visited.contains(startPos)) {
                continue;
            }

            Material material = entry.getValue();
            OreCluster cluster = new OreCluster(material);
            Queue<Long> queue = new ArrayDeque<>();
            queue.add(startPos);
            visited.add(startPos);

            while (!queue.isEmpty()) {
                long current = queue.poll();
                cluster.blocks.add(current);

                int currentX = getBlockX(current);
                int currentY = getBlockY(current);
                int currentZ = getBlockZ(current);

                for (int[] dir : DIRECTIONS) {
                    long neighbor = packBlockPos(currentX + dir[0], currentY + dir[1], currentZ + dir[2]);

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

    private boolean isSameOreType(Material mat1, Material mat2) {
        if (mat1 == mat2) {
            return true;
        }

        String base1 = mat1.name().replace("DEEPSLATE_", "");
        String base2 = mat2.name().replace("DEEPSLATE_", "");
        return base1.equals(base2);
    }

    private int getWorldMinHeight(World world) {
        try {
            return world.getMinHeight();
        } catch (NoSuchMethodError e) {
            return 0;
        }
    }

    private int calculateOffset(Random random, int min, int max) {
        int offset = min + random.nextInt(max - min + 1);
        return random.nextBoolean() ? -offset : offset;
    }

    private Material getReplacementMaterial(Material oreMaterial, int y, int minY) {
        String materialName = oreMaterial.name();

        if (materialName.startsWith("DEEPSLATE_")) {
            return getMaterialOrStone("DEEPSLATE");
        }

        if (materialName.startsWith("NETHER_") || materialName.equals("ANCIENT_DEBRIS")) {
            return Material.NETHERRACK;
        }

        if (minY < 0 && y < 0) {
            return getMaterialOrStone("DEEPSLATE");
        }

        return Material.STONE;
    }

    private Material getMaterialOrStone(String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Material.STONE;
        }
    }

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

    private long packBlockPos(int x, int y, int z) {
        return ((long) x & COORD_MASK) << 38
                | ((long) z & COORD_MASK) << 12
                | ((long) y & Y_MASK);
    }

    private int getBlockX(long packed) {
        return signExtend((packed >> 38) & COORD_MASK, 26);
    }

    private int getBlockY(long packed) {
        return signExtend(packed & Y_MASK, 12);
    }

    private int getBlockZ(long packed) {
        return signExtend((packed >> 12) & COORD_MASK, 26);
    }

    private int signExtend(long value, int bits) {
        long signBit = 1L << (bits - 1);
        return (int) ((value ^ signBit) - signBit);
    }

    public void clearCache() {
        synchronized (processedChunks) {
            processedChunks.clear();
        }
        queuedChunks.clear();
        processQueue.clear();
        worldCreationTimeCache.clear();
    }

    public void shutdown() {
        if (bukkitQueueTask != null) {
            bukkitQueueTask.cancel();
        }
        if (foliaQueueTask != null) {
            try {
                foliaQueueTask.getClass().getMethod("cancel").invoke(foliaQueueTask);
            } catch (Throwable ignored) {
            }
        }
        clearCache();
    }

    private static class ChunkProcessRequest {
        final String worldName;
        final int chunkX;
        final int chunkZ;
        final long timestamp;
        final boolean verifyPass;
        final long chunkKey;

        ChunkProcessRequest(String worldName, int chunkX, int chunkZ, long timestamp, boolean verifyPass, long chunkKey) {
            this.worldName = worldName;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.timestamp = timestamp;
            this.verifyPass = verifyPass;
            this.chunkKey = chunkKey;
        }
    }

    private static class OreCluster {
        final Material material;
        final Set<Long> blocks = new HashSet<>();

        OreCluster(Material material) {
            this.material = material;
        }
    }

    private static class MoveOperation {
        final long from;
        final long to;
        final Material material;

        MoveOperation(long from, long to, Material material) {
            this.from = from;
            this.to = to;
            this.material = material;
        }
    }
}
