package de.maxhenkel.simplevoiceanimations.sync;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.maxhenkel.simplevoiceanimations.SimpleVoiceAnimations;
import de.maxhenkel.simplevoiceanimations.SimpleVoiceAnimationsClient;
import de.maxhenkel.simplevoiceanimations.config.ClientConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

import javax.annotation.Nullable;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side per-player animation assignments.
 * <p>
 * Stored by UUID, displayed and entered as player names. When enabled, every
 * other player renders with their assigned style (or the local player's own
 * style when they have no assignment) instead of their synced preferences.
 * The own player always uses the local preferences. Everything lives in its
 * own JSON file, so no existing config schema is touched.
 */
public class PlayerAnimationOverrides {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded = false;
    private static boolean enabled = true;
    private static final Map<UUID, ClientConfig.HeadAnimationStyle> overrides = new LinkedHashMap<>();
    private static final Map<UUID, String> lastKnownNames = new LinkedHashMap<>();

    private PlayerAnimationOverrides() {
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(SimpleVoiceAnimations.MODID).resolve("player-animation-overrides.json");
    }

    public static synchronized boolean isEnabled() {
        ensureLoaded();
        return enabled;
    }

    public static synchronized void setEnabled(boolean enabled) {
        ensureLoaded();
        PlayerAnimationOverrides.enabled = enabled;
        save();
    }

    /**
     * The style used for players without their own assignment: simply the
     * local player's own style from the normal settings menu.
     */
    public static ClientConfig.HeadAnimationStyle getEffectiveDefaultStyle() {
        return SimpleVoiceAnimationsClient.CLIENT_CONFIG.getHeadAnimationStyle();
    }

    @Nullable
    public static synchronized ClientConfig.HeadAnimationStyle getOverride(UUID playerId) {
        ensureLoaded();
        return overrides.get(playerId);
    }

    public static synchronized void setOverride(UUID playerId, ClientConfig.HeadAnimationStyle style) {
        ensureLoaded();
        overrides.put(playerId, style);
        save();
    }

    public static synchronized void removeOverride(UUID playerId) {
        ensureLoaded();
        overrides.remove(playerId);
        lastKnownNames.remove(playerId);
        save();
    }

    public static synchronized Map<UUID, ClientConfig.HeadAnimationStyle> getOverrides() {
        ensureLoaded();
        return Collections.unmodifiableMap(new LinkedHashMap<>(overrides));
    }

    /**
     * Resolves a typed player name to a UUID via the currently online players.
     *
     * @return the UUID, or null when no online player matches
     */
    @Nullable
    public static synchronized UUID resolveUuid(String playerName) {
        ensureLoaded();
        if (Minecraft.getInstance().getConnection() == null || playerName == null) {
            return null;
        }
        String query = playerName.trim();
        if (query.isEmpty()) {
            return null;
        }
        for (PlayerInfo info : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
            if (info.getProfile().name().equalsIgnoreCase(query)) {
                UUID id = info.getProfile().id();
                lastKnownNames.put(id, info.getProfile().name());
                save();
                return id;
            }
        }
        return null;
    }

    /**
     * Display name for an override entry: current online name, last known name,
     * or a shortened UUID as fallback.
     */
    public static synchronized String getDisplayName(UUID playerId) {
        ensureLoaded();
        if (Minecraft.getInstance().getConnection() != null) {
            for (PlayerInfo info : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
                if (playerId.equals(info.getProfile().id())) {
                    String name = info.getProfile().name();
                    if (!name.equals(lastKnownNames.get(playerId))) {
                        lastKnownNames.put(playerId, name);
                        save();
                    }
                    return name;
                }
            }
        }
        String name = lastKnownNames.get(playerId);
        if (name != null) {
            return name;
        }
        return playerId.toString().substring(0, 8);
    }

    /**
     * Refreshes cached names from the currently online players.
     */
    public static synchronized void refreshOnlineNames() {
        ensureLoaded();
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        boolean changed = false;
        for (PlayerInfo info : Minecraft.getInstance().getConnection().getOnlinePlayers()) {
            UUID id = info.getProfile().id();
            if (overrides.containsKey(id) && !info.getProfile().name().equals(lastKnownNames.get(id))) {
                lastKnownNames.put(id, info.getProfile().name());
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        load();
    }

    private static void load() {
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("enabled")) {
                enabled = root.get("enabled").getAsBoolean();
            }

            if (root.has("overrides") && root.get("overrides").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("overrides").entrySet()) {
                    try {
                        UUID id = UUID.fromString(entry.getKey());
                        ClientConfig.HeadAnimationStyle parsed = parseStyle(entry.getValue().getAsString());
                        if (parsed != null) {
                            overrides.put(id, parsed);
                        }
                    } catch (RuntimeException ignored) {
                    }
                }
            }
            if (root.has("names") && root.get("names").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("names").entrySet()) {
                    try {
                        lastKnownNames.put(UUID.fromString(entry.getKey()), entry.getValue().getAsString());
                    } catch (RuntimeException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            SimpleVoiceAnimations.LOGGER.warn("Failed to load player animation overrides, using defaults", e);
        }
    }

    private static void save() {
        try {
            Path path = file();
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("enabled", enabled);
            JsonObject overrideObject = new JsonObject();
            for (Map.Entry<UUID, ClientConfig.HeadAnimationStyle> entry : overrides.entrySet()) {
                overrideObject.addProperty(entry.getKey().toString(), entry.getValue().name());
            }
            root.add("overrides", overrideObject);
            JsonObject nameObject = new JsonObject();
            for (Map.Entry<UUID, String> entry : lastKnownNames.entrySet()) {
                nameObject.addProperty(entry.getKey().toString(), entry.getValue());
            }
            root.add("names", nameObject);
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            SimpleVoiceAnimations.LOGGER.warn("Failed to save player animation overrides", e);
        }
    }

    @Nullable
    private static ClientConfig.HeadAnimationStyle parseStyle(String name) {
        try {
            return ClientConfig.HeadAnimationStyle.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            return null;
        }
    }

}
