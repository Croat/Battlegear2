package mods.battlegear2;

import mods.battlegear2.api.shield.IArrowCatcher;
import mods.battlegear2.api.PlayerEventChild;
import mods.battlegear2.api.core.IBattlePlayer;
import mods.battlegear2.api.IOffhandDual;
import mods.battlegear2.api.shield.IShield;
import mods.battlegear2.api.quiver.IArrowContainer2;
import mods.battlegear2.api.weapons.IExtendedReachWeapon;
import mods.battlegear2.enchantments.BaseEnchantment;
import mods.battlegear2.api.core.InventoryPlayerBattle;
import mods.battlegear2.packet.BattlegearShieldFlashPacket;
import mods.battlegear2.packet.BattlegearSyncItemPacket;
import mods.battlegear2.api.core.BattlegearUtils;
import mods.battlegear2.utils.EnumBGAnimations;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.Event.Result;
import net.minecraftforge.event.EventPriority;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.network.Player;

public class BattlemodeHookContainerClass {

    @ForgeSubscribe(priority = EventPriority.LOWEST)
    public void onEntityJoin(EntityJoinWorldEvent event){
        if(event.entity instanceof EntityPlayer){
            PacketDispatcher.sendPacketToPlayer(
                    new BattlegearSyncItemPacket((EntityPlayer)event.entity).generatePacket(),
                    (Player)event.entity);

        }
    }

    @ForgeSubscribe
    public void attackEntity(AttackEntityEvent event){
        ItemStack mainhand = event.entityPlayer.getCurrentEquippedItem();
        if(mainhand != null && mainhand.getItem() instanceof IExtendedReachWeapon){
            float reachMod = ((IExtendedReachWeapon) mainhand.getItem()).getReachModifierInBlocks(mainhand);
            if(reachMod < 0){
                if(reachMod + 4 < event.entityPlayer.getDistanceToEntity(event.target)){
                    event.setCanceled(true);
                }
            }
        }

        if(((IBattlePlayer) event.entityPlayer).getSpecialActionTimer() > 0){
            event.setCanceled(true);
        }

    }

    @ForgeSubscribe
    public void playerInterect(PlayerInteractEvent event) {
        if(((IBattlePlayer) event.entityPlayer).getSpecialActionTimer() > 0){
            event.setCanceled(true);
            event.entityPlayer.isSwingInProgress = false;
        }else if(((IBattlePlayer) event.entityPlayer).isBattlemode()) {
            ItemStack mainHandItem = event.entityPlayer.getCurrentEquippedItem();
            ItemStack offhandItem = ((InventoryPlayerBattle)event.entityPlayer.inventory).getCurrentOffhandWeapon();

            switch (event.action) {
                case LEFT_CLICK_BLOCK:
                    break;
                case RIGHT_CLICK_BLOCK:

                    if (offhandItem != null && offhandItem.getItem() instanceof IOffhandDual) {
                        event.useItem = Result.DENY;
                        boolean shouldSwing = ((IOffhandDual) offhandItem.getItem()).offhandClickBlock(event, mainHandItem, offhandItem);

                        if (shouldSwing) {
                        	((IBattlePlayer) event.entityPlayer).swingOffItem();
                            Battlegear.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, event.entityPlayer);
                        }

                    }else if (offhandItem != null && offhandItem.getItem() instanceof IShield){
                        event.useItem = Result.DENY;
                    }else{
                    	((IBattlePlayer) event.entityPlayer).swingOffItem();
                        Battlegear.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, event.entityPlayer);
                    }
                    break;

                case RIGHT_CLICK_AIR:

                    if (mainHandItem == null || BattlegearUtils.isMainHand(mainHandItem, offhandItem)) {

                        event.setCanceled(true);

                        if (offhandItem != null && offhandItem.getItem() instanceof IOffhandDual) {
                            boolean shouldSwing = ((IOffhandDual) offhandItem.getItem()).offhandClickAir(event, mainHandItem, offhandItem);

                            if (shouldSwing) {
                            	((IBattlePlayer) event.entityPlayer).swingOffItem();
                                Battlegear.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, event.entityPlayer);
                            }

                        }else if (offhandItem != null && (offhandItem.getItem() instanceof IShield)){
                            event.useItem = Result.DENY;
                        }else{
                        	((IBattlePlayer) event.entityPlayer).swingOffItem();
                            Battlegear.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, event.entityPlayer);
                        }
                    }
                    break;
            }
        }

    }

    @ForgeSubscribe
    public void playerIntereactEntity(EntityInteractEvent event) {
        if(((IBattlePlayer) event.entityPlayer).getSpecialActionTimer() > 0){
            event.setCanceled(true);
            event.setResult(Result.DENY);
            event.entityPlayer.isSwingInProgress = false;
        } else if (((IBattlePlayer) event.entityPlayer).isBattlemode()) {
            ItemStack mainHandItem = event.entityPlayer.getCurrentEquippedItem();
            ItemStack offhandItem = ((InventoryPlayerBattle)event.entityPlayer.inventory).getCurrentOffhandWeapon();
            if(mainHandItem == null || BattlegearUtils.isMainHand(mainHandItem, offhandItem)){
                PlayerEventChild.OffhandAttackEvent offAttackEvent = new PlayerEventChild.OffhandAttackEvent(event, mainHandItem, offhandItem);
                MinecraftForge.EVENT_BUS.post(offAttackEvent);
                if (offAttackEvent.swingOffhand) {
                    ((IBattlePlayer) event.entityPlayer).swingOffItem();
                    Battlegear.proxy.sendAnimationPacket(EnumBGAnimations.OffHandSwing, event.entityPlayer);
                }
                if (offAttackEvent.shouldAttack) {
                    //event.entityPlayer.attackTargetEntityWithCurrentOffItem(event.target);
                    BattlegearUtils.attackTargetEntityWithCurrentOffItem(event.entityPlayer, event.target);
                }
                if (offAttackEvent.cancelParent) {
                    event.setCanceled(true);
                    event.setResult(Result.DENY);
                }
            }

        }
    }

    @ForgeSubscribe(priority = EventPriority.HIGHEST)
    public void onOffhandAttack(PlayerEventChild.OffhandAttackEvent event){
        if(event.offHand!=null){
            if(event.offHand.getItem() instanceof IOffhandDual){
                event.swingOffhand =((IOffhandDual) event.offHand.getItem()).offhandAttackEntity(event, event.mainHand, event.offHand);
            }else if(event.offHand.getItem() instanceof IShield){
                event.swingOffhand = false;
                event.shouldAttack = false;
            }else if(event.offHand.getItem() instanceof IArrowContainer2){
                event.shouldAttack = false;
            }
        }
    }

    @ForgeSubscribe
    public void shieldHook(LivingHurtEvent event){

        if(event.entity instanceof IBattlePlayer){
            EntityPlayer player = (EntityPlayer)event.entity;
            if(((IBattlePlayer) player).getSpecialActionTimer() > 0){
                event.setCanceled(true);
            } else if(((IBattlePlayer) player).isBlockingWithShield()){
                ItemStack shield = ((InventoryPlayerBattle)player.inventory).getCurrentOffhandWeapon();
                if(((IShield)shield.getItem()).canBlock(shield, event.source)){
                    boolean shouldBlock = true;
                    Entity opponent = event.source.getEntity();
                    if(opponent != null){
                        double d0 = opponent.posX - event.entity.posX;
                        double d1;

                        for (d1 = opponent.posZ - player.posZ; d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D){
                            d0 = (Math.random() - Math.random()) * 0.01D;
                        }

                        float yaw = (float)(Math.atan2(d1, d0) * 180.0D / Math.PI) - player.rotationYaw;
                        yaw = yaw - 90;

                        while(yaw < -180){
                            yaw+= 360;
                        }
                        while(yaw >= 180){
                            yaw-=360;
                        }

                        float blockAngle = ((IShield) shield.getItem()).getBlockAngle(shield);

                        shouldBlock = yaw < blockAngle && yaw > -blockAngle;
                        //player.knockBack(opponent, 50, 100, 100);
                    }

                    if(shouldBlock){
                        event.setCanceled(true);

                        PacketDispatcher.sendPacketToAllAround(player.posX, player.posY, player.posZ, 32, player.dimension,
                                new BattlegearShieldFlashPacket(player, event.ammount).generatePacket());
                    	player.worldObj.playSoundAtEntity(player, "battlegear2:shield", 1, 1);
                    	
                        if(event.source.isProjectile() && event.source.getEntity() instanceof EntityArrow){
                            event.source.getEntity().setDead();
                            if(shield.getItem() instanceof IArrowCatcher){
                                ((IArrowCatcher)shield.getItem()).setArrowCount(shield, ((IArrowCatcher) shield.getItem()).getArrowCount(shield)+1);
                                ((InventoryPlayerBattle)player.inventory).hasChanged = true;
                                player.setArrowCountInEntity(player.getArrowCountInEntity()-1);
                            }
                        }

                        if(!player.capabilities.isCreativeMode){
                            shield.damageItem((int)event.ammount, player);
                            if(shield.getItemDamage() <= 0){
                                MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, shield));
                                player.inventory.setInventorySlotContents(player.inventory.currentItem + 3, null);
                                //TODO Render item break
                            }
                        }
                        ((InventoryPlayerBattle)player.inventory).hasChanged = true;
                    }
                }
            }
        }
    }
    
    @ForgeSubscribe
    public void onDrop(LivingDropsEvent event){
    	if(event.source.getEntity() instanceof EntityLivingBase){
    		ItemStack stack = ((EntityLivingBase) event.source.getEntity()).getCurrentItemOrArmor(0);
    		if(stack!=null && stack.getItem() instanceof ItemBow){
    			int lvl = EnchantmentHelper.getEnchantmentLevel(BaseEnchantment.bowLoot.effectId, stack);
    			if(lvl>0){
    				ItemStack drop;
    				for(EntityItem items:event.drops){
    					drop = items.getEntityItem();
    					if(drop!=null && drop.getMaxStackSize()<drop.stackSize+lvl){
    						drop.stackSize+=lvl;
    						items.setEntityItemStack(drop);
    					}
    				}
    			}
    		}
    	}
    }

}
