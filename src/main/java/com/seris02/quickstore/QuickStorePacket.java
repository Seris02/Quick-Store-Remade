package com.seris02.quickstore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkEvent;

public class QuickStorePacket {
	
	public static class PQueue<T extends Object> {
		public class Link<K extends Object> {
			public double priority;
			public K element;
			public Link<K> next;
			public Link(double priority, K element, Link<K> next) {
				this.priority = priority;
				this.element = element;
				this.next = next;
			}
		}
		public Link<T> first = null;
		public PQueue() {}
		public void enqueue(T element, double priority) {
			if (first == null) {
				first = new Link<T>(priority, element, null);
			} else {
				if (priority > first.priority) {
					first = new Link<T>(priority, element, first);
					return;
				}
				Link<T> k = first;
				while (k.next != null && k.next.priority > priority) {
					k = k.next;
				}
				k.next = new Link<T>(priority, element, k.next);
			}
		}
		public T dequeue() {
			if (first == null) return null;
			T l = first.element;
			first = first.next;
			return l;
		}
	}

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
	
	public static void quickStoreToContainerMenu(AbstractContainerMenu a, List<ItemStack> items) {
		Container ce = null;
		for (Slot x : a.slots) {
			if (x.container != null && !(x.container instanceof Inventory)) {
				ce = x.container;
			}
		}
		if (ce == null) return;
		for (ItemStack s : items) {
			if (ce.countItem(s.getItem()) > 0) {
				InvWrapper invw = new InvWrapper(ce);
				for (int n = 0; n < ce.getContainerSize(); n++) {
					if (invw.getStackInSlot(n).isEmpty()) continue;
					ItemStack d = invw.getStackInSlot(n);
					if (invw.isItemValid(n, s) && ItemStack.isSameItemSameTags(s, d)) {
						moveItemStackTo(s, 0, ce.getContainerSize(), false, ce);
					}
					if (s.isEmpty()) break;
				}
			}
		}
	}
	
	public static void quickStore(ServerPlayer p) {
		ServerLevel l = p.getLevel();
		List<ItemStack> items = QuickStorePacket.getInventoryMinusHotbarAndArmor(p.getInventory());
		PQueue<Container> pqueue = new PQueue<Container>();
		if (p.containerMenu != null) {
			quickStoreToContainerMenu(p.containerMenu, items);
			return;
		}
		for (int x = -5; x <= 5; x++) {
			for (int y = -5; y <= 5; y++) {
				for (int z = -5; z <= 5; z++) {
					BlockPos d = p.blockPosition().offset(x, y, z);
					if (l.getBlockEntity(d) instanceof Container ce) {
						pqueue.enqueue(ce, ce instanceof RandomizableContainerBlockEntity ? 1 : 0);
					}
				}
			}
		}
		while (pqueue.first != null) {
			Container ce = pqueue.dequeue();
			if (!ce.isEmpty()) {
				for (ItemStack s : items) {
					if (ce.countItem(s.getItem()) > 0) {
						InvWrapper invw = new InvWrapper(ce);
						for (int n = 0; n < ce.getContainerSize(); n++) {
							if (invw.getStackInSlot(n).isEmpty()) continue;
							ItemStack d = invw.getStackInSlot(n);
							if (invw.isItemValid(n, s) && ItemStack.isSameItemSameTags(s, d)) {
								moveItemStackTo(s, 0, ce.getContainerSize(), false, ce);
							}
							if (s.isEmpty()) break;
						}
					}
				}
			}
		}
	}
	
	//taken from AbstractContainerMenu and altered to interact with the container directly, rather than slots
	private static boolean moveItemStackTo(ItemStack stack, int minSlot, int maxSlot, boolean backwards, Container ce) {
	   boolean flag = false;
	   int i = minSlot;
	   if (backwards) {
	      i = maxSlot - 1;
	   }

	   if (stack.isStackable()) {
	      while(!stack.isEmpty()) {
	         if (backwards) {
	            if (i < minSlot) {
	               break;
	            }
	         } else if (i >= maxSlot) {
	            break;
	         }

	         ItemStack itemstack = ce.getItem(i);
	         if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(stack, itemstack)) {
	            int j = itemstack.getCount() + stack.getCount();
	            int maxSize = Math.min(ce.getMaxStackSize(), stack.getMaxStackSize());
	            if (j <= maxSize) {
	               stack.setCount(0);
	               itemstack.setCount(j);
	               flag = true;
	            } else if (itemstack.getCount() < maxSize) {
	               stack.shrink(maxSize - itemstack.getCount());
	               itemstack.setCount(maxSize);
	               flag = true;
	            }
	         }

	         if (backwards) {
	            --i;
	         } else {
	            ++i;
	         }
	      }
	   }

	   if (!stack.isEmpty()) {
		   if (backwards) {
	            i = maxSlot - 1;
	         } else {
	            i = minSlot;
	         }

	      while(true) {
	         if (backwards) {
	            if (i < minSlot) {
	               break;
	            }
	         } else if (i >= maxSlot) {
	            break;
	         }

	         ItemStack itemstack1 = ce.getItem(i);
	         if (itemstack1.isEmpty() && ce.canPlaceItem(i, stack)) {
	            if (stack.getCount() > ce.getMaxStackSize()) {
	               ce.setItem(i, stack.split(ce.getMaxStackSize()));
	            } else {
	            	ce.setItem(i, stack.split(stack.getCount()));
	            }
	            flag = true;
	            break;
	         }

	         if (backwards) {
	            --i;
	         } else {
	            ++i;
	         }
	      }
	   }

	   return flag;
	}
}