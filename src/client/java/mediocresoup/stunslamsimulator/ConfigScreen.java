package mediocresoup.stunslamsimulator;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Stun Slam Simulator Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig config = ModConfig.getInstance();

        // Toggle button
        Button toggleButton = Button.builder(
                        Component.literal("Mod Enabled: " + (config.isEnabled() ? "ON" : "OFF")),
                        button -> {
                            config.toggle();
                            button.setMessage(Component.literal("Mod Enabled: " + (config.isEnabled() ? "ON" : "OFF")));
                        })
                .bounds(this.width / 2 - 100, this.height / 2 - 40, 200, 20)
                .build();
        this.addRenderableWidget(toggleButton);

        // Show Inputs button
        Button inputsButton = Button.builder(
                        Component.literal("Show Inputs: " + (config.isShowInputs() ? "ON" : "OFF")),
                        button -> {
                            config.toggleShowInputs();
                            button.setMessage(Component.literal("Show Inputs: " + (config.isShowInputs() ? "ON" : "OFF")));
                        })
                .bounds(this.width / 2 - 100, this.height / 2 - 16, 200, 20)
                .build();
        this.addRenderableWidget(inputsButton);

        // Show Frame Lines button
        Button frameLinesButton = Button.builder(
                        Component.literal("Show Frame Lines: " + (config.isShowFrameLines() ? "ON" : "OFF")),
                        button -> {
                            config.toggleShowFrameLines();
                            button.setMessage(Component.literal("Show Frame Lines: " + (config.isShowFrameLines() ? "ON" : "OFF")));
                        })
                .bounds(this.width / 2 - 100, this.height / 2 + 8, 200, 20)
                .build();
        this.addRenderableWidget(frameLinesButton);

        // Done button
        Button doneButton = Button.builder(
                        Component.literal("Done"),
                        button -> onClose())
                .bounds(this.width / 2 - 100, this.height / 2 + 32, 200, 20)
                .build();

        this.addRenderableWidget(doneButton);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }
}
