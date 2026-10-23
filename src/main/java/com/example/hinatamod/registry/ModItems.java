package com.example.hinatamod.registry;

import com.example.hinatamod.HinataMod;
import com.example.hinatamod.item.RamenItem;
import com.example.hinatamod.item.ShurikenItem;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, HinataMod.MODID);

    public static final RegistryObject<Item> SPECIAL_SHURIKEN = ITEMS.register("special_shuriken",
            () -> new ShurikenItem(new Item.Properties().stacksTo(16)));

    /** Ramen normal: +12 de afecto. */
    public static final RegistryObject<Item> RAMEN_BOWL = ITEMS.register("ramen_bowl",
            () -> new RamenItem(new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(8).saturationMod(0.8F).build()), 12));

    /** Ramen especial Ichiraku: +30 de afecto. */
    public static final RegistryObject<Item> ICHIRAKU_RAMEN = ITEMS.register("ichiraku_ramen",
            () -> new RamenItem(new Item.Properties().stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(12).saturationMod(1.0F)
                            .effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 1.0F).build()), 30));

    public static final RegistryObject<Item> HINATA_SPAWN_EGG = ITEMS.register("hinata_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.HINATA, 0x1F2A5C, 0xDCD6F7, new Item.Properties()));
}
