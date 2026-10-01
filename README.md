# CrimsonCopperGrid

A lightweight Fabric power mod built only with vanilla copper and redstone.

只用原版铜与红石搭建的轻量级 Fabric 能源模组。

## 技术栈

| 项目 | 版本 | 说明 |
| --- | --- | --- |
| Minecraft | 26.3 | Java 版正式版 |
| Fabric Loader | 0.19.5 | 该 MC 版本下 Fabric 官方标记为 stable |
| Fabric API | 0.161.0+26.3 | |
| Fabric Loom | 1.18.2（`gradle.properties` 中声明 `1.18-SNAPSHOT`） | 插件 id 为 `net.fabricmc.fabric-loom` |
| Gradle | 9.8.0 | 由 wrapper 提供；**9.8.0 起才支持在 Java 27 上运行**（模板自带的 9.7.1 只支持到 Java 26） |
| JDK | 运行 Gradle 用本机 JDK 27（`D:\jdk27`） | 模组字节码目标为 **Java 25** |

> 已在本机实测通过：**JDK 27 + Gradle 9.8.0 + Loom 1.18.2** 下执行 `gradlew build`，
> 产物 `build/libs/crimsoncoppergrid-0.0.2.jar`，字节码 major 69（Java 25）。
> 本机没有独立安装 JDK 25，Gradle 直接跑在 JDK 27 上；若你换成 JDK 25，把 wrapper 降回 9.7.1 也可以。

> **映射说明**：Yarn 目前**没有** 26.3 的映射（`meta.fabricmc.net/v2/versions/yarn/26.3` 返回空数组），
> 所以 `build.gradle` 里**不写 `mappings` 行**，由 Loom 1.18 默认采用 Mojang 官方映射。
> 这意味着模组代码使用官方类名，例如 `net.minecraft.resources.Identifier`（旧的 Yarn 名 `ResourceLocation` 不再适用）。

## 已实现的内容（0.0.2）

| 分类 | 方块/物品 | 数值与行为 |
| --- | --- | --- |
| 输电 | 铜制电线 | 六向自动连接（连接状态写进 blockstate），纯导体、自身不耗电 |
| 输电 | 铜制电闸 | 右键或红石控制通断；**断开时在组网层面直接断路**，等同于电网总闸 |
| 发电 | 燃料发电机 | 1 燃料刻 = 40 FE。一块煤 = 1600 刻 = **64 000 FE** |
| 发电 | 太阳能发电机 | 20 FE/t，仅白天 + 晴天 + 正上方天空光满值 |
| 发电 | 风力发电机 | 10→30 FE/t，随高度线性增长；上方需留空作为迎风面 |
| 储能 | 铜制电池 | 1 000 000 FE 容量，单次吞吐 1 000 FE |
| 用电 | 电力熔炉 | 10 FE/烧炼刻 × 200 刻 = **2 000 FE/物品** |
| 用电 | 电力煤炭合成机 | **8 000 FE = 1 块煤**，内部可攒 64 块，每 20 刻自动送进相邻容器 |
| 用电 | 电力岩浆机 | 30 FE/t，25 刻产 1 mB → **1 桶 ≈ 750 000 FE**；内部罐 4 桶，桶可进可出 |
| 工具 | 电网总览器 | 右键任何接电方块，报出发电能力 / 储能 / 设备数 |

**能量账本**（有意如此，便于评估平衡）：

- 一块煤 → 64 000 FE → 电力熔炉能烧 32 个物品，是原版熔炉（8 个）的 **4 倍**；
  但需要先铺出发电规模才能兑现。
- 电→煤 8 000 FE 换回可发 64 000 FE 的煤，**在能量上是赚的**：它的定位是把多余电力
  转成可携带燃料，不是永动机。嫌宽松就调 `CoalSynthesizerBlockEntity.FE_PER_COAL`。
- 电→岩浆是全模组最贵的一环（一桶 ≈ 12 块煤的电），因为它是**可再生岩浆**，
  太便宜会直接破坏生存平衡。

## 电力系统设计

- 单位采用 **FE 语义**（与 Forge Energy / Team Reborn Energy 一致：整数、`receive/extract`、
  `maxReceive == 0` 表示只出不进）。**不额外引入能量 API 依赖**，保持零外部依赖；
  将来若要和别的科技模组互通，内部实现不需要改。
- 电网每个 **10 刻（0.5 秒）** 结算一次：发电机按需求发电 → 缺口由电池放电补 →
  用不完才拿去充电。
- 组网只在拓扑变化时重建（放/拆方块、开关切换），不做每 tick 全网遍历；电线是纯导体，
  从设备出发沿电线洪水填充成网，**不跨越未加载区块**。
- 已知取舍：电池/熔炉目前没有物品栏 GUI，交互是"手持物品右键放入、空手右键看状态"。
  真正的 `MenuType` 界面留到界面阶段统一做。

## 目录结构

```
.
├── build.gradle                 构建脚本（Loom、Java 25、Fabric API 依赖）
├── settings.gradle              插件仓库与 rootProject.name
├── gradle.properties            所有版本号集中在这里
├── gradlew / gradlew.bat        Gradle 9.8.0 wrapper
├── gradle/wrapper/              wrapper jar 与 distribution 配置
├── dev-env/                     本机提取的开发前置资源（jar / JSON / JDK，见其 README）
└── src
    ├── main
    │   ├── java/com/cyx/crimsoncoppergrid/
    │   │   ├── CrimsonCopperGrid.java          公共入口，按顺序触发各注册表
    │   │   ├── energy/                         能量核心
    │   │   │   ├── EnergyStorage.java          FE 语义的存储/收发接口
    │   │   │   ├── EnergyProducer.java         发电能力
    │   │   │   ├── EnergyConsumer.java         用电能力
    │   │   │   ├── Grid.java                   一张电网，负责结算
    │   │   │   └── GridRegistry.java           按维度组网、标脏重建
    │   │   ├── block/                          方块（电线、电闸、发电机、三台用电设备）
    │   │   ├── block/entity/                   方块实体（含 AbstractEnergyBlockEntity 基类）
    │   │   ├── item/EnergyMeterItem.java       电网总览器
    │   │   ├── registry/                       方块 / 方块实体 / 物品 / 物品栏注册
    │   │   └── mixin/                          服务端注入示例
    │   └── resources/
    │       ├── fabric.mod.json 与 *.mixins.json
    │       ├── assets/crimsoncoppergrid/       blockstate / 模型 / 中英语言
    │       └── data/crimsoncoppergrid/recipe/   10 条合成表
    └── client
        ├── java/com/cyx/crimsoncoppergrid/client/
        │   ├── CrimsonCopperGridClient.java       客户端入口（ClientModInitializer）
        │   └── mixin/CrimsonCopperGridClientMixin.java
        └── resources/
            └── crimsoncoppergrid.client.mixins.json
```

美术说明：所有模型贴图都直接引用原版材质（铜块、红石、玻璃、羊毛、岩浆、熔炉正面等），
0.0.2 没有自定义贴图，目的先保证"进游戏能看到东西"。

## 构建与运行

首次构建会联网下载 Gradle 发行包、Loom、Minecraft 与依赖库（本机缓存约 1 GB，耗时数分钟）。

```powershell
# JDK：本机为 D:\jdk27；若 PATH 里没有 java，先设置 JAVA_HOME
$env:JAVA_HOME = 'D:\jdk27'

# 构建（产物在 build/libs/，已实测通过）
.\gradlew.bat build

# 启动开发用客户端 / 服务端（Loom 自动生成 run/ 目录）
.\gradlew.bat runClient
.\gradlew.bat runServer

# 只做编译检查
.\gradlew.bat compileJava compileClientJava
```

### 本机网络（代理）说明

这台机器**直连会失败**：`services.gradle.org` 与 Maven 仓库的直连会在 TLS 握手阶段超时，
必须走本地代理 `127.0.0.1:7890`。代理已经写进配置：

- `gradle/wrapper/gradle-wrapper.properties`：`systemProp.https.proxyHost/Port`（供 wrapper 下载 Gradle 发行包）
- `gradle.properties`：`systemProp.http/https.proxyHost/Port`（供 Gradle 解析依赖）
- wrapper 的 `networkTimeout` 已从 10 秒提高到 300 秒，`retries` 设为 2

如果你的网络可以直连，把上述 4～6 行代理配置删掉即可。

## 开发注意事项

- 能量网络的核心逻辑放 `src/main`（服务端权威），渲染与界面放 `src/client`。
- 新增 mixin 时记得在两个 `*.mixins.json` 中登记对应类。
- **用电设备声明需求的时机很重要**：电网是「先汇总所有 `wantedEnergy()`，再按需求发电」，
  所以需求必须在 **tick 阶段**算好（见 `ElectricFurnaceBlockEntity`），
  不能在 `consumeEnergy()` 里才第一次提出——那样电网会以为没人要电，发电机和电池都不会动。
- `src/main/resources/assets/crimsoncoppergrid/icon.png` 尚未提供：缺少图标只会让加载器打印一条警告，不影响运行；补一张 128×128 PNG 即可。

### 26.3 与旧版本不同的 API（本项目已踩过）

| 变化 | 说明 |
| --- | --- |
| `BlockEntityType.Builder` 已删除 | 构造函数公开，直接 `new BlockEntityType<>(supplier, Set.of(blocks))` |
| `Properties` 必须 `setId(ResourceKey)` | 方块与物品都是，否则启动报错 |
| 方块实体存档 API | 从 `CompoundTag` 换成 `ValueInput` / `ValueOutput` |
| `Block` / `BlockBehaviour` 没有 `codec()` | 不要照旧版写 `simpleCodec(...)` 与 `codec()` 覆写 |
| `BlockEntity#loadAdditional/saveAdditional` | 参数类型同上改成 `ValueInput` / `ValueOutput` |
| `Item#getCraftingRemainder()` | 返回 `ItemStackTemplate`，需 `.create()` 转回 `ItemStack` |
| `Level#canSeeSky` 不存在 | 用 `getBrightness(LightLayer.SKY, pos)` 判断是否露天 |
| `Level#hasNeighborSignal` 等签名 | 部分方法在父接口上，编译期以 jar 为准 |
| 交互分发 | 主手有物品走 `useItemOn`，空手走 `useWithoutItem`，两者互斥 |

## 许可

MIT，见 [LICENSE](LICENSE)。
