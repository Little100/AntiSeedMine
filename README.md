# AntiSeedMine

> 此文件由ai编写并且我没有进行过查阅可能有些许不同

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Minecraft Version](https://img.shields.io/badge/Minecraft-1.16.5+-green.svg)](https://www.spigotmc.org/)
[![Java Version](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)

一个用于防止种子矿透的 Minecraft Spigot 插件，通过基于时间戳的随机偏移来重新分布矿物位置。

## 🎯 功能特性

- **智能矿物偏移**: 在区块生成时自动偏移矿物位置
- **多种时间戳源**: 支持世界创建时间、服务器启动时间或自定义时间戳
- **可配置偏移范围**: 支持 X、Y、Z 三个轴向的独立偏移配置
- **世界选择性启用**: 可以选择为所有世界或特定世界启用
- **Folia 兼容**: 完全支持 Folia 服务器
- **版本兼容**: 支持 Minecraft 1.16.5 到最新版本

## 🚀 安装

1. 下载最新版本的 [`AntiSeedMine.jar`](../../releases) 文件
2. 将文件放入服务器的 `plugins` 文件夹
3. 重启服务器
4. 根据需要修改 [`config.yml`](src/main/resources/config.yml) 和 [`block.yml`](src/main/resources/block.yml) 配置文件

## ⚙️ 配置

### 主配置文件 (config.yml)

```yaml
# 时间戳设置
timestamp:
  source: WORLD_CREATION  # WORLD_CREATION | SERVER_START | CUSTOM
  custom-value: 0         # 仅在 CUSTOM 时使用

# 偏移范围设置
offset:
  x: { min: 5, max: 10 }
  z: { min: 5, max: 10 }
  y: { min: 1, max: 2 }

# 世界设置
worlds:
  enable-all: true        # 为所有世界启用
  enabled-worlds:         # 仅在 enable-all 为 false 时使用
    - world
    - world_nether
    - world_the_end

# 调试模式
debug: false
```

### 支持的矿物

插件支持所有原版矿物，包括：
- 煤炭、铁、铜、金、红石、青金石、钻石、绿宝石矿石
- 深板岩变种 (1.17+)
- 下界石英矿石、远古残骸

## 🎮 命令

| 命令 | 权限 | 描述 |
|------|------|------|
| `/antiseedmine` | `antiseedmine.use` | 显示帮助信息 |
| `/antiseedmine reload` | `antiseedmine.reload` | 重新加载配置文件 |
| `/antiseedmine info` | `antiseedmine.info` | 显示插件信息 |

## 🔧 工作原理

1. 监听区块生成事件
2. 扫描新生成区块中的矿物方块
3. 基于时间戳和坐标生成确定性随机偏移
4. 将矿物移动到新位置，用相应石头替换原位置

**注意**: 本插件仅在区块首次生成时生效，对已生成区块无效。

## 🛠️ 开发

### 构建项目

```bash
git clone https://github.com/Little100/AntiSeedMine.git
cd AntiSeedMine
./gradlew build
```

### 环境要求

- Java 17+
- Gradle 7.0+
- Spigot API 1.16.5+

## 📝 许可证

本项目采用 GPL-3.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 📞 联系方式

- 作者: Little_100
- 网站: [www.little100.top](https://www.little100.top)
- 项目链接: [https://github.com/Little100/AntiSeedMine](https://github.com/Little100/AntiSeedMine)