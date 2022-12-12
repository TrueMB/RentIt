package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.YamlConfiguration;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.main.Main;

public class SignFileManager {
	
	private Main instance;
	
	private File file;
	private YamlConfiguration config;
	
	public SignFileManager(Main plugin) {
		this.instance = plugin;
		this.file = new File(this.instance.getDataFolder(), "Signs.yml");
		
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
	
	public void addSign(Location loc, RentTypes type, int id) {
		
		YamlConfiguration cfg = this.getConfig();
		
		String locS = loc.getWorld().getName() + "%" + loc.getBlockX() + "%" + loc.getBlockY() + "%" + loc.getBlockZ();
		
		List<String> signs = new ArrayList<>();
		if(cfg.isSet(type.toString() + "." + String.valueOf(id)))
			signs = cfg.getStringList(type.toString() + "." + String.valueOf(id));
		
		if(!signs.contains(locS))
			signs.add(locS);
		
		cfg.set(type.toString() + ".IDs." + locS, id);
		cfg.set(type.toString() + "." + String.valueOf(id), signs);
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void removeSign(Location loc, RentTypes type) {
		
		YamlConfiguration cfg = this.getConfig();

		String locS = loc.getWorld().getName() + "%" + loc.getBlockX() + "%" + loc.getBlockY() + "%" + loc.getBlockZ();

		int id = cfg.getInt(type.toString() + ".IDs." + locS);
		
		List<String> signs = new ArrayList<>();
		if(cfg.isSet(type.toString() + "." + String.valueOf(id)))
			signs = cfg.getStringList(type.toString() + "." + String.valueOf(id));

		if(signs.contains(locS))
			signs.remove(locS);
		
		cfg.set(type.toString() + ".IDs." + locS, null);
		cfg.set(type.toString() + "." + String.valueOf(id), signs);
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public int getIdFromSign(Location loc, RentTypes type) {
		
		YamlConfiguration cfg = this.getConfig();

		String locS = loc.getWorld().getName() + "%" + loc.getBlockX() + "%" + loc.getBlockY() + "%" + loc.getBlockZ();
		
		return cfg.getInt(type.toString() + ".IDs." + locS);
	}

	public List<Sign> getSigns(RentTypes type, int id) {
		
		YamlConfiguration cfg = this.getConfig();

		List<Sign> finalSigns = new ArrayList<>();
		
		List<String> signs = new ArrayList<>();
		if(cfg.isSet(type.toString() + "." + String.valueOf(id)))
			signs = cfg.getStringList(type.toString() + "." + String.valueOf(id));
		
		signs.forEach(signS -> {
			String[] array = signS.split("%");
			String worldName = array[0];
			int x = Integer.parseInt(array[1]);
			int y = Integer.parseInt(array[2]);
			int z = Integer.parseInt(array[3]);
			
			World w = Bukkit.getWorld(worldName);
			BlockState state = w.getBlockAt(x, y, z).getState();
			
			if(state instanceof Sign) {
				Sign sign = (Sign) state;
				finalSigns.add(sign);
			}
		});
		
		return finalSigns;
	}

	public void clearSigns(RentTypes type, int id) {
		YamlConfiguration cfg = this.getConfig();
		
		List<String> signs = new ArrayList<>();
		if(cfg.isSet(type.toString() + "." + String.valueOf(id)))
			signs = cfg.getStringList(type.toString() + "." + String.valueOf(id));

		signs.forEach(locS -> {
			cfg.set(type.toString() + ".IDs." + locS, null);
		});
		cfg.set(type.toString() + "." + String.valueOf(id), null);
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
