package com.roboo.mixin;

import com.roboo.macro.MacroInputPlayback;
import com.roboo.macro.MacroInputRecorder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class MouseMixin {

    @Inject(method = "onButton", at = @At("HEAD"))
    private void onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (!MacroInputPlayback.isActive()) {
            MacroInputRecorder.recordMouseButton(System.nanoTime(), input.button(), action);
        }
    }

    @Inject(method = "onMove", at = @At("HEAD"))
    private void onMouseMove(long window, double x, double y, CallbackInfo ci) {
        if (!MacroInputPlayback.isActive()) {
            MacroInputRecorder.recordMouseMove(System.nanoTime(), x, y);
        }
    }
}