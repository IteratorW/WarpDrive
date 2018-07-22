package cr0s.warpdrive.block.hull;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IDamageReceiver;
import cr0s.warpdrive.client.ClientProxy;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.data.Vector3;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockHullStairs extends BlockStairs implements IBlockBase, IDamageReceiver {
	
	protected final EnumTier enumTier;
	private final IBlockState blockStateHull;
	
	public BlockHullStairs(final String registryName, final EnumTier enumTier, final IBlockState blockStateHull) {
		super(blockStateHull);
		
		this.blockStateHull = blockStateHull;
		this.enumTier = enumTier;
		setCreativeTab(WarpDrive.creativeTabHull);
		setUnlocalizedName("warpdrive.hull." + enumTier.getName() + ".stairs." + EnumDyeColor.byMetadata(blockStateHull.getBlock().getMetaFromState(blockStateHull)).getUnlocalizedName());
		setRegistryName(registryName);
		WarpDrive.register(this, new ItemBlockHull(this));
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public EnumPushReaction getMobilityFlag(final IBlockState state) {
		return EnumPushReaction.BLOCK;
	}
	
	@Nonnull
	@Override
	public EnumTier getTier(final ItemStack itemStack) {
		return enumTier;
	}
	
	@Override
	public EnumRarity getRarity(final ItemStack itemStack) {
		return enumTier.getRarity();
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockHull(this);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		final Item item = Item.getItemFromBlock(this);
		ClientProxy.modelInitialisation(item);
	}
	
	@Override
	public float getBlockHardness(final IBlockState blockState, final World world, final BlockPos blockPos,
	                              final DamageSource damageSource, final int damageParameter, final Vector3 damageDirection, final int damageLevel) {
		// TODO: adjust hardness to damage type/color
		return WarpDriveConfig.HULL_HARDNESS[enumTier.getIndex()];
	}
	
	@Override
	public int applyDamage(final IBlockState blockState, final World world, final BlockPos blockPos,
	                       final DamageSource damageSource, final int damageParameter, final Vector3 damageDirection, final int damageLevel) {
		if (damageLevel <= 0) {
			return 0;
		}
		if (enumTier == EnumTier.BASIC) {
			world.setBlockToAir(blockPos);
		} else {
			world.setBlockState(blockPos, WarpDrive.blockHulls_stairs[enumTier.getIndex() - 1][blockStateHull.getBlock().getMetaFromState(blockStateHull)]
			                              .getDefaultState()
			                              .withProperty(FACING, blockState.getValue(FACING)), 2);
		}
		return 0;
	}
}
