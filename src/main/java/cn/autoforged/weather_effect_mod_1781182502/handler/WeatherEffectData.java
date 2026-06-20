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

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class WeatherEffectData extends SavedData {
    public static final String DATA_NAME = "weather_effect_data";

    private boolean enabled = true;
    private String difficultyName = "NORMAL";
    private int customInterval = 12000;
    private String customForcedWeather = "";
    private int tickCounter = 0;
    private int pendingForcedWeatherTicks = 0;
    private String mode = "RANDOM";
    private String specifiedClearEffect = "";
    private String specifiedRainEffect = "";
    private String specifiedThunderEffect = "";
    private int specifiedInterval = 12000;
    private int specifiedTickCounter = 0;
    private boolean specifiedInfinite = false;
    private String durationMode = "PERMANENT";
    private int customDuration = 0;

    public static Factory<WeatherEffectData> factory() {
        return new Factory<>(WeatherEffectData::new, WeatherEffectData::load);
    }

    public static WeatherEffectData load(CompoundTag tag, HolderLookup.Provider registries) {
        WeatherEffectData data = new WeatherEffectData();
        data.enabled = tag.getBoolean("enabled");
        data.difficultyName = tag.getString("difficultyName");
        data.customInterval = tag.getInt("customInterval");
        data.customForcedWeather = tag.getString("customForcedWeather");
        data.tickCounter = tag.getInt("tickCounter");
        data.pendingForcedWeatherTicks = tag.getInt("pendingForcedWeatherTicks");
        data.mode = tag.getString("mode");
        data.specifiedClearEffect = tag.getString("specifiedClearEffect");
        data.specifiedRainEffect = tag.getString("specifiedRainEffect");
        data.specifiedThunderEffect = tag.getString("specifiedThunderEffect");
        data.specifiedInterval = tag.getInt("specifiedInterval");
        data.specifiedTickCounter = tag.getInt("specifiedTickCounter");
        data.specifiedInfinite = tag.getBoolean("specifiedInfinite");
        data.durationMode = tag.contains("durationMode") ? tag.getString("durationMode") : "PERMANENT";
        data.customDuration = tag.getInt("customDuration");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("enabled", this.enabled);
        tag.putString("difficultyName", this.difficultyName);
        tag.putInt("customInterval", this.customInterval);
        tag.putString("customForcedWeather", this.customForcedWeather);
        tag.putInt("tickCounter", this.tickCounter);
        tag.putInt("pendingForcedWeatherTicks", this.pendingForcedWeatherTicks);
        tag.putString("mode", this.mode);
        tag.putString("specifiedClearEffect", this.specifiedClearEffect);
        tag.putString("specifiedRainEffect", this.specifiedRainEffect);
        tag.putString("specifiedThunderEffect", this.specifiedThunderEffect);
        tag.putInt("specifiedInterval", this.specifiedInterval);
        tag.putInt("specifiedTickCounter", this.specifiedTickCounter);
        tag.putBoolean("specifiedInfinite", this.specifiedInfinite);
        tag.putString("durationMode", this.durationMode);
        tag.putInt("customDuration", this.customDuration);
        return tag;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; setDirty(); }

    public String getDifficultyName() { return difficultyName; }
    public void setDifficultyName(String name) { this.difficultyName = name; setDirty(); }

    public WeatherEffectHandler.Difficulty getDifficulty() {
        try {
            return WeatherEffectHandler.Difficulty.valueOf(difficultyName);
        } catch (IllegalArgumentException e) {
            return WeatherEffectHandler.Difficulty.NORMAL;
        }
    }

    public void setDifficulty(WeatherEffectHandler.Difficulty diff) {
        this.difficultyName = diff.name();
        setDirty();
    }

    public int getCustomInterval() { return customInterval; }
    public void setCustomInterval(int interval) { this.customInterval = interval; setDirty(); }

    public String getCustomForcedWeather() { return customForcedWeather; }
    public void setCustomForcedWeather(String weather) { this.customForcedWeather = weather; setDirty(); }

    public int getTickCounter() { return tickCounter; }
    public void setTickCounter(int counter) { this.tickCounter = counter; }

    public int getPendingForcedWeatherTicks() { return pendingForcedWeatherTicks; }
    public void setPendingForcedWeatherTicks(int ticks) { this.pendingForcedWeatherTicks = ticks; setDirty(); }
    public void decrPendingForcedWeatherTicks() { if (pendingForcedWeatherTicks > 0) pendingForcedWeatherTicks--; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; setDirty(); }

    public String getSpecifiedClearEffect() { return specifiedClearEffect; }
    public void setSpecifiedClearEffect(String effect) { this.specifiedClearEffect = effect; setDirty(); }

    public String getSpecifiedRainEffect() { return specifiedRainEffect; }
    public void setSpecifiedRainEffect(String effect) { this.specifiedRainEffect = effect; setDirty(); }

    public String getSpecifiedThunderEffect() { return specifiedThunderEffect; }
    public void setSpecifiedThunderEffect(String effect) { this.specifiedThunderEffect = effect; setDirty(); }

    public int getSpecifiedInterval() { return specifiedInterval; }
    public void setSpecifiedInterval(int interval) { this.specifiedInterval = interval; setDirty(); }

    public int getSpecifiedTickCounter() { return specifiedTickCounter; }
    public void setSpecifiedTickCounter(int counter) { this.specifiedTickCounter = counter; }

    public boolean isSpecifiedInfinite() { return specifiedInfinite; }
    public void setSpecifiedInfinite(boolean infinite) { this.specifiedInfinite = infinite; setDirty(); }

    public String getDurationMode() { return durationMode; }
    public void setDurationMode(String mode) { this.durationMode = mode; setDirty(); }
    public int getCustomDuration() { return customDuration; }
    public void setCustomDuration(int duration) { this.customDuration = duration; setDirty(); }
}
