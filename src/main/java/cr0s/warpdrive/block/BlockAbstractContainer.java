package cr0s.warpdrive.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockUpdateDetector;

public abstract class BlockAbstractContainer extends BlockContainer {
	protected BlockAbstractContainer(Material material) {
		super(material);
		setHardness(5.0F);
		setResistance(6.0F * 5 / 3);
		setStepSound(Block.soundTypeMetal);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
	}
	
	@Override
	public void onBlockAdded(World world, int x, int y, int z) {
		super.onBlockAdded(world, x, y, z);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).updatedNeighbours();
		}
	}
	
	// FIXME untested
	/*
	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityPlayer, int side, float hitX, float hitY, float hitZ) {
		if (world.isRemote) {
			return false;
		}
		
		boolean hasResponse = false;
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof IUpgradable) {
			IUpgradable upgradable = (IUpgradable) tileEntity;
			ItemStack itemStack = entityPlayer.inventory.getCurrentItem();
			if (itemStack != null) {
				Item i = itemStack.getItem();
				if (i instanceof ItemWarpUpgrade) {
					if (upgradable.takeUpgrade(EnumUpgradeTypes.values()[itemStack.getItemDamage()], false)) {
						if (!entityPlayer.capabilities.isCreativeMode)
							entityPlayer.inventory.decrStackSize(entityPlayer.inventory.currentItem, 1);
						entityPlayer.addChatMessage("Upgrade accepted");
					} else {
						entityPlayer.addChatMessage("Upgrade declined");
					}
					hasResponse = true;
				}
			}
		}
		
		return hasResponse;
	}
	/**/
	
	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
		super.onNeighborBlockChange(world, x, y, z, block);
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (tileEntity instanceof IBlockUpdateDetector) {
			((IBlockUpdateDetector) tileEntity).updatedNeighbours();
		}
	}
}
