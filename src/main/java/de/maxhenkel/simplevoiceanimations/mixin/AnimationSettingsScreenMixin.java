package de.maxhenkel.simplevoiceanimations.mixin;

import de.maxhenkel.simplevoiceanimations.gui.AnimationSettingsScreen;
import de.maxhenkel.simplevoiceanimations.gui.PlayerOverrideScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the player-animations button to the settings screen.
 * The target class is not modified. Extends Screen instead of shadowing
 * inherited members, since @Shadow cannot resolve superclass methods
 * without a refmap.
 */
@Mixin(AnimationSettingsScreen.class)
public abstract class AnimationSettingsScreenMixin extends Screen {

    protected AnimationSettingsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init()V", at = @At("RETURN"))
    private void init(CallbackInfo ci) {
        AnimationSettingsScreen screen = (AnimationSettingsScreen) (Object) this;
        addRenderableWidget(Button.builder(Component.translatableWithFallback("gui.simplevoiceanimations.settings.players", "Players..."), button -> {
            Minecraft.getInstance().gui.setScreen(new PlayerOverrideScreen(screen));
        }).bounds(5, height - 28, 100, 20).build());
    }

}
