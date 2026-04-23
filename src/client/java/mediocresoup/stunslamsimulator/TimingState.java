package mediocresoup.stunslamsimulator;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public final class TimingState {
    private static final double TICK_MS = 50.0;
    private static final double PHASE_STEP_MS = 0.05; // how often the brute force search looks for success
    private static final long INACTIVITY_TIMEOUT_NS = 5 * 50_000_000L;  // 5 ticks

    private final List<InputEvent> liveEvents = new ArrayList<>();
    private List<InputEvent> lastEvents = List.of();
    
    private final Deque<Long> tickHistoryNs = new ArrayDeque<>();
    private final Deque<Long> frameHistoryNs = new ArrayDeque<>();
    private List<Double> lastTickOffsets = List.of();
    private List<Double> lastFrameOffsets = List.of();

    private long attemptStartNs = -1L;
    private long lastEventNs = -1L;

    private int attemptCount = 0;
    private double cumulativeChance = 0.0;
    private double lastChance = 0.0;
    private String status = "init"; // "init", "active", "idle"

    public synchronized void handleRawMouseButton(Minecraft client, int button, int action) {
        if (client.player == null || action != InputConstants.PRESS) {
            return;
        }

        long now = System.nanoTime();
        MouseButtonEvent mouseEvent = new MouseButtonEvent(0.0, 0.0, new MouseButtonInfo(button, 0));

        recordIfMatch(client.options.keyAttack.matchesMouse(mouseEvent), ActionType.ATTACK, now);

        for (int i = 0; i < 9; i++) {
            if (client.options.keyHotbarSlots[i].matchesMouse(mouseEvent)) {
                checkHotbarSlot(client, i, now);
            }
        }
    }

    public synchronized void handleRawKey(Minecraft client, int keyCode, int scanCode, int action) {
        if (client.player == null || action != InputConstants.PRESS) {
            return;
        }

        long now = System.nanoTime();
        KeyEvent keyEvent = new KeyEvent(keyCode, scanCode, 0);

        recordIfMatch(client.options.keyAttack.matches(keyEvent), ActionType.ATTACK, now);

        for (int i = 0; i < 9; i++) {
            if (client.options.keyHotbarSlots[i].matches(keyEvent)) {
                checkHotbarSlot(client, i, now);
            }
        }
    }

    private void checkHotbarSlot(Minecraft client, int slot, long now) {
        if (client.player == null) return;
        
        ItemStack stack = client.player.getInventory().getItem(slot);
        if (stack.isEmpty()) return;

        // Detect Axes (Shield breakers)
        if (stack.getItem() instanceof AxeItem) {
            recordIfMatch(true, ActionType.AXE, now);
        } 
        // Detect Mace
        else if (stack.getItem() == Items.MACE) {
            recordIfMatch(true, ActionType.MACE, now);
        }
    }

    public synchronized void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }

        tickHistoryNs.addLast(System.nanoTime());

        // Only keep a 5 second tick history
        long cutoff = System.nanoTime() - 5_000_000_000L;
        while (!tickHistoryNs.isEmpty() && tickHistoryNs.getFirst() < cutoff) {
            tickHistoryNs.removeFirst();
        }

        long now = System.nanoTime();

        // After some ticks of inactivity, calculate and display the chance (but keep events)
        if (!liveEvents.isEmpty() && lastEventNs > 0 && now - lastEventNs >= INACTIVITY_TIMEOUT_NS) {
            if (!hasLiveAttempt()) {
                // Already calculated, don't recalculate
                return;
            }
            // first frame of going idle
            // Calculate probability and show result
            lastEvents = List.copyOf(liveEvents);
            lastTickOffsets = tickOffsetsForRender();
            lastFrameOffsets = frameOffsetsForRender();
            lastChance = calculateProbability(lastEvents);
            status = "idle";
        }
    }

    public synchronized void resetCurrentAttempt() {
        liveEvents.clear();
        attemptStartNs = -1L;
        lastEventNs = -1L;
        status = "init";
    }

    public synchronized void recordFrame() {
        frameHistoryNs.addLast(System.nanoTime());

        // Only keep a 5 second frame history
        long cutoff = System.nanoTime() - 5_000_000_000L;
        while (!frameHistoryNs.isEmpty() && frameHistoryNs.getFirst() < cutoff) {
            frameHistoryNs.removeFirst();
        }

        // If we are live then make a full copy of the current frame history for rendering
        if (status.equals("active")) {
            lastFrameOffsets = frameOffsetsForRender();
        }
    }

    public synchronized void resetStats() {
        this.attemptCount = 0;
        this.cumulativeChance = 0.0;
        this.lastChance = 0.0;
        this.lastEvents = List.of();
        this.lastTickOffsets = List.of();
        this.lastFrameOffsets = List.of();
        resetCurrentAttempt();
    }

    public synchronized List<InputEvent> eventsForRender() {
        return liveEvents.isEmpty() ? lastEvents : List.copyOf(liveEvents);
    }

    public synchronized List<Double> tickOffsetsForRender() {
        if (!hasLiveAttempt()) return lastTickOffsets;

        long ref = attemptStartNs;

        List<Double> offsets = new ArrayList<>();
        for (Long t : tickHistoryNs) {
            offsets.add((double) (t - ref) / 1_000_000.0);
        }
        return offsets;
    }

    public synchronized List<Double> frameOffsetsForRender() {
        if (!hasLiveAttempt()) return lastFrameOffsets;
        long ref = attemptStartNs;

        List<Double> offsets = new ArrayList<>();
        for (Long f : frameHistoryNs) {
            offsets.add((double) (f - ref) / 1_000_000.0);
        }
        return offsets;
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
        return status.equals("active");
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

        // If previous attempt is complete, reset for new attempt
        if (!hasLiveAttempt()) {
            resetCurrentAttempt();
            attemptCount++;
            cumulativeChance += lastChance;
        }

        if (attemptStartNs < 0L) {
            attemptStartNs = nowNs;
            status = "active";
        }

        double relMs = (nowNs - attemptStartNs) / 1_000_000.0;
        liveEvents.add(new InputEvent(relMs, type));
        lastEventNs = nowNs;
    }

    private double calculateProbability(List<InputEvent> events) {
        // Need at least: AXE hotkey press, ATTACK (at least once), MACE hotkey press, ATTACK (at least once)
        // Though with spam, there could be many more ATTACKs
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
