package com.roboo;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.roboo.macro.MacroManager;
import com.roboo.macro.MacroInputRecorder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class Commands {

    private static final Minecraft mc = Minecraft.getInstance();

    public static void init() {

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("mbag")

                        .then(ClientCommandManager.literal("toggle")
                                .executes(ctx -> {
                                    AutoStoreHelper.toggle();
                                    return 1;
                                })
                        )

                        .then(ClientCommandManager.literal("timer")
                                .then(ClientCommandManager.argument("seconds", IntegerArgumentType.integer(5, 6000))
                                        .executes(ctx -> {
                                            int secs = IntegerArgumentType.getInteger(ctx, "seconds");
                                            ModConfig.setAutoStoreDelay(secs);
                                            ConfigManager.save();
                                            storeMsg("§fTimer set to §e" + secs + "s");
                                            return 1;
                                        })
                                )
                        )

                        .then(ClientCommandManager.literal("emptyinv")
                                .executes(ctx -> {
                                    if (EmptyInvHelper.isRunning()) {
                                        EmptyInvHelper.stop();
                                        EmptyInvHelper.msg("§cStopped.");
                                    } else {
                                        EmptyInvHelper.start();
                                    }
                                    return 1;
                                })
                        )
                )
        );

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("roboo")

                        .then(ClientCommandManager.literal("record")

                                .then(ClientCommandManager.literal("start")
                                        .executes(ctx -> {
                                            if (MacroManager.isRecording()) {
                                                macroMsg("§cAlready recording.");
                                                return 1;
                                            }
                                            MacroInputRecorder.startRecording();
                                            macroMsg("§aRecording started.");
                                            return 1;
                                        })
                                )

                                .then(ClientCommandManager.literal("stop")
                                        .executes(ctx -> {
                                            if (!MacroManager.isRecording()) {
                                                macroMsg("§cNot currently recording.");
                                                return 1;
                                            }
                                            MacroInputRecorder.stopRecording();
                                            int count = MacroInputRecorder.getEvents().size();
                                            macroMsg("§aStopped. §f" + count + " events captured. Use §e/roboo record save <name> §fto save.");
                                            return 1;
                                        })
                                )

                                .then(ClientCommandManager.literal("save")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                                    if (name.isEmpty()) {
                                                        macroMsg("§cName cannot be empty.");
                                                        return 1;
                                                    }
                                                    if (MacroManager.saveMacro(name)) {
                                                        macroMsg("§aSaved macro: §f" + name);
                                                    } else {
                                                        macroMsg("§cFailed to save. Make sure you have recorded something first.");
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(ClientCommandManager.literal("playback")

                                .then(ClientCommandManager.literal("start")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                                    if (MacroManager.isPlaying()) {
                                                        macroMsg("§cAlready playing a macro. Stop it first.");
                                                        return 1;
                                                    }
                                                    if (!MacroManager.loadMacro(name)) {
                                                        macroMsg("§cMacro not found: §f" + name);
                                                        return 1;
                                                    }
                                                    MacroManager.startPlayback();
                                                    macroMsg("§aPlaying: §f" + name);
                                                    return 1;
                                                })
                                        )
                                )

                                .then(ClientCommandManager.literal("stop")
                                        .executes(ctx -> {
                                            if (!MacroManager.isPlaying()) {
                                                macroMsg("§cNothing is playing.");
                                                return 1;
                                            }
                                            MacroManager.stopPlayback();
                                            macroMsg("§cPlayback stopped.");
                                            return 1;
                                        })
                                )

                                .then(ClientCommandManager.literal("list")
                                        .executes(ctx -> {
                                            var macros = MacroManager.listMacros();
                                            if (macros.isEmpty()) {
                                                macroMsg("§fNo saved macros found.");
                                            } else {
                                                macroMsg("§fSaved macros: §e" + String.join("§f, §e", macros));
                                            }
                                            return 1;
                                        })
                                )

                                .then(ClientCommandManager.literal("delete")
                                        .then(ClientCommandManager.argument("name", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    String name = StringArgumentType.getString(ctx, "name").trim();
                                                    if (MacroManager.deleteMacro(name)) {
                                                        macroMsg("§aDeleted: §f" + name);
                                                    } else {
                                                        macroMsg("§cFailed to delete: §f" + name);
                                                    }
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
        );
    }

    private static void storeMsg(String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal("§e[AutoStore] " + text), false);
    }

    static void macroMsg(String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal("§e[Macro] " + text), false);
    }
}