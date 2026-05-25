package com.roboo.macro;

public class MacroEvent {

    public enum Type {
        KEY,
        MOUSE_BUTTON,
        MOUSE_MOVE
    }

    public long tick;
    public double subTickOffset;
    public String type;
    public int keyOrButton;
    public int action;
    public double deltaX;
    public double deltaY;
    public CameraSnapshot camera;

    public MacroEvent() {}

    public MacroEvent(long tick, double subTickOffset, Type type, int keyOrButton, int action,
                      double deltaX, double deltaY, CameraSnapshot camera) {
        this.tick         = tick;
        this.subTickOffset = subTickOffset;
        this.type         = type.name();
        this.keyOrButton  = keyOrButton;
        this.action       = action;
        this.deltaX       = deltaX;
        this.deltaY       = deltaY;
        this.camera       = camera;
    }

    @Override
    public String toString() {
        return String.format("Tick %d + %.4f: %s [key=%d, action=%d]",
                tick, subTickOffset, type, keyOrButton, action);
    }

    public static class CameraSnapshot {
        public float yaw;
        public float pitch;

        public CameraSnapshot() {}

        public CameraSnapshot(float yaw, float pitch) {
            this.yaw   = yaw;
            this.pitch = pitch;
        }

        @Override
        public String toString() {
            return String.format("Camera(yaw=%.2f, pitch=%.2f)", yaw, pitch);
        }
    }
}