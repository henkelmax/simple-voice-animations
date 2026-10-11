package de.maxhenkel.simplevoiceanimations.gui;

import de.maxhenkel.simplevoiceanimations.config.ClientConfig;
import de.maxhenkel.simplevoiceanimations.sync.PlayerAnimationOverrides;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Settings screen for per-player animation assignments.
 * New code - the existing settings screen only links here.
 */
public class PlayerOverrideScreen extends Screen {

    private static final int ROW_WIDTH = 300;

    @Nullable
    private final Screen parent;
    private ObjectSelectionList<PlayerEntry> playerList;
    private CycleButton<ClientConfig.HeadAnimationStyle> styleCycleButton;
    private Button removeButton;
    private EditBox nameField;
    @Nullable
    private Component status;
    private int statusColor = 0xFFFFFFFF;

    public PlayerOverrideScreen(@Nullable Screen parent) {
        super(Component.translatableWithFallback("gui.simplevoiceanimations.overrides.title", "Player Animations"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PlayerAnimationOverrides.refreshOnlineNames();
        int centerX = width / 2;

        addRenderableWidget(CycleButton.onOffBuilder(PlayerAnimationOverrides.isEnabled())
                .create(centerX + 20, 30, 105, 20, Component.translatableWithFallback("gui.simplevoiceanimations.overrides.enabled", "Override Other Players"), (button, enabled) -> {
                    PlayerAnimationOverrides.setEnabled(enabled);
                }));
        addRenderableWidget(Button.builder(Component.literal("?"), button ->
                showHelp("gui.simplevoiceanimations.overrides.help_enabled", "ON ignores synced styles: others show your own style unless assigned."))
                .bounds(centerX + 130, 30, 20, 20).build());

        playerList = new PlayerList(minecraft, ROW_WIDTH, height - 178, 70, 24);
        playerList.setX(centerX - ROW_WIDTH / 2);
        addRenderableWidget(playerList);

        styleCycleButton = addRenderableWidget(CycleButton.builder(PlayerOverrideScreen::getStyleName, ClientConfig.HeadAnimationStyle.NONE)
                .withValues(ClientConfig.HeadAnimationStyle.values())
                .create(centerX - 150, height - 88, 165, 20, Component.translatableWithFallback("gui.simplevoiceanimations.overrides.selected_style", "Style"), (button, style) -> {
                    PlayerEntry selected = playerList.getSelected();
                    if (selected != null) {
                        PlayerAnimationOverrides.setOverride(selected.playerId, style);
                        rebuildPlayerList();
                    }
                }));
        addRenderableWidget(Button.builder(Component.literal("?"), button ->
                showHelp("gui.simplevoiceanimations.overrides.help_selected", "Click a player above, then choose their style here."))
                .bounds(centerX + 20, height - 88, 20, 20).build());
        removeButton = addRenderableWidget(Button.builder(Component.translatableWithFallback("gui.simplevoiceanimations.overrides.remove", "Remove"), button -> {
            PlayerEntry selected = playerList.getSelected();
            if (selected != null) {
                PlayerAnimationOverrides.removeOverride(selected.playerId);
                status = Component.translatableWithFallback("gui.simplevoiceanimations.overrides.removed", "Removed %s", selected.displayName);
                statusColor = 0xFFAAAAAA;
                rebuildPlayerList();
            }
        }).bounds(centerX + 45, height - 88, 105, 20).build());

        nameField = new EditBox(font, centerX - 150, height - 64, 165, 20, Component.translatableWithFallback("gui.simplevoiceanimations.overrides.name_hint", "Player name"));
        nameField.setHint(Component.translatableWithFallback("gui.simplevoiceanimations.overrides.name_hint", "Player name"));
        nameField.setMaxLength(16);
        addRenderableWidget(nameField);
        addRenderableWidget(Button.builder(Component.literal("?"), button ->
                showHelp("gui.simplevoiceanimations.overrides.help_add", "Type an online player's name. Saved by UUID, survives renames."))
                .bounds(centerX + 20, height - 64, 20, 20).build());
        addRenderableWidget(Button.builder(Component.translatableWithFallback("gui.simplevoiceanimations.overrides.add", "Add"), button -> addPlayer())
                .bounds(centerX + 45, height - 64, 105, 20).build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).bounds(centerX - 75, height - 28, 150, 20).build());

        rebuildPlayerList();
    }

    private void addPlayer() {
        String name = nameField.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        UUID id = PlayerAnimationOverrides.resolveUuid(name);
        if (id == null) {
            status = Component.translatableWithFallback("gui.simplevoiceanimations.overrides.unknown_player", "Unknown player (must be online): %s", name);
            statusColor = 0xFFFF5555;
            return;
        }
        String displayName = PlayerAnimationOverrides.getDisplayName(id);
        if (PlayerAnimationOverrides.getOverride(id) == null) {
            PlayerAnimationOverrides.setOverride(id, PlayerAnimationOverrides.getEffectiveDefaultStyle());
            status = Component.translatableWithFallback("gui.simplevoiceanimations.overrides.added", "Added %s", displayName);
            statusColor = 0xFF55FF55;
        } else {
            status = Component.translatableWithFallback("gui.simplevoiceanimations.overrides.already_added", "Already in list: %s", displayName);
            statusColor = 0xFFAAAAAA;
        }
        nameField.setValue("");
        rebuildPlayerList();
    }

    private void rebuildPlayerList() {
        PlayerEntry selected = playerList.getSelected();
        UUID selectedId = selected != null ? selected.playerId : null;
        List<PlayerEntry> entries = new ArrayList<>();
        for (Map.Entry<UUID, ClientConfig.HeadAnimationStyle> entry : PlayerAnimationOverrides.getOverrides().entrySet()) {
            UUID id = entry.getKey();
            entries.add(new PlayerEntry(font, id, PlayerAnimationOverrides.getDisplayName(id), entry.getValue(), isOnline(id), selectedEntry -> {
                playerList.setSelected(selectedEntry);
                updateActionWidgets();
            }));
        }
        playerList.replaceEntries(entries);
        if (selectedId != null) {
            boolean found = false;
            for (PlayerEntry entry : playerList.children()) {
                if (entry.playerId.equals(selectedId)) {
                    playerList.setSelected(entry);
                    found = true;
                    break;
                }
            }
            if (!found) {
                playerList.setSelected(null);
            }
        }
        updateActionWidgets();
    }

    private void showHelp(String key, String fallback) {
        status = Component.translatableWithFallback(key, fallback);
        statusColor = 0xFFAAAAAA;
    }

    private void updateActionWidgets() {
        PlayerEntry selected = playerList.getSelected();
        boolean hasSelection = selected != null;
        styleCycleButton.active = hasSelection;
        removeButton.active = hasSelection;
        if (hasSelection) {
            styleCycleButton.setValue(selected.style);
        }
    }

    private boolean isOnline(UUID playerId) {
        if (minecraft.getConnection() == null) {
            return false;
        }
        for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
            if (playerId.equals(info.getProfile().id())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.centeredText(font, title, width / 2, 12, -1);
        graphics.centeredText(font, Component.translatableWithFallback("gui.simplevoiceanimations.overrides.players", "Per-player animations"), width / 2, 58, 0xFFAAAAAA);
        PlayerEntry selected = playerList != null ? playerList.getSelected() : null;
        if (selected != null) {
            graphics.centeredText(font, Component.translatableWithFallback("gui.simplevoiceanimations.overrides.selected_player", "Selected player: %s", selected.displayName), width / 2, height - 102, 0xFFAAAAAA);
        }
        if (status != null) {
            graphics.centeredText(font, status, width / 2, height - 42, statusColor);
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    private static Component getStyleName(ClientConfig.HeadAnimationStyle style) {
        return switch (style) {
            case NONE -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.none", "None");
            case SQUISH -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.squish", "Squish");
            case SCALE_Y -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.scale_y", "Scale Height");
            case SCALE_FULL -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.scale_full", "Scale");
            case SPLIT_HINGE -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.split_hinge", "Split Head (Hinge)");
            case SPLIT_VERTICAL -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.split_vertical", "Split Head (Vertical)");
            case NOD -> Component.translatableWithFallback("gui.simplevoiceanimations.settings.style.nod", "Nod");
        };
    }

    private static class PlayerList extends ObjectSelectionList<PlayerEntry> {
        private PlayerList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }
    }

    private static class PlayerEntry extends ObjectSelectionList.Entry<PlayerEntry> {
        private final Font font;
        private final UUID playerId;
        private final String displayName;
        private final ClientConfig.HeadAnimationStyle style;
        private final boolean online;
        private final Consumer<PlayerEntry> onSelect;

        private PlayerEntry(Font font, UUID playerId, String displayName, ClientConfig.HeadAnimationStyle style, boolean online, Consumer<PlayerEntry> onSelect) {
            this.font = font;
            this.playerId = playerId;
            this.displayName = truncate(font, displayName, 150);
            this.style = style;
            this.online = online;
            this.onSelect = onSelect;
        }

        private static String truncate(Font font, String text, int maxWidth) {
            if (font.width(text) <= maxWidth) {
                return text;
            }
            while (text.length() > 1 && font.width(text + "...") > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            return text + "...";
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int nameColor = online ? 0xFFFFFFFF : 0xFFAAAAAA;
            graphics.text(font, displayName, getContentX() + 5, getContentYMiddle() - 4, nameColor);
            String styleName = getStyleName(style).getString();
            graphics.text(font, styleName, getContentRight() - 5 - font.width(styleName), getContentYMiddle() - 4, 0xFFAAAAAA);
        }

        @Override
        public Component getNarration() {
            return Component.translatableWithFallback("gui.simplevoiceanimations.overrides.entry_narration", "%s, %s", displayName, getStyleName(style).getString());
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            onSelect.accept(this);
            return true;
        }
    }

}
