# Weather Effect（天气效果）

**让 Minecraft 的天气决定你的命运——晴天赐福，雨天变脸，雷暴索命。**
**Let Minecraft's weather decide your fate — blessings under clear skies, trouble in the rain, doom in the thunderstorm.**

[中文](#中文) | [English](#english)

---

## 中文

### 天气效果（Weather Effect）

> 根据当前天气类型，自动为玩家施加药水效果：晴天随机正面效果；雨天随机、但可能混入负面效果；雷暴随机混合效果，甚至会一次触发多个。效果永久持续，玩家死亡后重置；重复获得同一效果时，效果等级 +1（上限 255）。

- **Mod ID**: `weather_effect_mod_1781182502`
- **Minecraft**: 1.21.1
- **模组加载器**: NeoForge 21.1.x
- **版本**: 1.20.0
- **许可证**: MIT（见 [LICENSE.txt](LICENSE.txt)）

---

### ✨ 功能特性

#### 三种天气，三种命运

| 天气 | 效果倾向 |
|------|----------|
| ☀️ 晴天 | 随机触发**正面**效果 |
| 🌧️ 雨天 | 随机触发，**可能混入负面**效果 |
| ⛈️ 雷暴 | 随机**混合**效果，可能一次叠加多个 |

#### 8 档难度

| 难度 | 触发间隔 | 天气行为 |
|------|----------|----------|
| 普通 Normal | 600 秒 | 跟随自然天气 |
| 简单 Easy | 900 秒 | 跟随自然天气 |
| 困难 Hard | 300 秒 | 跟随自然天气 |
| 极难 Extreme | 300 秒 | 强制转为雨天 |
| 超极难 Ultra Extreme | 300 秒 | 强制转为雷暴 |
| 地狱 Hell | 60 秒 | 强制转为雨天 |
| 超地狱 Ultra Hell | 60 秒 | 强制转为雷暴 |
| 自定义 Custom | 自定义 | 自定义强制天气 |

> 切换到强制天气的难度后，服务器会在一个间隔周期后开始改变所有世界的天气——"地狱"难度下，雷暴每 60 秒就会光顾你一次。

#### 随机模式 / 指定模式

- **随机模式**（默认）：从效果池随机抽取
  - 正面池 19 种：夜视、隐身、跳跃提升、防火、速度、水下呼吸、瞬间治疗、再生、力量、抗性提升、伤害吸收、饱和、发光、幸运、缓降、潮汐能量、海豚恩惠、村庄英雄、生命提升
  - 负面池：失明、缓慢、虚弱、饥饿、挖掘疲劳、反胃、中毒、凋零、漂浮、黑暗、霉运、不祥之兆、瞬间伤害 等
- **指定模式**：管理员为每种天气指定一个固定效果（晴天/雨天/雷暴各一个），触发间隔可设为无限

#### 持续时间

- **永久**（默认）：效果持续到玩家死亡，死亡后重置
- **特定时间**：效果持续指定 tick 数（可自定义）
- 持续时间设置仅在随机模式下可用

#### 其他

- **效果叠加**：重复获得同一效果 → 等级 +1，上限 255
- **数据持久化**：全部配置存于世界存档（开关、难度、模式、语言等），重启不丢
- **纯逻辑模组**：无渲染内容，客户端无需安装；单人游戏装在客户端即可，服务器装在服务端
- **中英双语**：默认跟随客户端语言，也可用指令切换

---

### 📋 指令

主指令 `/weathereffect`（无缩写）。

| 指令 | 说明 | 权限 |
|------|------|------|
| `/weathereffect weather` | 查看当前天气类型 | 所有玩家 |
| `/weathereffect list` | 查看自己当前生效的效果 | 所有玩家 |
| `/weathereffect mode random` / `specified` | 切换随机/指定模式 | 所有玩家 |
| `/weathereffect language toggle` / `auto` | 切换/恢复自动检测消息语言 | 所有玩家 |
| `/weathereffect trigger` | 立即触发一轮效果 | OP（等级 2） |
| `/weathereffect toggle` | 开启/关闭模组 | OP |
| `/weathereffect setInterval <ticks>` | 设置随机模式触发间隔 | OP |
| `/weathereffect seteffect <weather> <effect>` | 指定模式：为某天气设置效果 | OP |
| `/weathereffect setspecifiedinterval <ticks>` / `infinite` | 设置指定模式间隔 / 无限 | OP |
| `/weathereffect difficulty <normal/easy/hard/extreme/ultra_extreme/hell/ultra_hell>` | 切换难度 | OP |
| `/weathereffect difficulty custom <interval> <weather>` | 设置自定义难度 | OP |
| `/weathereffect durationmode permanent` / `specifictime` | 切换持续时间模式（仅随机模式） | OP |
| `/weathereffect setduration <ticks>` | 设置特定时间时长（仅随机模式） | OP |
| `/weathereffect language set <zh_cn/en_us>` | 固定消息语言 | OP |

---

### 📦 安装

1. 准备 Minecraft **1.21.1** + **NeoForge 21.1.x**
2. 将模组 jar 放入 `mods/` 文件夹（服务器装服务端，单人游戏装客户端）
3. 进入世界，下雨就知道了

### 🔨 构建

```bash
./gradlew build
# 需要 JDK 21（ModDevGradle 构建）
# 产物: build/libs/*.jar
```

### 📜 许可证

MIT License（Copyright (c) 2026 MowenQWQ），详见 [LICENSE.txt](LICENSE.txt)。

---

## English

### Weather Effect

> Automatically applies potion effects to players based on the current weather: clear skies bring random positive effects; rain brings random effects that may turn negative; thunderstorms bring mixed effects — sometimes several at once. Effects last until death, then reset. Re-gaining the same effect raises its amplifier by 1 (cap: 255).

- **Mod ID**: `weather_effect_mod_1781182502`
- **Minecraft**: 1.21.1
- **Mod Loader**: NeoForge 21.1.x
- **Version**: 1.20.0
- **License**: MIT (see [LICENSE.txt](LICENSE.txt))

---

### ✨ Features

#### Three weathers, three fates

| Weather | Effect tendency |
|---------|-----------------|
| ☀️ Clear | Random **positive** effects |
| 🌧️ Rain | Random, **may include negative** effects |
| ⛈️ Thunderstorm | Random **mixed** effects, possibly several at once |

#### 8 difficulty levels

| Difficulty | Interval | Weather behavior |
|------------|----------|------------------|
| Normal | 600s | Follows natural weather |
| Easy | 900s | Follows natural weather |
| Hard | 300s | Follows natural weather |
| Extreme | 300s | Forces rain |
| Ultra Extreme | 300s | Forces thunderstorm |
| Hell | 60s | Forces rain |
| Ultra Hell | 60s | Forces thunderstorm |
| Custom | Custom | Custom forced weather |

> After switching to a forced-weather difficulty, the server starts changing weather across all dimensions after one interval cycle. On Hell difficulty, a thunderstorm visits you every 60 seconds.

#### Random / Specified mode

- **Random mode** (default): effects are drawn randomly
  - Positive pool (19): Night Vision, Invisibility, Jump Boost, Fire Resistance, Speed, Water Breathing, Instant Health, Regeneration, Strength, Resistance, Absorption, Saturation, Glowing, Luck, Slow Falling, Conduit Power, Dolphin's Grace, Hero of the Village, Health Boost
  - Negative pool: Blindness, Slowness, Weakness, Hunger, Mining Fatigue, Nausea, Poison, Wither, Levitation, Darkness, Unluck, Bad Omen, Instant Damage, etc.
- **Specified mode**: admins assign one fixed effect per weather type (clear / rain / thunderstorm); the interval can be set to infinite

#### Effect duration

- **Permanent** (default): lasts until the player dies, then resets
- **Specific time**: lasts for a custom number of ticks
- Duration settings are only available in random mode

#### Others

- **Effect stacking**: re-gaining the same effect → amplifier +1, capped at 255
- **Persistent data**: all settings (toggle, difficulty, mode, language, ...) are stored in the world save
- **Logic-only mod**: no rendering content; clients don't need it — install on the server, or on the client for singleplayer
- **Bilingual**: follows the client language by default; switchable via command

---

### 📋 Commands

Root command: `/weathereffect` (no alias).

| Command | Description | Permission |
|---------|-------------|------------|
| `/weathereffect weather` | Show the current weather type | Everyone |
| `/weathereffect list` | Show your currently active effects | Everyone |
| `/weathereffect mode random` / `specified` | Switch random/specified mode | Everyone |
| `/weathereffect language toggle` / `auto` | Switch / auto-detect message language | Everyone |
| `/weathereffect trigger` | Trigger a round immediately | OP (level 2) |
| `/weathereffect toggle` | Enable/disable the mod | OP |
| `/weathereffect setInterval <ticks>` | Set random-mode interval | OP |
| `/weathereffect seteffect <weather> <effect>` | Specified mode: set effect for a weather | OP |
| `/weathereffect setspecifiedinterval <ticks>` / `infinite` | Set specified-mode interval / infinite | OP |
| `/weathereffect difficulty <normal/easy/hard/extreme/ultra_extreme/hell/ultra_hell>` | Switch difficulty | OP |
| `/weathereffect difficulty custom <interval> <weather>` | Configure custom difficulty | OP |
| `/weathereffect durationmode permanent` / `specifictime` | Switch duration mode (random mode only) | OP |
| `/weathereffect setduration <ticks>` | Set specific-time duration (random mode only) | OP |
| `/weathereffect language set <zh_cn/en_us>` | Pin the message language | OP |

---

### 📦 Installation

1. Prepare Minecraft **1.21.1** + **NeoForge 21.1.x**
2. Drop the mod jar into the `mods/` folder (server jar on servers, client jar for singleplayer)
3. Enter a world — wait for the rain

### 🔨 Building

```bash
./gradlew build
# Requires JDK 21 (ModDevGradle)
# Output: build/libs/*.jar
```

### 📜 License

MIT License (Copyright (c) 2026 MowenQWQ), see [LICENSE.txt](LICENSE.txt).

---

## 🤖 AI 使用声明 / AI Usage Disclosure

本项目在开发与维护过程中使用了 AI 编程助手（Claude / Anthropic）辅助代码编写、文档整理与问题排查；核心决策、内容审核与最终发布由维护者完成。

This project was developed and maintained with the assistance of an AI coding assistant (Claude / Anthropic) for coding, documentation, and troubleshooting. Core decisions, content review, and final releases are made by the maintainer.
