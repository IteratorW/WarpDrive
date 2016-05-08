package cr0s.warpdrive.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import cr0s.warpdrive.WarpDrive;

/*
 *   /wentity <radius> <filter> <kill?>
 */

public class CommandEntity extends CommandBase {
	private static List<String> entitiesNoRemoval = Arrays.asList(
			"item.EntityItemFrame_" 
			);
	private static List<String> entitiesNoCount = Arrays.asList(
			"item.EntityItemFrame_" 
			);
	
	@Override
	public String getCommandName() {
		return "wentity";
	}
	
	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getCommandUsage(ICommandSender par1ICommandSender) {
		return "/" + getCommandName() + " <radius> <filter> <kill?>"
			+ "\nradius: - or <= 0 to check all loaded in current world, 1+ blocks around player"
			+ "\nfilter: * to get all, anything else is a case insensitive string"
			+ "\nkill: yes/y/1 to kill, anything else is ignored";
	}
	
	@Override
	public void processCommand(ICommandSender icommandsender, String[] params) {
		EntityPlayerMP player = (EntityPlayerMP) icommandsender;
		if (params.length > 3) {
			WarpDrive.addChatMessage(player, getCommandUsage(icommandsender));
			return;
		}
		
		int radius = 20;
		String filter = "";
		boolean kill = false;
		try {
			if (params.length > 0) {
				String par = params[0].toLowerCase();
				if (par.equals("-") || par.equals("world") || par.equals("global") || par.equals("*")) {
					radius = -1;
				} else {
					radius = Integer.parseInt(par);
				}
			}
			if (params.length > 1) {
				if (!params[1].equalsIgnoreCase("*")) {
					filter = params[1];
				}
			}
			if (params.length > 2) {
				String par = params[2].toLowerCase();
				kill = par.equals("y") || par.equals("yes") || par.equals("1");
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			WarpDrive.addChatMessage(player, getCommandUsage(icommandsender));
			return;
		}
		
		WarpDrive.logger.info("/" + getCommandName() + " " + radius + " '*" + filter + "*' " + kill);
		
		Collection<Object> entities;
		if (radius <= 0) {
			entities = new ArrayList<Object>();
			entities.addAll(player.worldObj.loadedEntityList);
		} else {
			entities = player.worldObj.getEntitiesWithinAABBExcludingEntity(player, AxisAlignedBB.getBoundingBox(
					Math.floor(player.posX    ), Math.floor(player.posY    ), Math.floor(player.posZ    ),
					Math.floor(player.posX + 1), Math.floor(player.posY + 1), Math.floor(player.posZ + 1)).expand(radius, radius, radius));
		}
		HashMap<String, Integer> counts = new HashMap<String, Integer>(entities.size());
		int count = 0;
		for (Object object : entities) {
			if (object instanceof Entity) {
				String name = object.getClass().getCanonicalName();
				if (name == null) {
					name = "-null-";
				} else {
					name = name.replaceAll("net\\.minecraft\\.entity\\.", "") + "_";
				}
				if (filter.isEmpty() && !entitiesNoCount.isEmpty()) {
					for (String entityNoCount : entitiesNoCount) {
						if (name.contains(entityNoCount)) {
							continue;
						}
					}
				}
				if (filter.isEmpty() || name.contains(filter)) {
					// update statistics
					count++;
					if (!counts.containsKey(name)) {
						counts.put(name, 1);
					} else {
						counts.put(name, counts.get(name) + 1);
					}
					if (!filter.isEmpty()) {
						WarpDrive.addChatMessage(player, "§cFound " + object);
					}
					// remove entity
					if (kill && !((Entity) object).invulnerable) {
						if (!entitiesNoRemoval.isEmpty()) {
							for (String entityNoRemoval : entitiesNoRemoval) {
								if (name.contains(entityNoRemoval)) {
									continue;
								}
							}
						}
						((Entity) object).setDead();
					}
				}
			}
		}
		if (count == 0) {
			WarpDrive.addChatMessage(player, "§cNo matching entities found within " + radius + " blocks");
			return;
		}
		
		WarpDrive.addChatMessage(player, "§6" + count + " matching entities within " + radius + " blocks:");
		if (counts.size() < 10) {
			for (Entry<String, Integer> entry : counts.entrySet()) {
				WarpDrive.addChatMessage(player, "§f" + entry.getValue() + "§8x§d" + entry.getKey());
			}
		} else {
			String message = "";
			for (Entry<String, Integer> entry : counts.entrySet()) {
				if (!message.isEmpty()) {
					message += "§8" + ", ";
				}
				message += "§f" + entry.getValue() + "§8x§d" + entry.getKey();
			}
			WarpDrive.addChatMessage(player, message);
		}
	}
}