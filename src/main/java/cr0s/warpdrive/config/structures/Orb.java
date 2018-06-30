package cr0s.warpdrive.config.structures;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.config.GenericSet;
import cr0s.warpdrive.config.InvalidXmlException;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.config.XmlFileManager;
import cr0s.warpdrive.config.Filler;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Random;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Orb extends AbstractStructure {
	
	protected OrbShell[] orbShells;
	protected boolean hasStarCore = false;
	protected String schematicName;
	
	public Orb(final String group, final String name) {
		super(group, name);
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		super.loadFromXmlElement(element);
		
		final List<Element> listShells = XmlFileManager.getChildrenElementByTagName(element, "shell");
		orbShells = new OrbShell[listShells.size()];
		int shellIndexOut = 0;
		for (final Element elementShell : listShells) {
			final String orbShellName = elementShell.getAttribute("name");
			
			orbShells[shellIndexOut] = new OrbShell(getFullName(), orbShellName);
			try {
				orbShells[shellIndexOut].loadFromXmlElement(elementShell);
				shellIndexOut++;
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace();
				WarpDrive.logger.error(String.format("Skipping invalid shell %s", orbShellName));
			}
		}
		
		final List<Element> listSchematic = XmlFileManager.getChildrenElementByTagName(element, "schematic");
		if (listSchematic.size() > 1) {
			WarpDrive.logger.error(String.format("Too many schematic defined, only first one will be used in structure %s", getFullName()));
		}
		if (listSchematic.size() > 0) {
			schematicName = listSchematic.get(0).getAttribute("group");
		}
		
		return true;
	}
	
	@Override
	public boolean generate(final World world, final Random random, final BlockPos blockPos) {
		return instantiate(random).generate(world, random, blockPos);
	}
	
	@Override
	public AbstractStructureInstance instantiate(final Random random) {
		return new OrbInstance(this, random);
	}
	
	public class OrbShell extends GenericSet<Filler> {
		
		private final String parentFullName;
		protected int minThickness;
		protected int maxThickness;
		
		public OrbShell(final String parentFullName, final String name) {
			super(null, name, Filler.DEFAULT, "filler");
			this.parentFullName = parentFullName;
		}
		
		@Override
		public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
			if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
				WarpDrive.logger.info(String.format("  + found shell %s",
				                                    element.getAttribute("name")));
			}
			
			super.loadFromXmlElement(element);
			
			// resolve static imports
			for (final String importGroupName : getImportGroupNames()) {
				final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getGenericSet(importGroupName);
				if (fillerSet == null) {
					WarpDrive.logger.warn(String.format("Skipping missing FillerSet %s in shell %s:%s",
					                                    importGroupName, parentFullName, name));
				} else {
					loadFrom(fillerSet);
				}
			}
			
			// validate dynamic imports
			for (final String importGroup : getImportGroups()) {
				if (!WarpDriveConfig.FillerManager.doesGroupExist(importGroup)) {
					WarpDrive.logger.warn(String.format("An invalid FillerSet group %s is referenced in shell %s:%s",
					                                    importGroup, parentFullName, name));
				}
			}
			
			// shell thickness
			try {
				minThickness = Integer.parseInt(element.getAttribute("minThickness"));
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Invalid minThickness in shell %s of structure %s",
				                                            name, parentFullName));
			}
			
			try {
				maxThickness = Integer.parseInt(element.getAttribute("maxThickness"));
			} catch (final NumberFormatException exception) {
				throw new InvalidXmlException(String.format("Invalid maxThickness in shell %s of structure %s",
				                                            name, parentFullName));
			}
			
			if (maxThickness < minThickness) {
				throw new InvalidXmlException(String.format("Invalid maxThickness %d lower than minThickness %s in shell %s of orb %s",
				                                            maxThickness, minThickness, name, parentFullName));
			}
			
			return true;
		}
		
		public OrbShell instantiate(final Random random) {
			final OrbShell orbShell = new OrbShell(parentFullName, name);
			orbShell.minThickness = minThickness;
			orbShell.maxThickness = maxThickness;
			try {
				orbShell.loadFrom(this);
				for (final String importGroup : getImportGroups()) {
					final GenericSet<Filler> fillerSet = WarpDriveConfig.FillerManager.getRandomSetFromGroup(random, importGroup);
					if (fillerSet == null) {
						WarpDrive.logger.info(String.format("Ignoring invalid group %s in shell %s of structure %s",
						                                    importGroup, name, parentFullName));
						continue;
					}
					if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
						WarpDrive.logger.info(String.format("Filling %s:%s with %s:%s",
						                                    parentFullName, name, importGroup, fillerSet.getName()));
					}
					orbShell.loadFrom(fillerSet);
				}
			} catch (final InvalidXmlException exception) {
				exception.printStackTrace();
				WarpDrive.logger.error(String.format("Failed to instantiate shell %s from structure %s",
				                                     name, parentFullName));
			}
			if (orbShell.isEmpty()) {
				if (WarpDriveConfig.LOGGING_WORLD_GENERATION) {
					WarpDrive.logger.info(String.format("Ignoring empty shell %s in structure %s",
					                                    name, parentFullName));
				}
				return null;
			}
			return orbShell;
		}
	}
}
