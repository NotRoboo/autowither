package com.roboo;

public class ModConfig {

    private static boolean comboAttackEnabled = true;
    private static boolean witherMagicEnabled = true;
    private static boolean demonMagicEnabled = true;
    private static boolean autoSummonEnabled = true;
    private static boolean autoDodgeEnabled = true;
    private static boolean autoReconnectEnabled = true;
    private static boolean warpToVolcano = true;

    public static boolean isComboAttackEnabled() { return comboAttackEnabled; }
    public static boolean isWitherMagicEnabled() { return witherMagicEnabled; }
    public static boolean isDemonMagicEnabled() { return demonMagicEnabled; }
    public static boolean isAutoSummonEnabled() { return autoSummonEnabled; }
    public static boolean isAutoDodgeEnabled() { return autoDodgeEnabled; }
    public static boolean isAutoReconnectEnabled() { return autoReconnectEnabled; }
    public static boolean isWarpToVolcano() { return warpToVolcano; }

    public static void setComboAttackEnabled(boolean v) { comboAttackEnabled = v; }
    public static void setWitherMagicEnabled(boolean v) { witherMagicEnabled = v; }
    public static void setDemonMagicEnabled(boolean v) { demonMagicEnabled = v; }
    public static void setAutoSummonEnabled(boolean v) { autoSummonEnabled = v; }
    public static void setAutoDodgeEnabled(boolean v) { autoDodgeEnabled = v; }
    public static void setAutoReconnectEnabled(boolean v) { autoReconnectEnabled = v; }
    public static void setWarpToVolcano(boolean v) { warpToVolcano = v; }

    public static String getWarpDestination() {
        return warpToVolcano ? "Ancient Volcano" : "Crescent Tower";
    }
}

