package mediocresoup.stunslamsimulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

public final class TimingHudRenderer {
    private static final int DOT_RADIUS = 3;
    private static final int PLOT_MARGIN = 14;
    private static final double MIN_HALF_SPAN_MS = 60.0;
    private static final double MAX_HALF_SPAN_MS = 250.0;
    private static final double LABEL_PADDING_X = 5.0;
    
    private static final int HUD_X = 10;
    private static final int HUD_Y = 10;
    private static final int HUD_WIDTH = 280;
    private static final int PADDING = 8;
    private static final int LINE_SPACING = 10;
    private static final int GRAPH_TOP_MARGIN = 6;
    private static final int GRAPH_HEIGHT = 70;

    private static final List<Double> frameOffsets = new ArrayList<>();

    private TimingHudRenderer() {}

    private static long lastFrameTime = -1;

    public static void render(GuiGraphics ctx, TimingState state) {
        ModConfig config = ModConfig.getInstance();
        if (!config.isEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.font == null) {
            return;
        }

        // Capture frame timing
        long now = System.nanoTime();
        if (state.hasLiveAttempt()) {
            frameOffsets.add((now - state.getAttemptStartNs()) / 1_000_000.0);
        } else {
            frameOffsets.clear();
        }

        // Calculate dynamic dimensions for automatic scaling
        int x = HUD_X;
        int y = HUD_Y;
        int w = HUD_WIDTH;
        
        int headerLines = 2; // Always show Title and Chance
        if (config.isShowInputs()) {
            headerLines++;
        }
        
        int textHeight = headerLines * LINE_SPACING;
        int graphY = y + PADDING + textHeight + GRAPH_TOP_MARGIN;
        int h = (graphY + GRAPH_HEIGHT + PADDING) - y;

        ctx.fill(x, y, x + w, y + h, 0xB0181818);
        ctx.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);

        // Render text section with dynamic Y offsets
        int currentTextY = y + PADDING;
        ctx.drawString(client.font, "Stun Slam Tester", x + PADDING, currentTextY, 0xFFFFFFFF, true);
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

        double minMs = Double.POSITIVE_INFINITY;
        double maxMs = Double.NEGATIVE_INFINITY;
        double totalMs = 0.0;

        for (InputEvent event : events) {
            minMs = Math.min(minMs, event.timeMs());
            maxMs = Math.max(maxMs, event.timeMs());
            totalMs += event.timeMs();
        }

        double centerMs = totalMs / events.size();
        // Dynamic scale: tighter span if inputs are close together
        double halfSpanMs = Math.max(
                MIN_HALF_SPAN_MS,
                Math.min(MAX_HALF_SPAN_MS, Math.max(centerMs - minMs, maxMs - centerMs) * 1.5 + 20.0)
        );
        
        double startMs = Math.max(0.0, centerMs - halfSpanMs);
        double endMs = centerMs + halfSpanMs;
        double spanMs = Math.max(1.0, endMs - startMs);

        int plotX = graphX + PLOT_MARGIN;
        int plotW = graphW - (PLOT_MARGIN * 2);

        // Render frame lines (very faint)
        if (config.isShowInputs()) {
            for (Double fOffset : frameOffsets) {
                if (fOffset >= startMs && fOffset <= endMs) {
                    int px = plotX + (int) Math.round(((fOffset - startMs) / spanMs) * plotW);
                    ctx.fill(px, graphY + graphH - 10, px + 1, graphY + graphH, 0x11FFFFFF);
                }
            }
        }

        // Render actual game tick lines
        List<Double> ticks = state.tickOffsetsForRender();
        for (Double tickOffset : ticks) {
            if (tickOffset < startMs || tickOffset > endMs) continue;
            int px = plotX + (int) Math.round(((tickOffset - startMs) / spanMs) * plotW);
            ctx.fill(px, graphY, px + 1, graphY + graphH, 0x44FFFFFF);
        }

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
