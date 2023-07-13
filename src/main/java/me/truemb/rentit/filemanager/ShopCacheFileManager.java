package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import me.truemb.rentit.utils.chests.SupportedChest;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.InventoryUtils;
import me.truemb.rentit.utils.ShopItemManager;

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

	public void createShopBackup(UUID uuid, int id) {
		
		YamlConfiguration cfg = this.getConfig();

		List<SupportedChest> chests = this.instance.getChestsUtils().getShopChests(id);
		
		String basicPath = String.valueOf(id) + "." + uuid.toString();
		
		for(int i = 0; i < chests.size(); i++) {
			cfg.set(basicPath + "." + i, InventoryUtils.itemStackArrayToBase64(chests.get(i).getAllItems().toArray(ItemStack[]::new)));
		}
		
		RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, id);
		Collection<Inventory> sellInventories = rentHandler.getSellInventories();
		
		for(int i = 0; i < sellInventories.size(); i++) {
			Inventory sellInv = (Inventory) sellInventories.toArray()[i];
			if(sellInv != null) {
				ItemStack[] sellItems = sellInv.getContents();
				
				for(ItemStack item : sellItems) {
					if(item != null)
						item = ShopItemManager.removeShopItem(this.instance, item);
				}
				
				cfg.set(basicPath + "." + String.valueOf(chests.size() + i), InventoryUtils.itemStackArrayToBase64(sellItems));
			}
		}
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Deprecated
	public void updateShopBackup(UUID uuid, int id, List<Inventory> inventories) {
		
		YamlConfiguration cfg = this.getConfig();
		
		String basicPath = String.valueOf(id) + "." + uuid.toString();
		cfg.set(basicPath, null);
		
		for(int i = 0; i < inventories.size(); i++) {
			ItemStack[] items = inventories.get(i).getContents();
			
			for(ItemStack item : items) {
				if(item != null && item.hasItemMeta()) {
					ItemMeta meta = item.getItemMeta();
					PersistentDataContainer container = meta.getPersistentDataContainer();
					
					if(container.has(this.instance.guiItem, PersistentDataType.STRING) && container.get(this.instance.guiItem, PersistentDataType.STRING) != null && container.get(this.instance.guiItem, PersistentDataType.STRING).equalsIgnoreCase("true")) {
						//GUI ITEM
						item.setType(Material.AIR); //DELETE ITEM
					}
				}
			}
			
			cfg.set(basicPath + "." + i, InventoryUtils.itemStackArrayToBase64(items));
		}
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public List<ItemStack[]> getShopBackupList(UUID uuid, int id) {
		
		YamlConfiguration cfg = this.getConfig();

		String basicPath = String.valueOf(id) + "." + uuid.toString();
		
		List<ItemStack[]> contents = new ArrayList<>();
		ConfigurationSection section = cfg.getConfigurationSection(basicPath);
		
		if(section != null){
		
			for(String tempPath : section.getKeys(false)) {
				String hash = cfg.getString(basicPath + "." + tempPath);
	
				try {
					ItemStack[] content = InventoryUtils.itemStackArrayFromBase64(hash);
					contents.add(content);
				} catch (IOException e) {
					this.instance.getLogger().warning("Couldnt load data for Path: " + basicPath + "." + tempPath);
				}
			}
			
		}
		
		return contents;
	}
	
}
