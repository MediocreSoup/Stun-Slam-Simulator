package mediocresoup.stunslamsimulator;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;
import java.util.Locale;

public final class TimingHudRenderer {
    private static final int DOT_RADIUS = 4;
    private static final int PLOT_MARGIN = 14;
    private static final double MIN_HALF_SPAN_MS = 100.0;
    private static final double LABEL_PADDING_X = 6.0;

    private TimingHudRenderer() {}

    public static void render(GuiGraphics ctx, TimingState state) {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) {
            return;
        }

        int x = 10;
        int y = 10;
        int w = 360;
        int h = 184;

        ctx.fill(x, y, x + w, y + h, 0xB0181818);
        ctx.fill(x, y, x + w, y + 1, 0xFFFFFFFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0xFFFFFFFF);
        ctx.fill(x, y, x + 1, y + h, 0xFFFFFFFF);
        ctx.fill(x + w - 1, y, x + w, y + h, 0xFFFFFFFF);

        ctx.drawString(client.font, "Stun Slam Tester", x + 8, y + 8, 0xFFFFFF, true);
        ctx.drawString(client.font, chanceLine(state), x + 8, y + 22, 0xD0D0D0, true);
        ctx.drawString(client.font, "Status: " + state.getStatus(), x + 8, y + 36, 0xB8B8B8, true);
        ctx.drawString(client.font, "Inputs: " + state.getDisplayedInputsSummary(), x + 8, y + 50, 0xA0A0A0, true);

        int graphX = x + 8;
        int graphY = y + 72;
        int graphW = w - 16;
        int graphH = h - 80;

        ctx.fill(graphX, graphY, graphX + graphW, graphY + graphH, 0xFF0F0F0F);

        int rowAttack = graphY + 14;
        int rowAxe = graphY + graphH / 2;
        int rowMace = graphY + graphH - 14;

        ctx.drawString(client.font, "atk", graphX + 4, rowAttack - 4, 0xE0E0E0, true);
        ctx.drawString(client.font, "axe", graphX + 4, rowAxe - 4, 0xE0E0E0, true);
        ctx.drawString(client.font, "mace", graphX + 4, rowMace - 4, 0xE0E0E0, true);

        List<InputEvent> events = state.eventsForRender();
        if (events.isEmpty()) {
            ctx.drawString(client.font, "No inputs recorded yet.", graphX + 8, graphY + graphH / 2 - 4, 0x808080, true);
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
        double halfSpanMs = Math.max(
                MIN_HALF_SPAN_MS,
                Math.max(centerMs - minMs, maxMs - centerMs) + 25.0
        );
        double startMs = Math.max(0.0, centerMs - halfSpanMs);
        double endMs = centerMs + halfSpanMs;
        if (endMs - startMs < MIN_HALF_SPAN_MS * 2.0) {
            endMs = startMs + (MIN_HALF_SPAN_MS * 2.0);
        }
        double spanMs = Math.max(1.0, endMs - startMs);

        int plotX = graphX + PLOT_MARGIN;
        int plotW = graphW - (PLOT_MARGIN * 2);

        double tickStart = Math.floor(startMs / 50.0) * 50.0;
        for (double tick = tickStart; tick <= endMs + 0.001; tick += 50.0) {
            int px = plotX + (int) Math.round(((tick - startMs) / spanMs) * plotW);
            ctx.fill(px, graphY, px + 1, graphY + graphH, 0x33FFFFFF);
        }

        double majorTickStart = Math.floor(startMs / 100.0) * 100.0;
        for (double tick = majorTickStart; tick <= endMs + 0.001; tick += 100.0) {
            int px = plotX + (int) Math.round(((tick - startMs) / spanMs) * plotW);
            ctx.fill(px, graphY, px + 1, graphY + graphH, 0x66FFFFFF);
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
            ctx.drawString(client.font, label, labelX, labelY, 0xFFFFFF, false);
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
