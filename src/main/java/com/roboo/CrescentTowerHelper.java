package com.roboo;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Locale;

public class CrescentTowerHelper {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final double TARGET_Z = -12.0;
    private static final double Z_TOLERANCE = 0.4;

    private static final double TARGET_X = -105.5;
    private static final double X_TOLERANCE = 0.4;

    private static final float TARGET_YAW = -176f;
    private static final float TARGET_PITCH = 0f;
    private static final float ROTATION_TOLERANCE = 2f;

    private static final long COMMAND_DELAY_MS = 1000;

    // Sequence state
    private enum Stage {
        IDLE,
        MOVE_LEFT,
        MOVE_FORWARD,
        ROTATE,
        PENDING_MBAG
    }

    private static Stage stage = Stage.IDLE;
    private static boolean modTriggered = false;
    private static long pendingMbagTime = 0;

    // =========================
    // INIT
    // =========================
    public static void init() {
        ClientReceiveMessageEvents.GAME.register((msg, overlay) ->
                handleMessage(msg.getString()));

        ClientReceiveMessageEvents.CHAT.register((msg, signed, sender, params, timestamp) ->
                handleMessage(msg.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> onTick());
    }

    // =========================
    // TRIGGER
    // =========================
    public static void trigger() {
        modTriggered = true;
    }

    // =========================
    // CHAT
    // =========================
    private static void handleMessage(String msg) {
        if (msg == null) return;
        if (!ModConfig.isAutoReconnectEnabled()) return;
        if (!modTriggered) return;

        String clean = msg.toLowerCase(Locale.ROOT);

        if (clean.contains("warping...")) {
            stage = Stage.MOVE_LEFT;
            modTriggered = false;
            log("§eWarping detected — starting movement sequence");
        }
    }

    // =========================
    // TICK
    // =========================
    private static void onTick() {
        if (mc.player == null || !ModConfig.isAutoReconnectEnabled()) return;

        long now = System.currentTimeMillis();

        switch (stage) {
            case IDLE -> {}

            case MOVE_LEFT -> {
                double pz = mc.player.getZ();
                if (Math.abs(pz - TARGET_Z) <= Z_TOLERANCE) {
                    mc.options.keyLeft.setDown(false);
                    stage = Stage.MOVE_FORWARD;
                    log("§eReached Z target — moving forward");
                } else {
                    mc.options.keyLeft.setDown(true);
                }
            }

            case MOVE_FORWARD -> {
                double px = mc.player.getX();
                if (Math.abs(px - TARGET_X) <= X_TOLERANCE) {
                    mc.options.keyUp.setDown(false);
                    stage = Stage.ROTATE;
                    log("§eReached X target — rotating");
                } else {
                    mc.options.keyUp.setDown(true);
                }
            }

            case ROTATE -> {
                boolean done = RotationHelper.lookAt(TARGET_YAW, TARGET_PITCH);

                float yawDiff = Math.abs(wrapDegrees(mc.player.getYRot() - TARGET_YAW));
                float pitchDiff = Math.abs(mc.player.getXRot() - TARGET_PITCH);

                if (done || (yawDiff < ROTATION_TOLERANCE && pitchDiff < ROTATION_TOLERANCE)) {
                    stage = Stage.PENDING_MBAG;
                    pendingMbagTime = System.currentTimeMillis() + COMMAND_DELAY_MS;
                    log("§eRotation done — queuing /mbag toggle");
                }
            }

            case PENDING_MBAG -> {
                if (now >= pendingMbagTime) {
                    runCommand("mbag toggle");
                    stage = Stage.IDLE;
                    log("§aRan: /mbag toggle");
                }
            }
        }
    }

    // =========================
    // RESET
    // =========================
    public static void reset() {
        stage = Stage.IDLE;
        modTriggered = false;
        pendingMbagTime = 0;
        mc.options.keyLeft.setDown(false);
        mc.options.keyUp.setDown(false);
    }

    // =========================
    // HELPERS
    // =========================
    private static float wrapDegrees(float degrees) {
        degrees = degrees % 360.0f;
        if (degrees >= 180.0f) degrees -= 360.0f;
        if (degrees < -180.0f) degrees += 360.0f;
        return degrees;
    }

    private static void runCommand(String command) {
        if (mc.player == null) return;
        mc.player.connection.sendCommand(command);
    }

    private static void log(String text) {
        if (mc.gui != null) {
            mc.gui.getChat().addMessage(
                    Component.literal("§e[CrescentTower] " + text)
            );
        }
    }
}