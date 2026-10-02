# CrimsonCopperGrid

A lightweight Fabric power mod built only with vanilla copper and redstone.

只用原版铜与红石搭建的轻量级 Fabric 能源模组。

设计原则：**不加新矿物、不加新的工业材料体系、不加独立科技树**。所有设备都用原版材料就能合成 ——
铜锭与红石是核心，辅以玻璃、羊毛、铁、煤等原版物品。

## 技术栈

| 项目 | 版本 | 说明 |
| --- | --- | --- |
| Minecraft | 26.3 | Java 版正式版 |
| Fabric Loader | 0.19.5 | 该 MC 版本下 Fabric 官方标记为 stable |
| Fabric API | 0.161.0+26.3 | |
| Team Reborn Energy | 5.0.0 | 能量 API（与 TechReborn 同一套）。经 `include(...)` 内嵌进产物，玩家无需单独安装 |
| Fabric Loom | 1.18.2（`gradle.properties` 中声明 `1.18-SNAPSHOT`） | 插件 id 为 `net.fabricmc.fabric-loom` |
| Gradle | 9.8.0 | 由 wrapper 提供；**9.8.0 起才支持在 Java 27 上运行** |
| JDK | 运行 Gradle 用本机 JDK 27（`D:\jdk27`） | 模组字节码目标为 **Java 25** |

> 已在本机实测通过：**JDK 27 + Gradle 9.8.0 + Loom 1.18.2** 下执行 `gradlew build`，
> 并用 `gradlew runClient` / `runServer` 实机验证（模组加载、能量能力注册、导线传电、
> 方块与物品渲染均正常，详见下方运行期踩坑记录）。
> 产物 `build/libs/crimsoncoppergrid-0.0.7.jar`，字节码 major 69（Java 25）。

> **映射说明**：Yarn 目前**没有** 26.3 的映射（`meta.fabricmc.net/v2/versions/yarn/26.3` 返回空数组），
> 所以 `build.gradle` 里**不写 `mappings` 行**，由 Loom 1.18 默认采用 Mojang 官方映射。
> 这意味着模组代码使用官方类名，例如 `net.minecraft.resources.Identifier`（旧的 Yarn 名 `ResourceLocation` 不再适用）。

## 已实现的内容（0.0.7）

| 分类 | 方块/物品 | 数值与行为 |
| --- | --- | --- |
| 输电 | 铜制电线 | 六向自动连接（连接状态写进 blockstate），纯导体；自身带 128 FE 小缓冲，物理相连的导线合成一个共享池 |
| 输电 | 铜制电闸 | 右键或红石控制通断；**断开时在组网层面直接断路**，两侧成为彼此独立的电网 |
| 发电 | 燃料发电机 | 1 燃料刻 = 40 FE。一块煤 = 1600 刻 = **64 000 FE**；缓冲 4 000 FE |
| 发电 | 太阳能发电机 | 20 FE/t，仅白天 + 晴天 + 正上方天空光满值；缓冲 1 000 FE |
| 发电 | 风力发电机 | 10→30 FE/t，随 Y 高度线性增长（200 格封顶）；正上方需留空作为迎风面 |
| 储能 | 铜制电池 | 1 000 000 FE 容量，单 tick 收/发各 1 000 FE |
| 用电 | 电力熔炉 | 10 FE/烧炼刻 × 200 刻 = **2 000 FE/物品**；缓冲正好 2 000 FE（存量即进度） |
| 用电 | 电力煤炭合成机 | **8 000 FE = 1 块煤**，内部可攒 64 块，每 20 刻自动送进相邻容器 |
| 用电 | 电力岩浆机 | 30 FE/mB，25 刻产 1 mB → **1 桶 ≈ 750 000 FE**；内部罐 4 000 mB |
| 工具 | 电网总览器 | 右键任何接电方块或电线，报出设备类型 / 电量 / 档位 / 最大输入输出 |

十台设备全部走同一套能量基类，按档位限流（MICRO 8 / LOW 32 / MEDIUM 128 / HIGH 512 /
EXTREME 2 048 / INSANE 8 192 / INFINITE）。

**能量账本**（有意如此，便于评估平衡）：

- 一块煤 → 64 000 FE → 电力熔炉能烧 32 个物品，是原版熔炉（8 个）的 **4 倍**；
  但需要先铺出发电规模才能兑现。
- 电→煤 8 000 FE 换回可发 64 000 FE 的煤，**在能量上是赚的**：它的定位是把多余电力
  转成可携带燃料，不是永动机。嫌宽松就调 `CoalSynthesizerBlockEntity.FE_PER_COAL`。
- 电→岩浆是全模组最贵的一环（一桶 ≈ 12 块煤的电），因为它是**可再生岩浆**，
  太便宜会直接破坏生存平衡。

## 界面层

八台设备各有自己的界面，**全部空手右键方块打开**（有物品在手上时仍走各自的快捷交互，
例如煤炭合成机取煤、岩浆机倒桶）：

| 界面 | 布局 |
| --- | --- |
| 铜制电池 | 电量条 + 「正在充电 / 放电 / 待机」净流量 |
| 电力熔炉 | 输入槽 + 输出槽 + 电量条 + 烧炼进度条 |
| 燃料发电机 | 居中燃料槽 + 燃烧进度条 + 电量条 |
| 电力煤炭合成机 | 产物槽 + 电量条（**缓冲容量即一块煤的电量，所以这条同时也是进度条**） |
| 太阳能 / 风力发电机 | 电量条 + 额定输出与发电条件说明（客户端拿不到实时输出，见下） |
| 电力岩浆机 | 电量条 + 岩浆存量条 + 消耗率 |
| 电力控制器 | 电网总览：发电输出 / 用电输入 / 电池存量 / 导线与设备计数 |

**整个界面层不新增任何美术资源**：背景直接复用原版 `container/furnace.png`，
机器区用一块原版底色的面板盖掉后按各机器自己的布局重画；槽位凹槽原本画在那张贴图里，
被盖掉后需要按 `slot.x/slot.y` 用原版 `container/slot` sprite 补回来。

> 三台发电机界面只显示**额定输出**而不是实时输出 —— 客户端方块实体里 `currentOutput()`
> 恒为 0（真实值只在服务端算），而用同步包补一份实时值并不值当。额定值直接用方块实体的
> `public static final` 常量，零同步成本。

### 界面数据同步：`ContainerData` 每格只有 16 位

这是本项目踩得最深的一个坑，单列出来：

原版 `ClientboundContainerSetDataPacket` 的写入实现是

```java
buffer.writeContainerId(this.containerId);
buffer.writeShort(this.id);     // 下标：16 位
buffer.writeShort(this.value);  // 数值：16 位
```

也就是说 **`ContainerData` 的每个槽位在网络上只有 16 位有效，超出部分被静默丢弃**，
编译不报错、运行不抛异常。电量动辄上百万，若按「低 32 位 + 高 32 位」拆两格发送，
`1 000 000`（`0x000F4240`）到客户端只剩 `0x4240 = 16960`，于是 `stored > capacity`，
电量条整体填满、百分比显示 133%。

正确做法是按 **16 位一组拆成四格**，收发两侧都走 `common/menu/ContainerDataCodec`，
它同时负责拼回时的符号还原（`readShort` 会把 bit15 当符号位扩展，所以要先 `& 0xFFFF`）。

## 电力系统设计

能量的权威实现在 **Team Reborn Energy 5.0.0**（与 TechReborn 完全同一套），
分层与命名也对齐 TechReborn / RebornCore，便于以后直接参照 TR 的代码。

**核心模型是「推流」**，这也是 Team Reborn Energy 的官方约定：

> 电源负责把电**推向**邻居，机器和导线**不主动索取**。

由此得到一个很省心的性质：**只用电的设备只要把 `getBaseMaxOutput()` 设为 0，
推流就自动退化成空操作**，不需要写任何"我不接受推送"的保护代码。

- **设备侧**：`common/powerSystem/PowerAcceptorBlockEntity` 内部用官方的
  `SimpleSidedEnergyContainer`，转账走 Fabric `Transaction` —— 只有 `commit()` 时才真正生效，
  因此不会出现"扣了源头却没进目标"的半截状态。
- **导线侧**：`blockentity/cable/CableTickManager` 用 BFS 把物理相连的同级导线收集成一个
  **共享缓冲池**，每 tick 摊平能量再对相邻的非导线储能收发；
  `blockedSides` 位掩码避免同一对邻居在一个 tick 里来回弹跳。
  状态按 `ServerLevel` 隔离（用 `IdentityHashMap`），而不是像 TR 那样放在静态字段里，
  多世界/多维度下不会互相污染。
- **连通性判定不依赖 `instanceof`**：比较 `BlockEntityType` 对象 + 查 Fabric 官方的
  `EnergyStorage.SIDED` 查阅表。起因见下方踩坑记录里的"相邻电线不连"。
- 导线与电闸的差别只有一处：电闸的 `conducts()` 跟随 `powered` 状态，
  断开时组网 BFS 在此截断，**两侧自然成为两张独立的网**，不需要额外的"断路"特判。

## 目录结构

```
.
├── build.gradle                 构建脚本（Loom、Java 25、Fabric API、内嵌 energy）
├── settings.gradle              插件仓库与 rootProject.name
├── gradle.properties            所有版本号集中在这里
├── gradlew / gradlew.bat        Gradle 9.8.0 wrapper
├── gradle/wrapper/              wrapper jar 与 distribution 配置
├── dev-env/                     本机提取的开发前置资源（jar / JSON / JDK，见其 README）
└── src
    ├── main
    │   ├── java/com/cyx/crimsoncoppergrid/
    │   │   ├── CrimsonCopperGrid.java          公共入口，按顺序触发各注册表
    │   │   ├── common/                         对齐 TechReborn 的公共分层
    │   │   │   ├── powerSystem/                能量核心
    │   │   │   │   ├── PowerAcceptorBlockEntity.java   能量容器 + 侧向 I/O + 档位
    │   │   │   │   ├── CcgEnergyTier.java              档位表（对齐 RcEnergyTier）
    │   │   │   │   └── PowerSystem.java                数值格式化（FE 单位）
    │   │   │   ├── blockentity/MachineBaseBlockEntity.java  机器基类（tick / 同步 / 生命周期）
    │   │   │   └── blocks/BlockMachineBase.java        方块基类（FACING + ACTIVE）
    │   │   ├── blockentity/                    十台设备的方块实体
    │   │   │   └── cable/                      导线：CableBlockEntity / CableTickManager /
    │   │   │                                   SwitchBlockEntity / OfferedEnergyStorage
    │   │   ├── blocks/                         对应的十个方块类
    │   │   │   └── cable/                      CableBlock / SwitchBlock / CableShapeUtil
    │   │   ├── init/                           方块 / 方块实体 / 物品 / 物品栏 / 能量能力注册
    │   │   ├── item/EnergyMeterItem.java       电网总览器
    │   │   └── mixin/                          服务端注入示例
    │   └── resources/
    │       ├── fabric.mod.json 与 crimsoncoppergrid.mixins.json
    │       ├── assets/crimsoncoppergrid/
    │       │   ├── blockstates/  9 个方块状态
    │       │   ├── models/       block 与 item 模型
    │       │   ├── items/        ★ 26.3 必需的 item model definition（少了会全变紫黑方块）
    │       │   ├── lang/         中英双语
    │       │   └── icon.png      模组图标（128×128）
    │       └── data/crimsoncoppergrid/
    │           ├── recipe/        10 条合成表
    │           └── loot_table/    9 个方块的掉落表
    └── client
        ├── java/com/cyx/crimsoncoppergrid/client/
        │   ├── CrimsonCopperGridClient.java       客户端入口（ClientModInitializer）
        │   └── mixin/CrimsonCopperGridClientMixin.java
        └── resources/
            └── crimsoncoppergrid.client.mixins.json
```

## 构建与运行

首次构建会联网下载 Gradle 发行包、Loom、Minecraft 与依赖库（本机缓存约 1 GB，耗时数分钟）。

```powershell
# JDK：本机为 D:\jdk27；若 PATH 里没有 java，先设置 JAVA_HOME
$env:JAVA_HOME = 'D:\jdk27'

# 构建；产物在 build/libs/
.\gradlew.bat build

# 启动开发用客户端 / 服务端（Loom 自动生成 run/ 目录）
.\gradlew.bat runClient
.\gradlew.bat runServer

# 只做编译检查
.\gradlew.bat compileJava compileClientJava
```

> 关于单元测试：0.0.5 的自研电网结算层（`Grid` / `GridRegistry`）连同它的不变量测试
> 已在 0.0.6 一起移除 —— 结算职责现在归 Team Reborn Energy 与导线自己的缓冲池，
> 没有需要单独守护的自研结算逻辑。`build.gradle` 里保留了 JUnit 配置，供后续补充测试使用。

## 本机网络（代理）说明

这台机器**直连会失败**：`services.gradle.org` 与 Maven 仓库的直连会在 TLS 握手阶段超时，
必须走本地代理 `127.0.0.1:7890`。

**代理配置一律放在仓库之外**，即用户级文件 `~/.gradle/gradle.properties`：

```properties
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
```

> **不要把代理写进仓库里的 `gradle.properties` 或 `gradle/wrapper/gradle-wrapper.properties`。**
> 这两个文件会被提交，而 GitHub Actions 的构建机上并没有 `127.0.0.1:7890` 这个代理，
> 结果是每次 push 都失败在 `Connection refused`。
> 本项目 2026-10-02 之前的 **7 次 CI 全部失败**都是这个原因（见版本历史 0.0.7）。

如果哪天 `~/.gradle/wrapper/dists` 里的 Gradle 发行包被清掉、需要重新下载，
而 wrapper 读不到用户级代理配置，用环境变量临时指定即可：

```powershell
$env:JAVA_TOOL_OPTIONS = '-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890'
```

wrapper 的 `networkTimeout` 已从 10 秒提高到 300 秒，`retries` 设为 2。

## 开发注意事项

- 设备逻辑放 `src/main`（服务端权威），渲染与界面放 `src/client`。
  机器方块只在服务端 tick，客户端靠方块实体同步包拿到显示所需数据。
- 新增 mixin 时记得在两个 `*.mixins.json` 中登记对应类。
- **新增方块状态后必须同步 blockstate 的 variants**：只列了部分属性组合的 blockstate，
  一旦代码加入新属性就会整片匹配不到模型。用空 key `""` 的 blockstate 是通配，不受影响。
- 新增方块/物品时必须同时提供 **三样东西**：`blockstates/`、`models/`，以及
  `items/`（item model definition）与 `loot_table/`。缺 `items/` 只会让物品形态变紫黑方块，
  **服务端启动不会报任何错**，容易漏。
- 连通性与能量能力的判定**不要依赖自己类的 `instanceof`**，用 `BlockEntityType` 比较
  或 `EnergyStorage.SIDED` 查阅表，这样不会受类加载器/陈旧编译产物的影响。

### 运行期踩过的坑（都是实机跑 `runClient` 才暴露的）

| 坑 | 现象 | 正确做法 |
| --- | --- | --- |
| 物品图标全是洋红黑棋盘 | 只有 `models/item/*.json` 不够；方块在世界里渲染正常，**服务端启动毫无异常** | 26.3 必须有 `assets/<ns>/items/<id>.json`；同时注意 `models/item` 的 `parent` 要指向**模型**而不是 blockstate |
| 配方 pattern 里的空格 | 用 `.` 当空格 → `Pattern references symbol '.' but it's not defined in the key`，**客户端启动即崩**（`ReportedException: Registry Loading`） | 空格必须是**字面的 `' '`**，与原版一致 |
| 方块状态取值前没判属性 | 点电线时 `IllegalArgumentException: Cannot get property ... powered ... does not exist in Block{minecraft:grass_block}`，被服务端吞成 `Failed to handle packet` | 取属性前先 `state.hasProperty(...)`（或先判方块类型）。**短路或 `a \|\| b(state)` 会让 `b` 对任意方块状态都被调用** |
| 方块名对但 `instanceof` 为 false | v0.0.5 的"相邻电线不连"：坐标与方块名都打印正确，判定却始终 false | 正常 Java 语义下只能是**同一个类被加载了两份**（陈旧编译产物）。新代码改为比较 `BlockEntityType` 对象，从根上绕开 |
| 长会话日志轮转 | 退出时报 `Unable to delete file ... latest.log`，Gradle 以 exit 1 结束 | Windows 文件占用所致，**不是崩溃**（看日志末尾的 `Stopping!` 与 `BUILD SUCCESSFUL` 即可分辨） |

### 26.3 与旧版本不同的 API（本项目已踩过）

| 变化 | 说明 |
| --- | --- |
| `BlockEntityType.Builder` 已删除 | 构造函数公开，直接 `new BlockEntityType<>(supplier, Set.of(blocks))` |
| `Properties` 必须 `setId(ResourceKey)` | 方块与物品都是，否则启动报错 |
| 物品必须有 item model definition | `assets/<ns>/items/<id>.json`，内容形如 `{"model":{"type":"minecraft:model","model":"..."}}` |
| `BaseEntityBlock` 不再覆写 `getRenderShape` | 默认继承 `BlockBehaviour` 的 MODEL，机器方块**不需要**显式覆写（旧版本会返回 INVISIBLE） |
| 方块实体存档 API | 从 `CompoundTag` 换成 `ValueInput` / `ValueOutput` |
| `Block` / `BlockBehaviour` 没有 `codec()` | 不要照旧版写 `simpleCodec(...)` 与 `codec()` 覆写 |
| `BlockEntity#loadAdditional/saveAdditional` | 参数类型同上改成 `ValueInput` / `ValueOutput` |
| `Item#getCraftingRemainder()` | 返回 `ItemStackTemplate`，需 `.create()` 转回 `ItemStack` |
| `Level#canSeeSky` 不存在 | 用 `getBrightness(LightLayer.SKY, pos)` 判断是否露天 |
| `LevelReader#hasChunkAt` 已过时 | 改用 `isLoaded`（项目里已打开 `-Xlint:deprecation`） |
| 交互分发 | 主手有物品走 `useItemOn`，空手走 `useWithoutItem`，两者互斥 |
| loot table 字段名 | item entry 用 `"name"`，而配方 `result` 用 `"id"`，两套不通用；目录是单数 `loot_table/` |

## 版本历史

| 版本 | 内容 |
| --- | --- |
| 0.0.7 | **八台设备的界面层**：电池 / 电力熔炉 / 电力控制器 / 燃料发电机 / 电力煤炭合成机 / 太阳能 / 风力 / 电力岩浆机，全部空手右键打开；背景复用原版容器贴图，界面层零新增美术资源。**修掉界面电量读数全部错乱的根因** —— `ClientboundContainerSetDataPacket` 的 `value` 是 `writeShort`，`ContainerData` 每格只有 16 位，原来的「低 32 位 + 高 32 位」拆法被静默截断（`1 000 000` 变成 `16960`，电量条填满、显示 133%）；改为按 16 位拆四格并抽成 `ContainerDataCodec`。**修复 CI**：把本机代理从仓库配置里移出（此前 7 次构建全部因 `Connection refused` 失败）。 |
| 0.0.6 | **能量核心换成 Team Reborn Energy，删掉自研 Grid**。分层与命名对齐 TechReborn / RebornCore；能量模型改为官方「推流」。导线重写为共享缓冲池（按维度隔离），连通性判定不再依赖 `instanceof`，修掉"相邻电线不连"。十台设备全部迁移到新的能量基类。补齐 9 个方块掉落表与 10 个 item model definition（修掉物品图标全紫黑）、补模组图标。 |
| 0.0.5 | 修正电线放置位置与邻居通知；相邻电线连接问题仍未解决。 |
| 0.0.4 | 修复启动即崩的配方与点电线抛异常的问题，完成首次实机验证。 |

## 致谢与来源

- 能量 API：[Team Reborn Energy](https://github.com/TechReborn/Energy)（MIT），以内嵌方式打包。
- 分层结构、导线共享池模型与界面层的设计参考 [TechReborn](https://github.com/TechReborn/TechReborn) /
  RebornCore（MIT）。参考到的部分在代码注释中标注了出处。

## 许可

MIT，见 [LICENSE](LICENSE)。
