package de.maxhenkel.simplevoiceanimations.mixin;

import de.maxhenkel.simplevoiceanimations.config.ClientConfig;
import de.maxhenkel.simplevoiceanimations.sync.ClientPreferencesSync;
import de.maxhenkel.simplevoiceanimations.sync.PlayerAnimationOverrides;
import de.maxhenkel.simplevoiceanimations.sync.Preferences;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

/**
 * Applies client-side per-player animation assignments on top of the synced
 * preferences. The target class is not modified - only the resolved style is
 * swapped, split height and intensity are kept from the synced preferences.
 */
@Mixin(ClientPreferencesSync.class)
public class ClientPreferencesSyncMixin {

    @Inject(method = "getPreferences(Ljava/util/UUID;)Lde/maxhenkel/simplevoiceanimations/sync/Preferences;", at = @At("RETURN"), cancellable = true)
    private static void getPreferences(UUID playerId, CallbackInfoReturnable<Preferences> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && playerId.equals(player.getUUID())) {
            return;
        }
        if (!PlayerAnimationOverrides.isEnabled()) {
            return;
        }
        ClientConfig.HeadAnimationStyle style = PlayerAnimationOverrides.getOverride(playerId);
        if (style == null) {
            style = PlayerAnimationOverrides.getEffectiveDefaultStyle();
        }
        Preferences base = cir.getReturnValue();
        cir.setReturnValue(new Preferences(style, base.getHeadSplitHeight(), base.getIntensity()));
    }

}
