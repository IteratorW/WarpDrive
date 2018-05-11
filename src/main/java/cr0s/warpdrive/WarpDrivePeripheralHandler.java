package cr0s.warpdrive;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralProvider;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Optional;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

@Optional.InterfaceList({
	@Optional.Interface(iface = "dan200.computercraft.api.peripheral.IPeripheralProvider", modid = "ComputerCraft")
})
public class WarpDrivePeripheralHandler implements IPeripheralProvider {
	public void register() {
		ComputerCraftAPI.registerPeripheralProvider(this);
	}
	
	@Override
	@Optional.Method(modid = "ComputerCraft")
	public IPeripheral getPeripheral(final World world, final BlockPos blockPos, final EnumFacing side) {
		final TileEntity tileEntity = world.getTileEntity(new BlockPos(blockPos));
		if (tileEntity instanceof IPeripheral && ((IPeripheral) tileEntity).getType() != null) {
			return (IPeripheral)tileEntity;
		}
		return null;
	}
}