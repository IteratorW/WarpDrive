package cr0s.warpdrive.client;

import cr0s.warpdrive.CommonProxy;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IItemBase;
import cr0s.warpdrive.event.ClientHandler;
import cr0s.warpdrive.event.ModelBakeEventHandler;
import cr0s.warpdrive.render.*;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {
	
	@Override
	public void onForgePreInitialisation() {
		super.onForgePreInitialisation();
		
		OBJLoader.INSTANCE.addDomain(WarpDrive.MODID);
		
		ModelLoaderRegistry.registerLoader(MyCustomModelLoader.INSTANCE);
		MinecraftForge.EVENT_BUS.register(ModelBakeEventHandler.instance);
	}
	
	@Override
	public void onForgeInitialisation() {
		super.onForgeInitialisation();
		
		// event handlers
		MinecraftForge.EVENT_BUS.register(new ClientHandler());
		
		// generic rendering
		// MinecraftForge.EVENT_BUS.register(new WarpDriveKeyBindings());
		MinecraftForge.EVENT_BUS.register(new RenderOverlayAir());
		MinecraftForge.EVENT_BUS.register(new RenderOverlayCamera());
		MinecraftForge.EVENT_BUS.register(new RenderOverlayLocation());
		
		MinecraftForge.EVENT_BUS.register(new ClientCameraHandler());
		
		// entity rendering
		// RenderingRegistry.registerEntityRenderingHandler(EntityXXX.class, RenderXXX::new);
		// RenderingRegistry.registerEntityRenderingHandler(EntityParticleBunch.class, new RenderEntityParticleBunch());
		// @TODO MC1.10 force field rendering
		/*
		RenderBlockStandard.renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(RenderBlockStandard.instance);
		
		RenderBlockForceField.renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(RenderBlockForceField.instance);
		
		RenderBlockOmnipanel.renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(RenderBlockOmnipanel.instance);
		
		RenderBlockShipScanner.renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(RenderBlockShipScanner.instance);
		
		RenderBlockTransporterBeacon.renderId = RenderingRegistry.getNextAvailableRenderId();
		RenderingRegistry.registerBlockHandler(RenderBlockTransporterBeacon.instance);
		/**/
	}
	
	@Override
	public void onModelInitialisation(final Object object) {
		if (object instanceof IBlockBase) {
			((IBlockBase) object).modelInitialisation();
			
		} else if (object instanceof IItemBase) {
			((IItemBase) object).modelInitialisation();
			
		} else {
			throw new RuntimeException(String.format("Unsupported object, expecting an IBlockBase or IItemBase instance: %s",
			                                         object));
		}
	}
	
	@Nonnull
	public static ModelResourceLocation getModelResourceLocation(final ItemStack itemStack) {
		final Item item = itemStack.getItem();
		ResourceLocation resourceLocation = item.getRegistryName();
		assert resourceLocation != null;
		
		// reuse blockstate rendering for ItemBlocks
		if (item instanceof ItemBlock) {
			final int damage = itemStack.getItemDamage();
			if (damage < 0 || damage > 15) {
				throw new IllegalArgumentException(String.format("Invalid damage %d for %s",
				                                                 damage, itemStack.getItem()));
			}
			final Block block = ((ItemBlock) item).getBlock();
			final String variant = block.getStateFromMeta(damage).toString().split("[\\[\\]]")[1];
			return new ModelResourceLocation(resourceLocation, variant);
		}
		
		// use damage value as suffix for pure items
		if (item.getHasSubtypes()) {
			resourceLocation = new ResourceLocation(resourceLocation.getNamespace(), resourceLocation.getPath() + "-" + itemStack.getItemDamage());
		}
		return new ModelResourceLocation(resourceLocation, "inventory");
	}
	
	public static void modelInitialisation(final Item item) {
		if (!(item instanceof IItemBase)) {
			throw new RuntimeException(String.format("Unable to item, expecting an IItemBase instance: %s",
			                                         item));
		}
		
		if (!item.getHasSubtypes()) {
			assert item.getRegistryName() != null;
			ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
			
		} else {
			NonNullList<ItemStack> listItemStacks = NonNullList.create();
			assert item.getCreativeTab() != null;
			item.getSubItems(item.getCreativeTab(), listItemStacks);
			for (final ItemStack itemStack : listItemStacks) {
				final ModelResourceLocation modelResourceLocation = ((IItemBase) item).getModelResourceLocation(itemStack);
				ModelLoader.setCustomModelResourceLocation(item, itemStack.getMetadata(), modelResourceLocation);
			}
		}
	}
}