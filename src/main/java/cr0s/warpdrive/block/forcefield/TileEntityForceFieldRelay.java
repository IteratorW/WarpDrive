package cr0s.warpdrive.block.forcefield;

import cr0s.warpdrive.api.IForceFieldUpgrade;
import cr0s.warpdrive.api.IForceFieldUpgradeEffector;
import cr0s.warpdrive.data.EnumForceFieldUpgrade;
import cr0s.warpdrive.data.ForceFieldSetup;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;

import javax.annotation.Nonnull;

public class TileEntityForceFieldRelay extends TileEntityAbstractForceField implements IForceFieldUpgrade {

	// persistent properties
	private EnumForceFieldUpgrade upgrade = EnumForceFieldUpgrade.NONE;
	
	public TileEntityForceFieldRelay() {
		super();
		
		peripheralName = "warpdriveForceFieldRelay";
	}
	
	// onFirstUpdateTick
	// update
	
	protected EnumForceFieldUpgrade getUpgrade() {
		if (upgrade == null) {
			return EnumForceFieldUpgrade.NONE;
		}
		return upgrade;
	}
	
	protected void setUpgrade(EnumForceFieldUpgrade upgrade) {
		this.upgrade = upgrade;
		markDirty();
	}
	
	@Override
	protected ITextComponent getUpgradeStatus() {
		EnumForceFieldUpgrade enumForceFieldUpgrade = getUpgrade();
		ITextComponent strDisplayName = new TextComponentTranslation("warpdrive.forcefield.upgrade.statusLine." + enumForceFieldUpgrade.getName());
		if (enumForceFieldUpgrade == EnumForceFieldUpgrade.NONE) {
			return new TextComponentTranslation("warpdrive.upgrade.statusLine.none",
				strDisplayName);
		} else {
			return new TextComponentTranslation("warpdrive.upgrade.statusLine.valid",
				strDisplayName);
		}
	}
	
	@Override
	public ITextComponent getStatus() {
		return super.getStatus()
		    .appendSibling(new TextComponentString("\n")).appendSibling(getUpgradeStatus());
	}
	
	@Override
	public void readFromNBT(NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);
		setUpgrade(EnumForceFieldUpgrade.get(tagCompound.getByte("upgrade")));
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
		tagCompound = super.writeToNBT(tagCompound);
		tagCompound.setByte("upgrade", (byte) getUpgrade().ordinal());
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
	public void onDataPacket(NetworkManager networkManager, SPacketUpdateTileEntity packet) {
		final NBTTagCompound tagCompound = packet.getNbtCompound();
		readFromNBT(tagCompound);
	}
	
	@Override
	public IForceFieldUpgradeEffector getUpgradeEffector() {
		return isEnabled ? getUpgrade() : null;
	}
	
	@Override
	public float getUpgradeValue() {
		return isEnabled ? getUpgrade().getUpgradeValue() * (1.0F + (tier - 1) * ForceFieldSetup.FORCEFIELD_UPGRADE_BOOST_FACTOR_PER_RELAY_TIER) : 0.0F;
	}
}
