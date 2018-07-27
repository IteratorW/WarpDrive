package cr0s.warpdrive.block.decoration;

import cr0s.warpdrive.block.BlockAbstractBase;
import cr0s.warpdrive.data.EnumTier;

import javax.annotation.Nonnull;

import java.util.Random;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockBedrockGlass extends BlockAbstractBase {
	
	public BlockBedrockGlass(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.FIRE);
		
		setHardness(0.0F);
		setBlockUnbreakable();
		setResistance(6000000.0F);
		setSoundType(SoundType.STONE);
		disableStats();
		setTranslationKey("warpdrive.decoration.bedrock_glass");
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean causesSuffocation(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullBlock(final IBlockState blockState) {
		return true;
	}
	
	@Override
	public boolean isAir(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos pos) {
		return false;
	}
	
	@Override
	public boolean isReplaceable(final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos) {
		return false;
	}
	
	@Override
	public boolean canPlaceBlockAt(final World world, @Nonnull final BlockPos blockPos) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getPushReaction(final IBlockState blockState) {
		return EnumPushReaction.BLOCK;
	}
	
	@Nonnull
	@Override
	public Item getItemDropped(final IBlockState blockState, final Random rand, final int fortune) {
		return Items.AIR;
	}
	
	@Override
	public int quantityDropped(final Random random) {
		return 0;
	}
	
	@Nonnull
	@SideOnly(Side.CLIENT)
	@Override
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT;
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, final EnumFacing facing) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected boolean canSilkHarvest() {
		return false;
	}
	
	@Override
	public boolean isCollidable() {
		return true;
	}
}