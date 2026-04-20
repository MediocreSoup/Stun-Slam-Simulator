package mediocresoup.stunslamsimulator.client.mixin;

import mediocresoup.stunslamsimulator.StunSlamSimulatorClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void stunslam$onMouseButton(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        StunSlamSimulatorClient.onRawMouseButton(input.button(), action);
    }
}