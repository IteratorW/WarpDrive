package cr0s.warpdrive.block.hull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;

public class BlockHullGlass extends BlockColored {
	public BlockHullGlass(final int tier) {
		super(Material.glass);
		setHardness(WarpDriveConfig.HULL_HARDNESS[tier - 1]);
		setResistance(WarpDriveConfig.HULL_BLAST_RESISTANCE[tier - 1] * 5 / 3);
		setStepSound(Block.soundTypeGlass);
		setCreativeTab(WarpDrive.creativeTabWarpDrive);
		setBlockName("warpdrive.hull" + tier + ".glass.");
		setBlockTextureName("warpdrive:hull/glass");
	}
	
	@Override
	public int getMobilityFlag() {
		return 2;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}
	
	@Override
	public int getRenderBlockPass() {
		return 1;
	}
	
	@Override
	public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
		if (world.isAirBlock(x, y, z)) {
			return true;
		}
		ForgeDirection direction = ForgeDirection.getOrientation(side).getOpposite();
		Block sideBlock = world.getBlock(x, y, z);
		if (sideBlock instanceof BlockGlass || sideBlock instanceof BlockHullGlass) {
			return world.getBlockMetadata(x, y, z)
				!= world.getBlockMetadata(x + direction.offsetX, y + direction.offsetY, z + direction.offsetZ);
		}
		return !world.isSideSolid(x, y, z, direction, false);
	}
	
	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}
}