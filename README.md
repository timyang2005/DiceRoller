# 🎲 DiceRoller - 骰子投掷器

一款具有真实物理引擎的安卓骰子投掷器 App，支持多种骰子预设、自定义颜色和音效。

## ✨ 功能特性

- **🎯 真实物理引擎** — 重力、弹跳、摩擦、旋转，模拟真实骰子投掷体验
- **⚡ 力度感应** — 点击越快越多，骰子跳得越高、转得越快
- **🔷 多种预设** — 支持 D4 / D6 / D8 / D10 / D12 / D20 / D100
- **🎨 自定义颜色** — 9种快捷预设色 + RGB三通道精确调节
- **🔊 程序化音效** — 无需外部音频文件，自动生成投掷、弹跳、落定音效
- **📊 结果总和** — 多骰子时可选显示结果总和
- **🎲 多骰子投掷** — 支持 1~10 个骰子同时投掷
- **💥 骰子碰撞** — 骰子之间具有碰撞检测和物理响应

## 📸 截图

| 主界面 | 设置界面 |
|:---:|:---:|
| 投掷骰子 | 配置预设、颜色、数量 |

## 🏗️ 技术架构

```
app/src/main/java/com/diceroller/app/
├── DicePreset.kt          # 骰子预设枚举（D4~D100）
├── Dice.kt                # 骰子数据模型 + 多边形顶点计算
├── DicePhysicsEngine.kt   # 物理引擎核心
├── DiceSurfaceView.kt     # Canvas 渲染视图
├── SoundManager.kt        # 音效管理（程序化生成 WAV）
├── MainActivity.kt        # 主界面 + 力度检测
└── SettingsActivity.kt    # 设置界面
```

### 物理引擎参数

| 参数 | 值 | 说明 |
|------|------|------|
| 重力加速度 | 2500 px/s² | 模拟真实下落 |
| 弹跳衰减 | 0.45 | 每次弹跳损失55%能量 |
| 地面摩擦 | 0.97 | 水平速度衰减系数 |
| 角速度阻尼 | 0.96 | 旋转速度衰减系数 |
| 墙壁弹性 | 0.5 | 墙壁碰撞弹性系数 |
| 稳定时间 | 0.3s | 速度低于阈值后判定稳定 |

### 力度系统

在 1.5 秒时间窗口内统计点击次数，映射为力度倍率：

| 点击频率 | 力度倍率 | 效果 |
|----------|----------|------|
| 1次 | 0.3x | 轻柔投掷 |
| 3次 | 1.1x | 正常投掷 |
| 5次 | 1.65x | 用力投掷 |
| 10次 | 3.0x | 全力投掷 |

## 📥 下载安装

前往 [Releases](https://github.com/timyang2005/DiceRoller/releases) 页面下载最新 APK。

1. 下载 APK 文件到手机
2. 在手机设置中允许安装未知来源应用
3. 安装并打开

## 🔧 本地构建

### 环境要求

- Android Studio Hedgehog | 2023.1.1+
- JDK 17
- Android SDK 34
- Gradle 8.5

### 构建步骤

1. Clone 仓库
```bash
git clone https://github.com/timyang2005/DiceRoller.git
cd DiceRoller
```

2. 生成签名密钥（如需 Release 构建）
```bash
keytool -genkeypair -v \
  -keystore diceroller-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias diceroller
```

3. 配置 `local.properties`
```properties
RELEASE_STORE_FILE=diceroller-release.jks
RELEASE_STORE_PASSWORD=你的密码
RELEASE_KEY_ALIAS=diceroller
RELEASE_KEY_PASSWORD=你的密码
```

4. 构建运行
```bash
./gradlew assembleRelease
```

## 🔄 CI/CD

项目使用 GitHub Actions 自动构建：

- **触发条件**：推送到 `main` 分支或手动触发
- **构建流程**：解码签名密钥 → 构建 Release APK → 创建 GitHub Release
- **所需 Secrets**：
  - `KEYSTORE_BASE64` — 签名密钥的 Base64 编码
  - `KEYSTORE_PASSWORD` — 密钥库密码
  - `KEY_ALIAS` — 密钥别名
  - `KEY_PASSWORD` — 密钥密码

## 📄 许可证

MIT License

## 📜 版本历史

### v1.1.0 (2025-05-05)
- ✨ 新增自适应应用图标（支持 Android 8.0+）
- 🔧 优化 GitHub Actions 构建流程
- 🎨 改进应用图标设计

### v1.0.0
- 🎯 初始版本发布
- 支持多种骰子预设 (D4/D6/D8/D10/D12/D20/D100)
- 真实物理引擎模拟
- 自定义骰子颜色
- 程序化音效生成
