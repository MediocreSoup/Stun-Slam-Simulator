package mediocresoup.stunslamsimulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Locale;

public final class TimingHudRenderer {
    private static final int DOT_RADIUS = 3;
    private static final int PLOT_MARGIN = 14;
    private static final double MIN_HALF_SPAN_MS = 120.0;
    private static final double MAX_HALF_SPAN_MS = 250.0;
    private static final double LABEL_PADDING_X = 5.0;
    
    private static final int HUD_X = 10;
    private static final int HUD_Y = 10;
    private static final int HUD_WIDTH = 280;
    private static final int PADDING = 8;
    private static final int LINE_SPACING = 10;
    private static final int GRAPH_TOP_MARGIN = 6;
    private static final int GRAPH_HEIGHT = 70;

    private TimingHudRenderer() {}

    public static void render(GuiGraphics ctx, TimingState state) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.font == null) {
            return;
        }

        // Calculate dynamic dimensions for automatic scaling
        int x = HUD_X;
        int y = HUD_Y;
        int w = HUD_WIDTH;
        
        int headerLines = 2; // Always show Title and Chance
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

        // Render text section with dynamic Y offsets
        int currentTextY = y + PADDING;
        ctx.drawString(client.font, "Stun Slam Simulator", x + PADDING, currentTextY, 0xFFFFFFFF, true);
        currentTextY += LINE_SPACING;
        ctx.drawString(client.font, chanceLine(state), x + PADDING, currentTextY, 0xFFD0D0D0, true);
        currentTextY += LINE_SPACING;
        
        if (config.isShowInputs()) {
            ctx.drawString(client.font, "Inputs: " + state.getDisplayedInputsSummary(), x + PADDING, currentTextY, 0xFFA0A0A0, true);
        }

        int graphX = x + PADDING;
        int graphW = w - (PADDING * 2);
        int graphH = GRAPH_HEIGHT;

        ctx.fill(graphX, graphY, graphX + graphW, graphY + graphH, 0xFF0F0F0F);

        int rowAttack = graphY + 12;
        int rowAxe = graphY + graphH / 2;
        int rowMace = graphY + graphH - 12;

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

        int plotX = graphX + PLOT_MARGIN;
        int plotW = graphW - (PLOT_MARGIN * 2);

        // 1. Render Frame Lines
        if (config.isShowFrameLines()) {
            List<Double> frameOffsets = state.frameOffsetsForRender();
            for (Double fOffset : frameOffsets) {
                if (fOffset >= startMs && fOffset <= endMs) {
                    int px = plotX + (int) (((fOffset - startMs) / spanMs) * plotW);
                    ctx.fill(px, graphY + graphH - 10, px + 1, graphY + graphH, 0x22FFFFFF);
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

        // 3. Render Input Dots
        for (InputEvent event : events) {
            int px = plotX + (int) Math.round(((event.timeMs() - startMs) / spanMs) * plotW);
            int py = switch (event.type()) {
                case ATTACK -> rowAttack;
                case AXE -> rowAxe;
                case MACE -> rowMace;
            };

            ctx.fill(px - DOT_RADIUS, py - DOT_RADIUS, px + DOT_RADIUS + 1, py + DOT_RADIUS + 1, 0xFFE7E7E7);
            ctx.fill(px - DOT_RADIUS + 1, py - DOT_RADIUS + 1, px + DOT_RADIUS, py + DOT_RADIUS, colorFor(event.type()));

            String label = shortLabel(event.type());
            int labelX = px + DOT_RADIUS + (int) LABEL_PADDING_X;
            int labelY = py - 4;
            
            ctx.drawString(client.font, label, labelX, labelY, 0xFFFFFFFF, false);
        }
    }

    private static String chanceLine(TimingState state) {
        if (state.hasLiveAttempt()) {
            return "Stun slam chance: pending (" + state.getLiveEventCount() + "/4)"
                    + "   Avg: " + pct(state.getAverageChance())
                    + "   Attempts: " + state.getAttemptCount();
        }

        String chance = state.getAttemptCount() == 0 ? "--" : pct(state.getLastChance());
        return "Stun slam chance: " + chance
                + "   Avg: " + pct(state.getAverageChance())
                + "   Attempts: " + state.getAttemptCount();
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
