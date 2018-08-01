package cr0s.warpdrive.block.atomic;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IControlChannel;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractMachine;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.peripheral.IComputerAccess;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;

public class TileEntityAcceleratorControlPoint extends TileEntityAbstractMachine implements IControlChannel {
	
	// persistent properties
	private int controlChannel = -1;
	
	// computed properties
	private static final int UPDATE_INTERVAL_TICKS = 20;
	private int updateTicks;
	
	public TileEntityAcceleratorControlPoint() {
		super();
		
		peripheralName = "warpdriveAcceleratorControlPoint";
		addMethods(new String[] {
			"state",
			"controlChannel"
		});
	}
	
	@Override
	protected void onFirstUpdateTick() {
		super.onFirstUpdateTick();
	}
	
	@Override
	public void update() {
		super.update();
		
		if (world.isRemote) {
			return;
		}
		
		updateTicks--;
		if (updateTicks <= 0) {
			updateTicks = UPDATE_INTERVAL_TICKS;
			updateBlockState(null, BlockProperties.ACTIVE, (controlChannel != -1) && isEnabled);
		}
	}
	
	@Override
	public void invalidate() {
		super.invalidate();
	}
	
	@Override
	public int getControlChannel() {
		return controlChannel;
	}
	
	@Override
	public void setControlChannel(final int controlChannel) {
		if (this.controlChannel != controlChannel) {
			this.controlChannel = controlChannel;
			if (WarpDriveConfig.LOGGING_VIDEO_CHANNEL) {
				WarpDrive.logger.info(this + " Accelerator control point controlChannel channel set to " + controlChannel);
			}
			// force update through main thread since CC runs on server as 'client'
			markDirty();
		}
	}
	
	private WarpDriveText getControlChannelStatus() {
		if (controlChannel == -1) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.control_channel.status_line.undefined");
		} else if (controlChannel < CONTROL_CHANNEL_MIN || controlChannel > CONTROL_CHANNEL_MAX) {
			return new WarpDriveText(Commons.styleWarning, "warpdrive.control_channel.status_line.invalid",
			                         controlChannel);
		} else {
			return new WarpDriveText(Commons.styleCorrect, "warpdrive.control_channel.status_line.valid",
			                         controlChannel);
		}
	}
	
	@Override
	public WarpDriveText getStatus() {
		return super.getStatus()
		       .append(getControlChannelStatus());
	}
	
	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		
		controlChannel = tagCompound.getInteger(CONTROL_CHANNEL_TAG);
	}
	
	@Nonnull
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		
		tagCompound.setInteger(CONTROL_CHANNEL_TAG, controlChannel);
		return tagCompound;
	}
	
	@Nonnull
	@Override
	public NBTTagCompound getUpdateTag() {
		final NBTTagCompound tagCompound = new NBTTagCompound();
		writeToNBT(tagCompound);
		return tagCompound;
	}
	
	@Override
	public void onDataPacket(final NetworkManager networkManager, final SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
	}
	
	@Override
	public void setIsEnabled(final boolean isEnabled) {
		super.setIsEnabled(isEnabled);
		WarpDrive.starMap.onBlockUpdated(world, pos, world.getBlockState(pos));
	}
	
	// Common OC/CC methods
	public Object[] controlChannel(final Object[] arguments) {
		if ( arguments != null
		  && arguments.length == 1
		  && arguments[0] != null ) {
			final int controlChannelRequested;
			try {
				controlChannelRequested = Commons.toInt(arguments[0]);
			} catch (final Exception exception) {
				if (WarpDriveConfig.LOGGING_LUA) {
					WarpDrive.logger.error(String.format("%s LUA error on enable(): Integer expected for 1st argument %s",
					                                     this, arguments[0]));
				}
				return new Object[] { controlChannel };
			}
			setControlChannel(controlChannelRequested);
		}
		return new Integer[] { controlChannel };
	}
	
	private Object[] state() {
		final String status = getStatusHeaderInPureText();
		return new Object[] { status, isEnabled, controlChannel };
	}
	
	// OpenComputer callback methods
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] controlChannel(final Context context, final Arguments arguments) {
		return controlChannel(OC_convertArgumentsAndLogCall(context, arguments));
	}
	
	@Callback
	@Optional.Method(modid = "opencomputers")
	public Object[] state(final Context context, final Arguments arguments) {
		return state();
	}
	
	// ComputerCraft IPeripheral methods implementation
	@Override
	@Optional.Method(modid = "computercraft")
	public Object[] callMethod(@Nonnull final IComputerAccess computer, @Nonnull final ILuaContext context, final int method, @Nonnull final Object[] arguments) {
		final String methodName = CC_getMethodNameAndLogCall(method, arguments);
		
		switch (methodName) {
		case "controlChannel":
			return controlChannel(arguments);
			
		case "state":
			return state();
		}
		
		return super.callMethod(computer, context, method, arguments);
	}
}
