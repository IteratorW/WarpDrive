package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractBase.UpgradeSlot;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnumForceFieldShape;
import cr0s.warpdrive.data.EnumForceFieldState;
import cr0s.warpdrive.data.EnumTier;
import cr0s.warpdrive.item.ItemForceFieldShape;
import cr0s.warpdrive.item.ItemForceFieldUpgrade;
import cr0s.warpdrive.render.TileEntityForceFieldProjectorRenderer;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.common.property.Properties;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockForceFieldProjector extends BlockAbstractForceField {
	
	public static final PropertyBool IS_DOUBLE_SIDED = PropertyBool.create("is_double_sided");
	public static final IUnlistedProperty<EnumForceFieldShape> SHAPE = Properties.toUnlisted(PropertyEnum.create("shape", EnumForceFieldShape.class));
	public static final IUnlistedProperty<EnumForceFieldState> STATE = Properties.toUnlisted(PropertyEnum.create("state", EnumForceFieldState.class));
	
	private static final AxisAlignedBB AABB_DOWN  = new AxisAlignedBB(0.00D, 0.27D, 0.00D, 1.00D, 0.73D, 1.00D);
	private static final AxisAlignedBB AABB_UP    = new AxisAlignedBB(0.00D, 0.27D, 0.00D, 1.00D, 0.73D, 1.00D);
	private static final AxisAlignedBB AABB_NORTH = new AxisAlignedBB(0.00D, 0.00D, 0.27D, 1.00D, 1.00D, 0.73D);
	private static final AxisAlignedBB AABB_SOUTH = new AxisAlignedBB(0.00D, 0.00D, 0.27D, 1.00D, 1.00D, 0.73D);
	private static final AxisAlignedBB AABB_WEST  = new AxisAlignedBB(0.27D, 0.00D, 0.00D, 0.73D, 1.00D, 1.00D);
	private static final AxisAlignedBB AABB_EAST  = new AxisAlignedBB(0.27D, 0.00D, 0.00D, 0.73D, 1.00D, 1.00D);
	
	public BlockForceFieldProjector(final String registryName, final EnumTier enumTier) {
		super(registryName, enumTier, Material.IRON);
		
		setTranslationKey("warpdrive.force_field.projector." + enumTier.getName());
		
		setDefaultState(getDefaultState()
				                .withProperty(BlockProperties.FACING, EnumFacing.DOWN)
				                .withProperty(IS_DOUBLE_SIDED, false)
		               );
	}
	
	@Nonnull
	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this,
				new IProperty[] { BlockProperties.FACING, IS_DOUBLE_SIDED },
				new IUnlistedProperty[] { SHAPE, STATE });
	}
	
	@SuppressWarnings("deprecation")
	@Nonnull
	@Override
	public IBlockState getStateFromMeta(final int metadata) {
		return getDefaultState()
				.withProperty(BlockProperties.FACING, EnumFacing.byIndex(metadata & 0x7))
				.withProperty(IS_DOUBLE_SIDED, metadata > 7);
	}
	
	@Override
	public int getMetaFromState(@Nonnull final IBlockState blockState) {
		return blockState.getValue(BlockProperties.FACING).getIndex()
		       + (blockState.getValue(IS_DOUBLE_SIDED) ? 8 : 0);
	}
	
	@Nonnull
	@Override
	public IBlockState getExtendedState(@Nonnull final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		if (!(blockState instanceof IExtendedBlockState)) {
			return blockState;
		}
		final TileEntity tileEntity = blockAccess.getTileEntity(blockPos);
		EnumForceFieldShape forceFieldShape = EnumForceFieldShape.NONE;
		EnumForceFieldState forceFieldState = EnumForceFieldState.NOT_CONNECTED;
		if (tileEntity instanceof TileEntityForceFieldProjector) {
			final TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector) tileEntity;
			forceFieldShape = tileEntityForceFieldProjector.getShape();
			forceFieldState = tileEntityForceFieldProjector.getState();
		}
		
		return ((IExtendedBlockState) blockState)
				.withProperty(SHAPE, forceFieldShape)
				.withProperty(STATE, forceFieldState);
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void modelInitialisation() {
		super.modelInitialisation();
		
		// Bind our TESR to our tile entity
		ClientRegistry.bindTileEntitySpecialRenderer(TileEntityForceFieldProjector.class, new TileEntityForceFieldProjectorRenderer());
	}
	
	@SuppressWarnings("deprecation")
	@SideOnly(Side.CLIENT)
	@Override
	public boolean shouldSideBeRendered(@Nonnull final IBlockState blockState, @Nonnull final IBlockAccess blockAccess, @Nonnull final BlockPos blockPos, @Nonnull final EnumFacing facing) {
		return true;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isBlockNormalCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isOpaqueCube(final IBlockState blockState) {
		return false;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean isFullCube(final IBlockState blockState) {
		return false;
	}
	
	@Nonnull
	@SuppressWarnings("deprecation")
	@Override
	public AxisAlignedBB getBoundingBox(final IBlockState blockState, final IBlockAccess source, final BlockPos pos) {
		switch (blockState.getValue(BlockProperties.FACING)) {
			case DOWN : return AABB_DOWN ;
			case UP   : return AABB_UP   ;
			case NORTH: return AABB_NORTH;
			case SOUTH: return AABB_SOUTH;
			case WEST : return AABB_WEST ;
			case EAST : return AABB_EAST ;
			default   : return AABB_UP;
		}
	}
	
	@Nullable
	@Override
	public ItemBlock createItemBlock() {
		return new ItemBlockForceFieldProjector(this);
	}
	
	@Nonnull
	@Override
	public TileEntity createNewTileEntity(@Nonnull final World world, final int metadata) {
		return new TileEntityForceFieldProjector();
	}
	
	@SideOnly(Side.CLIENT)
	@Override
	public void getSubBlocks(final CreativeTabs creativeTab, final NonNullList<ItemStack> list) {
		for (int i = 0; i < 2; ++i) {
			list.add(new ItemStack(this, 1, i));
		}
	}
	
	@Override
	public int damageDropped(final IBlockState blockState) {
		return blockState.getValue(IS_DOUBLE_SIDED) ? 1 : 0;
	}
	
	@Override
	public void onBlockPlacedBy(final World world, final BlockPos blockPos, final IBlockState blockState,
	                            final EntityLivingBase entityLivingBase, final ItemStack itemStack) {
		super.onBlockPlacedBy(world, blockPos, blockState, entityLivingBase, itemStack);
		final TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector) world.getTileEntity(blockPos);
		if (!itemStack.hasTagCompound() && tileEntityForceFieldProjector != null) {
			tileEntityForceFieldProjector.isDoubleSided = (itemStack.getItemDamage() == 1);
		}
	}
	
	@Override
	public boolean onBlockActivated(final World world, final BlockPos blockPos, final IBlockState blockState,
	                                final EntityPlayer entityPlayer, final EnumHand enumHand,
	                                final EnumFacing enumFacing, final float hitX, final float hitY, final float hitZ) {
		if (world.isRemote) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		if (enumHand != EnumHand.MAIN_HAND) {
			return true;
		}
		
		// get context
		final ItemStack itemStackHeld = entityPlayer.getHeldItem(enumHand);
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (!(tileEntity instanceof TileEntityForceFieldProjector)) {
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		final TileEntityForceFieldProjector tileEntityForceFieldProjector = (TileEntityForceFieldProjector) tileEntity;
		
		final UpgradeSlot upgradeSlot = tileEntityForceFieldProjector.getUpgradeSlot(itemStackHeld);
		
		// sneaking with an empty hand or an upgrade/shape item in hand to dismount current upgrade/shape
		if (entityPlayer.isSneaking()) {
			// using an upgrade item or no shape defined means dismount upgrade, otherwise dismount shape
			if ( upgradeSlot != null
			  || (tileEntityForceFieldProjector.getShape() == EnumForceFieldShape.NONE)
			  || ( enumFacing != blockState.getValue(BlockProperties.FACING)
			    && ( !tileEntityForceFieldProjector.isDoubleSided
			      || enumFacing.getOpposite() != blockState.getValue(BlockProperties.FACING) ) ) ) {
				// user base handler
				return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
				
			} else {// default to dismount shape
				if (tileEntityForceFieldProjector.getShape() != EnumForceFieldShape.NONE) {
					if ( enumFacing == blockState.getValue(BlockProperties.FACING)
					  || ( tileEntityForceFieldProjector.isDoubleSided
					    && enumFacing.getOpposite() == blockState.getValue(BlockProperties.FACING) ) ) {
						if (!entityPlayer.capabilities.isCreativeMode) {
							// dismount the shape item(s)
							final ItemStack itemStackDrop = ItemForceFieldShape.getItemStackNoCache(tileEntityForceFieldProjector.getShape(), tileEntityForceFieldProjector.isDoubleSided ? 2 : 1);
							final EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
							entityItem.setNoPickupDelay();
							final boolean isSuccess = world.spawnEntity(entityItem);
							if (!isSuccess) {
								Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.spawn_denied",
								                                                       entityItem ));
								return true;
							}
						}
						
						tileEntityForceFieldProjector.setShape(EnumForceFieldShape.NONE);
						// shape dismounted
						Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.upgrade.result.shape_dismounted"));
						
					} else {
						// wrong side
						Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.wrong_shape_side"));
					}
					
				} else {
					// no shape to dismount
					Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.no_shape_to_dismount"));
				}
				return true;
			}
			
		} else if (itemStackHeld.isEmpty()) {// no sneaking and no item in hand => show status
			Commons.addChatMessage(entityPlayer, tileEntityForceFieldProjector.getStatus());
			return true;
			
		} else if (itemStackHeld.getItem() instanceof ItemForceFieldShape) {// no sneaking and shape in hand => mounting a shape
			if ( enumFacing == blockState.getValue(BlockProperties.FACING)
			  || ( tileEntityForceFieldProjector.isDoubleSided
			    && enumFacing.getOpposite() == blockState.getValue(BlockProperties.FACING) ) ) {
				if (!entityPlayer.capabilities.isCreativeMode) {
					// validate quantity
					if (itemStackHeld.getCount() < (tileEntityForceFieldProjector.isDoubleSided ? 2 : 1)) {
						// not enough shape items
						Commons.addChatMessage(entityPlayer, new TextComponentTranslation(
							tileEntityForceFieldProjector.isDoubleSided ?
								"warpdrive.upgrade.result.not_enough_shapes.double" : "warpdrive.upgrade.result.not_enough_shapes.single"));
						return true;
					}
					
					// update player inventory
					itemStackHeld.shrink( tileEntityForceFieldProjector.isDoubleSided ? 2 : 1 );
					
					// dismount the current shape item(s)
					if (tileEntityForceFieldProjector.getShape() != EnumForceFieldShape.NONE) {
						final ItemStack itemStackDrop = ItemForceFieldShape.getItemStackNoCache(tileEntityForceFieldProjector.getShape(), tileEntityForceFieldProjector.isDoubleSided ? 2 : 1);
						final EntityItem entityItem = new EntityItem(world, entityPlayer.posX, entityPlayer.posY + 0.5D, entityPlayer.posZ, itemStackDrop);
						entityItem.setNoPickupDelay();
						final boolean isSuccess = world.spawnEntity(entityItem);
						if (!isSuccess) {
							Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.spawn_denied",
							                                                       entityItem ));
							return true;
						}
					}
				}
				
				// mount the new shape item(s)
				tileEntityForceFieldProjector.setShape(EnumForceFieldShape.get(itemStackHeld.getItemDamage()));
				// shape mounted
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleCorrect(), "warpdrive.upgrade.result.shape_mounted"));
				
			} else {
				// wrong side
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.wrong_shape_side"));
			}
			return true;
			
		} else if (itemStackHeld.getItem() instanceof ItemForceFieldUpgrade) {// no sneaking and an upgrade in hand => mounting an upgrade
			// validate type
			if (upgradeSlot == null) {
				Commons.addChatMessage(entityPlayer, new WarpDriveText(Commons.getStyleWarning(), "warpdrive.upgrade.result.invalid_upgrade_for_projector"));
				return true;
			}
			
			// revert to common upgrade handler
			return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
		}
		
		return super.onBlockActivated(world, blockPos, blockState, entityPlayer, enumHand, enumFacing, hitX, hitY, hitZ);
	}
}
