package net.x7bbbbbbb.bl_trinkets_layer.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.emi.trinkets.api.SlotType;
import net.fabricmc.loader.api.FabricLoader;
import net.x7bbbbbbb.bl_trinkets_layer.BLTrinketsLayer;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BLTrinketsLayerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = BLTrinketsLayer.MOD_ID + ".json";
    private static final String KEY_SLOTS = "allowed_slots";
    private static final String KEY_IRIS = "enable_iris_compat";
    private static final List<String> DEFAULT_SLOTS = List.of("legs/belt");
    private static final boolean DEFAULT_IRIS = true;

    private static volatile List<String> allowedSlots = DEFAULT_SLOTS;
    private static volatile boolean irisCompat = DEFAULT_IRIS;

    private BLTrinketsLayerConfig() {}

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        JsonObject root = null;
        boolean exists = Files.exists(path);
        if (exists) {
            try (Reader reader = Files.newBufferedReader(path)) {
                root = GSON.fromJson(reader, JsonObject.class);
            } catch (IOException | RuntimeException e) {
                BLTrinketsLayer.LOGGER.error("Failed to read config, using defaults (file left untouched)", e);
                allowedSlots = DEFAULT_SLOTS;
                irisCompat = DEFAULT_IRIS;
                return;
            }
        }
        if (root == null) root = new JsonObject();

        boolean dirty = !exists;

        List<String> slots = null;
        if (root.has(KEY_SLOTS)) {
            try {
                List<String> parsed = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray(KEY_SLOTS)) {
                    String slot = el.getAsString().strip();
                    if (!slot.isEmpty()) parsed.add(slot);
                }
                if (!parsed.isEmpty()) slots = List.copyOf(parsed);
                else BLTrinketsLayer.LOGGER.warn("[BL Trinkets Layer] Config '{}' is empty, using defaults", KEY_SLOTS);
            } catch (RuntimeException e) {
                BLTrinketsLayer.LOGGER.warn("[BL Trinkets Layer] Config '{}' is malformed, using defaults", KEY_SLOTS);
            }
        } else {
            JsonArray arr = new JsonArray();
            for (String s : DEFAULT_SLOTS) arr.add(s);
            root.add(KEY_SLOTS, arr);
            dirty = true;
        }
        allowedSlots = slots != null ? slots : DEFAULT_SLOTS;

        boolean iris = DEFAULT_IRIS;
        if (root.has(KEY_IRIS)) {
            JsonElement el = root.get(KEY_IRIS);
            if (el.isJsonPrimitive() && ((JsonPrimitive) el).isBoolean()) {
                iris = el.getAsBoolean();
            } else {
                BLTrinketsLayer.LOGGER.warn("[BL Trinkets Layer] Config '{}' must be true or false, using default ({})", KEY_IRIS, DEFAULT_IRIS);
            }
        } else {
            root.addProperty(KEY_IRIS, DEFAULT_IRIS);
            dirty = true;
        }
        irisCompat = iris;

        if (dirty) {
            try {
                Files.createDirectories(path.getParent());
                try (Writer writer = Files.newBufferedWriter(path)) {
                    GSON.toJson(root, writer);
                }
                BLTrinketsLayer.LOGGER.info("[BL Trinkets Layer] {} config: {}", exists ? "Updated" : "Created default", path);
            } catch (IOException e) {
                BLTrinketsLayer.LOGGER.error("[BL Trinkets Layer] Failed to write config", e);
            }
        }
        BLTrinketsLayer.LOGGER.info("[BL Trinkets Layer] Loaded allowed slots: {}, Iris compat: {}", allowedSlots, irisCompat);
    }

    public static List<String> allowedSlots() {
        return allowedSlots;
    }

    public static boolean isIrisCompatEnabled() {
        return irisCompat;
    }

    public static boolean isAllowedSlot(SlotType type) {
        if (type == null) return false;
        String id = type.getId();     // "legs/belt"
        String name = type.getName(); // "belt"
        for (String allowed : allowedSlots) {
            if (allowed.equals(id) || allowed.equals(name)) return true;
        }
        return false;
    }
}
