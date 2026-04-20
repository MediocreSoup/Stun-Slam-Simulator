package mediocresoup.stunslamsimulator.client.mixin;

import mediocresoup.stunslamsimulator.StunSlamSimulatorClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public class KeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"))
    private void stunslam$onKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        StunSlamSimulatorClient.onRawKey(input.key(), input.scancode(), action);
    }
}