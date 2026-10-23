package com.example.hinatamod.entity;

import com.example.hinatamod.registry.ModEntities;
import com.example.hinatamod.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * Shuriken lanzable. Al impactar contra un árbol (troncos u hojas) invoca a Hinata
 * en el punto de impacto con una nube de humo ninja.
 */
public class ShurikenEntity extends ThrowableItemProjectile {
    private boolean consumed;

    public ShurikenEntity(EntityType<? extends ShurikenEntity> type, Level level) {
        super(type, level);
    }

    public ShurikenEntity(Level level, LivingEntity owner) {
        super(ModEntities.SHURIKEN.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.SPECIAL_SHURIKEN.get();
    }

    @Override
    protected float getGravity() {
        return 0.02F;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0, 0, 0);
        }
    }

    private static boolean isTree(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WART_BLOCKS);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!(level() instanceof ServerLevel server)) return;
        if (!isTree(server.getBlockState(hit.getBlockPos()))) return;
        consumed = true;
        summonHinata(server, hit);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            boolean creative = getOwner() instanceof Player p && p.getAbilities().instabuild;
            if (!consumed && !creative) {
                spawnAtLocation(getItem());   // no era un árbol: se devuelve el shuriken
            }
            discard();
        }
    }

    private void summonHinata(ServerLevel level, BlockHitResult hit) {
        Vec3 loc = hit.getLocation();
        var n = hit.getDirection().getNormal();
        Vec3 pos = loc.add(n.getX() * 0.45, n.getY() * 0.05, n.getZ() * 0.45);
        if (n.getY() < 0) {                       // golpeó por debajo de una hoja/rama
            pos = pos.add(0, -1.8, 0);
        }

        // Si el punto cae dentro de un bloque, sube hasta encontrar hueco libre.
        EntityDimensions dim = ModEntities.HINATA.get().getDimensions();
        for (int i = 0; i < 8 && !level.noCollision(dim.makeBoundingBox(pos)); i++) {
            pos = pos.add(0, 0.5, 0);
        }

        ServerPlayer owner = getOwner() instanceof ServerPlayer sp ? sp : null;
        UUID ownerId = owner != null ? owner.getUUID() : null;

        float yaw = level.random.nextFloat() * 360F;
        if (owner != null) {
            yaw = (float) (Mth.atan2(owner.getZ() - pos.z, owner.getX() - pos.x) * 180.0D / Math.PI) - 90.0F;
        }

        // Efecto de humo ninja en el punto de aparición
        smokeBurst(level, pos);

        HinataEntity hinata = findExisting(level, ownerId);
        if (hinata != null) {
            smokeBurst(level, hinata.position());         // humo también donde estaba
            hinata.getNavigation().stop();
            hinata.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
        } else {
            hinata = ModEntities.HINATA.get().create(level);
            if (hinata == null) return;
            hinata.moveTo(pos.x, pos.y, pos.z, yaw, 0.0F);
            hinata.yHeadRot = yaw;
            hinata.yBodyRot = yaw;
            hinata.setOwnerUUID(ownerId);
            DifficultyInstance diff = level.getCurrentDifficultyAt(hinata.blockPosition());
            hinata.finalizeSpawn(level, diff, MobSpawnType.MOB_SUMMONED, null, null);
            level.addFreshEntity(hinata);
        }

        hinata.say("dialogue.hinatamod.summon");
        if (owner != null) {
            hinata.getLookControl().setLookAt(owner);
            owner.sendSystemMessage(Component.translatable("message.hinatamod.summoned"));
        }
    }

    private static HinataEntity findExisting(ServerLevel level, UUID ownerId) {
        if (ownerId == null) return null;
        List<? extends HinataEntity> list = level.getEntities(
                EntityTypeTest.forClass(HinataEntity.class), h -> ownerId.equals(h.getOwnerUUID()));
        return list.isEmpty() ? null : list.get(0);
    }

    public static void smokeBurst(ServerLevel level, Vec3 pos) {
        level.sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.9, pos.z, 40, 0.35, 0.7, 0.35, 0.06);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y + 0.9, pos.z, 15, 0.3, 0.6, 0.3, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.9, pos.z, 25, 0.4, 0.8, 0.4, 0.03);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.NEUTRAL, 1.0F, 1.2F);
    }
}
