package com.example.hinatamod;

import com.example.hinatamod.entity.HinataEntity;
import com.example.hinatamod.network.ModNetwork;
import com.example.hinatamod.registry.ModCreativeTabs;
import com.example.hinatamod.registry.ModEntities;
import com.example.hinatamod.registry.ModItems;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(HinataMod.MODID)
public class HinataMod {
    public static final String MODID = "hinatamod";

    public HinataMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(bus);
        ModEntities.ENTITIES.register(bus);
        ModCreativeTabs.TABS.register(bus);
        bus.addListener(this::commonSetup);
        bus.addListener(this::registerAttributes);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::init);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.HINATA.get(), HinataEntity.createAttributes().build());
    }
}
