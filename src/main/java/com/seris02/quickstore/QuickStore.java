package com.seris02.quickstore;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ScreenEvent.KeyboardKeyPressedEvent;
import net.minecraftforge.client.event.ScreenEvent.KeyboardKeyReleasedEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod(QuickStore.MODID)
public class QuickStore {
	// Directly reference a slf4j logger
	public static final  String MODID = "quickstoreremade";
	public KeyMapping store = new KeyMapping("key.quickstoreremade.quickstore", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "key.categories.quickstoreremade");
	public boolean nomore = false;
	public static SimpleChannel channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> "v1.0", "v1.0"::equals, "v1.0"::equals);
	
	@SuppressWarnings("deprecation")
	public QuickStore() {
		// Register the setup method for modloading
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);
			MinecraftForge.EVENT_BUS.addListener(this::clientTick);
			MinecraftForge.EVENT_BUS.addListener(this::screenKeyPressed);
			MinecraftForge.EVENT_BUS.addListener(this::screenKeyReleased);
		});
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	@OnlyIn(Dist.CLIENT)
	public void setupClient(FMLClientSetupEvent event) {
		ClientRegistry.registerKeyBinding(store);
	}
	@OnlyIn(Dist.CLIENT)
	public void screenKeyPressed(KeyboardKeyPressedEvent.Pre event) {
		if (!nomore && store.getKey().getValue() == event.getKeyCode()) {
			nomore = true;
			QuickStore.channel.sendToServer(new QuickStorePacket());
		}
	}
	@OnlyIn(Dist.CLIENT)
	public void screenKeyReleased(KeyboardKeyReleasedEvent.Pre event) {
		if (nomore && store.getKey().getValue() == event.getKeyCode()) {
			nomore = false;
		}
	}
	@OnlyIn(Dist.CLIENT)
	public void clientTick(ClientTickEvent event) {
		if (store.consumeClick()) {
			nomore = false;
			QuickStore.channel.sendToServer(new QuickStorePacket());
		}
		//if (nomore && !store.isDown()) {
		//	nomore = false;
		//}
	}

	private void setup(final FMLCommonSetupEvent event) {
		// some preinit code
		QuickStore.channel.registerMessage(0, QuickStorePacket.class, QuickStorePacket::encode, QuickStorePacket::decode, QuickStorePacket::onMessage);
	}
	
}
