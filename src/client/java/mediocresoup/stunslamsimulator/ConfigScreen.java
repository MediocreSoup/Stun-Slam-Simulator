package mediocresoup.stunslamsimulator;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.literal("Stun Slam Simulator Config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig config = ModConfig.getInstance();

        int buttonWidth = 200;
        int centerX = this.width / 2;
        int startY = this.height / 2 - 100;
        int spacing = 28;

        // Toggle button
        Button toggleButton = Button.builder(
                        Component.literal("Mod Enabled: " + (config.isEnabled() ? "ON" : "OFF")),
                        button -> {
                            config.toggle();
                            button.setMessage(Component.literal("Mod Enabled: " + (config.isEnabled() ? "ON" : "OFF")));
                        })
                .bounds(centerX - buttonWidth / 2, startY, buttonWidth, 20)
                .build();
        this.addRenderableWidget(toggleButton);

        // Show Inputs button
        Button inputsButton = Button.builder(
                        Component.literal("Show Inputs: " + (config.isShowInputs() ? "ON" : "OFF")),
                        button -> {
                            config.toggleShowInputs();
                            button.setMessage(Component.literal("Show Inputs: " + (config.isShowInputs() ? "ON" : "OFF")));
                        })
                .bounds(centerX - buttonWidth / 2, startY + spacing, buttonWidth, 20)
                .build();
        this.addRenderableWidget(inputsButton);

        // Show Frame Lines button
        Button frameLinesButton = Button.builder(
                        Component.literal("Show Frame Lines: " + (config.isShowFrameLines() ? "ON" : "OFF")),
                        button -> {
                            config.toggleShowFrameLines();
                            button.setMessage(Component.literal("Show Frame Lines: " + (config.isShowFrameLines() ? "ON" : "OFF")));
                        })
                .bounds(centerX - buttonWidth / 2, startY + spacing * 2, buttonWidth, 20)
                .build();
        this.addRenderableWidget(frameLinesButton);

        // Show Title button
        Button titleButton = Button.builder(
                        Component.literal("Show Title: " + (config.isShowTitle() ? "ON" : "OFF")),
                        button -> {
                            config.toggleShowTitle();
                            button.setMessage(Component.literal("Show Title: " + (config.isShowTitle() ? "ON" : "OFF")));
                        })
                .bounds(centerX - buttonWidth / 2, startY + spacing * 2 + 24, buttonWidth, 20)
                .build();
        this.addRenderableWidget(titleButton);

        // HUD Size buttons (now 4: TINY, SMALL, MEDIUM, LARGE)
        int buttonY = startY + spacing * 3 + 24;
        int buttonWidthSmall = 60;
        int gap = 4;

        // total width = 4 buttons + 3 gaps
        int totalWidth = buttonWidthSmall * 4 + gap * 3;
        int startX = centerX - totalWidth / 2;

        Button sizeTinyButton = Button.builder(
                        Component.literal("TINY" + (config.getHudSize().equals("TINY") ? " ✓" : "")),
                        button -> {
                            config.setHudSize("TINY");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(startX, buttonY, buttonWidthSmall, 20)
                .build();
        this.addRenderableWidget(sizeTinyButton);

        Button sizeSmallButton = Button.builder(
                        Component.literal("SMALL" + (config.getHudSize().equals("SMALL") ? " ✓" : "")),
                        button -> {
                            config.setHudSize("SMALL");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(startX + (buttonWidthSmall + gap), buttonY, buttonWidthSmall, 20)
                .build();
        this.addRenderableWidget(sizeSmallButton);

        Button sizeMediumButton = Button.builder(
                        Component.literal("MEDIUM" + (config.getHudSize().equals("MEDIUM") ? " ✓" : "")),
                        button -> {
                            config.setHudSize("MEDIUM");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(startX + (buttonWidthSmall + gap) * 2, buttonY, buttonWidthSmall, 20)
                .build();
        this.addRenderableWidget(sizeMediumButton);
        
        Button sizeLargeButton = Button.builder(
                        Component.literal("LARGE" + (config.getHudSize().equals("LARGE") ? " ✓" : "")),
                        button -> {
                            config.setHudSize("LARGE");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(startX + (buttonWidthSmall + gap) * 3, buttonY, buttonWidthSmall, 20)
                .build();
        this.addRenderableWidget(sizeLargeButton);

        // HUD Anchor buttons
        Button anchorTopLeftButton = Button.builder(
                        Component.literal("Top-Left" + (config.getHudAnchor().equals("TOP_LEFT") ? " ✓" : "")),
                        button -> {
                            config.setHudAnchor("TOP_LEFT");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(centerX - buttonWidth / 2, startY + spacing * 4 + 32, 97, 20)
                .build();
        this.addRenderableWidget(anchorTopLeftButton);

        Button anchorTopRightButton = Button.builder(
                        Component.literal("Top-Right" + (config.getHudAnchor().equals("TOP_RIGHT") ? " ✓" : "")),
                        button -> {
                            config.setHudAnchor("TOP_RIGHT");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(centerX + 3, startY + spacing * 4 + 32, 97, 20)
                .build();
        this.addRenderableWidget(anchorTopRightButton);

        Button anchorBottomLeftButton = Button.builder(
                        Component.literal("Bot-Left" + (config.getHudAnchor().equals("BOTTOM_LEFT") ? " ✓" : "")),
                        button -> {
                            config.setHudAnchor("BOTTOM_LEFT");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(centerX - buttonWidth / 2, startY + spacing * 5 + 32, 97, 20)
                .build();
        this.addRenderableWidget(anchorBottomLeftButton);

        Button anchorBottomRightButton = Button.builder(
                        Component.literal("Bot-Right" + (config.getHudAnchor().equals("BOTTOM_RIGHT") ? " ✓" : "")),
                        button -> {
                            config.setHudAnchor("BOTTOM_RIGHT");
                            this.clearWidgets();
                            this.init();
                        })
                .bounds(centerX + 3, startY + spacing * 5 + 32, 97, 20)
                .build();
        this.addRenderableWidget(anchorBottomRightButton);

        // Done button
        Button doneButton = Button.builder(
                        Component.literal("Done"),
                        button -> onClose())
                .bounds(centerX - buttonWidth / 2, startY + spacing * 6 + 40, buttonWidth, 20)
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
