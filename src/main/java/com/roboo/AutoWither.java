package com.roboo;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class AutoWither implements ClientModInitializer {

	private static final Minecraft mc = Minecraft.getInstance();

	private static KeyMapping toggleKey;
	private static boolean enabled = false;

	// =========================
	// SYSTEM FLAGS
	// =========================
	private static boolean rotateToBoss = false;
	private static boolean moveForward = false;
	private static boolean moveToSafe = false;

	private static boolean holdClick = false;

	private static boolean comboAttack = false;
	private static boolean witherMagic = false;
	private static boolean demonMagic = false;

	private static boolean dodgeTriggered = false;
	private static boolean justKilled = false;

	private static long entryTime = 0;
	private static long safeTime = 0;
	private static boolean waitingToStopDodge = false;

	// =========================
	// SUMMON POSITION STATE
	// =========================
	private static final double SUMMON_X = -109;
	private static final double SUMMON_Y = 102;
	private static final double SUMMON_Z = 42;

	private static final double DEATH_SUMMON_X = -29;
	private static final double DEATH_SUMMON_Y = 107;
	private static final double DEATH_SUMMON_Z = -57;

	private static final double SUMMON_TOLERANCE = 1.0;

	private static boolean atSummonPos = false;
	private static long summonPosEntryTime = 0;
	private static int summonAttemptCount = 0;
	private static boolean summonFailed = false;

	// Positions
	private static final double FORWARD_X = -112;
	private static final double SAFE_X = -106;

	@Override
	public void onInitializeClient() {
		ConfigManager.load();

		KeyMapping.Category category = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath("autowither", "main")
		);

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"AutoWither",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_CONTROL,
				category
		));

		DodgeHelper.init();
		ParryHelper.init();
		ReconnectHelper.init();
		ContainerHelper.init();
		CrescentTowerHelper.init();

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("roboo")
						.executes(ctx -> {
							mc.execute(() -> mc.setScreen(ConfigScreen.build(mc.screen)));
							return 1;
						})
				)
		);

		ClientReceiveMessageEvents.GAME.register((message, overlay) -> handleChat(message.getString()));
		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, receptionTimestamp) ->
				handleChat(message.getString()));

		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft client) {
		if (toggleKey.consumeClick()) {
			enabled = !enabled;
			client.gui.getChat().addMessage(Component.literal(
					enabled ? "§e[AutoWither] §aEnabled" : "§e[AutoWither] §cDisabled"
			));
			resetAll();
			if (enabled) InventoryHelper.cacheSlot();
		}

		if (!enabled || mc.player == null || mc.level == null) return;

		// Rotation
		if (rotateToBoss) {
			if (System.currentTimeMillis() - entryTime < 1500) {
				if (RotationHelper.lookAt(90f, 0f)) {
					rotateToBoss = false;
					moveForward = true;
					justKilled = false;
				}
			}
		}

		// Movement forward
		if (moveForward) {
			if (MovementHelper.moveToX(FORWARD_X)) {
				moveForward = false;
				holdClick = true;
			}
		}

		// Movement to safe zone
		if (moveToSafe) {
			if (MovementHelper.moveToX(SAFE_X)) {
				moveToSafe = false;

				if (demonMagic && !dodgeTriggered && ModConfig.isAutoDodgeEnabled()) {
					DodgeHelper.setRequiredSafes(true);
					DodgeHelper.start();
					ParryHelper.trigger();
					dodgeTriggered = true;
				}
			}
		}

		InputHelper.holdRightClick(holdClick);

		// Delayed dodge stop
		if (waitingToStopDodge && System.currentTimeMillis() - safeTime > 100) {
			DodgeHelper.stop();
			dodgeTriggered = false;
			waitingToStopDodge = false;
		}

		// Combo attack dodge
		if (comboAttack && ModConfig.isAutoDodgeEnabled()) {
			holdClick = false;
			if (!dodgeTriggered) {
				DodgeHelper.setRequiredSafes(true);
				DodgeHelper.start();
				ParryHelper.trigger();
				dodgeTriggered = true;
			}
		}

		// =========================
// SUMMON POSITION LOGIC
// =========================
		if (ModConfig.isAutoSummonEnabled()) {
			double px = mc.player.getX();
			double py = mc.player.getY();
			double pz = mc.player.getZ();

			boolean nearSummonPos =
					(Math.abs(px - SUMMON_X) <= SUMMON_TOLERANCE &&
							Math.abs(py - SUMMON_Y) <= SUMMON_TOLERANCE &&
							Math.abs(pz - SUMMON_Z) <= SUMMON_TOLERANCE)
							||
							(Math.abs(px - DEATH_SUMMON_X) <= SUMMON_TOLERANCE &&
									Math.abs(py - DEATH_SUMMON_Y) <= SUMMON_TOLERANCE &&
									Math.abs(pz - DEATH_SUMMON_Z) <= SUMMON_TOLERANCE);

			if (nearSummonPos) {
				if (!atSummonPos) {
					atSummonPos = true;
					summonPosEntryTime = System.currentTimeMillis();
					summonAttemptCount = 1;
					summonFailed = false;
					InventoryHelper.useSummonItem();
				} else if (!summonFailed) {
					long elapsed = System.currentTimeMillis() - summonPosEntryTime;

					if (summonAttemptCount == 1 && elapsed >= 1000) {
						summonAttemptCount = 2;
						InventoryHelper.useSummonItem();
					} else if (summonAttemptCount == 2 && elapsed >= 5000) {
						summonAttemptCount = 3;
						InventoryHelper.useSummonItem();
					} else if (summonAttemptCount == 3 && elapsed >= 10000) {
						summonFailed = true;
						client.gui.getChat().addMessage(Component.literal(
								"§e[AutoWither] §cSummon failed — still at summon position after 15s!"
						));
					}
				}
			} else {
				atSummonPos = false;
				summonAttemptCount = 0;
				summonFailed = false;
			}
		}
	}

	public static void handleChat(String msg) {
		if (!enabled) return;

		String lower = msg.toLowerCase();

		if (lower.contains("invited you to his palace")) {
			rotateToBoss = true;
			entryTime = System.currentTimeMillis();
			resetCombatState();
		}
		else if (lower.contains("combo attack") && ModConfig.isComboAttackEnabled()) {
			comboAttack = true;
			dodgeTriggered = false;
		}
		else if (lower.contains("wither magic") && ModConfig.isWitherMagicEnabled()) {
			witherMagic = true;
			demonMagic = false;
			moveToSafe = true;
			holdClick = false;
			dodgeTriggered = false;
		}
		else if (lower.contains("demon magic") && ModConfig.isDemonMagicEnabled()) {
			demonMagic = true;
			witherMagic = false;
			moveToSafe = true;
			holdClick = false;
			dodgeTriggered = false;
		}
		else if (lower.contains("safe!!!")) {
			holdClick = true;
			moveForward = true;
			moveToSafe = false;
			witherMagic = false;
			demonMagic = false;
			comboAttack = false;
			safeTime = System.currentTimeMillis();
			waitingToStopDodge = true;
		}
		else if (lower.contains("uncommon drop! wither bone")) {
			justKilled = true;
			resetCombatState();
		}
		else if (lower.contains("you died")) {
			InputHelper.stopAll();
			DodgeHelper.stop();
			ParryHelper.reset();
			resetAllStatic();
		}
	}

	private static void resetCombatState() {
		moveForward = false;
		moveToSafe = false;
		MovementHelper.stopMovement();
		holdClick = false;
		comboAttack = false;
		witherMagic = false;
		demonMagic = false;
		dodgeTriggered = false;
		waitingToStopDodge = false;
	}

	private static void resetAllStatic() {
		rotateToBoss = false;
		resetCombatState();
		justKilled = false;
	}

	private void resetAll() {
		resetAllStatic();
		atSummonPos = false;
		summonAttemptCount = 0;
		summonFailed = false;
	}
}