package cr0s.warpdrive.data;

import cr0s.warpdrive.world.JumpgateGenerator;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class Jumpgate {
	public String name;
	public int xCoord, yCoord, zCoord;
	
	public Jumpgate(final String name, final BlockPos blockPos) {
		this.name = name;
		this.xCoord = blockPos.getX();
		this.yCoord = blockPos.getY();
		this.zCoord = blockPos.getZ();
	}
	
	public Jumpgate(final String line) {
		String[] params = line.split(":");
		
		if (params.length < 4) {
			return;
		}
		
		name = params[0];
		xCoord = Integer.parseInt(params[1]);
		yCoord = Integer.parseInt(params[2]);
		zCoord = Integer.parseInt(params[3]);
	}
	
	public AxisAlignedBB getGateAABB() {
		int xMin, yMin, zMin;
		int xMax, yMax, zMax;
		xMin = xCoord - (JumpgateGenerator.GATE_LENGTH_HALF * 2);
		xMax = xCoord + (JumpgateGenerator.GATE_LENGTH_HALF * 2);
		yMin = yCoord - (JumpgateGenerator.GATE_SIZE_HALF);
		yMax = yCoord + (JumpgateGenerator.GATE_SIZE_HALF);
		zMin = zCoord - (JumpgateGenerator.GATE_SIZE_HALF);
		zMax = zCoord + (JumpgateGenerator.GATE_SIZE_HALF);
		return new AxisAlignedBB(xMin, yMin, zMin, xMax, yMax, zMax);
	}
	
	@Override
	public String toString() {
		return name + ":" + xCoord + ":" + yCoord + ":" + zCoord;
	}
	
	public String toNiceString() {
		return name + " (" + xCoord + ", " + yCoord + ", " + zCoord + ")";
	}
}