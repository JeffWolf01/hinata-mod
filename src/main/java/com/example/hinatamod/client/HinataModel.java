package com.example.hinatamod.client;

import com.example.hinatamod.entity.HinataEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Modelo humanoide (formato de skin estándar 64x64) con animaciones procedurales:
 *  - idle tímido: se "toca" los dedos índice (gesto característico de Hinata)
 *  - respiración suave cuando es una desconocida
 *  - poses de abrazo, mano y caricia
 */
public class HinataModel extends PlayerModel<HinataEntity> {

    public HinataModel(ModelPart root, boolean slimArms) {
        super(root, slimArms);
    }

    @Override
    public void setupAnim(HinataEntity e, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {
        super.setupAnim(e, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        int action = e.getAction();
        boolean moving = limbSwingAmount > 0.05F;

        if (action == HinataEntity.ACTION_HUG) {
            rightArm.xRot = -1.35F; leftArm.xRot = -1.35F;
            rightArm.yRot = -0.55F; leftArm.yRot = 0.55F;
            rightArm.zRot = 0F;     leftArm.zRot = 0F;
            head.xRot += 0.30F;
        } else if (action == HinataEntity.ACTION_HEADPAT) {
            rightArm.xRot = -0.95F; leftArm.xRot = -0.95F;
            rightArm.yRot = -0.50F; leftArm.yRot = 0.50F;
            head.xRot += 0.45F;                      // agacha la cabeza, avergonzada
        } else if (action == HinataEntity.ACTION_HOLD) {
            rightArm.xRot = -0.85F;                  // brazo extendido hacia el jugador
            rightArm.yRot = -0.30F;
            rightArm.zRot = 0F;
            head.xRot += 0.12F;
        } else if (!moving && e.getStage() >= 1) {
            float poke = Mth.sin(ageInTicks * 0.22F) * 0.06F;
            rightArm.xRot = -0.95F + poke;  leftArm.xRot = -0.95F - poke;
            rightArm.yRot = -0.50F;         leftArm.yRot = 0.50F;
            rightArm.zRot = 0F;             leftArm.zRot = 0F;
            head.xRot += 0.10F + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        } else if (!moving) {
            rightArm.zRot += Mth.cos(ageInTicks * 0.09F) * 0.03F + 0.05F;
            leftArm.zRot  -= Mth.cos(ageInTicks * 0.09F) * 0.03F + 0.05F;
        }

        // Re-sincroniza las capas exteriores con las partes modificadas.
        leftSleeve.copyFrom(leftArm);
        rightSleeve.copyFrom(rightArm);
        hat.copyFrom(head);
    }
}
