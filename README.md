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
> 产物 `build/libs/crimsoncoppergrid-1.1.0.jar`，字节码 major 69（Java 25）。

> **映射说明**：Yarn 目前**没有** 26.3 的映射（`meta.fabricmc.net/v2/versions/yarn/26.3` 返回空数组），
> 所以 `build.gradle` 里**不写 `mappings` 行**，由 Loom 1.18 默认采用 Mojang 官方映射。
> 这意味着模组代码使用官方类名，例如 `net.minecraft.resources.Identifier`（旧的 Yarn 名 `ResourceLocation` 不再适用）。

## 已实现的内容（1.1.0）

| 分类 | 方块/物品 | 数值与行为 |
| --- | --- | --- |
| 输电 | 铜制电线 | 六向自动连接（连接状态写进 blockstate），纯导体；自身带 128 FE 小缓冲，物理相连的导线合成一个共享池 |
| 输电 | 铜制电闸 | 右键或红石控制通断；**断开时在组网层面直接断路**，两侧成为彼此独立的电网。外观是**在线开关**（面板 + 可扳动的拉杆手柄，见「方块外观」） |
| 发电 | 燃料发电机 | 1 燃料刻 = 40 FE。**接受原版全部燃料**（木头、煤炭、木炭、岩浆桶、干海带块等）；燃烧时长与原版熔炉一致。缓冲 4 000 FE |
| 发电 | 太阳能发电机 | 20 FE/t，仅白天 + 晴天 + 正上方天空光满值；缓冲 1 000 FE |
| 发电 | 风力发电机 | **10 / 20 / 30 FE/t 三档，由「塔高」决定**：正下方要有一列连续的方块（中间不能镂空）通到「风机底座」，底座与发电机之间隔 3~5 格 = 10、6~8 格 = 20、9 格及以上 = 30；不足 3 格或找不到底座则不发电。正上方需留空作为迎风面。因为量的是「与底座的高度差」而不是 Y 坐标，**同一座塔在任何海拔输出都一样**。外观是铜块 + 正面挂一个会转的三叶片叶轮（移植自 TechReborn） |
| 发电 | 风机底座 | 风力发电机的塔基，也是它测高的唯一锚点。本身不发电、不接电、没有方块实体，只是标记方块；外观是铜底盘 + 红石芯 + 中柱 |
| 储能 | 铜制电池 | 1 000 000 FE 容量，单 tick 收/发各 1 000 FE。外观是**立式电芯**（红石环带通电发亮，见「方块外观」） |
| 用电 | 电力熔炉 | 10 FE/烧炼刻 × 200 刻 = **2 000 FE/物品**；缓冲正好 2 000 FE（存量即进度）。外观是**铜块刻字**（正面「电力熔炉」、背面「ELECTRIC FURNACE」，见「方块外观」） |
| 用电 | 电力煤炭合成机 | **8 000 FE = 1 块煤**，内部可攒 64 块，每 20 刻自动送进相邻容器。外观是**铜块刻字**（正面「电力煤炭合成机」、背面「COAL SYNTHESIZER」，见「方块外观」） |
| 用电 | 电力岩浆机 | 界面像熔炉：左槽放空桶，右槽出岩浆桶，中间进度；每 **600 刻**灌满一桶，每桶耗电 **30 000 FE** |

这些机器方块全部走同一套能量基类，按档位限流（MICRO 8 / LOW 32 / MEDIUM 128 / HIGH 512 /
EXTREME 2 048 / INSANE 8 192 / INFINITE）。

**能量账本**（有意如此，便于评估平衡）：

- 一块煤 → 64 000 FE → 电力熔炉能烧 32 个物品，是原版熔炉（8 个）的 **4 倍**；
  但需要先铺出发电规模才能兑现。
- 电→煤 8 000 FE 换回可发 64 000 FE 的煤，**在能量上是赚的**：它的定位是把多余电力
  转成可携带燃料，不是永动机。嫌宽松就调 `CoalSynthesizerBlockEntity.FE_PER_COAL`。
- 电→岩浆按当前常数是 **30 000 FE/桶**（30 FE/mB × 1000 mB），约等于 0.5 块煤的电。
  这个数值是给可再生岩浆定的成本锚点，若嫌便宜/太贵可调
  `LavaGeneratorBlockEntity.FE_PER_MB`。

## 合成表

全部 10 条配方都在工作台上按 3×3 图案摆放，**材料清一色是原版物品**，没有任何模组中间产物
（也就是说不存在"必须先做 A 才能做 B"的解锁链，开局挖到铜就能开工）。
下面的网格按「上排 → 中排 → 下排」书写，`—` 表示该格留空。

### 铜制电线 ×3

```
铜锭  铜锭  铜锭
—     —     —
—     —     —
```

一条铜换三根线，是最先要量产的东西。

### 铜制电闸

```
铜锭  铜锭  铜锭
—     铜锭  —
—     —     —
```

### 铜制电池

```
铜锭  铜锭   铜锭
铜锭  红石块 铜锭
铜锭  铜锭   铜锭
```

铜锭把整块红石包起来。八个铜的造价对应它 1 000 000 FE 的容量。

### 燃料发电机

```
红石  铜锭  铜锭
铜锭  熔炉  铜锭
铜锭  铜锭  红石
```

### 太阳能发电机

```
玻璃  玻璃  玻璃
铜锭  铜锭  铜锭
红石  —     红石
```

顶部三块玻璃是"采光面"，也是它需要露天的外观暗示。

### 风力发电机

```
白色羊毛  白色羊毛  白色羊毛
铜锭      红石      铜锭
铜锭      铜锭      铜锭
```

顶部三块羊毛是叶片材料。**它必须配一个风机底座才有输出。**

### 风机底座

```
铜锭  避雷针  铜锭
铜锭  红石    铜锭
铜锭  铜锭    铜锭
```

避雷针做中柱，红石做芯 —— 风机靠它测塔高，详见「方块外观 · 风机底座」。

### 电力熔炉

```
铜锭  铜锭  铜锭
铜锭  熔炉  铜锭
铜锭  红石  铜锭
```

### 电力煤炭合成机

```
铜锭  铜锭  铜锭
铜锭  煤炭  铜锭
铜锭  红石  铜锭
```

和电力熔炉只差中间那颗（煤炭 ↔ 熔炉）；**红石都在正中下方**。

### 电力岩浆机

```
铜锭  铜锭    铜锭
铜锭  岩浆桶  铜锭
铜锭  红石    铜锭
```

> **别把电力熔炉和燃料发电机搞混**：两者的判定中心都是熔炉，区别只在下方红石的位置 ——
> 电力熔炉是**正中下方**（`CRC`），燃料发电机挪到了**对角**（`RCC` / `CCR`）。
> 这么改是因为两者原本图案完全相同，原版 `RecipeManager` 遇到重复配方只保留先加载的一条，
> 另一条会静默失效（详见版本历史 1.0.0）。

## 界面层

七台设备各有自己的界面，**全部空手右键方块打开**（有物品在手上时仍走各自的快捷交互，
例如煤炭合成机取煤）：

| 界面 | 布局 |
| --- | --- |
| 铜制电池 | 电量条 + 「正在充电 / 放电 / 待机」净流量 |
| 电力熔炉 | 输入槽 + 输出槽 + 电量条 + 烧炼进度条 |
| 燃料发电机 | 居中燃料槽 + 燃烧进度条 + 电量条 |
| 电力煤炭合成机 | 产物槽 + 电量条（**缓冲容量即一块煤的电量，所以这条同时也是进度条**） |
| 太阳能 / 风力发电机 | 都不画电量条。太阳能报状态（正在发电 / 夜间停机 / 雨天停机 / 被遮挡）；风力报当前输出（或停机原因）+ 当前塔高 + 档位表 + 两个硬性条件 |
| 电力岩浆机 | 左放空桶 + 右出岩浆桶 + 灌注进度条 + 电量条 |

**整个界面层不新增任何美术资源**：背景直接复用原版 `container/furnace.png`，
机器区用一块原版底色的面板盖掉后按各机器自己的布局重画；槽位凹槽原本画在那张贴图里，
被盖掉后需要按 `slot.x/slot.y` 用原版 `container/slot` sprite 补回来。

> 两台发电机界面都**不显示内部缓冲存量** —— 缓冲只有 1 000 FE（太阳能 2.5 秒填满、风力满输出
> 也只要 33 秒），读数不是贴着上限就是 0，对玩家没有信息量。面板改成回答「现在在不在发电、
> 在发多少、没在发是为什么」：随世界变化的那几个量（太阳能的状态、风力的输出与塔高）走
> `ContainerData` 同步；档位表之类的常量直接引用方块实体的 `public static final` 字段与静态换算
> 方法，零同步成本。

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

### 界面文字：不要画投影

26.3 的 `GuiGraphicsExtractor` 有两个 `text` 重载：

- `text(font, text, x, y, color)` —— 内部固定传 `dropShadow = true`；
- `text(font, text, x, y, color, dropShadow)` —— 由调用方决定。

原版 `AbstractContainerScreen.extractLabels` 画标题和「物品栏」标签时用的是**不画投影**的版本。
中文方块字本来就笔画密，再叠一层向 `+1,+1` 偏移的暗色投影，在浅灰底板上会糊成
「重叠模糊」的一团 —— 这正是之前 GUI 文字看不清的原因。CCG 的界面统一改为显式传
`dropShadow = false`，和原版容器文字保持一致。

## 方块外观

外观按「一眼能认出是什么」来设计，全部用原版贴图 + 少量自制小图，不引入任何建模依赖。

### 电力熔炉：铜块上刻字

原来的模型把原版 `furnace_front_on` 和 `iron_block` 混在一起，既不像熔炉也不像铜。
现在改成真正的**铜方块 + 正面刻字**：

- 六个面都用原版 `minecraft:block/copper_block`，就是个铜块。
- 正面叠一层透明贴图 `electric_furnace_label.png`，上面用黑体按 2×2 排布刻着
  **「电力熔炉」**四个汉字（上排「电力」，下排「熔炉」）。
- 字号尽可能大（256×256 贴图里每个字约 120×120 像素），远看是块铜，凑近能认出字。
- blockstate 加上 `FACING`，让刻字面朝向放置者；背面叠 `*_label_en.png` 印英文。

### 电力煤炭合成机：与电力熔炉同风格的铜块刻字

- 六个面同样用原版 `copper_block`。
- 正面贴图 `coal_synthesizer_label.png`，黑体 3 行刻 **「电力煤炭合成机」**
  （上排「电力」，中排「煤炭」，下排「合成机」），与物品名完全一致。
- 背面贴图 `coal_synthesizer_label_en.png`，印 **「COAL SYNTHESIZER」**。
- 与电力熔炉共享同一套 blockstate 旋转逻辑，刻字面跟随 `FACING`。

### 风力发电机：铜块机壳 + TechReborn 叶轮

- 方块模型回到铜块，六面原版 `copper_block`，正面「风力发电机」、背面「WIND GENERATOR」。
- 真正「像风车」的部分是一个**客户端方块实体渲染器**：三个带桨距角的叶片绕水平轴旋转，
  叶轮挂在方块正面外侧（叶片扫过的圆直径约 3 格），对应贴图是移植自 TechReborn 的
  `wind_mill_turbine.png`（MIT）。
- 转速随档位变化：档位越高叶轮越快，逐刻加速/减速（有惯性感），停机时慢慢停下。
- 物品栏图标是独立绘制的 32×32 风车小图（塔 + 叶轮），和铜块机器的物品图标风格区分开。

### 风机底座：风力发电机的测高锚点

风力发电机的发电量按**塔高**分档，而塔高是玩家搭出来的结构 —— 世界数据里看不出哪几块是玩家
放的，一根插在平原上的土柱和它脚下的泥土在数据上完全一样。所以需要一个明确的锚点：

- 底座放哪儿，塔就从哪儿开始量；发电机向下扫自己所在的**那一列**，数到底座为止；
- 数出来的「间隔方块数」就是塔高，**底座本身不算**，所以「底座 + 3 格方块 + 发电机」是塔高 3；
- 途中遇到空气即判定塔身断开（防止把发电机浮在底座上方 9 格白拿满输出）；
- 扫塔有 10 刻（0.5 秒）的缓存，且最多向下扫 64 格 —— 兜住「发电机直接放在地面上」这种
  会一路扫到基岩的情况。

因为量的是「与底座的高度差」而不是 Y 坐标，**同一座塔在平原（y=64）和空岛（y=200）输出完全
一致**，玩家在哪里建基地都不吃亏。外观是铜底盘 + 红石芯 + 中柱，全部复用原版铜系贴图。

### 铜制电闸：在线开关，不是一块铜

原来它整块渲染成 16³ 铜方块，和电池、和普通铜块都分不清。现在改成现实里装在电线上的
那种**在线开关**：

- **导线中枢**（与 `cable_core` 一致）+ **方形安装面板** + **可 ±45° 扳动的拉杆手柄**
- 通断两态靠**手柄倾倒方向**区分：断开往一侧倒，导通往另一侧倒 ——
  `switch_on.json` 用的 `-45°` 正是原版 `lever.json`（`powered=true`）的角度，
  所以读法和原版拉杆完全一致
- `SwitchBlock` 新增 `FACING`（水平朝向），手柄总是朝向放置者，和原版拉杆一样
- 自制 `switch_lever.png` 画铜色手柄，取样区沿用原版 `lever.png` 的 `x=7..9, y=6..16`
- blockstate 用 **multipart**：8 条 `powered × facing`（手柄）＋ 6 条导线臂 ——
  电闸因此能和导线**在视觉上连成一体**

> 26.3 里没有 `DirectionProperty`，水平朝向要用 `BlockStateProperties.HORIZONTAL_FACING`
> （类型是 `EnumProperty<Direction>`）。

> 顺带修了一个既有缺陷：电闸的选取/碰撞盒原来只有 2×2×2 的核心（`half = thickness×16`），
> 是「看得见、点不着」的。既然外观变大，形状也一并放大到面板 + 拉杆的包络，
> 否则玩家点不到那根拉杆。

### 铜制电池：立式电芯

原来也是 16³ 铜块（正面贴红石块）。现在是**立式电芯**：

- 底座 + 铜壳（12 宽柱体，四周留空不再顶着方块边）+ `cut_copper` 顶盖 +
  顶部**黄铜正极凸台** —— 凸台是「这是电池」最关键的信号
- 一圈**红石环带**箍在电芯上部，四面都能看见，凸出 0.1 格避免与上下壳体 z-fighting
- **环带会亮**：`FACING` + `ACTIVE` 两个状态终于被用上（此前 blockstate 是空 key 通配，
  等于白白浪费）。`BatteryBlockEntity.serverTick()` 里已有的 `setActive(powerChange != 0)`
  直接驱动 —— **通电时环带亮红石红，闲置时暗红**
- 新增 `battery_terminal` / `battery_band_off` / `battery_band_on` 三张小贴图
- **物品栏图标指向会亮的那一个变体**（`battery_active`）：闲置的暗环带在深色 GUI 里
  几乎看不见，红环带才醒目

> 碰撞/选取形状**故意保持满块**：电池是机器不是装饰，满块支持顶面放火把/红石，
> 也和其余八台机器保持一致。

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
    │   │   ├── blockentity/                    九台设备的方块实体（**风机底座没有方块实体**）
    │   │   │   └── cable/                      导线：CableBlockEntity / CableTickManager /
    │   │   │                                   SwitchBlockEntity / OfferedEnergyStorage
    │   │   ├── blocks/                         对应的九个方块类
    │   │   │   └── cable/                      CableBlock / SwitchBlock / CableShapeUtil
    │   │   ├── init/                           方块 / 方块实体 / 物品栏 / 能量能力注册
    │   │   └── mixin/                          服务端注入示例
    │   └── resources/
    │       ├── fabric.mod.json 与 crimsoncoppergrid.mixins.json
    │       ├── assets/crimsoncoppergrid/
    │       │   ├── blockstates/  10 个方块状态
    │       │   ├── models/       block 与 item 模型
    │       │   ├── items/        ★ 26.3 必需的 item model definition（少了会全变紫黑方块）
    │       │   ├── lang/         中英双语
    │       │   └── icon.png      模组图标（128×128）
    │       └── data/crimsoncoppergrid/
    │           ├── recipe/        10 条合成表
    │           └── loot_table/    10 个方块的掉落表
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
- **模型画到多远，和玩家能点到多远，是两件独立的事**。`VoxelShape` 决定选取与碰撞，
  模型只决定外观。只改模型不改形状，就会出现「看得见、点不着」——电闸曾长期如此
  （形状仍是最初按导线粗细算的 2×2×2 核心）。换外观时务必一并核对 `getShape` /
  `getCollisionShape`。

### 运行期踩过的坑（都是实机跑 `runClient` 才暴露的）

| 坑 | 现象 | 正确做法 |
| --- | --- | --- |
| 物品图标全是洋红黑棋盘 | 只有 `models/item/*.json` 不够；方块在世界里渲染正常，**服务端启动毫无异常** | 26.3 必须有 `assets/<ns>/items/<id>.json`；同时注意 `models/item` 的 `parent` 要指向**模型**而不是 blockstate |
| 配方 pattern 里的空格 | 用 `.` 当空格 → `Pattern references symbol '.' but it's not defined in the key`，**客户端启动即崩**（`ReportedException: Registry Loading`） | 空格必须是**字面的 `' '`**，与原版一致 |
| 方块状态取值前没判属性 | 点电线时 `IllegalArgumentException: Cannot get property ... powered ... does not exist in Block{minecraft:grass_block}`，被服务端吞成 `Failed to handle packet` | 取属性前先 `state.hasProperty(...)`（或先判方块类型）。**短路或 `a \|\| b(state)` 会让 `b` 对任意方块状态都被调用** |
| 方块名对但 `instanceof` 为 false | v0.0.5 的"相邻电线不连"：坐标与方块名都打印正确，判定却始终 false | 正常 Java 语义下只能是**同一个类被加载了两份**（陈旧编译产物）。新代码改为比较 `BlockEntityType` 对象，从根上绕开 |
| `useItemOn` 返回 `PASS` 会吞掉空手交互 | 电力熔炉 / 煤炭合成机右键打不开界面；26.3 的方块交互是「先用 `useItemOn`，若它返回 `TRY_WITH_EMPTY_HAND` 才调用 `useWithoutItem`」| 想让方块最终能打开界面，**不能返回 `PASS`**（`PASS` = 已表态、不再继续），必须返回 `InteractionResult.TRY_WITH_EMPTY_HAND` |
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
| 交互分发 | `useItemOn` 是「第一道闸」，主手有物品或空手都会先走它；默认返回值是 `InteractionResult.TRY_WITH_EMPTY_HAND`，含义是「请继续走 `useWithoutItem`」。若自定义 `useItemOn` 时返回 `PASS`，会阻止后续空手交互，从而打不开界面 |
| `DirectionProperty` 已删除 | 水平朝向直接用 `BlockStateProperties.HORIZONTAL_FACING`（类型是 `EnumProperty<Direction>`） |
| loot table 字段名 | item entry 用 `"name"`，而配方 `result` 用 `"id"`，两套不通用；目录是单数 `loot_table/` |

## 版本历史

| 版本 | 内容 |
| --- | --- |
| 1.1.0 | **风力发电机改为「塔高制」，与海拔解耦**：原来按 Y 坐标线性增长（海平面 10 → y200 满 30）在空岛或平地上手感都很怪，同一座塔换个高度输出就变。现在改成**向下扫描自己所在那一列、数到「风机底座」为止**，按间隔方块数分三档 —— 3~5 格 10 FE/t、6~8 格 20、9 格及以上 30；不足 3 格或找不到底座则不发电。因为量的是"与底座的高度差"而非 Y 坐标，**同一座塔在任何海拔输出都一样**。途中遇到空气即判定塔身断开（防止把发电机浮在底座上方白拿满输出），扫塔带 10 刻缓存、最多下探 64 格。**新增方块「风机底座」**：纯标记方块（无方块实体、不接电），铜底盘 + 红石芯 + 中柱，是测塔高的唯一锚点。**两台发电机面板重做**：都删掉电量条（缓冲 1 000 FE 太容易满，读数没有信息量），太阳能改为报状态（正在发电 / 夜间停机 / 雨天停机 / 被遮挡），风力改为报当前输出 + 停机原因 + 当前塔高 + 档位表 + 硬性条件，随世界变化的量走 `ContainerData` 同步。**修两处界面缺陷**：① 所有面板右下角一条细线画的方框 —— 26.3 的 `GuiGraphicsExtractor.outline` 签名是 `(x, y, width, height, color)` 而非两角语义，原写法把方框一路拉到界面边缘；② 「额定输出 20 **FE FE**/t」单位重复 —— `PowerSystem.getLocalizedPower()` 返回值已自带单位，模板又拼了一次，相关三处改用 `getLocalizedPowerNoSuffix()`。**README 补全合成表**。|
| 1.0.0 | **首个正式版**：九台方块、七套界面、九条配方全部实机跑通，功能面收敛完成，脱离 0.0.x 开发序列。**删除电力控制器**：整体移除方块 / 方块实体 / 菜单 / 界面 / 模型 / 贴图 / 战利品表 / 合成表与全部相关语言键（`controller.*` 六条），注册链与创造模式物品栏同步清理。原因：电网总览的读数依赖对整张电网的实时扫描，与「推流 + 共享缓冲池」的能量模型耦合过深，维护成本高于它带来的价值；模组回到「输电 / 储能 / 发电 / 用电」四类纯功能方块。**修燃料发电机配方与电力熔炉撞车**：两者原本都是 `CCC / CFC / CRC`（铜锭围熔炉、下方红石），原版 `RecipeManager` 遇重复配方只保留先加载的一条并记警告，另一条静默失效。燃料发电机改为**红石挪到对角**的 `RCC / CFC / CCR`，电力熔炉保持原样。|
| 0.0.12 | **风力发电机外观重做**：方块模型改为铜块 + 双面刻字（正面「风力发电机」、背面「WIND GENERATOR」），正面外侧加一个移植自 TechReborn 的三叶片旋转叶轮（`TurbineRenderer` + `wind_mill_turbine.png`），转速随风速变化、带加速/减速惯性；物品栏图标改为独立的风车小图。**客户端方块实体动画**：为风力发电机单独开启客户端 tick，在 `WindGeneratorBlockEntity#clientTick` 里推进 `bladeAngle` / `spinSpeed`；`MachineBaseBlockEntity` 新增 `clientTick()` 钩子供将来复用。 |
| 0.0.11 | **电力煤炭合成机刻字改成完整物品名**：正面由「煤炭合成」改为「电力煤炭合成机」（3 行排布），背面保持「COAL SYNTHESIZER」；刻字贴图统一提升到 256×256，中英文字都更锐利。**电力控制器改为铜块刻字**：六个面全部用原版铜块，正面「电力控制器」、背面「POWER CONTROLLER」，与电力熔炉/煤炭合成机共用同一套双面刻字模型结构。**删除电网总览器**：移除 `EnergyMeterItem`、物品注册、模型、合成表与所有相关语言键；创造模式物品栏里不再显示。 |
| 0.0.10 | **电力煤炭合成机外观重做**：改为铜方块 + 双面刻字（正面「煤炭合成」、背面「COAL SYNTHESIZER」），与电力熔炉共用同一套模型结构。**电力熔炉补英文背面**：正面「电力熔炉」，背面「ELECTRIC FURNACE」。**燃料发电机燃料数据化**：改用 `DataComponents.COOKING_FUEL` 判定原版燃料，木头、煤炭、木炭、岩浆桶、干海带块等原版熔炉能烧的都能烧；燃烧时长走原版 `context_int_provider` + LootContext 解析，与熔炉一致；修复空燃料槽存档报错（`ItemStack.CODEC` 不允许空栈 → 改用 `OPTIONAL_CODEC`）。 |
| 0.0.9 | **修 GUI 文字重影/模糊**：`MachineScreen` 的 `drawText` / `drawCenteredText` 改为显式传 `dropShadow=false`，与原版容器标签保持一致；标题改居中。**修电力熔炉/煤炭合成机右键打不开界面**：方块 `useItemOn` 在主手为空时必须返回 `InteractionResult.TRY_WITH_EMPTY_HAND` 才会继续走 `useWithoutItem` 开界面，返回 `PASS` 会把交互吞掉。**电力熔炉外观重做**：改为铜方块 + 正面透明贴图刻「电力熔炉」四字，2×2 排布，blockstate 加 `FACING` 让刻字面朝向放置者。**电力岩浆机逻辑重构**：从「内部岩浆罐」改为「左槽放空桶 → 右槽出岩浆桶」的灌注机，中间是进度条，每 600 刻灌满一桶，耗电 30 000 FE/桶；方块层不再处理桶交互，全部走 GUI。 |
| 0.0.8 | **电闸与电池的外观重做**（此前的两轮实机迭代）。**电闸**从 16³ 实心铜块改成「导线中枢 + 方形面板 + 可扳动的拉杆手柄」，通断两态靠手柄倾倒方向区分（角度照抄原版 `lever.json`）；新增 `FACING` 让手柄朝向放置者；blockstate 改 multipart，8 条 `powered × facing` + 6 条导线臂，电闸在视觉上与导线连成一体；顺带修掉「选取盒只有 2×2×2、看得见点不着」的既有缺陷。**电池**从 16³ 铜块改成**立式电芯**（底座 + 12 宽铜壳 + 顶盖 + 黄铜正极凸台 + 红石环带），并把一直被空 key blockstate 浪费掉的 `FACING`+`ACTIVE` 用上 —— 通电时环带亮红石红、闲置暗红；物品图标指向发亮变体。新增 4 张自制小贴图，其余复用原版铜系贴图，界面层与外观均零新增美术依赖。 |
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
