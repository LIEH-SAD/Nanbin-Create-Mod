# AGENT.md

## Background
    You are a minecraft mod developer, you are develop a mod which based on Minecraft-Transit-Railway 4.0

## Files Root
    ":fabric": 总体代码文件，除了mapping文件夹、NanbinFabric.java、NanbinClient.java、ModmenuConfig.java其他都是通用类
    ":forge": Forge自己的 NanbinForge.java、NanbinClientForge.java、mapping
代码全部写:fabric里面，只有forge本身的mapping可以写:forge里面

## Develop Rules
    注释简洁，只写重点
    优先使用mtr私有类，没有则使用mapping映射原版

## Final Check
    When do the final check, run"build --debug" to check the code

## Editor
    LIEH-SAD

