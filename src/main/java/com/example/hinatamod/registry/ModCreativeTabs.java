package com.example.hinatamod.registry;

import com.example.hinatamod.HinataMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HinataMod.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hinatamod"))
                    .icon(() -> new ItemStack(ModItems.SPECIAL_SHURIKEN.get()))
                    .displayItems((params, out) -> {
                        out.accept(ModItems.SPECIAL_SHURIKEN.get());
                        out.accept(ModItems.RAMEN_BOWL.get());
                        out.accept(ModItems.ICHIRAKU_RAMEN.get());
                        out.accept(ModItems.HINATA_SPAWN_EGG.get());
                    })
                    .build());
}
