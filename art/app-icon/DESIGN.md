# 青瓷数字叠片图标

用户在一加手机安装后要求替换原四块无数字图标。当前主图以大号“20 / 48”数字和一块浅色陶瓷主面为主体，后方保留琥珀、青瓷薄层，便于小尺寸辨认。

素材由内置图像生成工具生成，Noto Sans SC Black（SIL OFL 1.1）字形参考来自 numerals-noto-reference.png。主图 ceramic-2048.png 保留透明背景；tools/export_launcher.py 只做 Android 层尺寸、密度和系统遮罩预览导出。单色图标直接使用原始 Noto 字形。

生成提示要点：front-facing premium 2.5D ivory celadon ceramic tile; deep teal heavy 20 above 48 following supplied Noto numeral shapes; restrained amber and teal stacked edges; transparent background; legible at 48px; no enclosing border or backdrop。清理迭代只要求透明边缘与细纹理调整。

Android 前景、背景分离，108dp 图层、72dp可见窗口，主体适配66dp安全范围；另提供 API33 单色层及五种传统密度图标。不要将带底色的完整图再次作为自适应前景缩放。依据：https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

首页 app-mark 保持46×46逻辑单位，改用与桌面同源的 app-logo.png；原认可设计的字体、其他组件与布局不变。设计对象、生成器与 Android 运行设计已同步。旧 preview 是图标替换前的界面验收记录。
