package de.maxhenkel.simplevoiceanimations;

import de.maxhenkel.simplevoiceanimations.interfaces.AnimationDataExtension;
import de.maxhenkel.simplevoiceanimations.interfaces.AnimationRenderExtension;
import de.maxhenkel.simplevoiceanimations.interfaces.AnimationRenderState;
import de.maxhenkel.simplevoiceanimations.sync.ClientPreferencesSync;
import de.maxhenkel.simplevoiceanimations.sync.Preferences;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;

public class RenderStateExtractor {

    private static final float DEFAULT_HEAD_SPLIT_ROTATION = Mth.DEG_TO_RAD * 45F;
    private static final float DEFAULT_HEAD_SPLIT_DISTANCE = 0.2F;
    private static final float DEFAULT_HEAD_SCALE = 0.25F;
    private static final float DEFAULT_HEAD_NOD_ROTATION = Mth.DEG_TO_RAD * 20F;

    public static void extractRenderState(Avatar entity, AvatarRenderState state, float partialTicks, AnimationDataExtension entityExtension, AnimationRenderExtension stateExtension) {
        Preferences preferences = ClientPreferencesSync.getPreferences(entity.getUUID());
        applyStyle(stateExtension.simplevoiceanimations$getState(), preferences, entityExtension.simplevoiceanimations$getData().getAnimationProgress());
    }

    public static void applyStyle(AnimationRenderState renderState, Preferences preferences, float animationProgress) {
        renderState.headXScale = 1F;
        renderState.headYScale = 1F;
        renderState.headZScale = 1F;
        renderState.headXRot = 0F;
        renderState.splitHead = false;
        renderState.headSplitRotation = 0F;
        renderState.headSplitDistance = 0F;
        animationProgress *= preferences.getIntensity();
        switch (preferences.getHeadAnimationStyle()) {
            case SQUISH:
                float yScale = 1F + animationProgress * DEFAULT_HEAD_SCALE;
                float xzScale = 1F / Mth.sqrt(yScale);
                renderState.headXScale = xzScale;
                renderState.headYScale = yScale;
                renderState.headZScale = xzScale;
                break;
            case SCALE_Y:
                renderState.headYScale = 1F + animationProgress * DEFAULT_HEAD_SCALE;
                break;
            case SCALE_FULL:
                float scale = 1F + animationProgress * DEFAULT_HEAD_SCALE;
                renderState.headXScale = scale;
                renderState.headYScale = scale;
                renderState.headZScale = scale;
                break;
            case SPLIT_HINGE:
                renderState.splitHead = true;
                renderState.headSplitHeight = preferences.getHeadSplitHeight();
                renderState.headSplitRotation = animationProgress * DEFAULT_HEAD_SPLIT_ROTATION;
                break;
            case SPLIT_VERTICAL:
                renderState.splitHead = true;
                renderState.headSplitHeight = preferences.getHeadSplitHeight();
                renderState.headSplitDistance = animationProgress * DEFAULT_HEAD_SPLIT_DISTANCE;
                break;
            case NOD:
                renderState.headXRot = animationProgress * DEFAULT_HEAD_NOD_ROTATION;
                break;
        }
    }

}
