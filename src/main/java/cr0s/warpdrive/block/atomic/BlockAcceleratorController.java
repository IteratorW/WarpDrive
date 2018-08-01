package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.block.BlockAbstractContainer;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumTier;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class BlockAcceleratorController extends BlockAbstractContainer {
	
	public BlockAcceleratorController(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.atomic.accelerator_controller");
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.ACTIVE, false));
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockProperties.ACTIVE);
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				       .withProperty(BlockProperties.ACTIVE, (metadata & 8) != 0);
	}
	
	@Override
	public int getMetaFromState(final IBlockState blockState) {
		return (blockState.getValue(BlockProperties.ACTIVE) ? 8 : 0);
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityAcceleratorController();
	}
}
