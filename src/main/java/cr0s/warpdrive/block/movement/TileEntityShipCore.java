package cr0s.warpdrive.block.movement;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.TileEntitySecurityStation;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.EventWarpDrive.Ship.PreJump;
import cr0s.warpdrive.api.IStarMapRegistryTileEntity;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.ShipMovementCosts;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.data.EnumShipCommand;
import cr0s.warpdrive.data.EnumShipCoreState;
import cr0s.warpdrive.data.EnumShipMovementType;
import cr0s.warpdrive.data.Jumpgate;
import cr0s.warpdrive.data.EnumStarMapEntryType;
import cr0s.warpdrive.data.SoundEvents;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.event.JumpSequencer;
import cr0s.warpdrive.render.EntityFXBoundingBox;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityShipCore extends TileEntityAbstractShipController implements IStarMapRegistryTileEntity {
	
	private static final int LOG_INTERVAL_TICKS = 20 * 180;
	private static final int BOUNDING_BOX_INTERVAL_TICKS = 60;
	
	// persistent properties
	public EnumFacing facing;
	public UUID uuid = null;
	private double isolationRate = 0.0D;
	private int ticksCooldown = 0;
	private int warmupTime_ticks = 0;
	protected int jumpCount = 0;
	private boolean isValid = false;
	private String reasonInvalid = "";
	
	// computed properties
	public int maxX, maxY, maxZ;
	public int minX, minY, minZ;
	protected boolean showBoundingBox = false;
	private int ticksBoundingBoxUpdate = 0;
	
	private EnumShipCoreState stateCurrent = EnumShipCoreState.IDLE;
	private EnumShipCommand commandCurrent = EnumShipCommand.IDLE;
	
	private long timeLastShipScanDone = -1;
	private ShipScanner shipScanner = null;
	public int shipMass;
	public int shipVolume;
	private BlockPos posSecurityStation = null;
	
	private EnumShipMovementType shipMovementType;
	private ShipMovementCosts shipMovementCosts;
	
	private long distanceSquared = 0;
	private boolean isCooldownReported = false;
	private boolean isMotionSicknessApplied = false;
	private boolean isSoundPlayed = false;
	private boolean isWarmupReported = false;
	protected int randomWarmupAddition_ticks = 0;
	
	private int registryUpdateTicks = 0;
	private int logTicks = 120;
	
	private int isolationBlocksCount = 0;
	private int isolationUpdateTicks = 0;
	
	
	public TileEntityShipCore() {
		super();
		peripheralName = "warpdriveShipCore";
		// addMethods(new String[] {});
		CC_scripts = Collections.singletonList("startup");
	}
	
	@SideOnly(Side.CLIENT)
	private void doShowBoundingBox() {
		ticksBoundingBoxUpdate--;
		if (ticksBoundingBoxUpdate > 0) {
			return;
		}
		ticksBoundingBoxUpdate = BOUNDING_BOX_INTERVAL_TICKS;
		
		final Vector3 vector3 = new Vector3(this);
		vector3.translate(0.5D);
		
		FMLClientHandler.instance().getClient().effectRenderer.addEffect(
				new EntityFXBoundingBox(world, vector3,
				                        new Vector3(minX - 0.0D, minY - 0.0D, minZ - 0.0D),
				                        new Vector3(maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D),
				                        1.0F, 0.8F, 0.3F, BOUNDING_BOX_INTERVAL_TICKS + 1) );
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
		facing = world.getBlockState(pos).getValue(BlockProperties.FACING);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			if (showBoundingBox) {
				doShowBoundingBox();
			}
			return;
		}
		
		// always cooldown
		if (ticksCooldown > 0) {
			ticksCooldown--;
			
			// report cooldown time when a command is requested
			if ( isEnabled
			  && command != EnumShipCommand.IDLE
			  && command != EnumShipCommand.MAINTENANCE ) {
				if (ticksCooldown % 20 == 0) {
					final int seconds = ticksCooldown / 20;
					if (!isCooldownReported || (seconds < 5) || ((seconds < 30) && (seconds % 5 == 0)) || (seconds % 10 == 0)) {
						isCooldownReported = true;
						messageToAllPlayersOnShip(new TextComponentTranslation("Ship core is cooling down... %d s to go...",
						                                                       seconds));
					}
				}
			}
			
			if (ticksCooldown == 0) {
				cooldownDone();
			}
		} else {
			isCooldownReported = false;
		}
		
		// enforce emergency stop
		if (!isEnabled) {
			stateCurrent = EnumShipCoreState.IDLE;
		}
		
		// periodically update starmap registry
		registryUpdateTicks--;
		if (registryUpdateTicks <= 0) {
			registryUpdateTicks = 20 * WarpDriveConfig.STARMAP_REGISTRY_UPDATE_INTERVAL_SECONDS;
			if (uuid == null || (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0)) {
				uuid = UUID.randomUUID();
			}
			// recover registration, shouldn't be needed, in theory...
			WarpDrive.starMap.updateInRegistry(this);
		}
		
		// periodically log the ship state
		logTicks--;
		if (logTicks <= 0) {
			logTicks = LOG_INTERVAL_TICKS;
			if (WarpDriveConfig.LOGGING_JUMP) {
				WarpDrive.logger.info(String.format("%s %s, isEnabled %s, %d controllers, warmup %d, cooldown %d",
				                                    this,
				                                    stateCurrent,
				                                    isEnabled,
				                                    0, // @TODO countControllers,
				                                    warmupTime_ticks,
				                                    ticksCooldown));
			}
		}
		
		// periodically check isolation blocks
		isolationUpdateTicks--;
		if (isolationUpdateTicks <= 0) {
			isolationUpdateTicks = WarpDriveConfig.SHIP_CORE_ISOLATION_UPDATE_INTERVAL_SECONDS * 20;
			updateIsolationState();
		}
		
		// refresh rendering
		final boolean isActive = commandCurrent != EnumShipCommand.OFFLINE;
		updateBlockState(null, BlockProperties.ACTIVE, isActive);
		
		// scan ship content progressively
		if (timeLastShipScanDone <= 0L) {
			timeLastShipScanDone = world.getTotalWorldTime();
			shipScanner = new ShipScanner(world, minX, minY, minZ, maxX, maxY, maxZ);
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("%s scanning started",
				                                    this));
			}
		}
		if (shipScanner != null) {
			if (!shipScanner.tick()) {
				// still scanning => skip state handling
				return;
			}
			
			shipMass = shipScanner.mass;
			shipVolume = shipScanner.volume;
			posSecurityStation = shipScanner.posSecurityStation;
			shipScanner = null;
			if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
				WarpDrive.logger.info(String.format("%s scanning done: mass %d, volume %d",
				                                    this, shipMass, shipVolume));
			}
			
			// validate results
			boolean isUnlimited = false;
			final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
			final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
			for (final Entity entity : list) {
				if (!(entity instanceof EntityPlayer)) {
					continue;
				}
				
				final String playerName = entity.getName();
				for (final String nameUnlimited : WarpDriveConfig.SHIP_VOLUME_UNLIMITED_PLAYERNAMES) {
					isUnlimited = isUnlimited || nameUnlimited.equals(playerName);
				}
			}
			if ( !isUnlimited
			  && shipMass > WarpDriveConfig.SHIP_VOLUME_MAX_ON_PLANET_SURFACE
			  && CelestialObjectManager.isPlanet(world, pos.getX(), pos.getZ()) ) {
				reasonInvalid = String.format("Ship is too big for a planet (max is %d blocks)",
				                              WarpDriveConfig.SHIP_VOLUME_MAX_ON_PLANET_SURFACE);
				isValid = false;
				if (isEnabled) {
					commandDone(false, reasonInvalid);
				}
				return;
			}
		}
		
		// skip state handling while cooling down
		if (isCooling()) {
			return;
		}
		
		final StringBuilder reason = new StringBuilder();
		
		switch (stateCurrent) {
		case IDLE:
			if ( command != EnumShipCommand.IDLE
			  && command != EnumShipCommand.MAINTENANCE ) {
				commandCurrent = command;
				stateCurrent = EnumShipCoreState.ONLINE;
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info(String.format("%s state IDLE -> ONLINE",
					                                    this));
				}
			}
			break;
		
		case ONLINE:
			// (disabling will switch back to IDLE and clear variables)
			
			switch (commandCurrent) {
			case MANUAL:
			case HYPERDRIVE:
			case GATE:
				// initiating jump
				if (WarpDriveConfig.LOGGING_JUMPBLOCKS) {
					WarpDrive.logger.info(String.format("%s state ONLINE -> initiating jump",
					                                    this));
				}
				
				// compute distance
				distanceSquared = getMovement().getMagnitudeSquared();
				// rescan ship mass/volume if it's too old
				if (timeLastShipScanDone + WarpDriveConfig.SHIP_VOLUME_SCAN_AGE_TOLERANCE_SECONDS < world.getTotalWorldTime()) {
					timeLastShipScanDone = -1;
					break;
				}
				
				messageToAllPlayersOnShip(new TextComponentTranslation("Running pre-jump checklist..."));
				
				// update ship spatial parameters
				if (!isValid) {
					commandDone(false, reasonInvalid);
					return;
				}
				
				// update movement parameters
				if (!validateShipMovementParameters(reason)) {
					commandDone(false, reason.toString());
					return;
				}
				
				// compute random ticks to warmup so it's harder to 'dup' items
				randomWarmupAddition_ticks = world.rand.nextInt(WarpDriveConfig.SHIP_WARMUP_RANDOM_TICKS);
				
				stateCurrent = EnumShipCoreState.WARMING_UP;
				warmupTime_ticks = shipMovementCosts.warmup_seconds * 20 + randomWarmupAddition_ticks;
				isMotionSicknessApplied = false;
				isSoundPlayed = false;
				isWarmupReported = false;
				break;
			
			default:
				WarpDrive.logger.error(String.format("%s Invalid controller command %s for current state %s",
				                                     this, command, stateCurrent));
				break;
			}
			break;
			
		case WARMING_UP:
			// Apply motion sickness as applicable
			if (shipMovementCosts.sickness_seconds > 0) {
				final int motionSicknessThreshold_ticks = shipMovementCosts.sickness_seconds * 20 - randomWarmupAddition_ticks / 4; 
				if ( !isMotionSicknessApplied
				   && motionSicknessThreshold_ticks >= warmupTime_ticks ) {
					if (WarpDriveConfig.LOGGING_JUMP) {
						WarpDrive.logger.info(this + " Giving warp sickness to on-board players");
					}
					makePlayersOnShipDrunk(shipMovementCosts.sickness_seconds * 20 + WarpDriveConfig.SHIP_WARMUP_RANDOM_TICKS);
					isMotionSicknessApplied = true;
				}
			}
			
			// Select best sound file and adjust offset
			final int soundThreshold;
			final SoundEvent soundEvent;
			if (shipMovementCosts.warmup_seconds < 10) {
				soundThreshold =  4 * 20 - randomWarmupAddition_ticks;
				soundEvent = SoundEvents.WARP_4_SECONDS;
			} else if (shipMovementCosts.warmup_seconds > 29) {
				soundThreshold = 30 * 20 - randomWarmupAddition_ticks;
				soundEvent = SoundEvents.WARP_30_SECONDS;
			} else {
				soundThreshold = 10 * 20 - randomWarmupAddition_ticks;
				soundEvent = SoundEvents.WARP_10_SECONDS;
			}
			
			if ( !isSoundPlayed
			  && soundThreshold >= warmupTime_ticks ) {
				if (WarpDriveConfig.LOGGING_JUMP) {
					WarpDrive.logger.info(this + " Playing sound effect '" + soundEvent + "' soundThreshold " + soundThreshold + " warmupTime " + warmupTime_ticks);
				}
				world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, 4.0F, 1.0F);
				isSoundPlayed = true;
			}
			
			if (warmupTime_ticks % 20 == 0) {
				final int seconds = warmupTime_ticks / 20;
				if ( !isWarmupReported
				  || (seconds >= 60 && (seconds % 15 == 0))
				  || (seconds <  60 && seconds > 30 && (seconds % 10 == 0)) ) {
					isWarmupReported = true;
					messageToAllPlayersOnShip(new TextComponentTranslation("Ship core is warming up... %s s to go...",
					                                                       seconds));
				}
			}
			
			// Awaiting warm-up time
			if (warmupTime_ticks > 0) {
				warmupTime_ticks--;
				break;
			}
			
			warmupTime_ticks = 0;
			isMotionSicknessApplied = false;
			isSoundPlayed = false;
			isWarmupReported = false;
			
			if (!isValid) {
				commandDone(false, reasonInvalid);
				return;
			}
			
			if (WarpDrive.starMap.isWarpCoreIntersectsWithOthers(this)) {
				commandDone(false, "Warp field intersects with other ship's field. Disable the other core to jump.");
				return;
			}
			
			if (WarpDrive.cloaks.isCloaked(world.provider.getDimension(), pos)) {
				commandDone(false, "Core is inside a cloaking field. Aborting. Disable cloaking field to jump!");
				return;
			}
			
			doJump();
			setCooldown(shipMovementCosts.cooldown_seconds * 20);
			commandDone(true, "Ok");
			jumpCount++;
			stateCurrent = EnumShipCoreState.IDLE;
			isCooldownReported = false;
			break;
			
		default:
			break;
		}
	}
	
	public boolean isValid() {
		return isValid;
	}
	
	public boolean isOffline() {
		return command == EnumShipCommand.OFFLINE;
	}
	
	public boolean isBusy() {
		return timeLastShipScanDone < 0 || shipScanner != null
		    || isCooling()
		    || stateCurrent == EnumShipCoreState.WARMING_UP;
	}
	
	private void setCooldown(final int ticksCooldown) {
		this.ticksCooldown = Math.max(1, Math.max(this.ticksCooldown, ticksCooldown));
		isCooldownReported = false;
	}
	
	private int getCooldown() {
		return ticksCooldown;
	}
	
	private boolean isCooling() {
		return ticksCooldown > 0;
	}
	
	@Override
	protected void commandDone(final boolean success, @Nonnull final String reason) {
		assert success || !reason.isEmpty();
		super.commandDone(success, reason);
		if (!success) {
			messageToAllPlayersOnShip(new TextComponentString(reason));
		}
		// @TODO implement remote controllers
	}
	
	protected boolean refreshLink(final TileEntityShipController tileEntityShipController) {
		return false; // @TODO implement remote controllers
	}
	
	public void messageToAllPlayersOnShip(final ITextComponent textComponent) {
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		final ITextComponent messageFormatted = new TextComponentString("[" + (!shipName.isEmpty() ? shipName : "ShipCore") + "] ")
		                                             .appendSibling(textComponent);
		
		WarpDrive.logger.info(String.format("%s messageToAllPlayersOnShip: %s",
		                                    this, textComponent.getFormattedText()));
		for (final Entity entity : list) {
			if (!(entity instanceof EntityPlayer)) {
				continue;
			}
			
			Commons.addChatMessage(entity, messageFormatted);
		}
	}
	
	public String getAllPlayersInArea() {
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX + 0.99D, maxY + 0.99D, maxZ + 0.99D);
		final List list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		final StringBuilder stringBuilderResult = new StringBuilder();
		
		boolean isFirst = true;
		for (final Object object : list) {
			if (!(object instanceof EntityPlayer)) {
				continue;
			}
			if (isFirst) {
				isFirst = false;
			} else {
				stringBuilderResult.append(", ");
			}
			stringBuilderResult.append(((EntityPlayer) object).getName());
		}
		return stringBuilderResult.toString();
	}
	
	public String getFirstOnlineCrew() {
		if (posSecurityStation == null) {// no crew defined
			return null;
		}
		
		final TileEntity tileEntity = world.getTileEntity(posSecurityStation);
		if ( !(tileEntity instanceof TileEntitySecurityStation)
		  || tileEntity.isInvalid() ) {// we're desync
			// force a refresh
			timeLastShipScanDone = -1;
			return "-busy-";
		}
		return ((TileEntitySecurityStation) tileEntity).getFirstOnlinePlayer();
	}
	
	private void updateIsolationState() {
		// Search block in cube around core
		final int xMin = pos.getX() - WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		final int xMax = pos.getX() + WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		
		final int zMin = pos.getZ() - WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		final int zMax = pos.getZ() + WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE;
		
		// scan 1 block higher to encourage putting isolation block on both
		// ground and ceiling
		final int yMin = Math.max(  0, pos.getY() - WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE + 1);
		final int yMax = Math.min(255, pos.getY() + WarpDriveConfig.RADAR_MAX_ISOLATION_RANGE + 1);
		
		int newCount = 0;
		
		// Search for warp isolation blocks
		for (int y = yMin; y <= yMax; y++) {
			for (int x = xMin; x <= xMax; x++) {
				for (int z = zMin; z <= zMax; z++) {
					if (world.getBlockState(new BlockPos(x, y, z)).getBlock() == WarpDrive.blockWarpIsolation) {
						newCount++;
					}
				}
			}
		}
		isolationBlocksCount = newCount;
		final double legacy_isolationRate = isolationRate;
		if (isolationBlocksCount >= WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS) {
			isolationRate = Math.min(1.0, WarpDriveConfig.RADAR_MIN_ISOLATION_EFFECT
					+ (isolationBlocksCount - WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS) // bonus blocks
					* (WarpDriveConfig.RADAR_MAX_ISOLATION_EFFECT - WarpDriveConfig.RADAR_MIN_ISOLATION_EFFECT)
					/ (WarpDriveConfig.RADAR_MAX_ISOLATION_BLOCKS - WarpDriveConfig.RADAR_MIN_ISOLATION_BLOCKS));
		} else {
			isolationRate = 0.0D;
		}
		if (WarpDriveConfig.LOGGING_RADAR && (WarpDrive.isDev || legacy_isolationRate != isolationRate)) {
			WarpDrive.logger.info(String.format("%s Isolation updated to %d (%.1f%%)",
			                                    this, isolationBlocksCount , isolationRate * 100.0));
		}
	}
	
	private void makePlayersOnShipDrunk(final int tickDuration) {
		final AxisAlignedBB axisalignedbb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
		final List<Entity> list = world.getEntitiesWithinAABBExcludingEntity(null, axisalignedbb);
		
		for (final Entity entity : list) {
			if (!(entity instanceof EntityPlayer)) {
				continue;
			}
			
			// Set "drunk" effect
			((EntityPlayer) entity).addPotionEffect(
					new PotionEffect(MobEffects.NAUSEA, tickDuration, 0, true, true));
		}
	}
	
	private TileEntitySecurityStation findSecurityStation() {// @TODO
		return null;
	}
	
	public boolean summonOwnerOnDeploy(final EntityPlayerMP entityPlayerMP) {
		if (entityPlayerMP == null) {
			WarpDrive.logger.warn(this + " No player given to summonOwnerOnDeploy()");
			return false;
		}
		updateAfterResize();
		if (!isValid) {
			Commons.addChatMessage(entityPlayerMP, new TextComponentString("[" + (!shipName.isEmpty() ? shipName : "ShipCore") + "]").setStyle(Commons.styleHeader)
			                                       .appendSibling(new TextComponentString(reasonInvalid).setStyle(Commons.styleWarning)));
			return false;
		}
		
		final TileEntitySecurityStation tileEntitySecurityStation = findSecurityStation();
		if (tileEntitySecurityStation != null) {
			tileEntitySecurityStation.players.clear();
			tileEntitySecurityStation.players.add(entityPlayerMP.getName());
		} else {
			WarpDrive.logger.warn(this + " Failed to find controller block");
			return false;
		}
		
		final AxisAlignedBB aabb = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
		if (isOutsideBB(aabb, MathHelper.floor(entityPlayerMP.posX), MathHelper.floor(entityPlayerMP.posY), MathHelper.floor(entityPlayerMP.posZ))) {
			summonPlayer(entityPlayerMP);
		}
		return true;
	}
	
	private static final VectorI[] SUMMON_OFFSETS = { new VectorI(2, 0, 0), new VectorI(-1, 0, 0),
		new VectorI(2, 0, 1), new VectorI(2, 0, -1), new VectorI(-1, 0, 1), new VectorI(-1, 0, -1),
		new VectorI(1, 0, 1), new VectorI(1, 0, -1), new VectorI( 0, 0, 1), new VectorI( 0, 0, -1) };
	private void summonPlayer(final EntityPlayerMP entityPlayer) {
		// validate distance
		double distance = Math.sqrt(new VectorI(entityPlayer).distance2To(this));
		if (entityPlayer.world != this.world) {
			distance += 256;
			if (!WarpDriveConfig.SHIP_SUMMON_ACROSS_DIMENSIONS) {
				Commons.addChatMessage(entityPlayer, new TextComponentTranslation("%1$s is in a different dimension, too far away to be summoned",
				                                                       entityPlayer.getDisplayName())
						                                     .setStyle(Commons.styleWarning));
				return;
			}
		}
		if (WarpDriveConfig.SHIP_SUMMON_MAX_RANGE >= 0 && distance > WarpDriveConfig.SHIP_SUMMON_MAX_RANGE) {
			Commons.addChatMessage(entityPlayer, new TextComponentTranslation("You are to far away to be summoned aboard '%1$s' (max. is %2$s m)",
			                                                                  shipName, WarpDriveConfig.SHIP_SUMMON_MAX_RANGE)
					                                     .setStyle(Commons.styleWarning));
			return;
		}
		
		// find a free spot
		final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(pos);
		for (final VectorI vOffset : SUMMON_OFFSETS) {
			mutableBlockPos.setPos(
				pos.getX() + facing.getFrontOffsetX() * vOffset.x + facing.getFrontOffsetZ() * vOffset.z,
			    pos.getY(),
				pos.getZ() + facing.getFrontOffsetZ() * vOffset.x + facing.getFrontOffsetX() * vOffset.z);
			if (world.isAirBlock(mutableBlockPos)) {
				if (world.isAirBlock(mutableBlockPos.add(0, 1, 0))) {
					summonPlayer(entityPlayer, mutableBlockPos);
					return;
				}
				mutableBlockPos.move(EnumFacing.DOWN);
				if (world.isAirBlock(mutableBlockPos)) {
					summonPlayer(entityPlayer, mutableBlockPos);
					return;
				}
			} else if ( world.isAirBlock(mutableBlockPos.add(0, -1, 0))
			         && world.isAirBlock(mutableBlockPos.add(0, -2, 0))
			         && !world.isAirBlock(mutableBlockPos.add(0, -3, 0)) ) {
				summonPlayer(entityPlayer, mutableBlockPos.add(0, -2, 0));
				return;
			} else if ( world.isAirBlock(mutableBlockPos.add(0, 1, 0))
			         && world.isAirBlock(mutableBlockPos.add(0, 2, 0))
			         && !world.isAirBlock(mutableBlockPos) ) {
				summonPlayer(entityPlayer, mutableBlockPos.add(0, 1, 0));
				return;
			}
		}
		final ITextComponent message = new TextComponentTranslation("No safe spot found to summon player %1$s",
		                                                    entityPlayer.getDisplayName())
				                       .setStyle(Commons.styleWarning);
		messageToAllPlayersOnShip(message);
		Commons.addChatMessage(entityPlayer, message);
	}
	
	private void summonPlayer(final EntityPlayerMP player, final BlockPos blockPos) {
		Commons.moveEntity(player, world, new Vector3(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5D));
	}
	
	@Override
	protected void updateAfterResize() {
		super.updateAfterResize();
		shipMass = 0;
		shipVolume = 0;
		
		// compute dimensions in game coordinates
		if (facing.getFrontOffsetX() == 1) {
			minX = pos.getX() - getBack();
			maxX = pos.getX() + getFront();
			minZ = pos.getZ() - getLeft();
			maxZ = pos.getZ() + getRight();
		} else if (facing.getFrontOffsetX() == -1) {
			minX = pos.getX() - getFront();
			maxX = pos.getX() + getBack();
			minZ = pos.getZ() - getRight();
			maxZ = pos.getZ() + getLeft();
		} else if (facing.getFrontOffsetZ() == 1) {
			minZ = pos.getZ() - getBack();
			maxZ = pos.getZ() + getFront();
			minX = pos.getX() - getRight();
			maxX = pos.getX() + getLeft();
		} else if (facing.getFrontOffsetZ() == -1) {
			minZ = pos.getZ() - getFront();
			maxZ = pos.getZ() + getBack();
			minX = pos.getX() - getLeft();
			maxX = pos.getX() + getRight();
		}
		
		minY = pos.getY() - getDown();
		maxY = pos.getY() + getUp();
		
		// update dimensions to client
		markDirty();
		
		// validate ship side constrains
		if ( (getBack() + getFront()) > WarpDriveConfig.SHIP_MAX_SIDE_SIZE
		  || (getLeft() + getRight()) > WarpDriveConfig.SHIP_MAX_SIDE_SIZE
		  || (getDown() + getUp()   ) > WarpDriveConfig.SHIP_MAX_SIDE_SIZE ) {
			reasonInvalid = String.format("Ship is too big (max is %d per side)",
			                              WarpDriveConfig.SHIP_MAX_SIDE_SIZE);
			isValid = false;
			return;
		}
		
		// request new ship scan
		timeLastShipScanDone = -1;
		
		isValid = true;
	}
	
	private boolean validateShipMovementParameters(final StringBuilder reason) {
		shipMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(), commandCurrent, getMovement().y, reason);
		if (shipMovementType == null) {
			return false;
		}
		
		// compute movement costs
		shipMovementCosts = new ShipMovementCosts(world, pos,
		                                          this, shipMovementType,
		                                          shipMass, (int) Math.ceil(Math.sqrt(distanceSquared)));
		
		// allow other mods to validate too
		final PreJump preJump;
		preJump = new PreJump(world, pos, this, shipMovementType.getName());
		MinecraftForge.EVENT_BUS.post(preJump);
		if (preJump.isCanceled()) {
			reason.append(preJump.getReason());
			return false;
		}
		
		return true;
	}
	
	// Computer interface are running independently of updateTicks, hence doing local computations getMaxJumpDistance() and getEnergyRequired()
	protected int getMaxJumpDistance(final EnumShipCommand command, final StringBuilder reason) {
		final EnumShipMovementType shipMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(), command, getMovement().y, reason);
		if (shipMovementType == null) {
			commandDone(false, reason.toString());
			return -1;
		}
		
		// compute movement costs
		final ShipMovementCosts shipMovementCosts = new ShipMovementCosts(world, pos,
		                                                                  this, shipMovementType,
		                                                                  shipMass, (int) Math.ceil(Math.sqrt(distanceSquared)));
		return shipMovementCosts.maximumDistance_blocks;
	}
	
	protected int getEnergyRequired(final EnumShipCommand command, final StringBuilder reason) {
		final EnumShipMovementType shipMovementType = EnumShipMovementType.compute(world, pos.getX(), minY, maxY, pos.getZ(), command, getMovement().y, reason);
		if (shipMovementType == null) {
			commandDone(false, reason.toString());
			return -1;
		}
		
		// compute movement costs
		final ShipMovementCosts shipMovementCosts = new ShipMovementCosts(world, pos,
		                                                                  this, shipMovementType,
		                                                                  shipMass, (int) Math.ceil(Math.sqrt(distanceSquared)));
		return shipMovementCosts.energyRequired;
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isShipInJumpgate(final Jumpgate jumpgate, final StringBuilder reason) {
		final AxisAlignedBB aabb = jumpgate.getGateAABB();
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Jumpgate " + jumpgate.name + " AABB is " + aabb);
		}
		int countBlocksInside = 0;
		int countBlocksTotal = 0;
		
		if ( aabb.contains(new Vec3d(minX, minY, minZ))
		  && aabb.contains(new Vec3d(maxX, maxY, maxZ)) ) {
			// fully inside
			return true;
		}
		
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				for (int y = minY; y <= maxY; y++) {
					final IBlockState blockState = world.getBlockState(new BlockPos(x, y, z));
					
					// Skipping vanilla air & ignored blocks
					if (blockState.getBlock() == Blocks.AIR || Dictionary.BLOCKS_LEFTBEHIND.contains(blockState.getBlock())) {
						continue;
					}
					if (Dictionary.BLOCKS_NOMASS.contains(blockState.getBlock())) {
						continue;
					}
					
					if (aabb.minX <= x && aabb.maxX >= x && aabb.minY <= y && aabb.maxY >= y && aabb.minZ <= z && aabb.maxZ >= z) {
						countBlocksInside++;
					}
					countBlocksTotal++;
				}
			}
		}
		
		float percent = 0F;
		if (shipMass != 0) {
			percent = Math.round((((countBlocksInside * 1.0F) / shipMass) * 100.0F) * 10.0F) / 10.0F;
		}
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			if (shipMass != countBlocksTotal) {
				WarpDrive.logger.info(this + " Ship mass has changed from " + shipMass + " to " + countBlocksTotal + " blocks");
			}
			WarpDrive.logger.info(this + "Ship has " + countBlocksInside + " / " + shipMass + " blocks (" + percent + "%) in jumpgate '" + jumpgate.name + "'");
		}
		
		// At least 80% of ship must be inside jumpgate
		if (percent > 80F) {
			return true;
		} else if (percent <= 0.001) {
			reason.append(String.format("Ship is not inside a jumpgate. Jump rejected. Nearest jumpgate is %s",
				jumpgate.toNiceString()));
			return false;
		} else {
			reason.append(String.format("Ship is only %.1f%% inside a jumpgate. Sorry, we'll loose too much crew as is, jump rejected.",
				percent));
			return false;
		}
	}
	
	private boolean isFreePlaceForShip(final int destX, final int destY, final int destZ) {
		int newX, newZ;
		
		if ( destY + getUp() > 255
		  || destY - getDown() < 5 ) {
			return false;
		}
		
		final int moveX = destX - pos.getX();
		final int moveY = destY - pos.getY();
		final int moveZ = destZ - pos.getZ();
		
		for (int x = minX; x <= maxX; x++) {
			newX = moveX + x;
			for (int z = minZ; z <= maxZ; z++) {
				newZ = moveZ + z;
				for (int y = minY; y <= maxY; y++) {
					if (moveY + y < 0 || moveY + y > 255) {
						return false;
					}
					
					final Block blockSource = world.getBlockState(new BlockPos(x, y, z)).getBlock();
					final Block blockTarget = world.getBlockState(new BlockPos(newX, moveY + y, newZ)).getBlock();
					
					// not vanilla air nor ignored blocks at source
					// not vanilla air nor expandable blocks are target location
					if ( blockSource != Blocks.AIR
					  && !Dictionary.BLOCKS_EXPANDABLE.contains(blockSource)
					  && blockTarget != Blocks.AIR
					  && !Dictionary.BLOCKS_EXPANDABLE.contains(blockTarget)) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	private void doGateJump() {
		// Search nearest jump-gate
		final String targetName = getTargetName();
		final Jumpgate targetGate = WarpDrive.jumpgates.findGateByName(targetName);
		
		if (targetGate == null) {
			commandDone(false, String.format("Destination jumpgate '%s' is unknown. Check jumpgate name.", targetName));
			return;
		}
		
		// Now make jump to a beacon
		final int gateX = targetGate.xCoord;
		final int gateY = targetGate.yCoord;
		final int gateZ = targetGate.zCoord;
		int destX = gateX;
		int destY = gateY;
		int destZ = gateZ;
		final Jumpgate nearestGate = WarpDrive.jumpgates.findNearestGate(pos);
		
		final StringBuilder reason = new StringBuilder();
		if (!isShipInJumpgate(nearestGate, reason)) {
			commandDone(false, reason.toString());
			return;
		}
		
		// If gate is blocked by obstacle
		if (!isFreePlaceForShip(gateX, gateY, gateZ)) {
			// Randomize destination coordinates and check for collision with obstacles around jumpgate
			// Try to find good place for ship
			int numTries = 10; // num tries to check for collision
			boolean placeFound = false;
			
			for (; numTries > 0; numTries--) {
				// randomize destination coordinates around jumpgate
				destX = gateX + ((world.rand.nextBoolean()) ? -1 : 1) * (20 + world.rand.nextInt(100));
				destZ = gateZ + ((world.rand.nextBoolean()) ? -1 : 1) * (20 + world.rand.nextInt(100));
				destY = gateY + ((world.rand.nextBoolean()) ? -1 : 1) * (20 + world.rand.nextInt(50));
				
				// check for collision
				if (isFreePlaceForShip(destX, destY, destZ)) {
					placeFound = true;
					break;
				}
			}
			
			if (!placeFound) {
				commandDone(false, "Destination gate is blocked by obstacles. Aborting...");
				return;
			}
			
			WarpDrive.logger.info(String.format("[GATE] Place found over %d tries.",
			                                    10 - numTries));
		}
		
		// Consume energy
		if (energy_consume(shipMovementCosts.energyRequired, false)) {
			WarpDrive.logger.info(String.format("%s Moving ship to a place around gate '%s' (%d %d %d)",
			                                    this, targetGate.name, destX, destY, destZ));
			final JumpSequencer jump = new JumpSequencer(this, EnumShipMovementType.GATE_ACTIVATING, targetName, 0, 0, 0, (byte) 0, destX, destY, destZ);
			jump.enable();
		} else {
			messageToAllPlayersOnShip(new TextComponentTranslation("Insufficient energy level"));
		}
	}
	
	private void doJump() {
		
		final int requiredEnergy = shipMovementCosts.energyRequired;
		
		if (!energy_consume(requiredEnergy, true)) {
			commandDone(false, String.format("Insufficient energy to jump! Core is currently charged with %d EU while jump requires %d EU",
			                                 energy_getEnergyStored(), requiredEnergy));
			return;
		}
		
		final String shipInfo = "" + shipVolume + " blocks inside (" + minX + ", " + minY + ", " + minZ + ") to (" + maxX + ", " + maxY + ", " + maxZ + ") with an actual mass of " + shipMass + " blocks";
		switch (commandCurrent) {
		case GATE:
			WarpDrive.logger.info(this + " Performing gate jump of " + shipInfo);
			doGateJump();
			return;
			
		case HYPERDRIVE:
			WarpDrive.logger.info(this + " Performing hyperdrive jump of " + shipInfo);
			
			// Check ship size for hyper-space jump
			if (shipMass < WarpDriveConfig.SHIP_VOLUME_MIN_FOR_HYPERSPACE) {
				Jumpgate nearestGate = null;
				if (WarpDrive.jumpgates == null) {
					WarpDrive.logger.warn(this + " WarpDrive.jumpGates is NULL!");
				} else {
					nearestGate = WarpDrive.jumpgates.findNearestGate(pos);
				}
				
				final StringBuilder reason = new StringBuilder();
				if (nearestGate == null || !isShipInJumpgate(nearestGate, reason)) {
					commandDone(false, new TextComponentString(String.format("Ship is too small (%d/%d).\nInsufficient ship mass to engage alcubierre drive.\nIncrease your mass or use a jumpgate to reach or exit hyperspace.",
					                                                         shipMass, WarpDriveConfig.SHIP_VOLUME_MIN_FOR_HYPERSPACE)).getFormattedText());
					return;
				}
			}
			break;
			
		case MANUAL:
			WarpDrive.logger.info(String.format("%s Performing manual jump of %s, %s, movement %s, rotationSteps %d",
			                                    this, shipInfo, shipMovementType, getMovement(), getRotationSteps()));
			break;
			
		default:
			WarpDrive.logger.error(String.format("%s Aborting while trying to perform invalid jump command %s",
			                                     this, commandCurrent));
			commandDone(false, "Internal error, check console for details");
			commandCurrent = EnumShipCommand.IDLE;
			stateCurrent = EnumShipCoreState.IDLE;
			return;
		}
		
		if (!energy_consume(requiredEnergy, false)) {
			commandDone(false, "Insufficient energy level");
			return;
		}
		
		int moveX = 0;
		int moveY = 0;
		int moveZ = 0;
		
		if (commandCurrent != EnumShipCommand.HYPERDRIVE) {
			final VectorI movement = getMovement();
			final VectorI shipSize = new VectorI(getFront() + 1 + getBack(),
			                                     getUp()    + 1 + getDown(),
			                                     getRight() + 1 + getLeft());
			final int maxDistance = shipMovementCosts.maximumDistance_blocks;
			if (Math.abs(movement.x) - shipSize.x > maxDistance) {
				movement.x = (int) Math.signum(movement.x) * (shipSize.x + maxDistance);
			}
			if (Math.abs(movement.y) - shipSize.y > maxDistance) {
				movement.y = (int) Math.signum(movement.y) * (shipSize.y + maxDistance);
			}
			if (Math.abs(movement.z) - shipSize.z > maxDistance) {
				movement.z = (int) Math.signum(movement.z) * (shipSize.z + maxDistance);
			}
			moveX = facing.getFrontOffsetX() * movement.x - facing.getFrontOffsetZ() * movement.z;
			moveY = movement.y;
			moveZ = facing.getFrontOffsetZ() * movement.x + facing.getFrontOffsetX() * movement.z;
		}
		
		if (WarpDriveConfig.LOGGING_JUMP) {
			WarpDrive.logger.info(this + " Movement adjusted to (" + moveX + " " + moveY + " " + moveZ + ") blocks.");
		}
		final JumpSequencer jump = new JumpSequencer(this, shipMovementType, null,
				moveX, moveY, moveZ, getRotationSteps(),
				0, 0, 0);
		jump.enable();
	}
	
	private static boolean isOutsideBB(final AxisAlignedBB axisalignedbb, final int x, final int y, final int z) {
		return axisalignedbb.minX > x || axisalignedbb.maxX < x
		    || axisalignedbb.minY > y || axisalignedbb.maxY < y
		    || axisalignedbb.minZ > z || axisalignedbb.maxZ < z;
	}
	
	@Override
	public ITextComponent getStatus() {
		final String strIsolationRate = String.format("%.1f", isolationRate * 100.0D);
		return super.getStatus()
			.appendSibling((ticksCooldown > 0) ? new TextComponentString("\n").appendSibling(new TextComponentTranslation("warpdrive.ship.status_line.cooling", ticksCooldown / 20)) : new TextComponentString(""))
			.appendSibling((isolationBlocksCount > 0) ? new TextComponentString("\n").appendSibling(new TextComponentTranslation("warpdrive.ship.status_line.isolation", isolationBlocksCount, strIsolationRate)) : new TextComponentString(""));
	}
	
	public ITextComponent getBoundingBoxStatus() {
		return super.getStatusPrefix()
			.appendSibling(new TextComponentTranslation(showBoundingBox ? "tile.warpdrive.movement.ship_core.bounding_box.enabled" : "tile.warpdrive.movement.ship_core.bounding_box.disabled"));
	}
	
	@Override
	public int energy_getMaxStorage() {
		return WarpDriveConfig.SHIP_MAX_ENERGY_STORED;
	}
	
	@Override
	public boolean energy_canInput(final EnumFacing from) {
		return true;
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		uuid = new UUID(tagCompound.getLong("uuidMost"), tagCompound.getLong("uuidLeast"));
		if (uuid.getMostSignificantBits() == 0 && uuid.getLeastSignificantBits() == 0) {
			uuid = UUID.randomUUID();
		}
		isolationRate = tagCompound.getDouble("isolationRate");
		ticksCooldown = tagCompound.getInteger("cooldownTime");
		warmupTime_ticks = tagCompound.getInteger("warmupTime");
		jumpCount = tagCompound.getInteger("jumpCount");
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		if (uuid != null) {
			tagCompound.setLong("uuidMost", uuid.getMostSignificantBits());
			tagCompound.setLong("uuidLeast", uuid.getLeastSignificantBits());
		}
		tagCompound.setDouble("isolationRate", isolationRate);
		tagCompound.setInteger("cooldownTime", ticksCooldown);
		tagCompound.setInteger("warmupTime", warmupTime_ticks);
		tagCompound.setInteger("jumpCount", jumpCount);
		
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = writeToNBT(super.getUpdateTag());
		tagCompound.setInteger("minX", minX);
		tagCompound.setInteger("maxX", maxX);
		tagCompound.setInteger("minY", minY);
		tagCompound.setInteger("maxY", maxY);
		tagCompound.setInteger("minZ", minZ);
		tagCompound.setInteger("maxZ", maxZ);
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
		minX = tagCompound.getInteger("minX");
		maxX = tagCompound.getInteger("maxX");
		minY = tagCompound.getInteger("minY");
		maxY = tagCompound.getInteger("maxY");
		minZ = tagCompound.getInteger("minZ");
		maxZ = tagCompound.getInteger("maxZ");
	}
	
	@Override
	public void validate() {
		super.validate();
		
		if (world.isRemote) {
			return;
		}
		
		WarpDrive.starMap.updateInRegistry(this);
	}
	
	@Override
	public void invalidate() {
		if (!world.isRemote) {
			WarpDrive.starMap.removeFromRegistry(this);
		}
		super.invalidate();
	}
	
	// IStarMapRegistryTileEntity overrides
	@Override
	public EnumStarMapEntryType getStarMapType() {
		return EnumStarMapEntryType.SHIP;
	}
	
	@Override
	public UUID getUUID() {
		return uuid;
	}
	
	@Override
	public AxisAlignedBB getStarMapArea() {
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	@Override
	public int getMass() {
		return shipMass;
	}
	
	@Override
	public double getIsolationRate() {
		return isolationRate;
	}
	
	@Override
	public String getStarMapName() {
		return shipName;
	}
	
	@Override
	public void onBlockUpdatedInArea(final VectorI vector, final IBlockState blockState) {
		// no operation
	}
	
	// Common OC/CC methods
	@Override
	public Object[] isAssemblyValid() {
		if (isValid) {
			return new Object[] { true, "ok" };
		}
		return new Object[] { false, reasonInvalid };
	}
	
	@Override
	public Object[] getOrientation() {
		return new Object[] { facing.getFrontOffsetX(), 0, facing.getFrontOffsetZ() };
	}
	
	@Override
	public Object[] isInSpace() {
		return new Boolean[] { CelestialObjectManager.isInSpace(world, pos.getX(), pos.getZ()) };
	}
	
	@Override
	public Object[] isInHyperspace() {
		return new Boolean[] { CelestialObjectManager.isInHyperspace(world, pos.getX(), pos.getZ()) };
	}
	
	// public Object[] shipName(final Object[] arguments);
	
	@Override
	public Object[] getEnergyRequired() {
		final StringBuilder reason = new StringBuilder();
		final int energyRequired = getEnergyRequired(command, reason);
		if (energyRequired < 0) {
			return new Object[] { false, reason.toString() };
		}
		return new Object[] { true, energyRequired };
	}
	
	@Override
	public Object[] getShipSize() {
		return new Object[] { shipMass, shipVolume };
	}
	
	@Override
	public Object[] getMaxJumpDistance() {
		final StringBuilder reason = new StringBuilder();
		final int maximumDistance_blocks = getMaxJumpDistance(command, reason);
		if (maximumDistance_blocks < 0) {
			return new Object[] { false, reason.toString() };
		}
		return new Object[] { true, maximumDistance_blocks };
	}
	
	@Override
	public String toString() {
		return String.format("%s '%s' %s",
		                     getClass().getSimpleName(),
		                     shipName,
		                     Commons.format(world, pos));
	}
}
