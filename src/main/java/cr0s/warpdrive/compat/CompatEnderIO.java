package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants;

public class CompatEnderIO implements IBlockTransformer {
	
	private static Class<?> classTileEntityEIO;
	private static Class<?> classBlockReservoir;
	
	public static void register() {
		try {
			classTileEntityEIO = Class.forName("crazypants.enderio.base.TileEntityEio");
			classBlockReservoir = Class.forName("crazypants.enderio.machines.machine.reservoir.BlockReservoirBase");
			WarpDriveConfig.registerBlockTransformer("enderio", new CompatEnderIO());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classTileEntityEIO.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final World world, final int x, final int y, final int z,
	                             final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final Block block, final int blockMeta, final TileEntity tileEntity) {
		// nothing to do
	}
	
	private static final short[] mrot = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final Map<String, String> rotSideNames;
	private static final Map<String, String> rotFaceNames;
	static {
		Map<String, String> map = new HashMap<>();
		map.put("EAST", "SOUTH");
		map.put("SOUTH", "WEST");
		map.put("WEST", "NORTH");
		map.put("NORTH", "EAST");
		rotSideNames = Collections.unmodifiableMap(map);
		map = new HashMap<>();
		map.put("face2", "face5");
		map.put("face5", "face3");
		map.put("face3", "face4");
		map.put("face4", "face2");
		map.put("faceDisplay2", "faceDisplay5");
		map.put("faceDisplay5", "faceDisplay3");
		map.put("faceDisplay3", "faceDisplay4");
		map.put("faceDisplay4", "faceDisplay2");
		rotFaceNames = Collections.unmodifiableMap(map);
	}
	private static final short[] rotFront         = {  0,  1,  5,  3,  4,  2,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] rotRight         = {  0,  1,  4,  3,  2,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final short[] rotPosHorizontal = {  1,  3,  0,  2,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	private byte[] rotate_byteArray(final byte rotationSteps, final byte[] data) {
		final byte[] newData = data.clone();
		for (int index = 0; index < data.length; index++) {
			switch (rotationSteps) {
			case 1:
				newData[mrot[index]] = data[index];
				break;
			case 2:
				newData[mrot[mrot[index]]] = data[index];
				break;
			case 3:
				newData[mrot[mrot[mrot[index]]]] = data[index];
				break;
			default:
				break;
			}
		}
		return newData;
	}
	private NBTTagCompound rotate_conduit(final byte rotationSteps, final NBTTagCompound nbtConduit) {
		final NBTTagCompound nbtNewConduit = new NBTTagCompound();
		final Set<String> keys = nbtConduit.getKeySet();
		for (final String key : keys) {
			final NBTBase base = nbtConduit.getTag(key);
			switch(base.getId()) {
			case Constants.NBT.TAG_INT_ARRAY:	// "connections", "externalConnections"
				final int[] data = nbtConduit.getIntArray(key);
				final int[] newData = data.clone();
				for (int index = 0; index < data.length; index++) {
					switch (rotationSteps) {
					case 1:
						newData[index] = mrot[data[index]];
						break;
					case 2:
						newData[index] = mrot[mrot[data[index]]];
						break;
					case 3:
						newData[index] = mrot[mrot[mrot[data[index]]]];
						break;
					default:
						break;
					}
				}
				nbtNewConduit.setIntArray(key, newData);
				break;
				
			case Constants.NBT.TAG_BYTE_ARRAY:	// "conModes", "signalColors", "forcedConnections", "signalStrengths"
				nbtNewConduit.setByteArray(key, rotate_byteArray(rotationSteps, nbtConduit.getByteArray(key)));
				break;
				
			default:
				final String[] parts = key.split("\\.");
				if (parts.length != 2 || !rotSideNames.containsKey(parts[1])) {
					nbtNewConduit.setTag(key, base);
				} else {
					switch (rotationSteps) {
					case 1:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(parts[1]), base);
						break;
					case 2:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(rotSideNames.get(parts[1])), base);
						break;
					case 3:
						nbtNewConduit.setTag(parts[0] + "." + rotSideNames.get(rotSideNames.get(rotSideNames.get(parts[1]))), base);
						break;
					default:
						nbtNewConduit.setTag(key, base);
						break;
					}
				}
				break;
			}
		}
		return nbtNewConduit;
	}
	
	private void rotateReservoir(final NBTTagCompound nbtTileEntity, final ITransformation transformation, final byte rotationSteps) {
		if (nbtTileEntity.hasKey("front")) {
			final short front = nbtTileEntity.getShort("front");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("front", rotFront[front]);
				break;
			case 2:
				nbtTileEntity.setShort("front", rotFront[rotFront[front]]);
				break;
			case 3:
				nbtTileEntity.setShort("front", rotFront[rotFront[rotFront[front]]]);
				break;
			default:
				break;
			}
		}
		if (nbtTileEntity.hasKey("right")) {
			final short right = nbtTileEntity.getShort("right");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("right", rotRight[right]);
				break;
			case 2:
				nbtTileEntity.setShort("right", rotRight[rotRight[right]]);
				break;
			case 3:
				nbtTileEntity.setShort("right", rotRight[rotRight[rotRight[right]]]);
				break;
			default:
				break;
			}
		}
		
		// Multiblock
		if (nbtTileEntity.hasKey("multiblock") && nbtTileEntity.hasKey("pos")) {
			final int[] oldCoords = nbtTileEntity.getIntArray("multiblock");
			final BlockPos[] targets = new BlockPos[oldCoords.length / 3];
			for (int index = 0; index < oldCoords.length / 3; index++) {
				targets[index] = transformation.apply(oldCoords[3 * index], oldCoords[3 * index + 1], oldCoords[3 * index + 2]);
			}
			if (targets[0].getY() == targets[1].getY() && targets[1].getY() == targets[2].getY() && targets[2].getY() == targets[3].getY()) {
				final short pos = nbtTileEntity.getShort("pos");
				switch (rotationSteps) {
				case 1:
					nbtTileEntity.setShort("pos", rotPosHorizontal[pos]);
					break;
				case 2:
					nbtTileEntity.setShort("pos", rotPosHorizontal[rotPosHorizontal[pos]]);
					break;
				case 3:
					nbtTileEntity.setShort("pos", rotPosHorizontal[rotPosHorizontal[rotPosHorizontal[pos]]]);
					break;
				default:
					break;
				}
			} else {
				final BlockPos newPos = transformation.apply(nbtTileEntity.getInteger("x"), nbtTileEntity.getInteger("y"), nbtTileEntity.getInteger("z"));
				if (targets[0].getX() == targets[1].getX() && targets[1].getX() == targets[2].getX() && targets[2].getX() == targets[3].getX()) {
					final int minZ = Math.min(targets[0].getZ(), Math.min(targets[1].getZ(), targets[2].getZ()));
					final short pos = (short) ((nbtTileEntity.getShort("pos") & 2) + (newPos.getZ() == minZ ? 1 : 0));	// 2 & 3 are for bottom
					nbtTileEntity.setShort("pos", pos);
				} else {
					final int minX = Math.min(targets[0].getX(), Math.min(targets[1].getX(), targets[2].getX()));
					final short pos = (short) ((nbtTileEntity.getShort("pos") & 2) + (newPos.getZ() == minX ? 1 : 0));	// 2 & 3 are for bottom
					nbtTileEntity.setShort("pos", pos);
				}
			}
			
			final int[] newCoords = new int[oldCoords.length];
			for (int index = 0; index < oldCoords.length / 3; index++) {
				newCoords[3 * index    ] = targets[index].getX();
				newCoords[3 * index + 1] = targets[index].getY();
				newCoords[3 * index + 2] = targets[index].getZ();
			}
			nbtTileEntity.setIntArray("multiblock", newCoords);
		}
	}
	
	@Override
	public int rotate(final Block block, final int metadata, final NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		if (nbtTileEntity.hasKey("facing")) {
			final short facing = nbtTileEntity.getShort("facing");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.setShort("facing", mrot[facing]);
				return metadata;
			case 2:
				nbtTileEntity.setShort("facing", mrot[mrot[facing]]);
				return metadata;
			case 3:
				nbtTileEntity.setShort("facing", mrot[mrot[mrot[facing]]]);
				return metadata;
			default:
				return metadata;
			}
		}
		
		// Reservoir
		if (classBlockReservoir.isInstance(block)) {
			rotateReservoir(nbtTileEntity, transformation, rotationSteps);
		}
		
		// Faces
		final Map<String, Short> map = new HashMap<>();
		for (final String key : rotFaceNames.keySet()) {
			if (nbtTileEntity.hasKey(key)) {
				final short face = nbtTileEntity.getShort(key);
				switch (rotationSteps) {
				case 1:
					map.put(rotFaceNames.get(key), face);
					break;
				case 2:
					map.put(rotFaceNames.get(rotFaceNames.get(key)), face);
					break;
				case 3:
					map.put(rotFaceNames.get(rotFaceNames.get(rotFaceNames.get(key))), face);
					break;
				default:
					map.put(key, face);
					break;
				}
				nbtTileEntity.removeTag(key);
			}
		}
		if (!map.isEmpty()) {
			for (final Entry<String, Short> entry : map.entrySet()) {
				nbtTileEntity.setShort(entry.getKey(), entry.getValue());
			}
		}
		
		// Conduits
		if (nbtTileEntity.hasKey("conduits")) {
			final NBTTagList nbtConduits = nbtTileEntity.getTagList("conduits", Constants.NBT.TAG_COMPOUND);
			final NBTTagList nbtNewConduits = new NBTTagList(); 
			for (int index = 0; index < nbtConduits.tagCount(); index++) {
				final NBTTagCompound conduitTypeAndContent = nbtConduits.getCompoundTagAt(index);
				final NBTTagCompound newConduitTypeAndContent = new NBTTagCompound();
				newConduitTypeAndContent.setString("conduitType", conduitTypeAndContent.getString("conduitType"));
				newConduitTypeAndContent.setTag("conduit", rotate_conduit(rotationSteps, conduitTypeAndContent.getCompoundTag("conduit")));
				nbtNewConduits.appendTag(newConduitTypeAndContent);
			}
			nbtTileEntity.setTag("conduits", nbtNewConduits);
		}
		return metadata;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final IBlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final NBTBase nbtBase) {
		// nothing to do
	}
}
