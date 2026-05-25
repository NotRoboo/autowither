package com.roboo.macro;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public class MacroInputRecorder {

    private static final Minecraft mc = Minecraft.getInstance();

    private static final List<MacroEvent> events = new ArrayList<>();
    private static long currentTick = 0L;
    private static long tickStartNano = 0L;
    private static boolean recording = false;

    private static double recordStartX = 0.0;
    private static double recordStartY = 0.0;
    private static double recordStartZ = 0.0;
    private static boolean recordedWhileSprinting = false;

    public static void init() {
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            currentTick++;
            tickStartNano = System.nanoTime();
        });
    }

    public static void startRecording() {
        events.clear();
        currentTick = 0L;
        tickStartNano = 0L;
        recording = true;

        if (mc.player != null) {
            recordStartX = mc.player.getX();
            recordStartY = mc.player.getY();
            recordStartZ = mc.player.getZ();

            recordedWhileSprinting = mc.player.isSprinting();

            float yaw   = mc.player.getYRot();
            float pitch = mc.player.getXRot();
            events.add(new MacroEvent(0, 0.0, MacroEvent.Type.MOUSE_MOVE, 0, 0, 0, 0,
                    new MacroEvent.CameraSnapshot(yaw, pitch)));
        }
    }

    public static void stopRecording() {
        recording = false;
    }

    public static boolean isRecording() {
        return recording;
    }

    public static List<MacroEvent> getEvents() {
        return new ArrayList<>(events);
    }

    public static double  getRecordStartX()           { return recordStartX; }
    public static double  getRecordStartY()           { return recordStartY; }
    public static double  getRecordStartZ()           { return recordStartZ; }
    public static boolean wasRecordedWhileSprinting() { return recordedWhileSprinting; }

    public static void recordKey(long nanoTime, int keyCode, int action) {
        if (!recording) return;
        double sub = subTick(nanoTime);
        events.add(new MacroEvent(currentTick, sub, MacroEvent.Type.KEY, keyCode, action, 0, 0, null));
    }

    public static void recordMouseButton(long nanoTime, int button, int action) {
        if (!recording) return;
        double sub = subTick(nanoTime);
        events.add(new MacroEvent(currentTick, sub, MacroEvent.Type.MOUSE_BUTTON, button, action, 0, 0, null));
    }

    public static void recordMouseMove(long nanoTime, double deltaX, double deltaY) {
        if (!recording) return;
        double sub = subTick(nanoTime);
        float yaw = 0f, pitch = 0f;
        if (mc.player != null) {
            yaw   = mc.player.getYRot();
            pitch = mc.player.getXRot();
        }
        events.add(new MacroEvent(currentTick, sub, MacroEvent.Type.MOUSE_MOVE, 0, 0, deltaX, deltaY,
                new MacroEvent.CameraSnapshot(yaw, pitch)));
    }

    private static double subTick(long nanoTime) {
        if (tickStartNano == 0L) return 0.0;
        return (double)(nanoTime - tickStartNano) / 5.0e7;
    }
}