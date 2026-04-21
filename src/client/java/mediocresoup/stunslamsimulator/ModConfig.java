package mediocresoup.stunslamsimulator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("stun-slam-simulator.json");

    private static ModConfig INSTANCE;

    private boolean enabled = true;
    private boolean showInputs = true;
    private boolean showFrameLines = false;
    private boolean showTitle = true;  // Show/hide the "Stun Slam Simulator" title
    private String hudSize = "SMALL";  // TINY, SMALL, MEDIUM, LARGE (each has fixed scale)
    private String hudAnchor = "TOP_LEFT"; // Default anchor

    private ModConfig() {
    }

    public static ModConfig getInstance() {
        if (INSTANCE == null) {
            INSTANCE = load();
        }
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        save();
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public boolean isShowInputs() {
        return showInputs;
    }

    public void toggleShowInputs() {
        this.showInputs = !this.showInputs;
        save();
    }

    public boolean isShowFrameLines() {
        return showFrameLines;
    }

    public void toggleShowFrameLines() {
        this.showFrameLines = !this.showFrameLines;
        save();
    }

    public boolean isShowTitle() {
        return showTitle;
    }

    public void toggleShowTitle() {
        this.showTitle = !this.showTitle;
        save();
    }

    public String getHudSize() {
        return hudSize;
    }

    public void setHudSize(String size) {
        if (size != null && (size.equals("TINY") || size.equals("SMALL") || size.equals("MEDIUM") || size.equals("LARGE"))) {
            this.hudSize = size;
            save();
        }
    }

    public String getHudAnchor() {
        return hudAnchor;
    }

    public void setHudAnchor(String anchor) {
        if (anchor != null && (
                anchor.equals("TOP_LEFT") ||
                anchor.equals("TOP_RIGHT") ||
                anchor.equals("BOTTOM_LEFT") ||
                anchor.equals("BOTTOM_RIGHT"))) {
            this.hudAnchor = anchor;
            save();
        }
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            StunSlamSimulator.LOGGER.error("Failed to save stun-slam-simulator config", e);
        }
    }

    private static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (Exception e) {
                StunSlamSimulator.LOGGER.error("Failed to load stun-slam-simulator config, using defaults", e);
            }
        }

        ModConfig config = new ModConfig();
        config.save();
        return config;
    }
}