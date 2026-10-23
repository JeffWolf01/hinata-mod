package com.example.hinatamod.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Tazón de ramen: comestible por el jugador y regalable a Hinata. */
public class RamenItem extends Item {
    private final int affection;

    public RamenItem(Properties properties, int affection) {
        super(properties);
        this.affection = affection;
    }

    public int getAffection() {
        return affection;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        return entity instanceof Player p && p.getAbilities().instabuild ? result : new ItemStack(Items.BOWL);
    }
}
