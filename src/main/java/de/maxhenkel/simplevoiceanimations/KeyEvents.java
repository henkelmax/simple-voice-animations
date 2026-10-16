package de.maxhenkel.simplevoiceanimations;

import de.maxhenkel.simplevoiceanimations.gui.AnimationSettingsScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class KeyEvents {

    private static final Minecraft mc = Minecraft.getInstance();

    public KeyEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (mc.player == null) {
                return;
            }
if (SimpleVoiceAnimationsClient.SETTINGS.consumeClick()) {
            Minecraft.getInstance().setScreen(new AnimationSettingsScreen());
            }
        });
    }

}
