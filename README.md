# RPGMaker MV To Android

可用于把使用 RPG Maker MV 制作的游戏打包成安卓版。

### 使用方法

1. 将游戏资源存放到 **_app/src/main/assets/_** 目录下（RPGMMV 游戏目录下的'www'文件夹内的文件，不包含'www'文件夹）后打包即可（ **注意不要替换掉本项目的中 js 文件夹中的文件** ）

2. **注意！！！**请将项目中的 **“rpg_managers.js”和“gameEnd.js”、“UTA_CommonSave.js”（如果游戏中原本有的话）** 这几个文件里的**代码**，注意，是**代码**，**不是整个文件**，复制**替换**掉你的 RPGMMV 游戏里**同文件名**的文件里的代码。

3. 将 **_app/src/build.gradle.kts_** 中的 **applicationId = "game.YourGameName"** 改成你自己的游戏名称（英文）比如：**my.xiaohuangyou**，用于显示路径包名；将 **_app/src/main/res/values/strings.xml_** 中的 **YourGameName** 改成你自己的游戏名称，用于显示 App 的名称。

4. 使用 Android Studio 打开项目，删除 **_app/src/main/res_** 目录下的 mipmap ，然后右键 **_app/src/main/res_** 点击 **“New -> Image Asset”** 添加你自己的游戏图标，图标的 **“Name”** 需要和 **_app/src/main/AndroidManifest.xml_** 中的 **“android:icon”**、**“android:roundIcon”** 一致。 最后编译即可。

### 特性

- 点击游戏中存档的保存按钮后，存档将直接保存到 **_Android/data/game.YourGameName/files/save/_** 目录下，并且存档可以和 PC 版互通。

- 不需要获取任何权限，非 ROOT 用户可以直接进入 **_Android/data/game.YourGameName/files/save/_** 目录下对存档进行导入、导出操作。

- 游戏出现错误时会把错误记录到 **_Android/data/game.YourGameName/files/log.txt_** 文件中。

- 游戏加载、保存时会将存档文件（除了global.rpgsave、config.rpgsave、common.rpgsave）缓存到 **_Android/data/game.YourGameName/cache/save/_** 目录下，下次进入游戏时，会优先从缓存中加载，提高加载、读档速度。

### 捐赠

如果你希望这个项目，欢迎通过爱发电给我打赏：https://afdian.com/a/yun3812528
