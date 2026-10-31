# AGENT.md

## Background
    You are a minecraft mod developer, you are develop a mod which based on Minecraft-Transit-Railway 4.0

## Files Root
    ":fabric": 总体代码文件，除了mapping文件夹、NanbinFabric.java、NanbinClient.java、ModmenuConfig.java其他都是通用类
    ":forge": Forge自己的 NanbinForge.java、NanbinClientForge.java、mapping
代码全部写:fabric里面，只有forge本身的mapping可以写:forge里面

## Develop Rules
    注释简洁，只写重点
    禁用"/**""// --------""/* ------ */"等书写
    优先使用mtr私有类，没有则使用mapping映射原版

## Stonecutter 多版本
    版本列表：libs/ 中的 MTR-<loader>-<mtr>+<mc>[-server].jar，并与 gradle.properties 的 stonecutterVersions 合并。
    子项目：:fabric:<mc> 和 :forge:<mc>；共享源码在 fabric/src、forge/src。
    当前编辑版本（active）在 stonecutter.gradle 中设置，默认 1.20.1。
    切换 active 版本：./gradlew stonecutterSwitchTo<mc>（如 stonecutterSwitchTo1.20.4）
    构建单版本：./gradlew :fabric:1.20.1:build :forge:1.20.1:build
    生成 IDE 启动配置：./gradlew ideaRunConfigs
        （在 .idea/runConfigurations 生成各版本的 “Minecraft Client/Server (:fabric:<mc>)”、“(:forge:<mc>)”，
          对应 runClient / runServer，位于 IDE 的 Minecraft 分组下）
    构建全部版本：./gradlew build
    版本条件注释（Stonecutter 0.7.x 语法，没有 if 关键字）：
        //? >=1.19.4 {
        // 新版本代码
        //?} else {
        /*旧版本代码
        *///?}
    仅某个版本使用的文件放在 <branch>/versions/<mc>/src/main/java/ 下。

## Final Check
    When do the final check, run"./gradlew :fabric:1.20.1:build :forge:1.20.1:build --debug" to check the code

## Editor
    LIEH-SAD

