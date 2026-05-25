package com.roboo.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.HashMap;
import java.util.Map;

public class MacroInputPlayback {

    private static final Minecraft mc = Minecraft.getInstance();

    private static boolean active = false;
    private static boolean sneaking = false;
    private static final Map<Integer, Boolean> activeKeys = new HashMap<>();

    public static void setActive(boolean value) {
        active = value;
        if (!active) {
            activeKeys.clear();
            sneaking = false;
            if (mc.player != null) {
                mc.player.setSprinting(false);
                mc.player.setShiftKeyDown(false);
                if (mc.player.input != null) {
                    mc.player.input.keyPresses = Input.EMPTY;
                }
            }
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isSneaking() {
        return sneaking;
    }

    public static void executeEvent(MacroEvent event) {
        if (mc.player == null) return;

        switch (event.type) {
            case "KEY"          -> handleKey(event.keyOrButton, event.action);
            case "MOUSE_BUTTON" -> handleMouseButton(event.keyOrButton, event.action);
            case "MOUSE_MOVE"   -> {
                if (event.camera != null) {
                    mc.player.setYRot(event.camera.yaw);
                    mc.player.setXRot(event.camera.pitch);
                }
            }
        }
    }

    private static void handleKey(int key, int action) {
        activeKeys.put(key, action == 1 || action == 2);
    }

    private static void handleMouseButton(int button, int action) {
        if (mc.player == null || mc.gameMode == null) return;
        if (action != 1) return;

        if (button == 0) {
            if (mc.hitResult != null) {
                if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) mc.hitResult;
                    mc.gameMode.startDestroyBlock(bhr.getBlockPos(), bhr.getDirection());
                    mc.player.swing(InteractionHand.MAIN_HAND);
                } else if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult ehr = (EntityHitResult) mc.hitResult;
                    mc.gameMode.attack(mc.player, ehr.getEntity());
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        } else if (button == 1) {
            if (mc.hitResult != null) {
                if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) mc.hitResult;
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, bhr);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                } else if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                    EntityHitResult ehr = (EntityHitResult) mc.hitResult;
                    mc.gameMode.interact(mc.player, ehr.getEntity(), InteractionHand.MAIN_HAND);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        }
    }

    public static void applyMovement() {
        if (mc.player == null || !active) return;

        boolean forward  = isPressed(87);
        boolean backward = isPressed(83);
        boolean left     = isPressed(65);
        boolean right    = isPressed(68);
        boolean jump     = isPressed(32);
        boolean sneak    = isPressed(340);

        sneaking = sneak;
        mc.player.setShiftKeyDown(sneak);

        mc.options.keyUp.setDown(forward);
        mc.options.keyDown.setDown(backward);
        mc.options.keyLeft.setDown(left);
        mc.options.keyRight.setDown(right);
        mc.options.keyJump.setDown(jump);
        mc.options.keyShift.setDown(sneak);

        if (mc.player.input != null) {
            mc.player.input.keyPresses = new Input(forward, backward, left, right, jump, sneak, mc.player.isSprinting());
        }
    }

    private static boolean isPressed(int key) {
        return activeKeys.getOrDefault(key, false);
    }
}