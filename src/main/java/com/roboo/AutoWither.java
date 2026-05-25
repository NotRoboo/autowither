package com.roboo;

import com.mojang.blaze3d.platform.InputConstants;
import com.roboo.macro.MacroInputRecorder;
import com.roboo.macro.MacroManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoWither implements ClientModInitializer {

	private static final Minecraft mc = Minecraft.getInstance();

	private static KeyMapping toggleKey;
	private static KeyMapping recordStartKey;
	private static KeyMapping recordStopKey;

	private static boolean enabled = false;

	public static boolean isEnabled() { return enabled; }

	@Override
	public void onInitializeClient() {
		ConfigManager.load();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("slayersimaddons", "main")
		);

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"AutoWither",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_CONTROL,
				category
		));

		recordStartKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.slayersimaddons.record_start",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_KP_7,
				category
		));

		recordStopKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.slayersimaddons.record_stop",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_KP_8,
				category
		));

		DodgeHelper.init();
		ParryHelper.init();
		InventoryHelper.init();
		ReconnectHelper.init();
		BlazeHelper.init();
		ContainerHelper.init();
		CrescentTowerHelper.init();
		VampireHelper.init();
		PriestHelper.init();
		ElfHelper.init();
		EchoAngelHelper.init();
		EmptyInvHelper.init();
		IceDragHelper.init();
		WitherBossHelper.init();
		DragonBossHelper.init();
		DarkAuctionHelper.init();
		AutoStoreHelper.init();
		MacroManager.init();
		Commands.init();
		HudHelper.init();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("ss")
						.executes(ctx -> {
							mc.execute(() -> mc.setScreen(ConfigScreen.build(mc.screen)));
							return 1;
						})
				)
		);

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft client) {
		if (toggleKey.consumeClick()) {
			enabled = !enabled;
			client.gui.getChat().addMessage(Component.literal(
					enabled ? "§e[AutoSlayer] §aEnabled" : "§e[AutoSlayer] §cDisabled"
			));

			if (!enabled) {
				WitherBossHelper.fullReset();
				DragonBossHelper.fullReset();
				BlazeHelper.reset();
				CrescentTowerHelper.reset();
				VampireHelper.reset();
				PriestHelper.reset();
				ElfHelper.reset();
				EchoAngelHelper.reset();
				IceDragHelper.reset();
				InputHelper.stopAll();
				DodgeHelper.stop();
				ParryHelper.reset();
				OptionsHelper.restoreAll();
			} else {
				InventoryHelper.cacheSlot();
			}
		}

		if (recordStartKey.consumeClick()) {
			if (MacroManager.isRecording()) {
				client.gui.getChat().addMessage(Component.literal("§e[Macro] §cAlready recording."));
			} else {
				MacroInputRecorder.startRecording();
				client.gui.getChat().addMessage(Component.literal("§e[Macro] §aRecording started. (Numpad 8 to stop)"));
			}
		}

		if (recordStopKey.consumeClick()) {
			if (!MacroManager.isRecording()) {
				client.gui.getChat().addMessage(Component.literal("§e[Macro] §cNot recording."));
			} else {
				MacroInputRecorder.stopRecording();
				int count = MacroInputRecorder.getEvents().size();
				client.gui.getChat().addMessage(Component.literal(
						"§e[Macro] §aStopped. §f" + count + " events. Use §e/roboo record save <name> §fto save."
				));
			}
		}

		MacroManager.updatePlayback();
	}
}