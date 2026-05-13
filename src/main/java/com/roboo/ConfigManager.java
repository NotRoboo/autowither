package com.roboo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("autowither.json");

    // Mirror of ModConfig fields for serialization
    private static class ConfigData {
        boolean comboAttackEnabled = true;
        boolean witherMagicEnabled = true;
        boolean demonMagicEnabled = true;
        boolean autoSummonEnabled = true;
        boolean autoDodgeEnabled = true;
        boolean autoReconnectEnabled = true;
        boolean warpToVolcano = true;
    }

    public static void load() {
        File file = CONFIG_PATH.toFile();
        if (!file.exists()) {
            save(); // write defaults on first run
            return;
        }

        try (Reader reader = new FileReader(file)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) return;

            ModConfig.setComboAttackEnabled(data.comboAttackEnabled);
            ModConfig.setWitherMagicEnabled(data.witherMagicEnabled);
            ModConfig.setDemonMagicEnabled(data.demonMagicEnabled);
            ModConfig.setAutoSummonEnabled(data.autoSummonEnabled);
            ModConfig.setAutoDodgeEnabled(data.autoDodgeEnabled);
            ModConfig.setAutoReconnectEnabled(data.autoReconnectEnabled);
            ModConfig.setWarpToVolcano(data.warpToVolcano);
        } catch (IOException e) {
            System.err.println("[AutoWither] Failed to load config: " + e.getMessage());
        }
    }

    public static void save() {
        ConfigData data = new ConfigData();
        data.comboAttackEnabled = ModConfig.isComboAttackEnabled();
        data.witherMagicEnabled = ModConfig.isWitherMagicEnabled();
        data.demonMagicEnabled = ModConfig.isDemonMagicEnabled();
        data.autoSummonEnabled = ModConfig.isAutoSummonEnabled();
        data.autoDodgeEnabled = ModConfig.isAutoDodgeEnabled();
        data.autoReconnectEnabled = ModConfig.isAutoReconnectEnabled();
        data.warpToVolcano = ModConfig.isWarpToVolcano();

        try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(data, writer);
        } catch (IOException e) {
            System.err.println("[AutoWither] Failed to save config: " + e.getMessage());
        }
    }
}