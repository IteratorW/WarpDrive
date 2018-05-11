package cr0s.warpdrive.event;

import cr0s.warpdrive.BreathingManager;
import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.block.forcefield.BlockForceField;
import cr0s.warpdrive.data.CelestialObjectManager;
import cr0s.warpdrive.config.Dictionary;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.CelestialObject;
import cr0s.warpdrive.data.StateAir;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.data.VectorI;
import cr0s.warpdrive.world.SpaceTeleporter;

import java.util.HashMap;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LivingHandler {
	
	private final HashMap<UUID, Integer> player_cloakTicks;
	private final HashMap<Integer, Double> entity_yMotion;
	
	private static final int CLOAK_CHECK_TIMEOUT_TICKS = 100;
	
	private static final int BORDER_WARNING_RANGE_BLOCKS_SQUARED = 20 * 20;
	private static final int BORDER_BYPASS_RANGE_BLOCKS_SQUARED = 32 * 32;
	private static final int BORDER_BYPASS_PULL_BACK_BLOCKS = 16;
	private static final int BORDER_BYPASS_DAMAGES_PER_TICK = 9000;
	
	public LivingHandler() {
		player_cloakTicks = new HashMap<>();
		entity_yMotion = new HashMap<>();
	}
	
	@SubscribeEvent
	public void onLivingUpdate(final LivingUpdateEvent event) {
		if (event.getEntityLiving() == null || event.getEntityLiving().worldObj.isRemote) {
			return;
		}
		
		final EntityLivingBase entityLivingBase = event.getEntityLiving();
		final int x = MathHelper.floor_double(entityLivingBase.posX);
		final int y = MathHelper.floor_double(entityLivingBase.posY);
		final int z = MathHelper.floor_double(entityLivingBase.posZ);
		
		// *** save motion for fall damage computation
		if (!entityLivingBase.onGround) {
			entity_yMotion.put(entityLivingBase.getEntityId(), entityLivingBase.motionY);
		}
		
		// *** world border handling
		// Instant kill if entity exceeds world's limit
		final CelestialObject celestialObject = CelestialObjectManager.get(entityLivingBase.worldObj, x, z);
		if (celestialObject == null) {
			// unregistered dimension => exit
			return;
		}
		final double distanceSquared = celestialObject.getSquareDistanceOutsideBorder(x, z);
		if (distanceSquared <= 0.0D) {
			// are we close to the border?
			if ( Math.abs(distanceSquared) <= BORDER_WARNING_RANGE_BLOCKS_SQUARED
			  && entityLivingBase instanceof EntityPlayer
			  && entityLivingBase.ticksExisted % 40 == 0) {
				Commons.addChatMessage( entityLivingBase,
					new TextComponentString(String.format("§cProximity alert: world border is only %d m away!", 
				                                     (int) Math.sqrt(Math.abs(distanceSquared)))) );
			}
		} else {
			if (entityLivingBase instanceof EntityPlayerMP) {
				if (((EntityPlayerMP) entityLivingBase).capabilities.isCreativeMode) {
					if (entityLivingBase.ticksExisted % 100 == 0) {
						Commons.addChatMessage( entityLivingBase,
							new TextComponentString(String.format("§cYou're %d m outside the world border...", 
						                                     (int) Math.sqrt(Math.abs(distanceSquared)))) );
					}
					return;
				}
			}
			
			// pull back the entity
			final double relativeX = entityLivingBase.posX - celestialObject.dimensionCenterX;
			final double relativeZ = entityLivingBase.posZ - celestialObject.dimensionCenterZ;
			final double newAbsoluteX = Math.min(Math.abs(relativeX), Math.max(0.0D, celestialObject.borderRadiusX - BORDER_BYPASS_PULL_BACK_BLOCKS));
			final double newAbsoluteZ = Math.min(Math.abs(relativeZ), Math.max(0.0D, celestialObject.borderRadiusZ - BORDER_BYPASS_PULL_BACK_BLOCKS));
			final double newEntityX = celestialObject.dimensionCenterX + Math.signum(relativeX) * newAbsoluteX;
			final double newEntityY = entityLivingBase.posY + 0.1D;
			final double newEntityZ = celestialObject.dimensionCenterX + Math.signum(relativeZ) * newAbsoluteZ;
			// entity.isAirBorne = true;
			Commons.moveEntity(entityLivingBase, entityLivingBase.worldObj, new Vector3(newEntityX, newEntityY, newEntityZ));
			
			// spam chat if it's a player
			if (entityLivingBase instanceof EntityPlayer && !entityLivingBase.isDead && entityLivingBase.deathTime <= 0) {
				Commons.addChatMessage( entityLivingBase,
					new TextComponentString("§4You've reached the world border...") );
			}
			
			// delay damage for 'fast moving' players
			if (distanceSquared < BORDER_BYPASS_RANGE_BLOCKS_SQUARED) {
				// just set on fire
				entityLivingBase.setFire(1);
			} else {
				// full damage
				entityLivingBase.attackEntityFrom(DamageSource.outOfWorld, BORDER_BYPASS_DAMAGES_PER_TICK);
				return;
			}
		}
		
		if (entityLivingBase instanceof EntityPlayerMP) {
			// *** cloak handling
			updatePlayerCloakState(entityLivingBase);
			
			// *** air handling
			if ( WarpDriveConfig.BREATHING_AIR_AT_ENTITY_DEBUG
			  && entityLivingBase.worldObj.getWorldTime() % 20 == 0) {
				StateAir.dumpAroundEntity((EntityPlayer) entityLivingBase);
			}
		}
		
		// skip dead or invulnerable entities
		if (entityLivingBase.isDead || entityLivingBase.isEntityInvulnerable(WarpDrive.damageAsphyxia)) {
			return;
		}
		
		// If entity is in vacuum, check and start consuming air cells
		if (!celestialObject.hasAtmosphere()) {
			// skip players in creative
			if ( !(entityLivingBase instanceof EntityPlayerMP)
			  || !((EntityPlayerMP) entityLivingBase).capabilities.isCreativeMode ) {
				BreathingManager.onLivingUpdateEvent(entityLivingBase, x, y, z);
			}
		}
		
		
		// *** world transition handling
		// If player falling down, teleport to child celestial object
		if (entityLivingBase.posY < -10.0D) {
			final CelestialObject celestialObjectChild = CelestialObjectManager.getClosestChild(entityLivingBase.worldObj, x, z);
			// are we actually in orbit?
			if ( celestialObjectChild != null
			  && !celestialObject.isHyperspace()
			  && celestialObjectChild.isInOrbit(entityLivingBase.worldObj.provider.getDimension(), x, z) ) {
				
				final WorldServer worldTarget = Commons.getOrCreateWorldServer(celestialObjectChild.dimensionId);
				if (worldTarget == null) {
					WarpDrive.logger.error(String.format("Unable to initialize dimension %d for %s",
					                                     celestialObjectChild.dimensionId,
					                                     entityLivingBase));
				} else {
					final VectorI vEntry = celestialObjectChild.getEntryOffset();
					final int xTarget = x + vEntry.x;
					final int yTarget = worldTarget.getActualHeight() + 5;
					final int zTarget = z + vEntry.z;
					
					if (entityLivingBase instanceof EntityPlayerMP) {
						final EntityPlayerMP player = (EntityPlayerMP) entityLivingBase;
						
						// add tolerance to fall distance
						player.fallDistance = -5.0F;
						
						// transfer player to new dimension
						player.mcServer.getPlayerList().transferPlayerToDimension(
								player,
								celestialObjectChild.dimensionId,
								new SpaceTeleporter(worldTarget, 0, xTarget, yTarget, zTarget) );
						
						// add fire if we're entering an atmosphere
						if (!celestialObject.hasAtmosphere() && celestialObjectChild.hasAtmosphere()) {
							player.setFire(30);
						}
						
						// close player transfer 
						player.setPositionAndUpdate(xTarget + 0.5D, yTarget, zTarget + 0.5D);
						player.sendPlayerAbilities();
					}
				}
				
			} else if (celestialObject.isHyperspace() || celestialObject.isSpace()) {
				// player is in space or hyperspace, let's roll around
				entityLivingBase.setPositionAndUpdate(entityLivingBase.posX, 260.0D, entityLivingBase.posZ);
			}
		}
	}
	
	private void updatePlayerCloakState(final EntityLivingBase entity) {
		try {
			final EntityPlayerMP player = (EntityPlayerMP) entity;
			final Integer cloakTicks = player_cloakTicks.get(player.getUniqueID());
			
			if (cloakTicks == null) {
				player_cloakTicks.put(player.getUniqueID(), 0);
				return;
			}
			
			if (cloakTicks >= CLOAK_CHECK_TIMEOUT_TICKS) {
				player_cloakTicks.put(player.getUniqueID(), 0);
				
				WarpDrive.cloaks.updatePlayer(player);
			} else {
				player_cloakTicks.put(player.getUniqueID(), cloakTicks + 1);
			}
		} catch (final Exception exception) {
			exception.printStackTrace();
		}
	}
	
	@SubscribeEvent
	public void onLivingFall(final LivingFallEvent event) {
		// player in the overworld falling from
		// 3 blocks high is motionY = -0,6517088xxx
		// 4 blocks high is motionY = -0,717
		// 5 blocks high is motionY = -0.844
		// slime in the overworld falling from
		// 2.177279 blocks high is motionY -0.6517000126838683
		// spider in the overworld falling from
		// 3.346 blocks high is motionY -0.717
		
		// cancel in case of very low speed
		final EntityLivingBase entityLivingBase = event.getEntityLiving();
		Double motionY = entity_yMotion.get(entityLivingBase.getEntityId());
		if (motionY == null) {
			motionY = entityLivingBase.motionY;
		}
		if (motionY > -0.65170D) {
			event.setCanceled(true); // Don't damage entity
			if (WarpDrive.isDev && entityLivingBase instanceof EntityPlayerMP) {
				WarpDrive.logger.warn(String.format("(low speed     ) Entity fall damage at motionY %.3f from distance %.3f of %s, isCancelled %s",
				                                    motionY, event.getDistance(), entityLivingBase, event.isCanceled()));
			}
			return;
		}
		
		// get vanilla check for fall distance, as found in EntityLivingBase.fall()
		// we're ignoring the jump potion effect bonus
		final float distance = event.getDistance();
		final int check = MathHelper.ceiling_float_int(distance - 3.0F);
		// ignore small jumps
		if (check <= 0) {
			event.setCanceled(true); // Don't damage entity
			if (WarpDrive.isDev && entityLivingBase instanceof EntityPlayerMP) {
				WarpDrive.logger.warn(String.format("(short distance) Entity fall damage at motionY %.3f from distance %.3f of %s, isCancelled %s",
				                                    motionY, event.getDistance(), entityLivingBase, event.isCanceled()));
			}
			return;
		}
		
		if (WarpDrive.isDev) {
			WarpDrive.logger.warn(String.format("Entity fall damage at motionY %.3f from distance %.3f of %s, isCancelled %s",
			                                    motionY, event.getDistance(), entityLivingBase, event.isCanceled()));
		}
		
		// check for equipment with NOFALLDAMAGE tag
		for (EntityEquipmentSlot entityEquipmentSlot : EntityEquipmentSlot.values()) {
			final ItemStack itemStackInSlot = entityLivingBase.getItemStackFromSlot(entityEquipmentSlot);
			if (itemStackInSlot != null) {
				if (Dictionary.ITEMS_NOFALLDAMAGE.contains(itemStackInSlot.getItem())) {
					event.setCanceled(true); // Don't damage entity
					if (WarpDrive.isDev && entityLivingBase instanceof EntityPlayerMP) {
						WarpDrive.logger.warn(String.format("(boot absorbed ) Entity fall damage at motionY %.3f from distance %.3f of %s, isCancelled %s",
						                                    motionY, event.getDistance(), entityLivingBase, event.isCanceled()));
					}
				}
			}
		}
		
		// (entity has significant speed, above minimum distance and there's no absorption)
		
		// adjust distance to 'vanilla' scale and let it fall...
		event.setDistance( (float) (-6.582D + 4.148D * Math.exp(1.200D * Math.abs(motionY))) );
		if (WarpDrive.isDev && entityLivingBase instanceof EntityPlayerMP) {
			WarpDrive.logger.warn(String.format("(full damage   ) Entity fall damage at motionY %.3f from distance %.3f of %s, isCancelled %s",
			                                    motionY, event.getDistance(), entityLivingBase, event.isCanceled()));
		}
	}
	
	@SubscribeEvent
	public void onEnderTeleport(final EnderTeleportEvent event) {
		if ( event.getEntityLiving() == null
		  || event.getEntityLiving().worldObj.isRemote ) {
			return;
		}
		
		final World world = event.getEntityLiving().worldObj;
		final int x = MathHelper.floor_double(event.getTargetX());
		final int y = MathHelper.floor_double(event.getTargetY());
		final int z = MathHelper.floor_double(event.getTargetZ());
		
		final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(x, y, z);
		for (int xLoop = x - 1; xLoop <= x + 1; xLoop++) {
			for (int zLoop = z - 1; zLoop <= z + 1; zLoop++) {
				for (int yLoop = y - 1; yLoop <= y + 1; yLoop++) {
					if (yLoop <= 0 || yLoop > 255) {
						continue;
					}
					mutableBlockPos.setPos(xLoop, yLoop, zLoop);
					final Block block = world.getBlockState(mutableBlockPos).getBlock();
					if (block instanceof BlockForceField) {
						event.setCanceled(true);
						return;
					}
				}
			}
		}
	}
}
