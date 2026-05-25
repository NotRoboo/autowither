package com.roboo.macro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MacroManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path MACROS_DIR = Minecraft.getInstance()
            .gameDirectory.toPath().resolve("macros");

    static {
        try {
            Files.createDirectories(MACROS_DIR);
        } catch (IOException e) {
            System.err.println("[Macro] Failed to create macros directory: " + e.getMessage());
        }
    }

    private static final Minecraft mc = Minecraft.getInstance();

    private static SavedMacro loaded = null;
    private static boolean playing = false;
    private static int playbackIndex = 0;
    private static long playbackStartNano = 0L;

    private static int warmupTicksRemaining = 0;
    private static int sprintKeyHoldTicks = 0;
    private static final int WARMUP_TICKS = 5;
    private static final int SPRINT_KEY_HOLD_TICKS = 2;

    private static final double START_POS_TOLERANCE = 0.3;

    public static void init() {
        MacroInputRecorder.init();
    }

    public static boolean saveMacro(String name) {
        List<MacroEvent> events = MacroInputRecorder.getEvents();
        if (events.isEmpty()) {
            System.err.println("[Macro] No events to save.");
            return false;
        }

        SavedMacro macro = new SavedMacro(name, events,
                MacroInputRecorder.getRecordStartX(),
                MacroInputRecorder.getRecordStartY(),
                MacroInputRecorder.getRecordStartZ(),
                MacroInputRecorder.wasRecordedWhileSprinting());

        Path file = MACROS_DIR.resolve(sanitize(name) + ".json");
        try (Writer w = new FileWriter(file.toFile())) {
            GSON.toJson(macro, w);
            System.out.println("[Macro] Saved: " + name + " (" + events.size() + " events)");
            return true;
        } catch (IOException e) {
            System.err.println("[Macro] Failed to save: " + e.getMessage());
            return false;
        }
    }

    public static boolean loadMacro(String name) {
        Path file = MACROS_DIR.resolve(sanitize(name) + ".json");
        if (!Files.exists(file)) {
            System.err.println("[Macro] Not found: " + name);
            return false;
        }
        try (Reader r = new FileReader(file.toFile())) {
            loaded = GSON.fromJson(r, SavedMacro.class);
            System.out.println("[Macro] Loaded: " + loaded.name + " (" + loaded.events.size() + " events)");
            return true;
        } catch (IOException e) {
            System.err.println("[Macro] Failed to load: " + e.getMessage());
            return false;
        }
    }

    public static List<String> listMacros() {
        List<String> names = new ArrayList<>();
        try {
            Files.list(MACROS_DIR)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(p -> {
                        String fn = p.getFileName().toString();
                        names.add(fn.substring(0, fn.length() - 5));
                    });
        } catch (IOException e) {
            System.err.println("[Macro] Failed to list macros: " + e.getMessage());
        }
        return names;
    }

    public static boolean deleteMacro(String name) {
        Path file = MACROS_DIR.resolve(sanitize(name) + ".json");
        try {
            Files.deleteIfExists(file);
            System.out.println("[Macro] Deleted: " + name);
            if (loaded != null && loaded.name.equals(name)) loaded = null;
            return true;
        } catch (IOException e) {
            System.err.println("[Macro] Failed to delete: " + e.getMessage());
            return false;
        }
    }

    public static boolean startPlayback() {
        if (loaded == null) {
            System.err.println("[Macro] No macro loaded.");
            return false;
        }
        if (playing) {
            System.err.println("[Macro] Already playing.");
            return false;
        }
        if (mc.player == null) {
            System.err.println("[Macro] No player.");
            return false;
        }

        double dx = mc.player.getX() - loaded.startX;
        double dy = mc.player.getY() - loaded.startY;
        double dz = mc.player.getZ() - loaded.startZ;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > START_POS_TOLERANCE) {
            System.err.printf("[Macro] Too far from start position (%.3f blocks, max %.1f). Aborting.%n",
                    dist, START_POS_TOLERANCE);
            return false;
        }

        mc.player.setPos(loaded.startX, loaded.startY, loaded.startZ);

        snapToInitialCamera();

        warmupTicksRemaining = WARMUP_TICKS;
        sprintKeyHoldTicks   = 0;

        if (loaded.recordedWhileSprinting && !mc.player.isSprinting()) {
            sprintKeyHoldTicks = SPRINT_KEY_HOLD_TICKS;
            mc.options.keySprint.setDown(true);
        }

        playing = true;
        playbackIndex = 0;
        playbackStartNano = 0L;
        MacroInputPlayback.setActive(true);
        System.out.println("[Macro] Playback started: " + loaded.name);
        return true;
    }

    private static void snapToInitialCamera() {
        if (loaded == null || mc.player == null) return;
        for (MacroEvent e : loaded.events) {
            if ("MOUSE_MOVE".equals(e.type) && e.camera != null) {
                mc.player.setYRot(e.camera.yaw);
                mc.player.setXRot(e.camera.pitch);
                return;
            }
        }
    }

    public static void stopPlayback() {
        playing = false;
        playbackIndex = 0;
        playbackStartNano = 0L;
        warmupTicksRemaining = 0;
        sprintKeyHoldTicks = 0;
        mc.options.keySprint.setDown(false);
        MacroInputPlayback.setActive(false);
        System.out.println("[Macro] Playback stopped.");
    }

    public static void updatePlayback() {
        updatePlayback(System.nanoTime());
    }

    public static void updatePlayback(long nowNano) {
        if (!playing || loaded == null) return;

        if (sprintKeyHoldTicks > 0) {
            sprintKeyHoldTicks--;
            if (sprintKeyHoldTicks == 0) {
                mc.options.keySprint.setDown(false);
            }
            return;
        }

        if (warmupTicksRemaining > 0) {
            warmupTicksRemaining--;
            return;
        }

        if (playbackStartNano == 0L) playbackStartNano = nowNano;

        double elapsed = (double)(nowNano - playbackStartNano) / 5.0e7;
        if (elapsed < 0) elapsed = 0;

        long   timeTick    = (long) Math.floor(elapsed);
        double timeSubTick = elapsed - timeTick;

        while (playbackIndex < loaded.events.size()) {
            MacroEvent e = loaded.events.get(playbackIndex);
            if (e.tick > timeTick || (e.tick == timeTick && e.subTickOffset > timeSubTick)) break;
            MacroInputPlayback.executeEvent(e);
            playbackIndex++;
        }

        if (playbackIndex >= loaded.events.size()) {
            stopPlayback();
            System.out.println("[Macro] Playback finished.");
        }
    }

    public static boolean isPlaying()    { return playing; }
    public static boolean isRecording()  { return MacroInputRecorder.isRecording(); }
    public static SavedMacro getLoaded() { return loaded; }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }

    public static class SavedMacro {
        public String name;
        public List<MacroEvent> events;
        public double startX;
        public double startY;
        public double startZ;
        public boolean recordedWhileSprinting;

        public SavedMacro() {}

        public SavedMacro(String name, List<MacroEvent> events,
                          double startX, double startY, double startZ,
                          boolean recordedWhileSprinting) {
            this.name   = name;
            this.events = new ArrayList<>(events);
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
            this.recordedWhileSprinting = recordedWhileSprinting;
        }
    }
}