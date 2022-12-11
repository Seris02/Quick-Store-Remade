package com.seris02.quickstore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkEvent;

public class QuickStorePacket {

	public QuickStorePacket() {}

	public static void encode(QuickStorePacket message, FriendlyByteBuf buf) {
	}

	public static QuickStorePacket decode(FriendlyByteBuf buf) {
		QuickStorePacket message = new QuickStorePacket();
		return message;
	}
	
	public static List<ItemStack> getInventoryMinusHotbarAndArmor(Inventory i) {
		List<ItemStack> items = new ArrayList<ItemStack>();
		for (int c = 0; c < i.getContainerSize(); c++) {
			if (i.getItem(c).isEmpty()) continue;
			if (Inventory.isHotbarSlot(c)) continue;
			if (c == Inventory.SLOT_OFFHAND) continue;
			boolean j = false;
			for (int y : Inventory.ALL_ARMOR_SLOTS) {
				if (c == y) j = true; break;
			}
			if (j == true) continue;
			items.add(i.getItem(c));
		}
		return items;
	}

	public static void onMessage(QuickStorePacket message, Supplier<NetworkEvent.Context> ctx) {
		ctx.get().enqueueWork(() -> {
			quickStore(ctx.get().getSender());
		});

		ctx.get().setPacketHandled(true);
	}
	
	public static void quickStore(ServerPlayer p) {
		ServerLevel l = p.getLevel();
		List<ItemStack> items = QuickStorePacket.getInventoryMinusHotbarAndArmor(p.getInventory());
		for (int x = -5; x <= 5; x++) {
			for (int y = -5; y <= 5; y++) {
				for (int z = -5; z <= 5; z++) {
					if (l.getBlockEntity(p.blockPosition().offset(x, y, z)) instanceof Container ce) {
						if (!ce.isEmpty()) {
							for (ItemStack s : items) {
								if (ce.countItem(s.getItem()) > 0) {
									InvWrapper invw = new InvWrapper(ce);
									for (int n = 0; n < ce.getContainerSize(); n++) {
										if (invw.getStackInSlot(n).isEmpty()) continue;
										ItemStack d = invw.getStackInSlot(n);
										if (invw.isItemValid(n, s) && ItemStack.isSameItemSameTags(s, d)) {
											moveItemStackTo(s, n, ce.getContainerSize(), false, ce);
										}
										if (s.isEmpty()) break;
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	//taken from AbstractContainerMenu and altered to interact with the container directly, rather than slots
	private static boolean moveItemStackTo(ItemStack p_38904_, int p_38905_, int p_38906_, boolean p_38907_, Container ce) {
	   boolean flag = false;
	   int i = p_38905_;
	   if (p_38907_) {
	      i = p_38906_ - 1;
	   }

	   if (p_38904_.isStackable()) {
	      while(!p_38904_.isEmpty()) {
	         if (p_38907_) {
	            if (i < p_38905_) {
	               break;
	            }
	         } else if (i >= p_38906_) {
	            break;
	         }

	         ItemStack itemstack = ce.getItem(i);
	         if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(p_38904_, itemstack)) {
	            int j = itemstack.getCount() + p_38904_.getCount();
	            int maxSize = Math.min(ce.getMaxStackSize(), p_38904_.getMaxStackSize());
	            if (j <= maxSize) {
	               p_38904_.setCount(0);
	               itemstack.setCount(j);
	               flag = true;
	            } else if (itemstack.getCount() < maxSize) {
	               p_38904_.shrink(maxSize - itemstack.getCount());
	               itemstack.setCount(maxSize);
	               flag = true;
	            }
	         }

	         if (p_38907_) {
	            --i;
	         } else {
	            ++i;
	         }
	      }
	   }

	   if (!p_38904_.isEmpty()) {
	      if (p_38907_) {
	         i = p_38906_ - 1;
	      } else {
	         i = p_38905_;
	      }

	      while(true) {
	         if (p_38907_) {
	            if (i < p_38905_) {
	               break;
	            }
	         } else if (i >= p_38906_) {
	            break;
	         }

	         ItemStack itemstack1 = ce.getItem(i);
	         if (itemstack1.isEmpty() && ce.canPlaceItem(i, p_38904_)) {
	            if (p_38904_.getCount() > ce.getMaxStackSize()) {
	               ce.setItem(i, p_38904_.split(ce.getMaxStackSize()));
	            } else {
	            	ce.setItem(i, p_38904_.split(p_38904_.getCount()));
	            }
	            flag = true;
	            break;
	         }

	         if (p_38907_) {
	            --i;
	         } else {
	            ++i;
	         }
	      }
	   }

	   return flag;
	}
}