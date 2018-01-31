package cr0s.warpdrive.network;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.Vector3;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFirework;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumParticleTypes;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class MessageSpawnParticle implements IMessage, IMessageHandler<MessageSpawnParticle, IMessage> {
	
	private String type;
	private byte quantity;
	private Vector3 origin;
	private Vector3 direction;
	private float baseRed;
	private float baseGreen;
	private float baseBlue;
	private float fadeRed;
	private float fadeGreen;
	private float fadeBlue;
	
	@SuppressWarnings("unused")
	public MessageSpawnParticle() {
		// required on receiving side
	}
	
	MessageSpawnParticle(final String type, final byte quantity, final Vector3 origin, final Vector3 direction,
	                     final float baseRed, final float baseGreen, final float baseBlue,
	                     final float fadeRed, final float fadeGreen, final float fadeBlue) {
		this.type = type;
		this.quantity = quantity;
		this.origin = origin;
		this.direction = direction;
		this.baseRed = baseRed;
		this.baseGreen = baseGreen;
		this.baseBlue = baseBlue;
		this.fadeRed = fadeRed;
		this.fadeGreen = fadeGreen;
		this.fadeBlue = fadeBlue;
	}
	
	@Override
	public void fromBytes(ByteBuf buffer) {
		int typeSize = buffer.readByte();
		type = buffer.toString(buffer.readerIndex(), typeSize, StandardCharsets.US_ASCII);
		buffer.skipBytes(typeSize);
		
		quantity = buffer.readByte();
		
		double x = buffer.readDouble();
		double y = buffer.readDouble();
		double z = buffer.readDouble();
		origin = new Vector3(x, y, z);
		
		x = buffer.readDouble();
		y = buffer.readDouble();
		z = buffer.readDouble();
		direction = new Vector3(x, y, z);
		
		baseRed = buffer.readFloat();
		baseGreen = buffer.readFloat();
		baseBlue = buffer.readFloat();
		fadeRed = buffer.readFloat();
		fadeGreen = buffer.readFloat();
		fadeBlue = buffer.readFloat();
	}
	
	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeByte(type.length());
		buffer.writeBytes(type.getBytes(StandardCharsets.US_ASCII), 0, type.length());
		buffer.writeByte(quantity);
		buffer.writeDouble(origin.x);
		buffer.writeDouble(origin.y);
		buffer.writeDouble(origin.z);
		buffer.writeDouble(direction.x);
		buffer.writeDouble(direction.y);
		buffer.writeDouble(direction.z);
		buffer.writeFloat(baseRed);
		buffer.writeFloat(baseGreen);
		buffer.writeFloat(baseBlue);
		buffer.writeFloat(fadeRed);
		buffer.writeFloat(fadeGreen);
		buffer.writeFloat(fadeBlue);
	}
	
	private int integerFromRGB(final float red, final float green, final float blue) {
		return (Math.round(red * 255.0F) << 16)
			+  (Math.round(green * 255.0F) << 8)
			+   Math.round(blue * 255.0F);
	}
	
	@SideOnly(Side.CLIENT)
	private void handle(World world) {
		// Directly spawn particle as per RenderGlobal.spawnParticle, bypassing range check
		// adjust color as needed
		Minecraft mc = Minecraft.getMinecraft();
		Entity entity = mc.getRenderViewEntity();
		if (entity == null || mc.effectRenderer == null) {
			return;
		}
		if (mc.gameSettings.particleSetting == 1 && world.rand.nextInt(3) != 0) {
			return;
		}
		
		Particle particle;
		double noiseLevel = direction.getMagnitude() * 0.35D;
		for (int index = 0; index < quantity; index++) {
			Vector3 directionRandomized = new Vector3(
					direction.x + noiseLevel * (world.rand.nextFloat() - world.rand.nextFloat()),
					direction.y + noiseLevel * (world.rand.nextFloat() - world.rand.nextFloat()),
					direction.z + noiseLevel * (world.rand.nextFloat() - world.rand.nextFloat()));
			switch (type) {
			default:
				WarpDrive.logger.error(String.format("Invalid particle type '%s' at %s", type, origin.toString()));
			case "explode":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.EXPLOSION_NORMAL.getParticleID(),
						origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "fireworksSpark":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FIREWORKS_SPARK.getParticleID(),
						origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				if (particle instanceof ParticleFirework.Spark) {
					((ParticleFirework.Spark) particle).setColorFade(integerFromRGB(fadeRed, fadeGreen, fadeBlue));
				}
				break;
			
			case "flame":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.getParticleID(),
						origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "snowballpoof":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.SNOWBALL.getParticleID(),
						origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "snowshovel":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.SNOW_SHOVEL.getParticleID(),
						origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "mobSpell":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.SPELL_MOB.getParticleID(),
						origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
				
			case "cloud":
				particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.CLOUD.getParticleID(),
					origin.x, origin.y, origin.z, directionRandomized.x, directionRandomized.y, directionRandomized.z);
				break;
			
			case "jammed":// jammed machine particle reusing vanilla angryVillager particle
				// as of MC1.7.10, direction vector is ignored by upstream
				final EnumFacing directionFacing = Commons.getHorizontalDirectionFromEntity(Minecraft.getMinecraft().thePlayer);
				if (directionFacing.getFrontOffsetX() != 0) {
					particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.VILLAGER_ANGRY.getParticleID(),
					        origin.x + 0.51D * directionFacing.getFrontOffsetX(),
					        origin.y - 0.50D + world.rand.nextDouble(),
					        origin.z - 0.50D + world.rand.nextDouble(),
					        directionRandomized.x, directionRandomized.y, directionRandomized.z);
				} else {
					particle = mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.VILLAGER_ANGRY.getParticleID(),
					        origin.x - 0.50D + world.rand.nextDouble(),
					        origin.y - 0.50D + world.rand.nextDouble(),
					        origin.z + 0.51D * directionFacing.getFrontOffsetZ(),
					        directionRandomized.x, directionRandomized.y, directionRandomized.z);
				}
				assert(particle != null);
				particle.multipleParticleScaleBy(0.5F + world.rand.nextFloat() * 1.5F);
				particle.setParticleTextureIndex(81);
				break;
			} 
			
			if (particle == null) {
				continue;
			}
			
			if (baseRed >= 0.0F && baseGreen >= 0.0F && baseBlue >= 0.0F) {
				particle.setRBGColorF(baseRed, baseGreen, baseBlue);
			}
			mc.effectRenderer.addEffect(particle);
		}
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(MessageSpawnParticle messageSpawnParticle, MessageContext context) {
		// skip in case player just logged in
		if (Minecraft.getMinecraft().theWorld == null) {
			WarpDrive.logger.error("WorldObj is null, ignoring particle packet");
			return null;
		}
		
		if (WarpDriveConfig.LOGGING_EFFECTS) {
			WarpDrive.logger.info("Received particle effect '%s' x %d from %s towards %s as RGB %.2f %.2f %.2f fading to %.2f %.2f %.2f",
				messageSpawnParticle.type, messageSpawnParticle.quantity, messageSpawnParticle.origin, messageSpawnParticle.direction,
				messageSpawnParticle.baseRed, messageSpawnParticle.baseGreen, messageSpawnParticle.baseBlue,
				messageSpawnParticle.fadeRed, messageSpawnParticle.fadeGreen, messageSpawnParticle.fadeBlue);
		}
		
		messageSpawnParticle.handle(Minecraft.getMinecraft().theWorld);
		
		return null;	// no response
	}
}
