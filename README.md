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
> 产物 `build/libs/crimsoncoppergrid-0.0.1.jar`，字节码 major 69（Java 25）。
> 本机没有独立安装 JDK 25，Gradle 直接跑在 JDK 27 上；若你换成 JDK 25，把 wrapper 降回 9.7.1 也可以。

> **映射说明**：Yarn 目前**没有** 26.3 的映射（`meta.fabricmc.net/v2/versions/yarn/26.3` 返回空数组），
> 所以 `build.gradle` 里**不写 `mappings` 行**，由 Loom 1.18 默认采用 Mojang 官方映射。
> 这意味着模组代码使用官方类名，例如 `net.minecraft.resources.Identifier`（旧的 Yarn 名 `ResourceLocation` 不再适用）。

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
    │   │   ├── CrimsonCopperGrid.java             公共入口（ModInitializer）
    │   │   └── mixin/CrimsonCopperGridMixin.java  服务端注入示例
    │   └── resources/
    │       ├── fabric.mod.json                    模组元数据
    │       └── crimsoncoppergrid.mixins.json
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

## 后续开发建议

- 能量网络的核心逻辑放 `src/main`（服务端权威），渲染与界面放 `src/client`。
- 新增 mixin 时记得在两个 `*.mixins.json` 中登记对应类。
- `src/main/resources/assets/crimsoncoppergrid/icon.png` 尚未提供：缺少图标只会让加载器打印一条警告，不影响运行；补一张 128×128 PNG 即可。

## 许可

MIT，见 [LICENSE](LICENSE)。
