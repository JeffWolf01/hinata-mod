package com.example.hinatamod.client;

import com.example.hinatamod.HinataMod;
import com.example.hinatamod.entity.HinataEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HinataRenderer extends MobRenderer<HinataEntity, HinataModel> {
    /** hinata_0 = neutral/tímida, 1 = sonrojo leve, 2 = sonrojo medio, 3 = sonrojo intenso. */
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[4];
    static {
        for (int i = 0; i < TEXTURES.length; i++) {
            TEXTURES[i] = new ResourceLocation(HinataMod.MODID, "textures/entity/hinata/hinata_" + i + ".png");
        }
    }

    public HinataRenderer(EntityRendererProvider.Context ctx) {
        // 'true' = brazos delgados (skin tipo Alex). Cámbialo a 'false' y usa ModelLayers.PLAYER si tu skin es "Steve".
        super(ctx, new HinataModel(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(HinataEntity entity) {
        return TEXTURES[Mth.clamp(entity.getStage(), 0, TEXTURES.length - 1)];
    }
}
