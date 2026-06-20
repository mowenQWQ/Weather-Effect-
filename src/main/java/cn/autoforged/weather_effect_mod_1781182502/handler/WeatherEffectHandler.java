/*
 * MIT License
 *
 * Copyright (c) [2026] [MowenQWQ]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cn.autoforged.weather_effect_mod_1781182502.handler;

import cn.autoforged.weather_effect_mod_1781182502.WeatherEffectMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = WeatherEffectMod.MODID)
public final class WeatherEffectHandler {

    private static final int MAX_AMPLIFIER = 255;
    private static final int EFFECT_DURATION = -1;
    private static WeatherEffectData currentData;

    private static WeatherEffectData getData(ServerLevel level) {
        currentData = level.getServer().overworld().getDataStorage()
            .computeIfAbsent(WeatherEffectData.factory(), WeatherEffectData.DATA_NAME);
        return currentData;
    }

    private static WeatherEffectData getData(ServerPlayer player) {
        return getData(player.serverLevel());
    }

    private enum WeatherType { CLEAR, RAIN, THUNDER }

    enum Difficulty {
        NORMAL("普通", 12000, null),
        EASY("简单", 18000, null),
        HARD("困难", 6000, null),
        EXTREME("极难", 6000, WeatherType.RAIN),
        ULTRA_EXTREME("超极难", 6000, WeatherType.THUNDER),
        HELL("地狱", 1200, WeatherType.RAIN),
        ULTRA_HELL("超地狱", 1200, WeatherType.THUNDER),
        CUSTOM("自定义", 12000, null);

        final String displayName;
        final int interval;
        final WeatherType forcedWeather;

        Difficulty(String displayName, int interval, WeatherType forcedWeather) {
            this.displayName = displayName;
            this.interval = interval;
            this.forcedWeather = forcedWeather;
        }
    }

    private record PlayerData(WeatherType lastWeather, Map<String, Integer> appliedEffects) {
        PlayerData(WeatherType lastWeather) {
            this(lastWeather, new ConcurrentHashMap<>());
        }
    }

    private static final Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    private static final List<Holder<MobEffect>> POSITIVE_EFFECTS = List.of(
        MobEffects.NIGHT_VISION,
        MobEffects.INVISIBILITY,
        MobEffects.JUMP,
        MobEffects.FIRE_RESISTANCE,
        MobEffects.MOVEMENT_SPEED,
        MobEffects.WATER_BREATHING,
        MobEffects.HEAL,
        MobEffects.REGENERATION,
        MobEffects.DAMAGE_BOOST,
        MobEffects.DAMAGE_RESISTANCE,
        MobEffects.ABSORPTION,
        MobEffects.SATURATION,
        MobEffects.GLOWING,
        MobEffects.LUCK,
        MobEffects.SLOW_FALLING,
        MobEffects.CONDUIT_POWER,
        MobEffects.DOLPHINS_GRACE,
        MobEffects.HERO_OF_THE_VILLAGE,
        MobEffects.HEALTH_BOOST
    );

    private static final List<Holder<MobEffect>> NEGATIVE_EFFECTS = List.of(
        MobEffects.BLINDNESS,
        MobEffects.MOVEMENT_SLOWDOWN,
        MobEffects.WEAKNESS,
        MobEffects.HUNGER,
        MobEffects.DIG_SLOWDOWN,
        MobEffects.CONFUSION,
        MobEffects.POISON,
        MobEffects.WITHER,
        MobEffects.LEVITATION,
        MobEffects.DARKNESS,
        MobEffects.UNLUCK,
        MobEffects.BAD_OMEN,
        MobEffects.HARM,
        MobEffects.OOZING,
        MobEffects.WEAVING,
        MobEffects.WIND_CHARGED
    );

    private static final List<Holder<MobEffect>> RAIN_EFFECTS;
    private static final List<Holder<MobEffect>> THUNDER_EFFECTS;

    static {
        List<Holder<MobEffect>> rain = new ArrayList<>();
        rain.addAll(POSITIVE_EFFECTS);
        rain.addAll(NEGATIVE_EFFECTS);
        RAIN_EFFECTS = Collections.unmodifiableList(rain);

        List<Holder<MobEffect>> thunder = new ArrayList<>();
        thunder.addAll(POSITIVE_EFFECTS);
        thunder.addAll(NEGATIVE_EFFECTS);
        THUNDER_EFFECTS = Collections.unmodifiableList(thunder);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel overworld = event.getServer().overworld();
        WeatherEffectData data = getData(overworld);
        if (!data.isEnabled()) return;

        if (data.getPendingForcedWeatherTicks() > 0) {
            data.decrPendingForcedWeatherTicks();
        }

        for (ServerLevel level : event.getServer().getAllLevels()) {
            forceWeatherIfNeeded(level, data);
        }

        if (data.getMode().equals("SPECIFIED")) {
            if (!data.isSpecifiedInfinite()) {
                data.setSpecifiedTickCounter(data.getSpecifiedTickCounter() + 1);
            }
            if (!data.isSpecifiedInfinite() && data.getSpecifiedTickCounter() >= getEffectiveSpecifiedInterval(data)) {
                data.setSpecifiedTickCounter(0);
                data.setDirty();

                rerollSpecifiedEffects(data, overworld.random);
                Component announcement = buildSpecifiedAnnouncement(data);

                for (ServerLevel level : event.getServer().getAllLevels()) {
                    WeatherType currentWeather = getWeatherType(level);
                    for (ServerPlayer player : level.players()) {
                        player.sendSystemMessage(announcement);
                        var newlyApplied = applySpecifiedEffect(player, currentWeather, data);
                        sendEffectFeedback(player, newlyApplied, getEffectiveSpecifiedInterval(data) / 20);
                    }
                }
            }
        } else {
            data.setTickCounter(data.getTickCounter() + 1);
            if (data.getTickCounter() < getEffectiveInterval(data)) return;
            data.setTickCounter(0);
            data.setDirty();

            for (ServerLevel level : event.getServer().getAllLevels()) {
                WeatherType currentWeather = getWeatherType(level);
                for (ServerPlayer player : level.players()) {
                    var newlyApplied = applyWeatherEffect(player, currentWeather, data);
                    sendEffectFeedback(player, newlyApplied, getEffectiveInterval(data));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WeatherEffectData data = getData(player);
            if (!data.isEnabled()) return;
            if (player.getActiveEffects().isEmpty()) {
                playerDataMap.remove(player.getUUID());
                ServerLevel level = player.serverLevel();
                if (data.getMode().equals("SPECIFIED")) {
                    var newlyApplied = applySpecifiedEffect(player, getWeatherType(level), data);
                    sendEffectFeedback(player, newlyApplied, getEffectiveSpecifiedInterval(data) / 20);
                } else {
                    var newlyApplied = applyWeatherEffect(player, getWeatherType(level), data);
                    sendEffectFeedback(player, newlyApplied, getEffectiveInterval(data));
                }
            }
            showModInfo(player, data);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WeatherEffectData data = getData(player);
            if (!data.isEnabled()) return;
            if (!player.getActiveEffects().isEmpty()) return;
            playerDataMap.remove(player.getUUID());
            ServerLevel level = player.serverLevel();
            if (data.getMode().equals("SPECIFIED")) {
                var newlyApplied = applySpecifiedEffect(player, getWeatherType(level), data);
                sendEffectFeedback(player, newlyApplied, getEffectiveSpecifiedInterval(data) / 20);
            } else {
                var newlyApplied = applyWeatherEffect(player, getWeatherType(level), data);
                sendEffectFeedback(player, newlyApplied, getEffectiveInterval(data));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WeatherEffectData data = getData(player);
            if (!data.isEnabled()) return;
            if (!player.getActiveEffects().isEmpty()) return;
            playerDataMap.remove(player.getUUID());
            if (data.getMode().equals("SPECIFIED")) {
                applySpecifiedEffect(player, getWeatherType(player.serverLevel()), data);
            } else {
                applyWeatherEffect(player, getWeatherType(player.serverLevel()), data);
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("weathereffect")
            .then(Commands.literal("trigger")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherEffectData data = getData(player);
                    if (!data.isEnabled()) {
                        player.sendSystemMessage(Component.literal("§c[天气效果] §f模组已关闭，无法触发效果"));
                        return 0;
                    }
                    if (data.getMode().equals("SPECIFIED")) {
                        var newlyApplied = applySpecifiedEffect(player, getWeatherType(player.serverLevel()), data);
                        sendEffectFeedback(player, newlyApplied, getEffectiveSpecifiedInterval(data) / 20);
                    } else {
                        var newlyApplied = applyWeatherEffect(player, getWeatherType(player.serverLevel()), data);
                        sendEffectFeedback(player, newlyApplied, getEffectiveInterval(data));
                    }
                    return 1;
                })
            )
            .then(Commands.literal("toggle")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherEffectData data = getData(player);
                    boolean newEnabled = !data.isEnabled();
                    data.setEnabled(newEnabled);
                    ctx.getSource().sendSuccess(() ->
                        Component.literal("§a[天气效果] §f模组已" + (newEnabled ? "§a开启" : "§c关闭")),
                        true
                    );
                    return 1;
                })
            )
            .then(Commands.literal("setInterval")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        data.setCustomInterval(ticks);
                        data.setCustomForcedWeather("");
                        data.setDifficulty(Difficulty.CUSTOM);
                        data.setTickCounter(0);
                        data.setPendingForcedWeatherTicks(0);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f已切换至自定义难度，检查间隔已设为 " + ticks + " tick（" + (ticks / 20) + " 秒）"),
                            true
                        );
                        return 1;
                    })
                )
            )
            .then(Commands.literal("seteffect")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("weather", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("clear");
                        builder.suggest("rain");
                        builder.suggest("thunder");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument("effect", StringArgumentType.string())
                        .executes(ctx -> {
                            String weather = StringArgumentType.getString(ctx, "weather");
                            String effectId = StringArgumentType.getString(ctx, "effect");
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            WeatherEffectData data = getData(player);
                            if (!data.getMode().equals("SPECIFIED")) {
                                ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在指定模式下可用"));
                                return 0;
                            }
                            ResourceLocation rl;
                            try {
                                rl = ResourceLocation.parse(effectId);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f无效的效果ID：" + effectId));
                                return 0;
                            }
                            ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, rl);
                            if (BuiltInRegistries.MOB_EFFECT.getHolder(key).isEmpty()) {
                                ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f未找到效果：" + effectId));
                                return 0;
                            }
                            switch (weather) {
                                case "clear" -> data.setSpecifiedClearEffect(effectId);
                                case "rain" -> data.setSpecifiedRainEffect(effectId);
                                case "thunder" -> data.setSpecifiedThunderEffect(effectId);
                                default -> {
                                    ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f未知天气：" + weather + "（可选：clear, rain, thunder）"));
                                    return 0;
                                }
                            }
                            String weatherName = switch (weather) {
                                case "clear" -> "晴天";
                                case "rain" -> "雨天";
                                case "thunder" -> "雷暴";
                                default -> "";
                            };
                            Component effectComp = BuiltInRegistries.MOB_EFFECT.getHolder(key)
                                .map(h -> Component.translatable(h.value().getDescriptionId()))
                                .orElse(Component.literal(effectId));
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a[天气效果] §f已设置" + weatherName + "的效果为：").append(effectComp),
                                true
                            );
                            return 1;
                        })
                    )
                )
            )
            .then(Commands.literal("setspecifiedinterval")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        if (!data.getMode().equals("SPECIFIED")) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在指定模式下可用"));
                            return 0;
                        }
                        data.setSpecifiedInterval(ticks);
                        data.setSpecifiedInfinite(false);
                        data.setSpecifiedTickCounter(0);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f指定模式刷新间隔已设为 " + ticks + " tick（" + (ticks / 20) + " 秒）"),
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("infinite")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        if (!data.getMode().equals("SPECIFIED")) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在指定模式下可用"));
                            return 0;
                        }
                        data.setSpecifiedInfinite(true);
                        data.setSpecifiedTickCounter(0);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f指定模式刷新间隔已设为无限，效果将不再自动刷新"),
                            true
                        );
                        return 1;
                    })
                )
            )
            .then(Commands.literal("mode")
                .then(Commands.literal("random")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        data.setMode("RANDOM");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f已切换至随机模式"),
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("specified")
                    .requires(source -> source.hasPermission(2))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        data.setMode("SPECIFIED");
                        if (data.getSpecifiedClearEffect().isEmpty()) {
                            rerollSpecifiedEffects(data, player.serverLevel().random);
                        }
                        data.setSpecifiedTickCounter(0);
                        Component announcement = buildSpecifiedAnnouncement(data);
                        for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                            p.sendSystemMessage(announcement);
                        }
                        var newlyApplied = applySpecifiedEffect(player, getWeatherType(player.serverLevel()), data);
                        sendEffectFeedback(player, newlyApplied, getEffectiveSpecifiedInterval(data) / 20);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f已切换至指定模式，效果已公告"),
                            true
                        );
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherEffectData data = getData(player);
                    String modeName = data.getMode().equals("SPECIFIED") ? "指定模式" : "随机模式";
                    player.sendSystemMessage(Component.literal("§a[天气效果] §f当前模式：" + modeName));
                    return 1;
                })
            )
            .then(Commands.literal("difficulty")
                .then(Commands.argument("name", StringArgumentType.word())
                    .requires(source -> source.hasPermission(2))
                    .suggests((ctx, builder) -> {
                        for (Difficulty d : Difficulty.values()) {
                            builder.suggest(d.name().toLowerCase());
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "name");
                        Difficulty diff;
                        try {
                            diff = Difficulty.valueOf(name.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f未知难度：" + name));
                            return 0;
                        }
                        if (diff == Difficulty.CUSTOM) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f请使用 /weathereffect difficulty custom <interval> <weather> 设置自定义难度"));
                            return 0;
                        }
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        data.setDifficulty(diff);
                        data.setTickCounter(0);
                        data.setPendingForcedWeatherTicks(0);
                        boolean hasForceDelay = diff.forcedWeather != null
                            && (diff == Difficulty.EXTREME || diff == Difficulty.ULTRA_EXTREME || diff == Difficulty.HELL || diff == Difficulty.ULTRA_HELL);
                        if (hasForceDelay) {
                            data.setPendingForcedWeatherTicks(diff.interval);
                            int delaySec = diff.interval / 20;
                            int delayMin = delaySec / 60;
                            int delaySecRem = delaySec % 60;
                            String weatherName = diff.forcedWeather == WeatherType.RAIN ? "雨天" : "雷暴";
                            String timeMsg = delayMin > 0
                                ? delayMin + "分" + (delaySecRem > 0 ? delaySecRem + "秒" : "")
                                : delaySec + "秒";
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a[天气效果] §f§l【天气效果】将在" + timeMsg + "后一直为" + weatherName + "。"),
                                true
                            );
                        } else {
                            String extra = diff.forcedWeather != null
                                ? "，强制" + (diff.forcedWeather == WeatherType.RAIN ? "雨天" : "雷暴")
                                : "";
                            ctx.getSource().sendSuccess(() ->
                                Component.literal("§a[天气效果] §f难度已设为 " + diff.displayName + "，间隔 " + (diff.interval / 20) + " 秒" + extra),
                                true
                            );
                        }
                        return 1;
                    })
                )
                .then(Commands.literal("custom")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("interval", IntegerArgumentType.integer(1))
                        .then(Commands.argument("weather", StringArgumentType.word())
                            .suggests((ctx, builder) -> {
                                builder.suggest("none");
                                builder.suggest("rain");
                                builder.suggest("thunder");
                                return builder.buildFuture();
                            })
                            .executes(ctx -> {
                                int interval = IntegerArgumentType.getInteger(ctx, "interval");
                                String weatherStr = StringArgumentType.getString(ctx, "weather");
                                WeatherType forced = switch (weatherStr) {
                                    case "none" -> null;
                                    case "rain" -> WeatherType.RAIN;
                                    case "thunder" -> WeatherType.THUNDER;
                                    default -> {
                                        ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f未知天气选项：" + weatherStr + "（可选：none, rain, thunder）"));
                                        yield null;
                                    }
                                };
                                if (weatherStr.equals("none") || weatherStr.equals("rain") || weatherStr.equals("thunder")) {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    WeatherEffectData data = getData(player);
                                    data.setCustomInterval(interval);
                                    data.setCustomForcedWeather(forced != null ? forced.name().toLowerCase() : "");
                                    data.setDifficulty(Difficulty.CUSTOM);
                                    data.setTickCounter(0);
                                    data.setPendingForcedWeatherTicks(0);
                                    String extra = forced != null
                                        ? "，强制" + (forced == WeatherType.RAIN ? "雨天" : "雷暴")
                                        : "";
                                    ctx.getSource().sendSuccess(() ->
                                        Component.literal("§a[天气效果] §f已设为自定义难度，间隔 " + (interval / 20) + " 秒" + extra),
                                        true
                                    );
                                }
                                return 1;
                            })
                        )
                    )
                )
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherEffectData data = getData(player);
                    int interval = getEffectiveInterval(data);
                    WeatherType forced = getEffectiveForcedWeather(data);
                    String extra = forced != null
                        ? "，强制" + (forced == WeatherType.RAIN ? "雨天" : "雷暴")
                        : "";
                    player.sendSystemMessage(Component.literal("§a[天气效果] §f当前难度：" + data.getDifficulty().displayName + "，间隔：" + (interval / 20) + "秒" + extra));
                    return 1;
                })
            )
            .then(Commands.literal("list")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    var effects = player.getActiveEffects();
                    if (effects.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§a[天气效果] §f当前无药水效果"));
                    } else {
                        MutableComponent msg = Component.literal("§a[天气效果] §f当前药水效果：");
                        boolean first = true;
                        for (MobEffectInstance instance : effects) {
                            if (!first) msg = msg.append(Component.literal("§7, §f"));
                            first = false;
                            Component effectComp = Component.translatable(instance.getEffect().value().getDescriptionId());
                            if (instance.getAmplifier() > 0) {
                                effectComp = Component.literal("").append(effectComp)
                                    .append(Component.literal(" "))
                                    .append(Component.translatable("enchantment.level." + (instance.getAmplifier() + 1)));
                            }
                            msg = msg.append(effectComp);
                        }
                        player.sendSystemMessage(msg);
                    }
                    return 1;
                })
            )
            .then(Commands.literal("weather")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherType weather = getWeatherType(player.serverLevel());
                    String weatherName = switch (weather) {
                        case CLEAR -> "晴天";
                        case RAIN -> "雨天";
                        case THUNDER -> "雷暴";
                    };
                    player.sendSystemMessage(Component.literal("§a[天气效果] §f当前天气：" + weatherName));
                    return 1;
                })
            )
            .then(Commands.literal("durationmode")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("permanent")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        if (!data.getMode().equals("RANDOM")) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在随机模式下可用"));
                            return 0;
                        }
                        data.setDurationMode("PERMANENT");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f效果持续时间模式已设为永久"),
                            true
                        );
                        return 1;
                    })
                )
                .then(Commands.literal("specifictime")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        if (!data.getMode().equals("RANDOM")) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在随机模式下可用"));
                            return 0;
                        }
                        data.setDurationMode("SPECIFIC_TIME");
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f效果持续时间模式已设为特定时间"),
                            true
                        );
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherEffectData data = getData(player);
                    String modeName = "PERMANENT".equals(data.getDurationMode()) ? "永久" : "特定时间";
                    player.sendSystemMessage(Component.literal("§a[天气效果] §f当前效果持续时间模式：" + modeName));
                    return 1;
                })
            )
            .then(Commands.literal("setduration")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        WeatherEffectData data = getData(player);
                        if (!data.getMode().equals("RANDOM")) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在随机模式下可用"));
                            return 0;
                        }
                        if (!"SPECIFIC_TIME".equals(data.getDurationMode())) {
                            ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在特定时间模式下可用"));
                            return 0;
                        }
                        data.setCustomDuration(ticks);
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("§a[天气效果] §f自定义效果持续时间已设为 " + ticks + " tick（" + (ticks / 20) + " 秒）"),
                            true
                        );
                        return 1;
                    })
                )
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    WeatherEffectData data = getData(player);
                    if (!data.getMode().equals("RANDOM")) {
                        ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在随机模式下可用"));
                        return 0;
                    }
                    if (!"SPECIFIC_TIME".equals(data.getDurationMode())) {
                        ctx.getSource().sendFailure(Component.literal("§c[天气效果] §f该指令仅在特定时间模式下可用"));
                        return 0;
                    }
                    String durationStr = data.getCustomDuration() > 0
                        ? data.getCustomDuration() + " tick（" + (data.getCustomDuration() / 20) + " 秒）"
                        : "未设置（使用难度触发间隔）";
                    player.sendSystemMessage(Component.literal("§a[天气效果] §f当前自定义效果持续时间：" + durationStr));
                    return 1;
                })
            )
        );
    }

    @SubscribeEvent
    public static void onItemUseFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getItem().is(Items.MILK_BUCKET)) {
            WeatherEffectData data = getData(player);
            if (!data.isEnabled()) return;
            playerDataMap.remove(player.getUUID());
            if (data.getMode().equals("SPECIFIED")) {
                var newlyApplied = applySpecifiedEffect(player, getWeatherType(player.serverLevel()), data);
                sendEffectFeedback(player, newlyApplied, getEffectiveSpecifiedInterval(data) / 20);
            } else {
                var newlyApplied = applyWeatherEffect(player, getWeatherType(player.serverLevel()), data);
                sendEffectFeedback(player, newlyApplied, getEffectiveInterval(data));
            }
        }
    }

    private static void sendEffectFeedback(ServerPlayer player, Map<String, Integer> newlyApplied, int intervalSec) {
        if (newlyApplied == null || newlyApplied.isEmpty()) return;
        MutableComponent msg = Component.literal("§a[天气效果] §f已触发 ")
            .append(Component.literal(String.valueOf(newlyApplied.size())))
            .append(Component.literal(" 个效果（"))
            .append(Component.literal(String.valueOf(intervalSec)))
            .append(Component.literal("秒触发一次）："));
        boolean first = true;
        for (var entry : newlyApplied.entrySet()) {
            ResourceLocation rl = ResourceLocation.parse(entry.getKey());
            ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, rl);
            var holder = BuiltInRegistries.MOB_EFFECT.getHolder(key).orElse(null);
            if (holder != null) {
                if (!first) msg = msg.append(Component.literal("§7, §f"));
                first = false;
                Component effectComp = Component.translatable(holder.value().getDescriptionId());
                if (entry.getValue() > 0) {
                    effectComp = Component.literal("").append(effectComp)
                        .append(Component.literal(" "))
                        .append(Component.translatable("enchantment.level." + (entry.getValue() + 1)));
                }
                msg = msg.append(effectComp);
            }
        }
        player.sendSystemMessage(msg);
    }

    private static void showModInfo(ServerPlayer player, WeatherEffectData data) {
        WeatherType weather = getWeatherType(player.serverLevel());
        String weatherName = switch (weather) {
            case CLEAR -> "晴天";
            case RAIN -> "雨天";
            case THUNDER -> "雷暴";
        };
        String modeName = data.getMode().equals("SPECIFIED") ? "指定模式" : "随机模式";
        int intervalSec = getEffectiveInterval(data) / 20;
        player.sendSystemMessage(Component.literal("§a[天气效果] §f当前模式：" + modeName + "，当前难度：" + data.getDifficulty().displayName + "，间隔：" + intervalSec + "秒"));
        player.sendSystemMessage(Component.literal("§a[天气效果] §f当前天气：" + weatherName));
        var effects = player.getActiveEffects();
        if (effects.isEmpty()) {
            player.sendSystemMessage(Component.literal("§a[天气效果] §f当前无药水效果"));
        } else {
            MutableComponent msg = Component.literal("§a[天气效果] §f当前效果：");
            boolean first = true;
            for (MobEffectInstance instance : effects) {
                if (!first) msg = msg.append(Component.literal("§7, §f"));
                first = false;
                Component effectComp = Component.translatable(instance.getEffect().value().getDescriptionId());
                if (instance.getAmplifier() > 0) {
                    effectComp = Component.literal("").append(effectComp)
                        .append(Component.literal(" "))
                        .append(Component.translatable("enchantment.level." + (instance.getAmplifier() + 1)));
                }
                msg = msg.append(effectComp);
            }
            player.sendSystemMessage(msg);
        }
    }

    private static WeatherType getWeatherType(ServerLevel level) {
        if (level.isThundering()) return WeatherType.THUNDER;
        if (level.isRaining()) return WeatherType.RAIN;
        return WeatherType.CLEAR;
    }

    private static Map<String, Integer> applyWeatherEffect(ServerPlayer player, WeatherType currentWeather, WeatherEffectData weatherData) {
        UUID uuid = player.getUUID();
        PlayerData data = playerDataMap.get(uuid);

        if (data == null) {
            data = new PlayerData(currentWeather);
            playerDataMap.put(uuid, data);
        } else if (data.lastWeather() != currentWeather) {
            data = new PlayerData(currentWeather, data.appliedEffects());
            playerDataMap.put(uuid, data);
        }

        return switch (currentWeather) {
            case CLEAR -> applySingleEffect(player, data, POSITIVE_EFFECTS, weatherData);
            case RAIN -> applySingleEffect(player, data, RAIN_EFFECTS, weatherData);
            case THUNDER -> applyRainThunderEffects(player, data, THUNDER_EFFECTS, weatherData);
        };
    }

    private static Map<String, Integer> applySingleEffect(ServerPlayer player, PlayerData data, List<Holder<MobEffect>> pool, WeatherEffectData weatherData) {
        int poolSize = pool.size();
        Holder<MobEffect> chosenEffect = pool.get(player.getRandom().nextInt(poolSize));
        String chosenEffectId = chosenEffect.unwrapKey().orElseThrow().location().toString();

        int duration = getDurationForRandomMode(chosenEffect, weatherData);
        Integer existingAmp = data.appliedEffects().get(chosenEffectId);
        if (existingAmp != null) {
            int newAmplifier = Math.min(existingAmp + 1, MAX_AMPLIFIER);
            player.removeEffect(chosenEffect);
            player.addEffect(new MobEffectInstance(chosenEffect, duration, newAmplifier, false, true, true));
            data.appliedEffects().put(chosenEffectId, newAmplifier);
            return Map.of(chosenEffectId, newAmplifier);
        } else {
            player.addEffect(new MobEffectInstance(chosenEffect, duration, 0, false, true, true));
            data.appliedEffects().put(chosenEffectId, 0);
            return Map.of(chosenEffectId, 0);
        }
    }

    private static Map<String, Integer> applyRainThunderEffects(ServerPlayer player, PlayerData data, List<Holder<MobEffect>> pool, WeatherEffectData weatherData) {
        Map<String, Integer> newlyApplied = new ConcurrentHashMap<>();
        int poolSize = pool.size();
        int count = player.getRandom().nextInt(poolSize + 1);
        List<Holder<MobEffect>> remaining = new ArrayList<>(pool);
        for (int i = 0; i < count && !remaining.isEmpty(); i++) {
            int pickIdx = player.getRandom().nextInt(remaining.size());
            Holder<MobEffect> effect = remaining.remove(pickIdx);
            int duration = getDurationForRandomMode(effect, weatherData);
            String effectId = effect.unwrapKey().orElseThrow().location().toString();
            Integer existingAmp = data.appliedEffects().get(effectId);
            if (existingAmp != null) {
                int newAmplifier = Math.min(existingAmp + 1, MAX_AMPLIFIER);
                player.removeEffect(effect);
                player.addEffect(new MobEffectInstance(effect, duration, newAmplifier, false, true, true));
                data.appliedEffects().put(effectId, newAmplifier);
                newlyApplied.put(effectId, newAmplifier);
            } else {
                player.addEffect(new MobEffectInstance(effect, duration, 0, false, true, true));
                data.appliedEffects().put(effectId, 0);
                newlyApplied.put(effectId, 0);
            }
        }
        return newlyApplied;
    }

    private static int getDurationFor(Holder<MobEffect> effect) {
        return effect.value().isInstantenous() ? 1 : -1;
    }

    private static int getDurationForRandomMode(Holder<MobEffect> effect, WeatherEffectData data) {
        if (effect.value().isInstantenous()) return 1;
        if ("PERMANENT".equals(data.getDurationMode())) return -1;
        if (data.getCustomDuration() > 0) return data.getCustomDuration();
        return getEffectiveInterval(data);
    }

    private static int getEffectiveInterval(WeatherEffectData data) {
        if (data.getDifficulty() == Difficulty.CUSTOM) {
            return data.getCustomInterval();
        }
        return data.getDifficulty().interval;
    }

    private static WeatherType getEffectiveForcedWeather(WeatherEffectData data) {
        if (data.getPendingForcedWeatherTicks() > 0) return null;
        if (data.getDifficulty() == Difficulty.CUSTOM) {
            if (data.getCustomForcedWeather().isEmpty()) return null;
            return WeatherType.valueOf(data.getCustomForcedWeather().toUpperCase());
        }
        return data.getDifficulty().forcedWeather;
    }

    private static void forceWeatherIfNeeded(ServerLevel level, WeatherEffectData data) {
        WeatherType forced = getEffectiveForcedWeather(data);
        if (forced == null) return;
        int veryLong = 24000 * 1000;
        switch (forced) {
            case RAIN -> level.setWeatherParameters(0, veryLong, true, false);
            case THUNDER -> level.setWeatherParameters(0, veryLong, true, true);
        }
    }

    private static void removeOldEffect(ServerPlayer player, String effectId) {
        ResourceLocation rl = ResourceLocation.parse(effectId);
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, rl);
        BuiltInRegistries.MOB_EFFECT.getHolder(key).ifPresent(player::removeEffect);
    }

    private static int getEffectiveSpecifiedInterval(WeatherEffectData data) {
        return data.getSpecifiedInterval();
    }

    private static void rerollSpecifiedEffects(WeatherEffectData data, RandomSource random) {
        List<Holder<MobEffect>> pool = new ArrayList<>();
        pool.addAll(POSITIVE_EFFECTS);
        pool.addAll(NEGATIVE_EFFECTS);

        data.setSpecifiedClearEffect(pool.get(random.nextInt(pool.size())).unwrapKey().orElseThrow().location().toString());
        data.setSpecifiedRainEffect(pool.get(random.nextInt(pool.size())).unwrapKey().orElseThrow().location().toString());
        data.setSpecifiedThunderEffect(pool.get(random.nextInt(pool.size())).unwrapKey().orElseThrow().location().toString());
        data.setDirty();
    }

    private static Component buildSpecifiedAnnouncement(WeatherEffectData data) {
        MutableComponent msg = Component.literal("§a[天气效果] §f指定模式效果已刷新：");
        msg = msg.append(Component.literal("\n  §e晴天§f：").append(buildEffectComponent(data.getSpecifiedClearEffect())));
        msg = msg.append(Component.literal("\n  §b雨天§f：").append(buildEffectComponent(data.getSpecifiedRainEffect())));
        msg = msg.append(Component.literal("\n  §d雷暴§f：").append(buildEffectComponent(data.getSpecifiedThunderEffect())));
        return msg;
    }

    private static Component buildEffectComponent(String effectId) {
        if (effectId == null || effectId.isEmpty()) return Component.literal("§7无");
        ResourceLocation rl = ResourceLocation.parse(effectId);
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, rl);
        return BuiltInRegistries.MOB_EFFECT.getHolder(key)
            .map(h -> Component.translatable(h.value().getDescriptionId()))
            .orElse(Component.literal("§7未知"));
    }

    private static Map<String, Integer> applySpecifiedEffect(ServerPlayer player, WeatherType currentWeather, WeatherEffectData data) {
        String effectId = switch (currentWeather) {
            case CLEAR -> data.getSpecifiedClearEffect();
            case RAIN -> data.getSpecifiedRainEffect();
            case THUNDER -> data.getSpecifiedThunderEffect();
        };

        if (effectId == null || effectId.isEmpty()) {
            return Map.of();
        }

        UUID uuid = player.getUUID();
        PlayerData pData = playerDataMap.get(uuid);

        if (pData == null) {
            pData = new PlayerData(currentWeather);
            playerDataMap.put(uuid, pData);
        } else if (pData.lastWeather() != currentWeather) {
            pData = new PlayerData(currentWeather, pData.appliedEffects());
            playerDataMap.put(uuid, pData);
        }

        ResourceLocation rl = ResourceLocation.parse(effectId);
        ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, rl);
        Holder<MobEffect> effectHolder = BuiltInRegistries.MOB_EFFECT.getHolder(key).orElse(null);
        if (effectHolder == null) return Map.of();

        int duration = getDurationFor(effectHolder);
        Integer existingAmp = pData.appliedEffects().get(effectId);

        if (existingAmp != null) {
            int newAmplifier = Math.min(existingAmp + 1, MAX_AMPLIFIER);
            player.removeEffect(effectHolder);
            player.addEffect(new MobEffectInstance(effectHolder, duration, newAmplifier, false, true, true));
            pData.appliedEffects().put(effectId, newAmplifier);
            return Map.of(effectId, newAmplifier);
        } else {
            player.addEffect(new MobEffectInstance(effectHolder, duration, 0, false, true, true));
            pData.appliedEffects().put(effectId, 0);
            return Map.of(effectId, 0);
        }
    }
}
