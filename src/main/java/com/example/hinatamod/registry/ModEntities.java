package com.example.hinatamod.registry;

import com.example.hinatamod.HinataMod;
import com.example.hinatamod.entity.HinataEntity;
import com.example.hinatamod.entity.ShurikenEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, HinataMod.MODID);

    public static final RegistryObject<EntityType<HinataEntity>> HINATA = ENTITIES.register("hinata",
            () -> EntityType.Builder.<HinataEntity>of(HinataEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .clientTrackingRange(10)
                    .build(HinataMod.MODID + ":hinata"));

    public static final RegistryObject<EntityType<ShurikenEntity>> SHURIKEN = ENTITIES.register("special_shuriken",
            () -> EntityType.Builder.<ShurikenEntity>of(ShurikenEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build(HinataMod.MODID + ":special_shuriken"));
}
