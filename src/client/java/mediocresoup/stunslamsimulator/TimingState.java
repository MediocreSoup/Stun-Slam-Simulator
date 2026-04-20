package mediocresoup.stunslamsimulator;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TimingState {
    private static final double TICK_MS = 50.0;
    private static final double PHASE_STEP_MS = 0.05;
    private static final long INACTIVITY_TIMEOUT_NS = 3L * 50_000_000L;

    private final List<InputEvent> liveEvents = new ArrayList<>();
    private List<InputEvent> lastEvents = List.of();
    private final List<Double> liveTickOffsets = new ArrayList<>();
    private List<Double> lastTickOffsets = List.of();

    private long attemptStartNs = -1L;
    private long lastEventNs = -1L;

    private boolean wasOnGround = true;

    private int attemptCount = 0;
    private double cumulativeChance = 0.0;
    private double lastChance = 0.0;
    private String status = "Waiting for jump";

    public synchronized void handleRawMouseButton(Minecraft client, int button, int action) {
        if (client.player == null || action != InputConstants.PRESS) {
            return;
        }

        long now = System.nanoTime();
        MouseButtonEvent mouseEvent = new MouseButtonEvent(0.0, 0.0, new MouseButtonInfo(button, 0));

        recordIfMatch(client.options.keyAttack.matchesMouse(mouseEvent), ActionType.ATTACK, now);
        recordIfMatch(client.options.keyHotbarSlots[1].matchesMouse(mouseEvent), ActionType.AXE, now);
        recordIfMatch(client.options.keyHotbarSlots[3].matchesMouse(mouseEvent), ActionType.MACE, now);
    }

    public synchronized void handleRawKey(Minecraft client, int keyCode, int scanCode, int action) {
        if (client.player == null || action != InputConstants.PRESS) {
            return;
        }

        long now = System.nanoTime();
        KeyEvent keyEvent = new KeyEvent(keyCode, scanCode, 0);

        recordIfMatch(client.options.keyAttack.matches(keyEvent), ActionType.ATTACK, now);
        recordIfMatch(client.options.keyHotbarSlots[1].matches(keyEvent), ActionType.AXE, now);
        recordIfMatch(client.options.keyHotbarSlots[3].matches(keyEvent), ActionType.MACE, now);
    }

    public synchronized void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        if (attemptStartNs > 0) {
            double relMs = (System.nanoTime() - attemptStartNs) / 1_000_000.0;
            liveTickOffsets.add(relMs);
        }

        boolean onGround = client.player.onGround();

        if (wasOnGround && !onGround) {
            resetCurrentAttempt();
            status = "Airborne - waiting for inputs";
        }

        if (!wasOnGround && onGround) {
            if (!liveEvents.isEmpty()) {
                finishAttempt(true);
            } else {
                resetCurrentAttempt();
            }
        }

        wasOnGround = onGround;

        long now = System.nanoTime();

        if (!liveEvents.isEmpty() && liveEvents.size() >= 4) {
            finishAttempt(false);
            return;
        }

        if (!liveEvents.isEmpty() && lastEventNs > 0 && now - lastEventNs >= INACTIVITY_TIMEOUT_NS) {
            finishAttempt(true);
        }
    }

    public synchronized void resetCurrentAttempt() {
        liveEvents.clear();
        liveTickOffsets.clear();
        attemptStartNs = -1L;
        lastEventNs = -1L;
        status = "Waiting for jump";
    }

    public synchronized void resetStats() {
        this.attemptCount = 0;
        this.cumulativeChance = 0.0;
        this.lastChance = 0.0;
        this.lastEvents = List.of();
        this.lastTickOffsets = List.of();
        resetCurrentAttempt();
    }

    public synchronized List<InputEvent> eventsForRender() {
        return liveEvents.isEmpty() ? lastEvents : List.copyOf(liveEvents);
    }

    public synchronized List<Double> tickOffsetsForRender() {
        return liveEvents.isEmpty() ? lastTickOffsets : List.copyOf(liveTickOffsets);
    }

    public synchronized int getAttemptCount() {
        return attemptCount;
    }

    public synchronized double getLastChance() {
        return lastChance;
    }

    public synchronized double getAverageChance() {
        return attemptCount == 0 ? 0.0 : cumulativeChance / attemptCount;
    }

    public synchronized String getStatus() {
        return status;
    }

    public synchronized boolean hasLiveAttempt() {
        return !liveEvents.isEmpty();
    }

    public synchronized long getAttemptStartNs() {
        return attemptStartNs;
    }

    public synchronized int getLiveEventCount() {
        return liveEvents.size();
    }

    public synchronized String getDisplayedInputsSummary() {
        List<InputEvent> events = eventsForRender();
        if (events.isEmpty()) {
            return "none";
        }

        StringBuilder summary = new StringBuilder();

        for (int i = 0; i < events.size(); i++) {
            InputEvent event = events.get(i);
            if (i > 0) {
                summary.append("  ");
            }

            summary.append(shortLabel(event.type()))
                    .append("@")
                    .append(String.format(Locale.ROOT, "%.1f", event.timeMs()))
                    .append("ms");
        }

        return summary.toString();
    }

    private void recordIfMatch(boolean matched, ActionType type, long nowNs) {
        if (!matched) {
            return;
        }

        if (attemptStartNs < 0L) {
            attemptStartNs = nowNs;
            status = "Attempt started";
        }

        double relMs = (nowNs - attemptStartNs) / 1_000_000.0;
        liveEvents.add(new InputEvent(relMs, type));
        lastEventNs = nowNs;
    }

    private void finishAttempt(boolean incomplete) {
        lastEvents = List.copyOf(liveEvents);
        lastTickOffsets = List.copyOf(liveTickOffsets);

        if (incomplete) {
            lastChance = 0.0;
            status = "Incomplete attempt";
        } else {
            lastChance = calculateProbability(lastEvents);
            status = "Attempt complete";
        }

        attemptCount++;
        cumulativeChance += lastChance;

        liveEvents.clear();
        liveTickOffsets.clear();
        attemptStartNs = -1L;
        lastEventNs = -1L;
    }

    private double calculateProbability(List<InputEvent> events) {
        if (events.size() < 4) {
            return 0.0;
        }

        int success = 0;
        int total = 0;

        for (double phase = 0.0; phase < TICK_MS; phase += PHASE_STEP_MS) {
            total++;
            if (successForPhase(phase, events)) {
                success++;
            }
        }

        return total == 0 ? 0.0 : (double) success / (double) total;
    }

    private boolean successForPhase(double phaseMs, List<InputEvent> events) {
        Map<Integer, EnumSet<ActionType>> byTick = new HashMap<>();

        for (InputEvent event : events) {
            int tick = (int) Math.floor((event.timeMs() - phaseMs) / TICK_MS);
            byTick.computeIfAbsent(tick, ignored -> EnumSet.noneOf(ActionType.class)).add(event.type());
        }

        for (Map.Entry<Integer, EnumSet<ActionType>> entry : byTick.entrySet()) {
            int tick = entry.getKey();
            EnumSet<ActionType> current = entry.getValue();
            EnumSet<ActionType> next = byTick.get(tick + 1);

            if (current.contains(ActionType.ATTACK)
                    && current.contains(ActionType.AXE)
                    && next != null
                    && next.contains(ActionType.ATTACK)
                    && next.contains(ActionType.MACE)) {
                return true;
            }
        }

        return false;
    }

    private static String shortLabel(ActionType type) {
        return switch (type) {
            case ATTACK -> "atk";
            case AXE -> "axe";
            case MACE -> "mace";
        };
    }
}
