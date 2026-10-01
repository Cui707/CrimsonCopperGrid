# dev-env —— CrimsonCopperGrid 开发前置资源

本目录是从本机现有安装中挑出的、开发 **Minecraft Java 版 26.3 + Fabric 0.19.5** 模组所需的**基础前置资源**，来源：

- `D:\PCL2\.minecraft`（PCL2 启动器实例：`versions\26.3` 与 `versions\26.3-Fabric 0.19.5`）
- `D:\download`（下载的 jar 与 JDK 安装包）

共 12 个文件，约 332 MB。所有文件的 SHA-1 记录在 `manifest.sha1`，可用 `sha1sum -c manifest.sha1`（Git for Windows 自带）校验。

## 目录结构

```
dev-env/
├── minecraft/
│   ├── versions/
│   │   ├── 26.3/                        原版 26.3 客户端
│   │   │   ├── 26.3.jar                 客户端 jar（Mojang client.jar，sha1 e877b6a0…）
│   │   │   └── 26.3.json                原版版本元数据（114 个依赖库、assetIndex）
│   │   └── 26.3-Fabric 0.19.5/          Fabric 实例
│   │       ├── 26.3-Fabric 0.19.5.jar   同版本客户端 jar
│   │       └── 26.3-Fabric 0.19.5.json  含 Fabric 的完整启动描述（mainClass=KnotClient）
│   ├── server/
│   │   └── minecraftserver26.3.jar      官方服务端（sha1 33680f5f…，62.3 MB）
│   ├── logs/latest.log                  本机 Fabric 实例最近一次启动日志（环境参考）
│   └── options.txt                      本机客户端设置（仅供对照）
├── fabric/
│   ├── loader/
│   │   ├── fabric-loader-0.19.5.jar                          Fabric Loader 本体
│   │   ├── sponge-mixin-0.17.4+mixin.0.8.7.jar               Mixin 运行时（Loader 依赖）
│   │   └── fabric-server-mc.26.3-loader.0.19.5-launcher.1.1.2.jar  Fabric 服务端启动器
│   └── mods/
│       └── fabric-api-0.161.0+26.3.jar                       Fabric API（模组运行/开发前置）
└── tools/
    └── jdk-27_windows-x64_bin.exe       JDK 27 Windows x64 安装包
```

## 关键版本信息

| 项目 | 值 |
| --- | --- |
| Minecraft | 26.3（正式版，发布时间 2026-09-15 19:23） |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Mixin | 0.17.4+mixin.0.8.7 |
| Java 要求 | 版本元数据声明的运行时为 `java-runtime-epsilon`，**majorVersion 25**（JDK 27 可用且向后兼容） |
| 客户端 mainClass | `net.fabricmc.loader.impl.launch.knot.KnotClient` |

## 开发时怎么用

- **Gradle + Fabric Loom**：`build.gradle` 里写 `minecraft "com.mojang:minecraft:26.3"`、`mappings ...`、`modImplementation "net.fabricmc:fabric-loader:0.19.5"`、`modImplementation "net.fabricmc.fabric-api:fabric-api:0.161.0+26.3"`。Loom 会自行下载依赖；本目录的 jar 用于离线核对、版本对齐或本地依赖替换。
- **本地运行/联调**：把 `fabric/mods/fabric-api-0.161.0+26.3.jar` 放进实例 `mods` 目录；`fabric/loader/fabric-server-mc.26.3-loader.0.19.5-launcher.1.1.2.jar` 可直接启动 Fabric 服务端（配合 `minecraft/server/minecraftserver26.3.jar`）。
- **JDK**：`tools/jdk-27_windows-x64_bin.exe` 安装后作为 `JAVA_HOME`；Gradle 工具链设为 25 或更高。

## 有意未纳入的资源（需要时再从原目录取）

| 资源 | 原路径 | 为什么没放进来 |
| --- | --- | --- |
| 114/121 个依赖库（87 MB） | `D:\PCL2\.minecraft\libraries` | Loom/启动器会按 `*.json` 自动下载；仅离线全量复现时才需要 |
| 资源对象与资源索引（462 MB） | `D:\PCL2\.minecraft\assets` | 同上，属于运行期资源，不是编译前置 |
| Sodium 0.9.2 / Iris 1.11.6 | Fabric 实例 `mods` | 客户端性能/光影模组，开发不需要 |
| BlueMap 5.28 与 Complementary 光影 | `D:\download`、实例 `shaderpacks` | 测试/展示用，非开发前置 |
| `D:\download\rjw-1.6.zip` | `D:\download` | 这是 **RimWorld** 模组源码，与 Minecraft 无关 |
| `D:\download\chengjie512概念集2609.zip` | `D:\download` | 图文训练集，与模组开发无关 |
| bedrock-server-1.26.52.3 等 | `D:\download` | 基岩版服务端，与 Java 版模组无关 |

> 提示：仓库根 `.gitignore` 忽略了 `*.jar`，所以本目录的 jar 不会被提交；`README.md` 与 `manifest.sha1` 可以正常提交。
