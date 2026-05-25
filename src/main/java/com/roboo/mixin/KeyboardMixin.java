package com.roboo.mixin;

import com.roboo.macro.MacroInputPlayback;
import com.roboo.macro.MacroInputRecorder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(KeyboardHandler.class)
public class KeyboardMixin {

    @Inject(method = "keyPress", at = @At("HEAD"))
    private void onKeyPress(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (!MacroInputPlayback.isActive()) {
            MacroInputRecorder.recordKey(System.nanoTime(), input.key(), action);
        }
    }
}