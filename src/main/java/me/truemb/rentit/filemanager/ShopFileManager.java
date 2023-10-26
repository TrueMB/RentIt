package me.truemb.rentit.filemanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.truemb.rentit.main.Main;

public class ShopFileManager {
	
	private Main instance;
	
	private File file;
	private YamlConfiguration config;
	
	private HashMap<String, Inventory> shopInvs = new HashMap<>();
	
	public ShopFileManager(Main plugin) {
		this.instance = plugin;
		this.file = new File(this.instance.getDataFolder(), "ShopItems.yml");
		
		if(!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		this.config = YamlConfiguration.loadConfiguration(this.file);
		this.setupShopInvs();
		
	}
	
	private YamlConfiguration getConfig() {
		return this.config;
	}
	
	public void setShopInventory(Inventory inv, String shopname) {
		
		YamlConfiguration cfg = this.getConfig();
		
		for(int i = 0; i < inv.getSize(); i++) {
			cfg.set("Shops." + shopname + ".items." + i + ".item", inv.getItem(i));
		}
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setShopItemCosts(String shopname, int slot, double costs) {
		
		YamlConfiguration cfg = this.getConfig();
		
		cfg.set("Shops." + shopname + ".items." + slot + ".cost", costs);
		
		
		try {
			cfg.save(this.file);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void setupShopInvs() {
		
		YamlConfiguration cfg = this.getConfig();
		
		if(!cfg.isSet("Shops"))
			return;
		
		for(String shopname : cfg.getConfigurationSection("Shops").getKeys(false)) {
			if(this.instance.manageFile().isSet("Options.vshop." + shopname.toLowerCase() + ".customName")) {
				
				this.updateInventory(shopname);
			}
		}
	}
	
	public Inventory getShopInvAdmin(String shopname, boolean shopLore) {
		YamlConfiguration cfg = this.getConfig();
		
		String title = this.instance.translateHexColorCodes(this.instance.manageFile().getString("Options.vshop." + shopname + ".customName"));
		Inventory inv = Bukkit.createInventory(null, 27, title);
		
		for(int i = 0; i < inv.getSize(); i++) {
			if(cfg.isSet("Shops." + shopname + ".items." + i + ".item")) {
				ItemStack item = cfg.getItemStack("Shops." + shopname + ".items." + i + ".item");
				double costs = cfg.getDouble("Shops." + shopname + ".items." + i + ".cost");
				ItemMeta meta = item.getItemMeta();
				
				if(shopLore) {
					List<String> lore = new ArrayList<>();
					
					for(String s : this.instance.manageFile().getStringList("Options.shopSettings.shopItem.lore"))
						lore.add(this.instance.translateHexColorCodes(s
								.replace("%buyAmount%", String.valueOf(costs)))
								.replace("%sellAmount%", String.valueOf(costs / 100 * this.instance.manageFile().getInt("Options.shopSettings.sellPercent"))));
					meta.setLore(lore);
				}
				item.setItemMeta(meta);
				
				inv.setItem(i, item);
			}else if(shopLore) {
				ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName("");
				item.setItemMeta(meta);
				inv.setItem(i, item);
			}
		}
		return inv;
	}
	
	public void updateInventory(String shopname) {
		this.shopInvs.put(shopname, this.getShopInvAdmin(shopname, true));
	}
	
	public Inventory getShopInv(String shopname) {
		return this.shopInvs.get(shopname);
	}
	
	public double getShopCost(String shopname, int slot) {
		return this.getConfig().getDouble("Shops." + shopname + ".items." + slot + ".cost");
	}
	
	public ItemStack getShopItemstack(String shopname, int slot) {
		return this.getConfig().getItemStack("Shops." + shopname + ".items." + slot + ".item");
	}
}
