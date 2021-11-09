package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.InventoryUtils;

public class ShopCacheFileManager {
	
	private Main instance;
	
	private File file;
	private YamlConfiguration config;
	
	public ShopCacheFileManager(Main plugin) {
		this.instance = plugin;
		this.file = new File(this.instance.getDataFolder(), "ShopBackups.yml");
		
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
	
	public void setShopBackup(UUID uuid, int id, ItemStack[] content) {
		
		if(content == null)
			return;
		
		YamlConfiguration cfg = this.getConfig();
		
		String path = String.valueOf(id) + "." + uuid.toString();
		
		cfg.set(path, InventoryUtils.itemStackArrayToBase64(content));
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void removeShopBackup(UUID uuid, int id) {
		
		YamlConfiguration cfg = this.getConfig();

		String path = String.valueOf(id) + "." + uuid.toString();
		
		cfg.set(path, null);
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Inventory getShopBackup(UUID uuid, int id) {
		
		YamlConfiguration cfg = this.getConfig();

		String path = String.valueOf(id) + "." + uuid.toString();
		String hash = cfg.getString(path);
		
		if(hash == null)
			return null;
		
		ItemStack[] content;
		try {
			content = InventoryUtils.itemStackArrayFromBase64(hash);
		} catch (IOException e) {
			return null;
		}
		Inventory inv = Bukkit.createInventory(null, 54, "BACKUP SHOP " + id);
		
		inv.setContents(content);
		
		return inv;
	}
	
}
