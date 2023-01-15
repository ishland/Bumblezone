package com.telepathicgrunt.the_bumblezone.items;

import com.telepathicgrunt.the_bumblezone.entities.nonliving.BeeStingerEntity;
import com.telepathicgrunt.the_bumblezone.events.player.PlayerLocateProjectileEvent;
import com.telepathicgrunt.the_bumblezone.modinit.BzItems;
import com.telepathicgrunt.the_bumblezone.modules.EntityMiscHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingGetProjectileEvent;

public class BeeStinger extends ArrowItem {
    public BeeStinger(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity livingEntity) {
        if (!stack.is(BzItems.CRYSTAL_CANNON.get()) && livingEntity instanceof ServerPlayer serverPlayer) {
            EntityMiscHandler.onBeeStingerFired(serverPlayer);
        }
        return new BeeStingerEntity(level, livingEntity);
    }

    //TODO forge method
    @Override
    public boolean isInfinite(ItemStack stack, ItemStack bow, Player player) {
        int enchantLevel = bow.getEnchantmentLevel(Enchantments.INFINITY_ARROWS);
        return enchantLevel > 0;
    }

    public static ItemStack bowUsable(ItemStack ammo, PlayerLocateProjectileEvent event) {
        if (event.shooter() instanceof Player player && (event.weapon().is(Items.BOW) || event.weapon().is(Items.CROSSBOW))) {
            Inventory inventory = player.getInventory();
            for(int i = 0; i < inventory.getContainerSize(); ++i) {
                ItemStack itemstack1 = inventory.getItem(i);
                if (itemstack1.is(BzItems.BEE_STINGER.get())) {
                    return itemstack1;
                }
            }
        }
        return null;
    }
}
