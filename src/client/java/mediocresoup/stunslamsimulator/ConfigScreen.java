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

        // Axe Slot button
        Button axeSlotButton = Button.builder(
                        Component.literal("Axe Hotbar Slot: " + (config.getAxeSlot() + 1)),
                        button -> {
                            config.cycleAxeSlot();
                            button.setMessage(Component.literal("Axe Hotbar Slot: " + (config.getAxeSlot() + 1)));
                        })
                .bounds(this.width / 2 - 100, this.height / 2 - 16, 200, 20)
                .build();

        this.addRenderableWidget(axeSlotButton);

        // Mace Slot button
        Button maceSlotButton = Button.builder(
                        Component.literal("Mace Hotbar Slot: " + (config.getMaceSlot() + 1)),
                        button -> {
                            config.cycleMaceSlot();
                            button.setMessage(Component.literal("Mace Hotbar Slot: " + (config.getMaceSlot() + 1)));
                        })
                .bounds(this.width / 2 - 100, this.height / 2 + 8, 200, 20)
                .build();

        this.addRenderableWidget(maceSlotButton);

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
