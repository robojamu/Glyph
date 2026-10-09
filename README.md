# ColorOS 通知图标增强（自用改动线）

本仓库为自用改动线，用于个人设备。

## 上游来源

- [fankes/ColorOSNotifyIcon](https://github.com/fankes/ColorOSNotifyIcon)：原项目，版权归 Fankes Studio 所有
- [Mangi-11/Glyph](https://github.com/Mangi-11/Glyph)：基于原项目，用 libxposed API 102 重写 Hook 层、用 Miuix 重写 UI 的分支，本仓库的直接上游
- [Android Notification Icon Project](https://github.com/BetterAndroid/android-notification-icon-project)：通知图标规则库，规则由该项目维护

## 说明

- 本仓库与上述项目无隶属关系，非官方发布。
- 上游仓库不负责本仓库的改动；相关问题与改动记录留在本仓库。
- 构建使用仓库内附带的公共开发证书（非生产密钥），任何人可用同一证书重新签名，不作官方发布使用。
- 本仓库基于上游 `Mangi-11/Glyph 3.4.0` 修改而来；改动明细与日期见 git 提交记录。

## 功能

- 状态栏通知图标替换
- 息屏（AOD）时钟通知图标替换
- ColorOS 16 锁屏岛（底部通知胶囊）图标替换
- 通知中心小图标替换（可关）
- 图标来源可选规则图标或桌面主题，支持通知图标跟随桌面当前主题 / 自定义图标
- Oplus Push 系统推送特判（可关）
- 未适配应用使用通用圆形通知图标作为占位符（可关）
- 本地规则管理，支持按应用启用或全部替换
- 支持为应用手动指定规则库图标，优先于包名匹配
- 手动同步远程规则，不做后台自动同步
- 配置写入 Xposed 框架侧，通知 SystemUI 刷新
- 桌面图标可隐藏，隐藏后仍可从 LSPosed 模块列表打开

## 功能设置

| 设置 | 说明 |
| --- | --- |
| 启用图标增强 | 总开关。关闭后不再使用规则或主题图标，只保留符合通知规范的原始 `smallIcon` 保护 |
| 图标来源 | 在规则图标和桌面主题之间二选一。桌面主题模式下，通知图标跟随桌面当前主题或自定义图标 |
| 通知中心图标替换 | 控制通知面板内的小图标。关闭后通知中心保持 ColorOS 默认行为，不影响状态栏 |
| 系统推送特判 | 规则图标模式下，Oplus Push 通知是否优先使用目标应用的已启用规则。关闭后保持系统默认 |
| 未适配占位符 | 规则图标模式下，未命中规则且原始图标不是可着色的通知遮罩时，使用通用圆形通知图标替代 |
| 隐藏桌面图标 | 隐藏 Glyph 的桌面入口，不影响从 LSPosed 模块列表打开 |

规则图标模式下，单条规则里的两个选项：

- **启用替换**：允许该应用使用规则图标
- **全部替换**：忽略应用自带且符合规范的 `smallIcon`，始终使用规则图标

## 图标来源

状态栏、息屏时钟、锁屏岛、通知中心普通图标、Oplus 聚合摘要图标共享同一套图标来源逻辑。

- **规则图标**：使用模块规则库图标，支持手动指定、未适配占位符和单应用强制替换。
- **桌面主题**：读取桌面当前主题或自定义图标。读取失败时回退系统原始通知图标，不回退规则图标，避免两套来源混用。

## 图标决策逻辑

规则图标模式下，状态栏和通知中心用同一套逻辑，不会无条件覆盖系统结果：

1. Oplus Push 通知 + 系统推送特判已关闭 → 保持系统默认
2. Oplus Push 通知 + 系统推送特判已开启 + 命中已启用规则 → 使用规则图标
3. 命中已启用规则 + 全部替换已开启 → 使用规则图标
4. 命中已启用规则 + 原始 `smallIcon` 不是可着色的通知遮罩 → 使用规则图标
5. 未命中规则 + 未适配占位符已开启 + 原始 `smallIcon` 不是可着色的通知遮罩 → 使用通用圆形通知图标占位
6. 原始 `smallIcon` 是可着色的通知遮罩 → 恢复原始 `smallIcon`
7. 以上均不满足 → 保持 ColorOS 当前结果

「命中规则」按通知的包名精确查找。用户可在规则管理页为某个应用手动指定规则库中的另一枚图标，手动指定优先于包名匹配；图标只取被借用条目在规则库中的原始定义，不传递该条目自己的手动指定，避免循环。未进入规则库的已安装应用也可以添加一条手动指定，清除后回退包名匹配（未适配应用则从列表中移除）。
启用替换、全部替换、适配系统推送仍按**当前应用**自己的开关判断，手动指定只改图标。切换到「桌面主题」后不会使用手动指定。

兼容性判断同时验证通知遮罩的 Alpha 几何和单色前景合同：图标需要有透明背景，
且可见内容应为灰阶或单一前景色。多彩图、彩色与中性色混合、致密圆角底板、
`AdaptiveIconDrawable`、全透明图和全不透明图均不会被当作通知遮罩强制着色；
命中规则或占位符时使用对应替代图标，否则保留 ColorOS 当前结果。无法安全读取像素的
硬件位图属于未知结果，直接交回 SystemUI 判定。RGB 不参与最终渲染，但会用于区分
可着色的通知资源与桌面应用图标。
Oplus Push 代表目标应用生成的位图不等同于应用原生 `smallIcon`；命中已启用规则时，
不会因为该位图带有透明圆角而跳过规则。

恢复的原生 `smallIcon`、规则图标和占位符继续由 SystemUI 统一着色；桌面主题来源是
全彩应用图标，保持原色，不套用通知遮罩的灰度着色。
重要会话的联系人头像保留 SystemUI 原生路径，不替换成通知遮罩。

通知中心额外跳过媒体通知，不破坏系统媒体样式。

## 安装

1. 从本仓库的 [Releases](https://github.com/robojamu/Glyph/releases/latest) 下载 APK
   （上游 Releases 属于另一条线，不含本仓库的改动）
2. 在 LSPosed 中启用模块
3. 勾选作用域：系统框架 `system`、系统界面 `com.android.systemui`
4. 打开 Glyph（隐藏桌面图标后可从 LSPosed 模块列表打开），同步规则
5. 按需调整图标来源、开关和单个应用规则
6. 点击 **重启 SystemUI** 使配置生效

首次安装或更新了涉及 `system_server` Hook 的版本，建议完整重启一次系统。

覆盖安装：相邻版本使用同一证书（仓库内的公共开发证书），`adb install -r` 可直接覆盖，配置与规则不会丢失。

## 适配说明

适配 ColorOS 16 / ColorOS 17 + LSPosed；不兼容旧版系统、旧版 Xposed 或其他框架。

只服务最新系统版本，不保留历史兼容层。本仓库的改动主要在 ColorOS 17（OnePlus PLK110）上验证。

## 规则来源

沿用 [Android Notification Icon Project (ANIP)](https://github.com/BetterAndroid/android-notification-icon-project) 规则仓库：

- [Android Notification Icon Project](https://github.com/BetterAndroid/android-notification-icon-project)
- [ColorOS 规则清单](https://raw.githubusercontent.com/BetterAndroid/android-notification-icon-project/main/icons/system/coloros/manifest.json)
- [游戏规则清单](https://raw.githubusercontent.com/BetterAndroid/android-notification-icon-project/main/icons/game/manifest.json)
- [APP 规则清单](https://raw.githubusercontent.com/BetterAndroid/android-notification-icon-project/main/icons/app/manifest.json)

同步时按 ColorOS → 游戏 → APP 合并。同一包名出现多次的，以后者为准。

## 更新

App 内「关于 → 检查更新」读取本仓库的 Releases。本仓库版本号带 `-cn` 后缀（例如 `3.4.4-cn`），版本比较会忽略该后缀。

规则图标单独同步：首页「更新规则」按钮拉取 [ANIP](https://github.com/BetterAndroid/android-notification-icon-project)
的清单，无后台自动同步；补充规则应向 ANIP 提交。

## 上游 Glyph 分支的改动（Mangi-11）

以下为上游分支相对 [fankes/ColorOSNotifyIcon](https://github.com/fankes/ColorOSNotifyIcon) 的改动，列出以说明来源；
本仓库在该分支之上另有上方「本仓库的改动」一节的修改。

主要改动：

- 用 [modern libxposed API 102](https://github.com/libxposed/api) 重写了 Hook 入口
- 移除了旧框架兼容层
- 用 [Miuix](https://github.com/compose-miuix-ui/miuix) 重写了 App UI（首页 / 规则 / 关于三栏，液态玻璃底栏）
- 关于页支持打开本项目与 GitHub Releases 检查更新
- 功能收敛到通知图标增强，去除其余杂项功能
- 支持通知图标跟随桌面主题或自定义图标
- 补齐 ColorOS 经典时钟息屏通知图标（`LockScreenNotificationIconData` → AodPlugin，以及旧版 `NotificationLayout`）
- 补齐 ColorOS 16 锁屏岛底部通知胶囊图标（`CapsuleNotificationDataController`）。规则图标在锁屏岛染白轮廓，桌面主题保持彩色，避免多条聚合被白色遮罩盖成色块；白色着色不作用于下拉通知与锁屏堆叠
- 规则管理支持为单个应用手动指定规则库图标，优先于包名匹配
- 不做后台自动同步

上游分支以作者自身的维护计划为准。

## 注意事项

1. 本软件免费、兴趣驱动，仅供学习交流；付费获取的版本均非官方渠道。
2. 本软件采用 **AGPL 3.0** 许可证。分发或修改时必须遵守条款并提供源代码。
3. 保留原始版权声明与许可证信息，不得冒用原作者名义。
4. 本仓库为自用改动线，与上游作者无隶属关系；相关问题与改动记录留在本仓库，无需提交到上游仓库。

## 隐私政策

- [PRIVACY](PRIVACY.md)

## 许可证

- [AGPL-3.0](https://www.gnu.org/licenses/agpl-3.0.html)

代码可自由查看、修改、分发，条件是保持同样的自由：分发修改版时保留版权声明、标明改动并提供源码。

原始项目版权归 Fankes Studio(qzmmcn@163.com) 所有；上游 Glyph 分支由 [Mangi-11](https://github.com/Mangi-11/Glyph) 维护；
本仓库的改动同样按 AGPL-3.0 发布。

## 致谢

感谢原作者 [fankes](https://github.com/fankes) 的开源基础，以及各位图标规则维护者的持续付出。

也感谢 [Mangi-11](https://github.com/Mangi-11/Glyph) 维护的 Glyph 分支，本仓库基于该分支。

通用占位符采用 Google Material Icons 的 [`circle_notifications`](https://github.com/google/material-design-icons)，按 [Apache-2.0](third_party/material-design-icons/LICENSE) 许可证使用。
