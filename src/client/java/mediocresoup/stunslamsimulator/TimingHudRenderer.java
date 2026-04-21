package mediocresoup.stunslamsimulator;

import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class TimingHudRenderer {
    // Size-dependent constants
    private static class SizeConfig {
        final int dotRadius;
        final int plotMargin;
        final int baseWidth;
        final int padding;
        final int lineSpacing;
        final int graphHeight;
        final double labelPadding;

        SizeConfig(int dotRadius, int plotMargin, int baseWidth, int padding, int lineSpacing, int graphHeight, double labelPadding) {
            this.dotRadius = dotRadius;
            this.plotMargin = plotMargin;
            this.baseWidth = baseWidth;
            this.padding = padding;
            this.lineSpacing = lineSpacing;
            this.graphHeight = graphHeight;
            this.labelPadding = labelPadding;
        }
    }

    private static final double MIN_HALF_SPAN_MS = 120.0;
    private static final double MAX_HALF_SPAN_MS = 250.0;
    private static final int GRAPH_TOP_MARGIN = 2;

    // Size presets - SMALL is now the tiny baseline
    private static final SizeConfig TINY = new SizeConfig(1, 4, 130, 3, 9, 30, 2.0);
    private static final SizeConfig SMALL = new SizeConfig(1, 6, 180, 4, 9, 50, 2.5);
    private static final SizeConfig MEDIUM = new SizeConfig(3, 14, 230, 8, 10, 70, 5.0);
    private static final SizeConfig LARGE = new SizeConfig(4, 16, 280, 10, 12, 90, 6.0);

    private TimingHudRenderer() {}

    private static SizeConfig getSizeConfig(String hudSize) {
        return switch (hudSize) {
            case "TINY" -> TINY;
            case "MEDIUM" -> MEDIUM;
            case "LARGE" -> LARGE;
            default -> SMALL;
        };
    }

    public static void render(GuiGraphics ctx, TimingState state) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.font == null) {
            return;
        }

        String hudSize = config.getHudSize();
        SizeConfig baseSize = getSizeConfig(hudSize);
        double scale = 1; //config.getHudScale(); // is it fully deprecated?

        // Apply scale multiplier to dimensions
        int dotRadius = Math.max(1, (int)(baseSize.dotRadius * scale));
        int plotMargin = Math.max(6, (int)(baseSize.plotMargin * scale));
        int baseWidth = (int)(baseSize.baseWidth * scale);
        int padding = Math.max(4, (int)(baseSize.padding * scale));
        int lineSpacing = Math.max(7, (int)(baseSize.lineSpacing * scale));
        int graphHeight = Math.max(30, (int)(baseSize.graphHeight * scale));

        // Calculate position based on anchor
        int x, y;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int headerLines = 1; // Always have the chance line
        if (config.isShowTitle()) {
            headerLines++;
        }
        if (config.isShowInputs()) {
            headerLines++;
        }

        int textHeight = headerLines * lineSpacing;
        int hudHeight = padding + textHeight + GRAPH_TOP_MARGIN + graphHeight + padding;

        switch (config.getHudAnchor()) {
            case "TOP_RIGHT":
                x = screenWidth - baseWidth - 10;
                y = 10;
                break;
            case "BOTTOM_LEFT":
                x = 10;
                y = screenHeight - hudHeight - 10;
                break;
            case "BOTTOM_RIGHT":
                x = screenWidth - baseWidth - 10;
                y = screenHeight - hudHeight - 10;
                break;
            case "TOP_LEFT":
            default:
                x = 10;
                y = 10;
                break;
        }

        int w = baseWidth;
        int graphY = y + padding + textHeight + GRAPH_TOP_MARGIN;
        int h = (graphY + graphHeight + padding) - y;

        // Always record frame timing at the start of HUD render
        state.recordFrame();

        ctx.fill(x, y, x + w, y + h, 0xB0181818);
        ctx.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);

        // Render text section
        int currentTextY = y + padding;
        
        // Render title if enabled
        if (config.isShowTitle()) {
            drawScaledString(ctx, client.font, "Stun Slam Simulator", x + padding, currentTextY, 0xFFFFFFFF, scale);
            currentTextY += lineSpacing;
        }
        
        // Draw the chance line
        String chanceTxt = chanceLine(state, hudSize);

        drawScaledString(ctx, client.font, chanceTxt, x + padding, currentTextY, 0xFFD0D0D0, scale);
        currentTextY += lineSpacing;

        if (config.isShowInputs()) {
            String inputTxt = state.getDisplayedInputsSummary();
            if (inputTxt.length() > 30 && w < 180) {
                // Abbreviate inputs on very small HUDs
                inputTxt = inputTxt.substring(0, Math.min(30, inputTxt.length())) + "...";
            }
            drawScaledString(ctx, client.font, "Inputs: " + inputTxt, x + padding, currentTextY, 0xFFA0A0A0, scale);
        }

        int graphX = x + padding;
        int graphW = w - (padding * 2);
        int graphH = graphHeight;

        ctx.fill(graphX, graphY, graphX + graphW, graphY + graphH, 0xFF0F0F0F);

        int rowAttack = graphY + graphHeight / 6;
        int rowAxe = graphY + graphHeight / 2;
        int rowMace = graphY + graphHeight - graphHeight / 6;

        List<InputEvent> events = state.eventsForRender();
        if (events.isEmpty()) {
            ctx.drawString(client.font, "No inputs recorded yet.", graphX + 8, graphY + graphH / 2 - 4, 0xFF808080, true);
            return;
        }

        // Robust centering: find the middle of the actual input events
        double minE = events.stream().mapToDouble(InputEvent::timeMs).min().orElse(0.0);
        double maxE = events.stream().mapToDouble(InputEvent::timeMs).max().orElse(0.0);
        double centerMs = (minE + maxE) / 2.0;

        // Calculate dynamic horizontal zoom based on input spread
        double spread = maxE - minE;
        double halfSpanMs = Math.max(MIN_HALF_SPAN_MS, Math.min(MAX_HALF_SPAN_MS, (spread * 0.75) + 40.0));
        double startMs = centerMs - halfSpanMs;
        double endMs = centerMs + halfSpanMs;
        double spanMs = Math.max(1.0, endMs - startMs);

        int plotX = graphX + plotMargin;
        int plotW = graphW - (plotMargin * 2);

        // 1. Render Frame Lines (half opacity of tick lines)
        if (config.isShowFrameLines()) {
            List<Double> frameOffsets = state.frameOffsetsForRender();
            for (Double fOffset : frameOffsets) {
                if (fOffset >= startMs && fOffset <= endMs) {
                    int px = plotX + (int) (((fOffset - startMs) / spanMs) * plotW);
                    ctx.fill(px, graphY + graphH - 10, px + 1, graphY + graphH, 0x33FFFFFF);
                }
            }
        }

        // 2. Render Actual Game Tick Lines
        List<Double> ticks = state.tickOffsetsForRender();
        for (Double tickOffset : ticks) {
            if (tickOffset < startMs || tickOffset > endMs) continue;
            int px = plotX + (int) Math.round(((tickOffset - startMs) / spanMs) * plotW);
            ctx.fill(px, graphY, px + 1, graphY + graphH, 0xCCFFFFFF);
        }

        // 3. Render Input Dots (with thinner outline for small dots)
        for (InputEvent event : events) {
            int px = plotX + (int) Math.round(((event.timeMs() - startMs) / spanMs) * plotW);
            int py = switch (event.type()) {
                case ATTACK -> rowAttack;
                case AXE -> rowAxe;
                case MACE -> rowMace;
            };

            // Draw outline - only if dot is large enough to see it
            if (dotRadius > 1) {
                ctx.fill(px - dotRadius, py - dotRadius, px + dotRadius + 1, py + dotRadius + 1, 0xFFE7E7E7);
                ctx.fill(px - dotRadius + 1, py - dotRadius + 1, px + dotRadius, py + dotRadius, colorFor(event.type()));
            } else {
                // For tiny dots, just draw the colored dot without outline
                ctx.fill(px - dotRadius, py - dotRadius, px + dotRadius + 1, py + dotRadius + 1, colorFor(event.type()));
            }

            String label = shortLabel(event.type());
            int labelX = px + dotRadius + (int)(2.5 * scale);
            int labelY = py - 4;

            ctx.drawString(client.font, label, labelX, labelY, 0xFFFFFFFF, false);
        }
    }

    private static void drawScaledString(GuiGraphics ctx, net.minecraft.client.gui.Font font, String text, int x, int y, int color, double scale) {
        // For small scales, just draw normally - the text will appear smaller due to the HUD size being smaller
        // We avoid using matrix transforms which are complex in this API version
        ctx.drawString(font, text, x, y, color, true);
    }

    // Size is either TINY, SMALL, MEDIUM, or LARGE - the chance line adapts to fit
    private static String chanceLine(TimingState state, String size) {

        boolean live = state.hasLiveAttempt();
        String avg = pct(state.getAverageChance());
        String chance = state.getAttemptCount() == 0 ? "--" : pct(state.getLastChance());
        int attempts = state.getAttemptCount();

        switch (size) {
            case "TINY":
                // ~130px → extremely compact
                if (live) {
                    return "Pending (" + state.getLiveEventCount() + "/4)  " + avg;
                } else {
                    return chance + "  Avg: " + avg;
                }

            case "SMALL":
                // ~180px → slightly more info, still compact
                if (live) {
                    return "Pending (" + state.getLiveEventCount() + "/4)"
                            + "  Avg: " + avg
                            + "  #" + attempts;
                } else {
                    return chance
                            + "  Avg: " + avg
                            + "  #" + attempts;
                }

            case "MEDIUM":
                // ~230px → readable labels, shortened title
                if (live) {
                    return "Chance: pending (" + state.getLiveEventCount() + "/4)"
                            + "  Avg: " + avg
                            + "  Attempts: " + attempts;
                } else {
                    return "Chance: " + chance
                            + "  Avg: " + avg
                            + "  Attempts: " + attempts;
                }

            case "LARGE":
            default:
                // ~280px → full verbose version (your original)
                if (live) {
                    return "Stun slam chance: pending (" + state.getLiveEventCount() + "/4)"
                            + "   Avg: " + avg
                            + "   Attempts: " + attempts;
                } else {
                    return "Stun slam chance: " + chance
                            + "   Avg: " + avg
                            + "   Attempts: " + attempts;
                }
        }
    }

    private static String pct(double value) {
        return String.format(Locale.ROOT, "%.2f%%", value * 100.0);
    }

    private static String shortLabel(ActionType type) {
        return switch (type) {
            case ATTACK -> "atk";
            case AXE -> "axe";
            case MACE -> "mace";
        };
    }

    private static int colorFor(ActionType type) {
        return switch (type) {
            case ATTACK -> 0xFFD85A5A;
            case AXE -> 0xFF68A8FF;
            case MACE -> 0xFFF0B35A;
        };
    }
}
