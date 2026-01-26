# AntiSeedMine

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5+-green.svg)](https://www.spigotmc.org/)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

A Minecraft Spigot plugin designed to prevent seed-based ore X-ray by redistributing ore positions through timestamp-based random offsets.

## 🎯 Features

- **Smart Ore Offsetting**: Automatically offsets ore positions during chunk generation
- **Multiple Timestamp Sources**: Supports world creation time, server start time, or custom timestamps
- **Configurable Offset Range**: Independent offset configuration for X, Y, Z axes
- **World-Selective Enabling**: Enable for all worlds or specific worlds only
- **Folia Compatible**: Full support for Folia servers
- **Version Compatible**: Supports Minecraft 1.16.5 to latest versions

## 🚀 Installation

1. Download the latest [`AntiSeedMine.jar`](../../releases) file
2. Place the file in your server's `plugins` folder
3. Restart the server
4. Modify [`config.yml`](src/main/resources/config.yml) and [`block.yml`](src/main/resources/block.yml) configuration files as needed

## ⚙️ Configuration

### Main Configuration (config.yml)

```yaml
# Timestamp settings
timestamp:
  source: WORLD_CREATION  # WORLD_CREATION | SERVER_START | CUSTOM
  custom-value: 0         # Only used when CUSTOM

# Offset range settings
offset:
  x: { min: 5, max: 10 }
  z: { min: 5, max: 10 }
  y: { min: 1, max: 2 }

# World settings
worlds:
  enable-all: true        # Enable for all worlds
  enabled-worlds:         # Only used when enable-all is false
    - world
    - world_nether
    - world_the_end

# Debug mode
debug: false
```

### Supported Ores

The plugin supports all vanilla ores, including:
- Coal, Iron, Copper, Gold, Redstone, Lapis Lazuli, Diamond, Emerald ores
- Deepslate variants (1.17+)
- Nether Quartz ore, Ancient Debris

## 🎮 Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/antiseedmine` | `antiseedmine.use` | Show help information |
| `/antiseedmine reload` | `antiseedmine.reload` | Reload configuration files |
| `/antiseedmine info` | `antiseedmine.info` | Show plugin information |

## 🔧 How It Works

1. Listens to chunk generation events
2. Scans ore blocks in newly generated chunks
3. Generates deterministic random offsets based on timestamp and coordinates
4. Moves ores to new positions and replaces original positions with appropriate stone

**Note**: This plugin only works during initial chunk generation and has no effect on already generated chunks.

## 🛠️ Development

### Building the Project

```bash
git clone https://github.com/Little100/AntiSeedMine.git
cd AntiSeedMine
./gradlew build
```

### Requirements

- Java 17+
- Gradle 7.0+
- Spigot API 1.16.5+

## 📝 License

This project is licensed under the GPL-3.0 License - see the [LICENSE](LICENSE) file for details.

## 📞 Contact

- Author: Little_100
- Website: [www.little100.top](https://www.little100.top)
- Project Link: [https://github.com/Little100/AntiSeedMine](https://github.com/Little100/AntiSeedMine)