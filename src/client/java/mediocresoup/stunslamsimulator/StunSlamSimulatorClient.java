package mediocresoup.stunslamsimulator;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class StunSlamSimulatorClient implements ClientModInitializer {
    public static final String MOD_ID = "stun-slam-simulator";

    private static final Identifier HUD_LAYER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "timing_hud");

    private static TimingState STATE;

    @Override
    public void onInitializeClient() {
        STATE = new TimingState();

        ClientTickEvents.END_CLIENT_TICK.register(client -> STATE.tick(client));

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.MISC_OVERLAYS,
                HUD_LAYER_ID,
                (drawContext, tickCounter) -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.player != null) {
                        TimingHudRenderer.render(drawContext, STATE);
                    }
                }
        );
    }

    public static void onRawMouseButton(int button, int action) {
        Minecraft client = Minecraft.getInstance();
        if (STATE != null) {
            STATE.handleRawMouseButton(client, button, action);
        }
    }

    public static void onRawKey(int keyCode, int scanCode, int action) {
        Minecraft client = Minecraft.getInstance();
        if (STATE != null) {
            STATE.handleRawKey(client, keyCode, scanCode, action);
        }
    }
}
