package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.Gate;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.configuration.file.YamlConfiguration;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;

public class DoorFileManager {
	
	private Main instance;
	
	private File file;
	private YamlConfiguration config;
	
	public DoorFileManager(Main plugin) {
		this.instance = plugin;
		this.file = new File(this.instance.getDataFolder(), "Doors.yml");
		
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.config = YamlConfiguration.loadConfiguration(this.file);
	}
	
	private YamlConfiguration getConfig() {
		return this.config;
	}
	
	private String getLocationAsString(Location loc) {
		return loc.getWorld().getName() + "%" + loc.getBlockX() + "%" + loc.getBlockY() + "%" + loc.getBlockZ();
	}
	
	private Location getLocationFromString(String s) {
		String[] array = s.split("%");
		return new Location(Bukkit.getWorld(array[0]), Integer.parseInt(array[1]), Integer.parseInt(array[2]), Integer.parseInt(array[3]));
	}

	public boolean addDoor(Location loc, RentTypes type, int id) {
		YamlConfiguration cfg = this.getConfig();
		String locS = this.getLocationAsString(loc);
		
		cfg.set(locS + ".Type", type.toString().toUpperCase());
		cfg.set(locS + ".ID", id);
		cfg.set(locS + ".Single", true);
		
		try {
			cfg.save(this.file);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean addDoor(Door door, Location loc, RentTypes type, int id) {
		
		if(door.getHalf() == null) 
			return false;
				
		boolean isTop = door.getHalf().equals(Bisected.Half.TOP);
		
		Location locTop;
		Location locBottom;
		
		if(isTop) {
			//UPPER PART OF THE DOOR
			locTop = loc.clone();
			locBottom = loc.clone().subtract(0, 1, 0);
		}else {
			//LOWER PART OF THE DOOR
			locTop = loc.clone().add(0, 1, 0);
			locBottom = loc.clone();
		}
		
		YamlConfiguration cfg = this.getConfig();
		String topLocS = this.getLocationAsString(locTop);
		String bottomLocS = this.getLocationAsString(locBottom);
		
		cfg.set(topLocS + ".Type", type.toString().toUpperCase());
		cfg.set(topLocS + ".ID", id);
		cfg.set(topLocS + ".Top", true);
		
		cfg.set(bottomLocS + ".Type", type.toString().toUpperCase());
		cfg.set(bottomLocS + ".ID", id);
		cfg.set(bottomLocS + ".Top", false);
		
		try {
			cfg.save(this.file);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public boolean removeDoor(Location loc) {
		
		YamlConfiguration cfg = this.getConfig();
		String locS = this.getLocationAsString(loc);

		if(cfg.isSet(locS)) {
			try {
				cfg.set(locS, null);
				cfg.save(this.file);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	public boolean removeDoor(Door door, Location loc) {

		if(door.getHalf() == null) 
			return false;
				
		boolean isTop = door.getHalf().equals(Bisected.Half.TOP);
		
		Location locTop;
		Location locBottom;
		
		if(isTop) {
			//UPPER PART OF THE DOOR
			locTop = loc.clone();
			locBottom = loc.clone().subtract(0, 1, 0);
		}else {
			//LOWER PART OF THE DOOR
			locTop = loc.clone().add(0, 1, 0);
			locBottom = loc.clone();
		}
		
		YamlConfiguration cfg = this.getConfig();
		String locTopS = this.getLocationAsString(locTop);
		String locBottomS = this.getLocationAsString(locBottom);
		
		if(cfg.isSet(locTopS) || cfg.isSet(locBottomS)) {
			try {
				cfg.set(locTopS, null);
				cfg.set(locBottomS, null);
				cfg.save(this.file);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	public int getIdFromDoor(Location loc) {
		
		YamlConfiguration cfg = this.getConfig();
		String locS = this.getLocationAsString(loc);
		
		return cfg.getInt(locS + ".ID");
	}
	
	public RentTypes getTypeFromDoor(Location loc) { 
		
		YamlConfiguration cfg = this.getConfig();
		String locS = this.getLocationAsString(loc);
		
		return RentTypes.valueOf(cfg.getString(locS + ".Type").toUpperCase());
	}

	public boolean isProtectedDoor(Location loc) {
		
		YamlConfiguration cfg = this.getConfig();
		String locS = this.getLocationAsString(loc);
		
		return cfg.isSet(locS);
	}
	
	//ONLY THE BOTTOM DOOR, THEN CAST IT TO A DOOR AND CLOSE IT
	public void closeDoors(RentTypes type, int id) {
		this.setDoors(type, id, false);
	}
	
	public void openDoors(RentTypes type, int id) {
		this.setDoors(type, id, true);
	}
	
	private void setDoors(RentTypes type, int id, boolean open) {
		YamlConfiguration cfg = this.getConfig();
			
		if(cfg.getConfigurationSection("") == null)
			return;
			
		cfg.getConfigurationSection("").getKeys(false).forEach(locSPath -> {
			if(cfg.getString(locSPath + ".Type").equalsIgnoreCase(type.toString())) {
				if(cfg.getInt(locSPath + ".ID") == id) {
					if(cfg.getBoolean(locSPath + ".Single")) {
						Location loc = this.getLocationFromString(locSPath);
						
						if(loc.getBlock() != null) {
							if(loc.getBlock().getBlockData() instanceof TrapDoor) {
								TrapDoor trapDoor = (TrapDoor) loc.getBlock().getBlockData();
								trapDoor.setOpen(open);
								loc.getBlock().setBlockData(trapDoor);
							}else if(loc.getBlock().getBlockData() instanceof Gate) {
								Gate gate = (Gate) loc.getBlock().getBlockData();
								gate.setOpen(open);
								loc.getBlock().setBlockData(gate);
							}
						}
						
					}else if(!cfg.getBoolean(locSPath + ".Top")) {
						Location loc = this.getLocationFromString(locSPath);
						
						if(loc.getBlock() != null && loc.getBlock().getBlockData() instanceof Door) {
							Door door = (Door) loc.getBlock().getBlockData();
							door.setOpen(open);
							loc.getBlock().setBlockData(door);
						}
					}
				}
			}
		});
	}
	
	//ONLY THE BOTTOM DOOR, THEN CAST IT TO A DOOR AND CLOSE IT
	public void clearDoors(RentTypes type, int id) {
		YamlConfiguration cfg = this.getConfig();
			
		if(cfg.getConfigurationSection("") == null)
			return;
			
		cfg.getConfigurationSection("").getKeys(false).forEach(locSPath -> {
			if(cfg.getString(locSPath + ".Type").equalsIgnoreCase(type.toString())) {
				if(cfg.getInt(locSPath + ".ID") == id) {
					if(cfg.getBoolean(locSPath + ".Single")) {
						Location loc = this.getLocationFromString(locSPath);
						
						if(loc.getBlock() != null) {
							if(loc.getBlock().getBlockData() instanceof TrapDoor) {
								TrapDoor trapDoor = (TrapDoor) loc.getBlock().getBlockData();
								trapDoor.setOpen(false);
								loc.getBlock().setBlockData(trapDoor);
							}else if(loc.getBlock().getBlockData() instanceof Gate) {
								Gate gate = (Gate) loc.getBlock().getBlockData();
								gate.setOpen(false);
								loc.getBlock().setBlockData(gate);
							}
						}
						cfg.set(locSPath, null);
						
					}else if(!cfg.getBoolean(locSPath + ".Top")) {
						Location loc = this.getLocationFromString(locSPath);
						
						if(loc.getBlock() != null && loc.getBlock().getBlockData() instanceof Door) {
							Door door = (Door) loc.getBlock().getBlockData();
							door.setOpen(false);
							loc.getBlock().setBlockData(door);
						}
						
						cfg.set(locSPath, null);
					}
				}
			}
		});
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
