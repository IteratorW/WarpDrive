package cr0s.warpdrive.data;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockBase;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.block.energy.BlockCapacitor;
import cr0s.warpdrive.block.movement.BlockShipCore;
import cr0s.warpdrive.compat.CompatForgeMultipart;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.Filler;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.Constants.NBT;

public class JumpBlock {
	
	public Block block;
	public int blockMeta;
	public WeakReference<TileEntity> weakTileEntity;
	public NBTTagCompound blockNBT;
	public int x;
	public int y;
	public int z;
	public HashMap<String, NBTBase> externals;
	
	public JumpBlock() {
	}
	
	public JumpBlock(final World world, final BlockPos blockPos, final IBlockState blockState, final TileEntity tileEntity) {
		this.block = blockState.getBlock();
		this.blockMeta = blockState.getBlock().getMetaFromState(blockState);
		if (tileEntity == null) {
			weakTileEntity = null;
			blockNBT = null;
		} else {
			weakTileEntity = new WeakReference<>(tileEntity);
			blockNBT = new NBTTagCompound();
			tileEntity.writeToNBT(blockNBT);
		}
		this.x = blockPos.getX();
		this.y = blockPos.getY();
		this.z = blockPos.getZ();
		
		// save externals
		for (final Entry<String, IBlockTransformer> entryBlockTransformer : WarpDriveConfig.blockTransformers.entrySet()) {
			if (entryBlockTransformer.getValue().isApplicable(block, blockMeta, tileEntity)) {
				final NBTBase nbtBase = entryBlockTransformer.getValue().saveExternals(world, x, y, z, block, blockMeta, tileEntity);
				// (we always save, even if null as a reminder on which transformer applies to this block)
				setExternal(entryBlockTransformer.getKey(), nbtBase);
			}
		}
	}
	
	public JumpBlock(final Filler filler, final int x, final int y, final int z) {
		if (filler.block == null) {
			WarpDrive.logger.info(String.format("Forcing glass for invalid filler with null block at (%d %d %d)", x, y, z));
			filler.block = Blocks.GLASS;
		}
		block = filler.block;
		blockMeta = filler.metadata;
		weakTileEntity = null;
		blockNBT = (filler.tagCompound != null) ? filler.tagCompound.copy() : null;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public TileEntity getTileEntity(final World worldSource) {
		if (weakTileEntity == null) {
			return null;
		}
		final TileEntity tileEntity = weakTileEntity.get();
		if (tileEntity != null) {
			return tileEntity;
		}
		WarpDrive.logger.error(String.format("Tile entity lost in %s",
		                                     this));
		return worldSource.getTileEntity(new BlockPos(x, y, z));
	}
	
	private NBTTagCompound getBlockNBT() {
		if (weakTileEntity == null) {
			return blockNBT == null ? null : blockNBT.copy();
		}
		final TileEntity tileEntity = weakTileEntity.get();
		if (tileEntity != null) {
			final NBTTagCompound tagCompound = new NBTTagCompound();
			tileEntity.writeToNBT(tagCompound);
			return tagCompound;
		}
		WarpDrive.logger.error(String.format("Tile entity lost in %s",
		                                     this));
		return blockNBT == null ? null : blockNBT.copy();
	}
	
	public NBTBase getExternal(final String modId) {
		if (externals == null) {
			return null;
		}
		final NBTBase nbtExternal = externals.get(modId);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Returning %s externals at (%d %d %d) %s",
			                                    modId, x, y, z, nbtExternal));
		}
		if (nbtExternal == null) {
			return null;
		}
		return nbtExternal.copy();
	}
	
	private void setExternal(final String modId, final NBTBase nbtExternal) {
		if (externals == null) {
			externals = new HashMap<>();
		}
		externals.put(modId, nbtExternal);
		if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
			WarpDrive.logger.info(String.format("Saved %s externals at (%d %d %d) %s",
			                                    modId, x, y, z, nbtExternal));
		}
	}
	
	private static final byte[] mrotNone           = {  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotRail           = {  1,  0,  5,  4,  2,  3,  7,  8,  9,  6, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotAnvil          = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotFenceGate      = {  1,  0,  2,  3,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotPumpkin        = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Tripwire hook, Pumpkin, Jack-o-lantern
	private static final byte[] mrotEndPortalFrame = {  1,  2,  3,  0,  5,  6,  7,  4,  8,  9, 10, 11, 12, 13, 14, 15 };	// EndPortal, doors (open/closed, base/top)
	private static final byte[] mrotCocoa          = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotRepeater       = {  1,  2,  3,  0,  5,  6,  7,  4,  9, 10, 11,  8, 13, 14, 15, 12 };	// Repeater (normal/lit), Comparator
	private static final byte[] mrotBed            = {  1,  2,  3,  0,  4,  5,  6,  7,  9, 10, 11,  8, 12, 13, 14, 15 };
	private static final byte[] mrotStair          = {  2,  3,  1,  0,  6,  7,  5,  4,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotSign           = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };	// Sign, Skull
	private static final byte[] mrotTrapDoor       = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13 };
	private static final byte[] mrotLever          = {  7,  3,  4,  2,  1,  6,  5,  0, 15, 11, 12, 10,  9, 14, 13,  8 };
	private static final byte[] mrotNetherPortal   = {  0,  2,  1,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final byte[] mrotVine           = {  0,  2,  4,  6,  8, 10, 12, 14,  1,  3,  5,  7,  9, 11, 13, 15 };
	private static final byte[] mrotButton         = {  0,  3,  4,  2,  1,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Button, torch (normal, redstone lit/unlit)
	private static final byte[] mrotMushroom       = {  0,  3,  6,  9,  2,  5,  8,  1,  4,  7, 10, 11, 12, 13, 14, 15 };	// Red/brown mushroom block
	private static final byte[] mrotForgeDirection = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };	// Furnace (lit/normal), Dispenser/Dropper, Enderchest, Chest (normal/trapped), Hopper, Ladder, Wall sign
	private static final byte[] mrotPiston         = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 13, 12, 10, 11, 14, 15 };	// Pistons (sticky/normal, base/head)
	private static final byte[] mrotWoodLog        = {  0,  1,  2,  3,  8,  9, 10, 11,  4,  5,  6,  7, 12, 13, 14, 15 };
	
	// Return updated metadata from rotating a vanilla block
	private int getMetadataRotation(final NBTTagCompound nbtTileEntity, final byte rotationSteps) {
		if (rotationSteps == 0) {
			return blockMeta;
		}
		
		byte[] mrot = mrotNone;
		if (block instanceof BlockRailBase) {
			mrot = mrotRail;
		} else if (block instanceof BlockAnvil) {
			mrot = mrotAnvil;
		} else if (block instanceof BlockFenceGate) {
			mrot = mrotFenceGate;
		} else if (block instanceof BlockPumpkin || block instanceof BlockTripWireHook) {
			mrot = mrotPumpkin;
		} else if (block instanceof BlockEndPortalFrame || block instanceof BlockDoor) {
			mrot = mrotEndPortalFrame;
		} else if (block instanceof BlockCocoa) {
			mrot = mrotCocoa;
		} else if (block instanceof BlockRedstoneDiode) {
			mrot = mrotRepeater;
		} else if (block instanceof BlockBed) {
			mrot = mrotBed;
		} else if (block instanceof BlockStairs) {
			mrot = mrotStair;
		} else if (block instanceof BlockSign) {
			if (block == Blocks.WALL_SIGN) {
				mrot = mrotForgeDirection;
			} else {
				mrot = mrotSign;
			}
		} else if (block instanceof BlockTrapDoor) {
			mrot = mrotTrapDoor;
		} else if (block instanceof BlockLever) {
			mrot = mrotLever;
		} else if (block instanceof BlockPortal) {
			mrot = mrotNetherPortal;
		} else if (block instanceof BlockVine) {
			mrot = mrotVine;
		} else if (block instanceof BlockButton || block instanceof BlockTorch) {
			mrot = mrotButton;
		} else if (block instanceof BlockHugeMushroom) {
			mrot = mrotMushroom;
		} else if (block instanceof BlockFurnace || block instanceof BlockDispenser || block instanceof BlockHopper
				|| block instanceof BlockChest || block instanceof BlockEnderChest || block instanceof BlockLadder) {
			mrot = mrotForgeDirection;
		} else if (block instanceof BlockPistonBase || block instanceof BlockPistonExtension || block instanceof BlockPistonMoving) {
			mrot = mrotPiston;
		} else if (block instanceof BlockLog) {
			mrot = mrotWoodLog;
		} else if (block instanceof BlockSkull) {
			// mrot = mrotNone;
			final short facing = nbtTileEntity.getShort("Rot");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("Rot", mrotSign[facing]);
				break;
			case 2:
				nbtTileEntity.setShort("Rot", mrotSign[mrotSign[facing]]);
				break;
			case 3:
				nbtTileEntity.setShort("Rot", mrotSign[mrotSign[mrotSign[facing]]]);
				break;
			default:
				break;
			}
		}
		
		switch (rotationSteps) {
		case 1:
			return mrot[blockMeta];
		case 2:
			return mrot[mrot[blockMeta]];
		case 3:
			return mrot[mrot[mrot[blockMeta]]];
		default:
			return blockMeta;
		}
	}
	
	public BlockPos deploy(final World targetWorld, final ITransformation transformation) {
		try {
			final NBTTagCompound nbtToDeploy = getBlockNBT();
			int newBlockMeta = blockMeta;
			if (externals != null) {
				for (final Entry<String, NBTBase> external : externals.entrySet()) {
					final IBlockTransformer blockTransformer = WarpDriveConfig.blockTransformers.get(external.getKey());
					if (blockTransformer != null) {
						newBlockMeta = blockTransformer.rotate(block, blockMeta, nbtToDeploy, transformation);
					}
				}
			} else {
				newBlockMeta = getMetadataRotation(nbtToDeploy, transformation.getRotationSteps());
			}
			final BlockPos target = transformation.apply(x, y, z);
			final IBlockState blockState = block.getStateFromMeta(newBlockMeta);
			setBlockNoLight(targetWorld, target, blockState, 2);
			
			// Re-schedule air blocks update
			if (block == WarpDrive.blockAir) {
				targetWorld.notifyBlockUpdate(target, blockState, blockState, 3);
				targetWorld.scheduleBlockUpdate(target, block, 40 + targetWorld.rand.nextInt(20), 0);
			}
			
			if (nbtToDeploy != null) {
				nbtToDeploy.setInteger("x", target.getX());
				nbtToDeploy.setInteger("y", target.getY());
				nbtToDeploy.setInteger("z", target.getZ());
				
				if (nbtToDeploy.hasKey("mainX") && nbtToDeploy.hasKey("mainY") && nbtToDeploy.hasKey("mainZ")) {// Mekanism 6.0.4.44
					if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
						WarpDrive.logger.info(String.format("%s deploy: TileEntity has mainXYZ", this));
					}
					final BlockPos mainTarget = transformation.apply(nbtToDeploy.getInteger("mainX"), nbtToDeploy.getInteger("mainY"), nbtToDeploy.getInteger("mainZ"));
					nbtToDeploy.setInteger("mainX", mainTarget.getX());
					nbtToDeploy.setInteger("mainY", mainTarget.getY());
					nbtToDeploy.setInteger("mainZ", mainTarget.getZ());
				}
				
				if (nbtToDeploy.hasKey("screenData")) {// IC2NuclearControl 2.2.5a
					final NBTTagCompound nbtScreenData = nbtToDeploy.getCompoundTag("screenData");
					if ( nbtScreenData.hasKey("minX") && nbtScreenData.hasKey("minY") && nbtScreenData.hasKey("minZ")
					  && nbtScreenData.hasKey("maxX") && nbtScreenData.hasKey("maxY") && nbtScreenData.hasKey("maxZ")) {
						if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
							WarpDrive.logger.info(String.format("%s deploy: TileEntity has screenData.min/maxXYZ", this));
						}
						final BlockPos minTarget = transformation.apply(nbtScreenData.getInteger("minX"), nbtScreenData.getInteger("minY"), nbtScreenData.getInteger("minZ"));
						nbtScreenData.setInteger("minX", minTarget.getX());
						nbtScreenData.setInteger("minY", minTarget.getY());
						nbtScreenData.setInteger("minZ", minTarget.getZ());
						final BlockPos maxTarget = transformation.apply(nbtScreenData.getInteger("maxX"), nbtScreenData.getInteger("maxY"), nbtScreenData.getInteger("maxZ"));
						nbtScreenData.setInteger("maxX", maxTarget.getX());
						nbtScreenData.setInteger("maxY", maxTarget.getY());
						nbtScreenData.setInteger("maxZ", maxTarget.getZ());
						nbtToDeploy.setTag("screenData", nbtScreenData);
					}
				}
				
				if (nbtToDeploy.hasKey("hasValidBubble")) {// Galacticraft 3.0.11.333
					nbtToDeploy.setBoolean("hasValidBubble", false);
					// old bubble will die naturally due to missing tile entity, new one will be spawned
				}
				
				TileEntity newTileEntity = null;
				boolean isForgeMultipart = false;
				if (WarpDriveConfig.isForgeMultipartLoaded && nbtToDeploy.hasKey("id") && nbtToDeploy.getString("id").equals("savedMultipart")) {
					isForgeMultipart = true;
					newTileEntity = (TileEntity) CompatForgeMultipart.methodMultipartHelper_createTileFromNBT.invoke(null, targetWorld, nbtToDeploy);
					
				}
				
				if (newTileEntity == null) {
					newTileEntity = TileEntity.create(targetWorld, nbtToDeploy);
					if (newTileEntity == null) {
						WarpDrive.logger.error(String.format("%s deploy failed to create new tile entity %s block %s:%d",
						                                     this, Commons.format(targetWorld, x, y, z), block, blockMeta));
						WarpDrive.logger.error(String.format("NBT data was %s",
						                                     nbtToDeploy));
					}
				}
				
				if ( newTileEntity != null
				  && ( block == WarpDriveConfig.CC_Computer
				    || block == WarpDriveConfig.CC_peripheral
				    || block == WarpDriveConfig.CCT_Turtle
				    || block == WarpDriveConfig.CCT_Expanded
				    || block == WarpDriveConfig.CCT_Advanced ) ) {
					newTileEntity.invalidate();
				}
				
				if (newTileEntity != null) {
					newTileEntity.setWorld(targetWorld);
					newTileEntity.validate();
					
					targetWorld.setTileEntity(target, newTileEntity);
					if (isForgeMultipart) {
						CompatForgeMultipart.methodTileMultipart_onChunkLoad.invoke(newTileEntity);
						CompatForgeMultipart.methodMultipartHelper_sendDescPacket.invoke(null, targetWorld, newTileEntity);
					}
					
					newTileEntity.markDirty();
				}
			}
			return target;
			
		} catch (final Exception exception) {
			exception.printStackTrace();
			String coordinates;
			try {
				coordinates = " at " + x + " " + y + " " + z + " blockId " + block + ":" + blockMeta;
			} catch (final Exception dropMe) {
				coordinates = " (unknown coordinates)";
			}
			WarpDrive.logger.error(String.format("moveBlockSimple exception %s", coordinates));
		}
		return null;
	}
	
	public static void refreshBlockStateOnClient(final World world, final BlockPos blockPos) {
		final TileEntity tileEntity = world.getTileEntity(blockPos);
		if (tileEntity != null) {
			final Class<?> teClass = tileEntity.getClass();
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("Refreshing clients %s with %s derived from %s",
				                                    Commons.format(world, blockPos),
				                                    teClass,
				                                    teClass.getSuperclass()));
			}
			try {
				final String superClassName = teClass.getSuperclass().getName();
				final boolean isIC2 = superClassName.contains("ic2.core.block");
				if (isIC2 || superClassName.contains("advsolar.common.tiles")) {// IC2
					final Method onUnloaded = teClass.getMethod("onUnloaded");
					final Method onLoaded = teClass.getMethod("onLoaded");
					if (onUnloaded != null && onLoaded != null) {
						onUnloaded.invoke(tileEntity);
						onLoaded.invoke(tileEntity);
					} else {
						WarpDrive.logger.error(String.format("Missing IC2 (un)loaded events for TileEntity %s %s. Please report this issue!",
						                                     teClass.getName(),
						                                     Commons.format(world, blockPos)));
					}
					
					tileEntity.updateContainingBlockInfo();
				}
				
				if (isIC2) {// IC2
					// required in SSP during same dimension jump to update client with rotation data
					if (teClass.getName().equals("ic2.core.block.wiring.TileEntityCable")) {
						NetworkHelper_updateTileEntityField(tileEntity, "color");
						NetworkHelper_updateTileEntityField(tileEntity, "foamColor");
						NetworkHelper_updateTileEntityField(tileEntity, "foamed");
					} else {
						NetworkHelper_updateTileEntityField(tileEntity, "active");
						NetworkHelper_updateTileEntityField(tileEntity, "facing");
						if (teClass.getName().equals("ic2.core.block.reactor.TileEntityNuclearReactorElectric")) {
							NetworkHelper_updateTileEntityField(tileEntity, "heat");	// not working, probably an IC2 bug here...
						}
						// not needed: if ic2.core.block.machine.tileentity.TileEntityMatter then updated "state"
					}
				} else {// IC2 extensions without network optimization (transferring all fields) 
					try {
						final Method getNetworkedFields = teClass.getMethod("getNetworkedFields");
						@SuppressWarnings("unchecked")
						final List<String> fields = (List<String>) getNetworkedFields.invoke(tileEntity);
						if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
							WarpDrive.logger.info(String.format("Tile has %d networked fields: %s",
							                                    fields.size(), fields));
						}
						for (final String field : fields) {
							NetworkHelper_updateTileEntityField(tileEntity, field);
						}
					} catch (final NoSuchMethodException exception) {
						// WarpDrive.logger.info("Tile has no getNetworkedFields method");
					} catch (final NoClassDefFoundError exception) {
						if (WarpDriveConfig.LOGGING_JUMP) {
							WarpDrive.logger.info(String.format("TileEntity %s %s is missing a class definition",
							                                    teClass.getName(), Commons.format(world, blockPos)));
							if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
								exception.printStackTrace();
							}
						}
					}
				}
			} catch (final Exception exception) {
				WarpDrive.logger.info(String.format("Exception involving TileEntity %s %s",
				                                    teClass.getName(), Commons.format(world, blockPos)));
				exception.printStackTrace();
			}
		}
	}
	
	public void readFromNBT(final NBTTagCompound tagCompound) {
		block = Block.getBlockFromName(tagCompound.getString("block"));
		if (block == null) {
			if (WarpDriveConfig.LOGGING_BUILDING) {
				WarpDrive.logger.warn(String.format("Ignoring unknown block %s from tag %s",
				                                    tagCompound.getString("block"), tagCompound));
			}
			block = Blocks.AIR;
			return;
		}
		blockMeta = tagCompound.getByte("blockMeta");
		weakTileEntity = null;
		if (tagCompound.hasKey("blockNBT")) {
			blockNBT = tagCompound.getCompoundTag("blockNBT");
			
			// Clear computer IDs
			if (blockNBT.hasKey("computerID")) {
				blockNBT.removeTag("computerID");
			}
			if (blockNBT.hasKey("oc:computer")) {
				final NBTTagCompound tagComputer = blockNBT.getCompoundTag("oc:computer");
				tagComputer.removeTag("components");
				tagComputer.removeTag("node");
				blockNBT.setTag("oc:computer", tagComputer);
			}
		} else {
			blockNBT = null;
		}
		x = tagCompound.getInteger("x");
		y = tagCompound.getInteger("y");
		z = tagCompound.getInteger("z");
		if (tagCompound.hasKey("externals")) {
			final NBTTagCompound tagCompoundExternals = tagCompound.getCompoundTag("externals");
			externals = new HashMap<>();
			for (final Object key : tagCompoundExternals.getKeySet()) {
				assert key instanceof String;
				externals.put((String) key, tagCompoundExternals.getTag((String) key));
			}
		} else {
			externals = null;
		}
	}
	
	public void writeToNBT(final NBTTagCompound tagCompound) {
		tagCompound.setString("block", Block.REGISTRY.getNameForObject(block).toString());
		tagCompound.setByte("blockMeta", (byte) blockMeta);
		final NBTTagCompound nbtTileEntity = getBlockNBT();
		if (nbtTileEntity != null) {
			tagCompound.setTag("blockNBT", nbtTileEntity);
		}
		tagCompound.setInteger("x", x);
		tagCompound.setInteger("y", y);
		tagCompound.setInteger("z", z);
		if (externals != null && !externals.isEmpty()) {
			final NBTTagCompound tagCompoundExternals = new NBTTagCompound();
			for (final Entry<String, NBTBase> entry : externals.entrySet()) {
				if (entry.getValue() == null) {
					tagCompoundExternals.setString(entry.getKey(), "");
				} else {
					tagCompoundExternals.setTag(entry.getKey(), entry.getValue());
				}
			}
			tagCompound.setTag("externals", tagCompoundExternals);
		}
	}
	
	public void removeUniqueIDs() {
		removeUniqueIDs(blockNBT);
	}
	
	public static void removeUniqueIDs(final NBTTagCompound tagCompound) {
		if (tagCompound == null) {
			return;
		}
		
		// ComputerCraft computer
		if (tagCompound.hasKey("computerID")) {
			tagCompound.removeTag("computerID");
			tagCompound.removeTag("label");
		}
		
		// WarpDrive UUID
		if (tagCompound.hasKey("uuidMost")) {
			tagCompound.removeTag("uuidMost");
			tagCompound.removeTag("uuidLeast");
		}
		
		// WarpDrive any OC connected tile
		if (tagCompound.hasKey("oc:node")) {
			tagCompound.removeTag("oc:node");
		}
		
		// OpenComputers case
		if (tagCompound.hasKey("oc:computer")) {
			final NBTTagCompound tagComputer = tagCompound.getCompoundTag("oc:computer");
			tagComputer.removeTag("chunkX");
			tagComputer.removeTag("chunkZ");
			tagComputer.removeTag("components");
			tagComputer.removeTag("dimension");
			tagComputer.removeTag("node");
			tagCompound.setTag("oc:computer", tagComputer);
		}
		
		// OpenComputers case
		if (tagCompound.hasKey("oc:items")) {
			final NBTTagList tagListItems = tagCompound.getTagList("oc:items", Constants.NBT.TAG_COMPOUND);
			for (int indexItemSlot = 0; indexItemSlot < tagListItems.tagCount(); indexItemSlot++) {
				final NBTTagCompound tagCompoundItemSlot = tagListItems.getCompoundTagAt(indexItemSlot);
				final NBTTagCompound tagCompoundItem = tagCompoundItemSlot.getCompoundTag("item");
				final NBTTagCompound tagCompoundTag = tagCompoundItem.getCompoundTag("tag");
				final NBTTagCompound tagCompoundOCData = tagCompoundTag.getCompoundTag("oc:data");
				final NBTTagCompound tagCompoundNode = tagCompoundOCData.getCompoundTag("node");
				if (tagCompoundNode.hasKey("address")) {
					tagCompoundNode.removeTag("address");
				}
			}
		}
		
		// OpenComputers keyboard
		if (tagCompound.hasKey("oc:keyboard")) {
			final NBTTagCompound tagCompoundKeyboard = tagCompound.getCompoundTag("oc:keyboard");
			tagCompoundKeyboard.removeTag("node");
		}
		
		// OpenComputers screen
		if (tagCompound.hasKey("oc:hasPower")) {
			tagCompound.removeTag("node");
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.hasKey("Owner")) {
			tagCompound.setString("Owner", "None");
		}
		
		// Mekanism
		if (tagCompound.hasKey("owner")) {
			tagCompound.setString("owner", "None");
		}
	}
	
	public static void emptyEnergyStorage(final NBTTagCompound tagCompound) {
		// BuildCraft
		if (tagCompound.hasKey("battery", NBT.TAG_COMPOUND)) {
			final NBTTagCompound tagCompoundBattery = tagCompound.getCompoundTag("battery");
			if (tagCompoundBattery.hasKey("energy", NBT.TAG_INT)) {
				tagCompoundBattery.setInteger("energy", 0);
			}
		}
		
		// Gregtech
		if (tagCompound.hasKey("mStoredEnergy", NBT.TAG_INT)) {
			tagCompound.setInteger("mStoredEnergy", 0);
		}
		
		// IC2
		if (tagCompound.hasKey("energy", NBT.TAG_DOUBLE)) {
			// energy_consume((int)Math.round(blockNBT.getDouble("energy")), true);
			tagCompound.setDouble("energy", 0);
		}
		
		// Immersive Engineering & Thermal Expansion
		if (tagCompound.hasKey("Energy", NBT.TAG_INT)) {
			// energy_consume(blockNBT.getInteger("Energy"), true);
			tagCompound.setInteger("Energy", 0);
		}
		
		// Mekanism
		if (tagCompound.hasKey("electricityStored", NBT.TAG_DOUBLE)) {
			tagCompound.setDouble("electricityStored", 0);
		}
		
		// WarpDrive
		if (tagCompound.hasKey("energy", NBT.TAG_LONG)) {
			tagCompound.setLong("energy", 0L);
		}
	}
	
	public void fillEnergyStorage() {
		if (block instanceof IBlockBase) {
			final EnumTier enumTier = ((IBlockBase) block).getTier(null);
			if (enumTier != EnumTier.CREATIVE) {
				if (block instanceof BlockShipCore) {
					blockNBT.setLong("energy", WarpDriveConfig.SHIP_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
				if (block instanceof BlockCapacitor) {
					blockNBT.setLong("energy", WarpDriveConfig.CAPACITOR_MAX_ENERGY_STORED_BY_TIER[enumTier.getIndex()]);
				}
			}
		}
	}
	
	// IC2 support for updating tile entity fields
	private static Object NetworkManager_instance;
	private static Method NetworkManager_updateTileEntityField;
	
	private static void NetworkHelper_init() {
		try {
			NetworkManager_updateTileEntityField = Class.forName("ic2.core.network.NetworkManager").getMethod("updateTileEntityField", TileEntity.class, String.class);
			
			NetworkManager_instance = Class.forName("ic2.core.IC2").getDeclaredField("network").get(null);
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	
	private static void NetworkHelper_updateTileEntityField(final TileEntity tileEntity, final String field) {
		try {
			if (NetworkManager_instance == null) {
				NetworkHelper_init();
			}
			NetworkManager_updateTileEntityField.invoke(NetworkManager_instance, tileEntity, field);
		} catch (final Exception exception) {
			throw new RuntimeException(exception);
		}
	}
	// IC2 support ends here
	
	@Override
	public String toString() {
		return String.format("%s @ (%d %d %d) %s:%d %s nbt %s",
		                     getClass().getSimpleName(),
		                     x, y, z,
		                     block.getRegistryName(), blockMeta,
		                     weakTileEntity == null ? null : weakTileEntity.get(),
		                     blockNBT);
	}
	
	// This code is a straight copy from Vanilla net.minecraft.world.World.setBlock to remove lighting computations
	public static boolean setBlockNoLight(final World w, final BlockPos blockPos, final IBlockState blockState, final int flags) {
		return w.setBlockState(blockPos, blockState, flags);
		/*
		// x, y, z -> blockPos
		// par6 -> flags
		if (x >= -30000000 && z >= -30000000 && x < 30000000 && z < 30000000) {
			if (y < 0) {
				return false;
			} else if (y >= 256) {
				return false;
			} else {
				final Chunk chunk = w.getChunkFromChunkCoords(x >> 4, z >> 4);
				Block block1 = null;
				// net.minecraftforge.common.util.BlockSnapshot blockSnapshot = null;
				
				if ((par6 & 1) != 0) {
					block1 = chunk.getBlock(x & 15, y, z & 15);
				}
				
				// Disable rollback on item use
				// if (w.captureBlockSnapshots && !w.isRemote) {
				// 	blockSnapshot = net.minecraftforge.common.util.BlockSnapshot.getBlockSnapshot(w, x, y, z, par6);
				// 	w.capturedBlockSnapshots.add(blockSnapshot);
				// }
				
				final boolean flag = myChunkSBIDWMT(chunk, x & 15, y, z & 15, block, blockMeta);
				
				// Disable rollback on item use
				// if (!flag && blockSnapshot != null) {
				//	w.capturedBlockSnapshots.remove(blockSnapshot);
				//	blockSnapshot = null;
				// }
				
				// Remove light computations
				// w.theProfiler.startSection("checkLight");
				// w.func_147451_t(x, y, z);
				// w.theProfiler.endSection();
				
				// Disable rollback on item use
				// if (flag && blockSnapshot == null) {// Don't notify clients or update physics while capturing blockstates
					// Modularize client and physic updates
					// w.markAndNotifyBlock(x, y, z, chunk, block1, block, par6);
				// }
				if (flag) {
					w.markAndNotifyBlock(x, y, z, chunk, block1, block, par6);
				}
				return flag;
			}
		} else {
			return false;
		}
		/**/
	}
	/*
	// This code is a straight copy from Vanilla net.minecraft.world.Chunk.func_150807_a to remove lighting computations
	private static boolean myChunkSBIDWMT(final Chunk c, final int x, final int y, final int z, final Block block, final int blockMeta) {
		final int i1 = z << 4 | x;
		
		if (y >= c.precipitationHeightMap[i1] - 1) {
			c.precipitationHeightMap[i1] = -999;
		}
		
		// Removed light recalculations
		// int j1 = c.heightMap[i1];
		final Block block1 = c.getBlock(x, y, z);
		final int k1 = c.getBlockMetadata(x, y, z);
		
		if (block1 == block && k1 == blockMeta) {
			return false;
		} else {
			final ExtendedBlockStorage[] storageArrays = c.getBlockStorageArray();
			ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];
			// Removed light recalculations
			// boolean flag = false;
			
			if (extendedblockstorage == null) {
				if (block == Blocks.air) {
					return false;
				}
				
				extendedblockstorage = storageArrays[y >> 4] = new ExtendedBlockStorage(y >> 4 << 4, !c.world.provider.hasNoSky);
				// Removed light recalculations
				// flag = y >= j1;
			}
			
			final int l1 = c.xPosition * 16 + x;
			final int i2 = c.zPosition * 16 + z;
			
			// Removed light recalculations
			// int k2 = block1.getLightOpacity(c.world, l1, y, i2);
			
			// Removed preDestroy event
			// if (!c.world.isRemote) {
			// 	block1.onBlockPreDestroy(c.world, l1, y, i2, k1);
			// }
			
			extendedblockstorage.func_150818_a(x, y & 15, z, block);
			extendedblockstorage.setExtBlockMetadata(x, y & 15, z, blockMeta); // This line duplicates the one below, so breakBlock fires with valid worldstate
			
			// Skip air at destination
			if (block1 != Blocks.air) {
				if (!c.world.isRemote) {
					block1.breakBlock(c.world, l1, y, i2, block1, k1);
					// After breakBlock a phantom TE might have been created with incorrect meta. This attempts to kill that phantom TE so the normal one can be created properly later
					final TileEntity te = c.getTileEntityUnsafe(x & 0x0F, y, z & 0x0F);
					if (te != null && te.shouldRefresh(block1, c.getBlock(x & 0x0F, y, z & 0x0F), k1, c.getBlockMetadata(x & 0x0F, y, z & 0x0F), c.world, l1, y, i2)) {
						c.removeTileEntity(x & 0x0F, y, z & 0x0F);
					}
				} else if (block1.hasTileEntity(k1)) {
					final TileEntity te = c.getTileEntityUnsafe(x & 0x0F, y, z & 0x0F);
					if (te != null && te.shouldRefresh(block1, block, k1, blockMeta, c.world, l1, y, i2)) {
						c.world.removeTileEntity(l1, y, i2);
					}
				}
			}
			
			if (extendedblockstorage.getBlockByExtId(x, y & 15, z) != block) {
				return false;
			} else {
				extendedblockstorage.setExtBlockMetadata(x, y & 15, z, blockMeta);
				// Removed light recalculations
				/*
				if (flag) {
					c.generateSkylightMap();
				} else {
					int j2 = block.getLightOpacity(c.world, l1, y, i2);
	
					if (j2 > 0) {
						if (y >= j1) {
							c.relightBlock(x, y + 1, z);
						}
					} else if (y == j1 - 1) {
						c.relightBlock(x, y, z);
					}
	
					if (j2 != k2 && (j2 < k2 || c.getSavedLightValue(EnumSkyBlock.Sky, x, y, z) > 0 || c.getSavedLightValue(EnumSkyBlock.Block, x, y, z) > 0)) {
						c.propagateSkylightOcclusion(x, z);
					}
				}
				/**/
				/*
				final TileEntity tileentity;
				
				// Removed onBlockAdded event
				// if (!c.world.isRemote) {
				//	block.onBlockAdded(c.world, l1, y, i2);
				// }
				
				// Skip air at destination
				if (block1 != Blocks.air) {
					if (block.hasTileEntity(blockMeta)) {
						tileentity = c.func_150806_e(x, y, z);
						
						if (tileentity != null) {
							tileentity.updateContainingBlockInfo();
							tileentity.blockMetadata = blockMeta;
						}
					}
				}
				
				c.isModified = true;
				return true;
			}
		}
	}
	/**/
}
