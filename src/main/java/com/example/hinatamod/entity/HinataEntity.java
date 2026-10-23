package com.example.hinatamod.entity;

import com.example.hinatamod.item.RamenItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.EnumSet;
import java.util.UUID;

public class HinataEntity extends PathfinderMob {

    // ---- Ajustes de progresión (edítalos libremente) ----
    public static final int MAX_AFFECTION = 1000;
    /** Umbrales de etapa: 0 = tímida/neutral, 1 = sonrojo leve, 2 = sonrojo medio, 3 = sonrojo intenso. */
    public static final int[] STAGE_THRESHOLDS = {0, 100, 300, 600};
    public static final int REQ_HEADPAT = 100;
    public static final int REQ_HOLD = 300;
    public static final int REQ_HUG = 600;
    public static final int REQ_SPECIAL = 900;
    public static final int TALK_POINTS = 8;          // por día de juego
    public static final int DAILY_GIFT_LIMIT = 3;     // ramen aceptados por día

    // ---- Estados de pose (sincronizados al cliente) ----
    public static final int ACTION_NONE = 0, ACTION_HUG = 1, ACTION_HOLD = 2, ACTION_HEADPAT = 3;

    // ---- Comandos enviados desde la GUI ----
    public static final int CMD_TALK = 0, CMD_GIFT = 1, CMD_FOLLOW = 2, CMD_HOLD = 3,
            CMD_HUG = 4, CMD_HEADPAT = 5, CMD_SPECIAL = 6;

    private static final EntityDataAccessor<Integer> AFFECTION = SynchedEntityData.defineId(HinataEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(HinataEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ACTION = SynchedEntityData.defineId(HinataEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DIALOGUE = SynchedEntityData.defineId(HinataEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DIALOGUE_SERIAL = SynchedEntityData.defineId(HinataEntity.class, EntityDataSerializers.INT);

    private UUID ownerUUID;
    private long lastTalkDay = -1;
    private long lastGiftDay = -1;
    private int giftsToday;
    private boolean confessed;
    private int actionTicks;

    public HinataEntity(EntityType<? extends HinataEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setCustomName(Component.translatable("entity.hinatamod.hinata"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(AFFECTION, 0);
        entityData.define(FOLLOWING, false);
        entityData.define(ACTION, ACTION_NONE);
        entityData.define(DIALOGUE, "");
        entityData.define(DIALOGUE_SERIAL, 0);
    }

    // ------------------------------------------------------------------ IA
    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BusyGoal(this));
        goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.15D));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.7D) {
            @Override public boolean canUse() { return !isFollowing() && !isBusy() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !isFollowing() && !isBusy() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    /** Se queda quieta mientras abraza / recibe una caricia. */
    private static class BusyGoal extends Goal {
        private final HinataEntity mob;
        BusyGoal(HinataEntity mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }
        @Override public boolean canUse() { return mob.isBusy(); }
        @Override public boolean canContinueToUse() { return mob.isBusy(); }
        @Override public void start() { mob.getNavigation().stop(); }
    }

    /** Sigue a su dueño cuando se lo pides (o cuando van de la mano). */
    private static class FollowOwnerGoal extends Goal {
        private final HinataEntity mob;
        private final double speed;
        private Player owner;
        private int recalc;

        FollowOwnerGoal(HinataEntity mob, double speed) {
            this.mob = mob;
            this.speed = speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private double stopDist() { return mob.getAction() == ACTION_HOLD ? 1.6D : 3.0D; }

        @Override public boolean canUse() {
            if (!mob.isFollowing() || mob.isBusy()) return false;
            Player o = mob.getOwnerPlayer();
            if (o == null || o.isSpectator()) return false;
            if (mob.distanceToSqr(o) < stopDist() * stopDist()) return false;
            owner = o;
            return true;
        }

        @Override public boolean canContinueToUse() {
            return owner != null && owner.isAlive() && !owner.isSpectator() && mob.isFollowing() && !mob.isBusy()
                    && mob.distanceToSqr(owner) > (stopDist() * 0.7D) * (stopDist() * 0.7D);
        }

        @Override public void start() { recalc = 0; }
        @Override public void stop() { owner = null; mob.getNavigation().stop(); }

        @Override public void tick() {
            mob.getLookControl().setLookAt(owner, 10.0F, mob.getMaxHeadXRot());
            if (--recalc <= 0) {
                recalc = adjustedTickDelay(10);
                if (mob.distanceToSqr(owner) >= 24.0D * 24.0D) {
                    mob.teleportNear(owner);
                } else {
                    mob.getNavigation().moveTo(owner, speed);
                }
            }
        }
    }

    /** Teletransporte ninja hacia el jugador si se queda muy atrás. */
    private void teleportNear(Player target) {
        if (!(level() instanceof ServerLevel server)) return;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (Math.abs(dx) < 2 && Math.abs(dz) < 2) continue;
                Vec3 cand = new Vec3(target.getX() + dx, target.getY(), target.getZ() + dz);
                AABB box = getDimensions(getPose()).makeBoundingBox(cand);
                if (level().noCollision(this, box)) {
                    ShurikenEntity.smokeBurst(server, position());
                    getNavigation().stop();
                    moveTo(cand.x, cand.y, cand.z, getYRot(), getXRot());
                    ShurikenEntity.smokeBurst(server, cand);
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------ Getters
    public int getAffection() { return entityData.get(AFFECTION); }
    public boolean isFollowing() { return entityData.get(FOLLOWING); }
    public int getAction() { return entityData.get(ACTION); }
    public String getDialogueKey() { return entityData.get(DIALOGUE); }
    public int getDialogueSerial() { return entityData.get(DIALOGUE_SERIAL); }
    public UUID getOwnerUUID() { return ownerUUID; }
    public void setOwnerUUID(UUID id) { this.ownerUUID = id; }
    public boolean isBusy() { int a = getAction(); return a == ACTION_HUG || a == ACTION_HEADPAT; }

    public Player getOwnerPlayer() {
        return ownerUUID == null ? null : level().getPlayerByUUID(ownerUUID);
    }

    /** 0..3: determina la textura (expresión/sonrojo) y las líneas de diálogo. */
    public int getStage() {
        int aff = getAffection();
        int stage = 0;
        for (int i = 0; i < STAGE_THRESHOLDS.length; i++) {
            if (aff >= STAGE_THRESHOLDS[i]) stage = i;
        }
        return stage;
    }

    // ------------------------------------------------------- Interacción
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (level().isClientSide) {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.example.hinatamod.client.ClientHooks.openHinataScreen(this));
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** Llamado en el servidor desde el paquete de la GUI. */
    public void handleAction(ServerPlayer player, int cmd) {
        if (ownerUUID == null) ownerUUID = player.getUUID();   // el primero que la toca la "adopta"
        if (!player.getUUID().equals(ownerUUID)) {
            say("dialogue.hinatamod.not_owner");
            return;
        }
        ServerLevel level = (ServerLevel) level();

        switch (cmd) {
            case CMD_TALK -> talk(level, player);
            case CMD_GIFT -> gift(level, player);
            case CMD_FOLLOW -> {
                boolean follow = !isFollowing();
                entityData.set(FOLLOWING, follow);
                if (!follow && getAction() == ACTION_HOLD) setAction(ACTION_NONE, 0);
                say(follow ? "dialogue.hinatamod.follow_on" : "dialogue.hinatamod.follow_off");
            }
            case CMD_HOLD -> {
                if (getAction() == ACTION_HOLD) {
                    setAction(ACTION_NONE, 0);
                    say("dialogue.hinatamod.hold_off");
                } else if (require(REQ_HOLD)) {
                    setAction(ACTION_HOLD, 0);
                    entityData.set(FOLLOWING, true);
                    say("dialogue.hinatamod.hold_on");
                    hearts(5);
                }
            }
            case CMD_HUG -> {
                if (require(REQ_HUG)) {
                    setAction(ACTION_HUG, 70);
                    sayRandom("dialogue.hinatamod.hug", 2);
                    hearts(10);
                }
            }
            case CMD_HEADPAT -> {
                if (require(REQ_HEADPAT)) {
                    setAction(ACTION_HEADPAT, 50);
                    sayRandom("dialogue.hinatamod.headpat", 2);
                    hearts(6);
                }
            }
            case CMD_SPECIAL -> special(level, player);
            default -> { }
        }
    }

    private boolean require(int needed) {
        if (getAffection() >= needed) return true;
        say("dialogue.hinatamod.locked");
        return false;
    }

    private void talk(ServerLevel level, ServerPlayer player) {
        long day = level.getDayTime() / 24000L;
        if (day != lastTalkDay) {
            lastTalkDay = day;
            addAffection(TALK_POINTS, player);
            sayRandom("dialogue.hinatamod.talk." + getStage(), 3);
            hearts(3);
        } else {
            sayRandom("dialogue.hinatamod.talk_repeat", 2);
        }
    }

    private void gift(ServerLevel level, ServerPlayer player) {
        ItemStack stack = findRamen(player);
        if (stack.isEmpty()) {
            say("dialogue.hinatamod.gift_none");
            return;
        }
        long day = level.getDayTime() / 24000L;
        if (day != lastGiftDay) {
            lastGiftDay = day;
            giftsToday = 0;
        }
        if (giftsToday >= DAILY_GIFT_LIMIT) {
            say("dialogue.hinatamod.gift_full");
            return;
        }
        int points = ((RamenItem) stack.getItem()).getAffection();
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (!player.getInventory().add(bowl)) player.drop(bowl, false);
        }
        giftsToday++;
        addAffection(points, player);
        sayRandom("dialogue.hinatamod.gift." + getStage(), 2);
        playSound(SoundEvents.GENERIC_EAT, 1.0F, 1.1F);
        hearts(7);
    }

    private ItemStack findRamen(Player player) {
        if (player.getMainHandItem().getItem() instanceof RamenItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof RamenItem) return player.getOffhandItem();
        for (ItemStack s : player.getInventory().items) {
            if (s.getItem() instanceof RamenItem) return s;
        }
        return ItemStack.EMPTY;
    }

    private void special(ServerLevel level, ServerPlayer player) {
        if (!require(REQ_SPECIAL)) return;
        setAction(ACTION_HUG, 100);
        hearts(25);
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.5, getZ(), 40, 0.8, 0.8, 0.8, 0.05);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1.0F, 1.4F);
        if (!confessed) {
            confessed = true;
            addAffection(50, player);
            say("dialogue.hinatamod.confession");
            player.sendSystemMessage(Component.translatable("message.hinatamod.confession"));
        } else {
            say("dialogue.hinatamod.special_repeat");
        }
    }

    public void addAffection(int amount, ServerPlayer player) {
        int old = getAffection();
        int oldStage = getStage();
        entityData.set(AFFECTION, Mth.clamp(old + amount, 0, MAX_AFFECTION));
        int now = getAffection();
        if (getStage() > oldStage && player != null) {
            player.sendSystemMessage(Component.translatable("message.hinatamod.stage." + getStage()));
            hearts(15);
        }
        if (old < REQ_SPECIAL && now >= REQ_SPECIAL && player != null) {
            player.sendSystemMessage(Component.translatable("message.hinatamod.special_unlocked"));
        }
    }

    // ----------------------------------------------------------- Helpers
    public void say(String key) {
        entityData.set(DIALOGUE, key);
        entityData.set(DIALOGUE_SERIAL, getDialogueSerial() + 1);
    }

    private void sayRandom(String base, int count) {
        say(base + "." + (1 + random.nextInt(count)));
    }

    private void setAction(int action, int ticks) {
        entityData.set(ACTION, action);
        actionTicks = ticks;
        getNavigation().stop();
    }

    private void hearts(int count) {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.9, getZ(), count, 0.4, 0.3, 0.4, 0.02);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        Player owner = getOwnerPlayer();
        int action = getAction();
        if (action == ACTION_HOLD) {
            if (owner == null || distanceToSqr(owner) > 100.0D) setAction(ACTION_NONE, 0);
        } else if (action != ACTION_NONE) {
            if (owner != null) getLookControl().setLookAt(owner, 30.0F, 30.0F);
            getNavigation().stop();
            if (--actionTicks <= 0) {
                entityData.set(ACTION, ACTION_NONE);
            } else if (tickCount % 10 == 0) {
                hearts(2);
            }
        }

        // Bonus de vínculo: regeneración leve si te sigue y la relación es alta.
        if (tickCount % 200 == 0 && isFollowing() && getAffection() >= REQ_HUG
                && owner != null && distanceToSqr(owner) < 64.0D) {
            owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, true, true));
        }
    }

    // ----------------------------------------------- Pacífica y persistente
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof Player) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Affection", getAffection());
        tag.putBoolean("Following", isFollowing());
        if (ownerUUID != null) tag.putUUID("Owner", ownerUUID);
        tag.putLong("LastTalkDay", lastTalkDay);
        tag.putLong("LastGiftDay", lastGiftDay);
        tag.putInt("GiftsToday", giftsToday);
        tag.putBoolean("Confessed", confessed);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(AFFECTION, tag.getInt("Affection"));
        entityData.set(FOLLOWING, tag.getBoolean("Following"));
        if (tag.hasUUID("Owner")) ownerUUID = tag.getUUID("Owner");
        lastTalkDay = tag.getLong("LastTalkDay");
        lastGiftDay = tag.getLong("LastGiftDay");
        giftsToday = tag.getInt("GiftsToday");
        confessed = tag.getBoolean("Confessed");
    }
}
