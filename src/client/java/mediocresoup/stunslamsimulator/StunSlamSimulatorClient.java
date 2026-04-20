package mediocresoup.stunslamsimulator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class StunSlamSimulatorClient implements ClientModInitializer {
    public static final String MOD_ID = "stun-slam-simulator";

    private static final Identifier HUD_LAYER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "timing_hud");

    private static TimingState STATE;

    @Override
    public void onInitializeClient() {
        STATE = new TimingState();

        // Load config on startup
        ModConfig.getInstance();

        // Register tick event (only processes when enabled)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (ModConfig.getInstance().isEnabled()) {
                STATE.tick(client);
            }
        });

        // Register HUD overlay (only renders when enabled)
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                HUD_LAYER_ID,
                (drawContext, tickCounter) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null && ModConfig.getInstance().isEnabled()) {
                        TimingHudRenderer.render(drawContext, STATE);
                    }
                }
        );

        // Register /stunslam command
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("stunslam")
                    .then(ClientCommandManager.literal("toggle")
                            .executes(context -> {
                                ModConfig config = ModConfig.getInstance();
                                config.toggle();
                                context.getSource().sendFeedback(Component.literal(
                                        "§7[Stun Slam] §fMod " + (config.isEnabled() ? "§aenabled" : "§cdisabled")
                                ));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("enable")
                            .executes(context -> {
                                ModConfig.getInstance().setEnabled(true);
                                context.getSource().sendFeedback(Component.literal(
                                        "§7[Stun Slam] §fMod §aenabled"
                                ));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("disable")
                            .executes(context -> {
                                ModConfig.getInstance().setEnabled(false);
                                context.getSource().sendFeedback(Component.literal(
                                        "§7[Stun Slam] §fMod §cdisabled"
                                ));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("reset")
                            .executes(context -> {
                                if (STATE != null) {
                                    STATE.resetStats();
                                }
                                context.getSource().sendFeedback(Component.literal(
                                        "§7[Stun Slam] §fStats reset."
                                ));
                                return 1;
                            })
                    )
                    // Default (no subcommand) shows current status
                    .executes(context -> {
                        ModConfig config = ModConfig.getInstance();
                        context.getSource().sendFeedback(Component.literal(
                                "§7[Stun Slam] §fMod is currently " + (config.isEnabled() ? "§aenabled" : "§cdisabled")
                                        + "\n§7Usage: /stunslam <toggle|enable|disable>"
                        ));
                        return 1;
                    })
            );
        });
    }

    public static void onRawMouseButton(int button, int action) {
        if (!ModConfig.getInstance().isEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (STATE != null) {
            STATE.handleRawMouseButton(client, button, action);
        }
    }

    public static void onRawKey(int keyCode, int scanCode, int action) {
        if (!ModConfig.getInstance().isEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (STATE != null) {
            STATE.handleRawKey(client, keyCode, scanCode, action);
        }
    }
}
