package me.truemb.rentit.listener;

import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.gui.CategoryGUI;
import me.truemb.rentit.gui.UserConfirmGUI;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.PlayerHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.main.Main;

public class CategoryGUIListener implements Listener {

	private Main instance;

	public CategoryGUIListener(Main plugin) {
		this.instance = plugin;
		this.instance.getServer().getPluginManager().registerEvents(this, this.instance);
	}

	@EventHandler
	public void onCatClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		UUID uuid = p.getUniqueId();
		
		PlayerHandler playerHandler = this.instance.getMethodes().getPlayerHandler(uuid);
		if (playerHandler == null) {
			p.sendMessage(this.instance.getMessage("pleaseReconnect"));
			return;
		}

		//SHOWS ALL SHOP CATEGORIES
		if (e.getView().getTitle().equalsIgnoreCase(this.instance.translateHexColorCodes(this.instance.manageFile().getString("GUI.categoryShop.displayName")))) {

			e.setCancelled(true);

			if (e.getClickedInventory() == null)
				return;

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
				return;

			ItemStack item = e.getCurrentItem();
			
			if(item == null)
				return;
			
			ItemMeta meta = item.getItemMeta();
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int catID = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			
			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, catID);

			if (catHandler == null) {
				p.sendMessage(this.instance.getMessage("categoryError"));
				return;
			}

			p.closeInventory();
			p.openInventory(CategoryGUI.getSubCategoryGUI(this.instance, RentTypes.SHOP, catID, 1));
			return;
			
		//SHOWS ALL HOTEL CATEGORIES
		}else if (e.getView().getTitle().equalsIgnoreCase(this.instance.translateHexColorCodes(this.instance.manageFile().getString("GUI.categoryHotel.displayName")))) {

			e.setCancelled(true);

			if (e.getClickedInventory() == null)
				return;

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
				return;

			ItemStack item = e.getCurrentItem();

			if(item == null)
				return;
			
			ItemMeta meta = item.getItemMeta();
			NamespacedKey key = new NamespacedKey(this.instance, "ID");
			
			if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
				return;
			
			int catID = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
			
			CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.HOTEL, catID);

			if (catHandler == null) {
				p.sendMessage(this.instance.getMessage("categoryError"));
				return;
			}


			p.closeInventory();
			p.openInventory(CategoryGUI.getSubCategoryGUI(this.instance, RentTypes.HOTEL, catID, 1));
			return;
			
		//SHOWS ALL SHOPS FROM A CATEGORIES
		}else if (e.getView().getTitle().equalsIgnoreCase(this.instance.translateHexColorCodes(this.instance.manageFile().getString("GUI.categorySub.displayNameShop")))) {

			e.setCancelled(true);

			if (e.getClickedInventory() == null)
				return;

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
				return;

			ItemStack item = e.getCurrentItem();

			if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "backItem"))) {
				
				p.closeInventory();
				p.openInventory(CategoryGUI.getCategoryGUI(this.instance, RentTypes.SHOP));
				
			}else if(this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "nextSiteItem"))
					|| this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "beforeSiteItem"))) {

				ItemMeta meta = item.getItemMeta();
				NamespacedKey siteKey = new NamespacedKey(this.instance, "Site");
				if (!meta.getPersistentDataContainer().has(siteKey, PersistentDataType.INTEGER))
					return;
					
				int site = meta.getPersistentDataContainer().get(siteKey, PersistentDataType.INTEGER);
				
				ItemStack firstItem = p.getOpenInventory().getTopInventory().getItem(0);
				if(firstItem == null || firstItem.getType() == Material.AIR)
					return;
				
				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, id);

				if (rentHandler == null)
					return;
				
				p.getOpenInventory().getTopInventory().setContents(CategoryGUI.getSubCategoryGUI(this.instance, RentTypes.SHOP, rentHandler.getCatID(), site).getContents());
				
			}else {
				//SHOP
				ItemMeta meta = item.getItemMeta();
				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				
				RentTypeHandler typeHandler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, id);
				if(typeHandler == null) return;
				
				if(e.isRightClick()) {
					
					//OPEN BUY MENU
					p.closeInventory();
					//this.instance.openId.put(uuid, id);
					p.openInventory(UserConfirmGUI.getShopConfirmationGUI(this.instance, id));
					
				}else if(e.isLeftClick()) {
					//Teleport to Shop
					
					CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.SHOP, typeHandler.getCatID());
					if(catHandler == null) return;
					
					if(this.instance.manageFile().isSet("Options.categorySettings.ShopCategory." + String.valueOf(catHandler.getCatID()) + ".teleport") &&
							!this.instance.manageFile().getBoolean("Options.categorySettings.ShopCategory." + String.valueOf(catHandler.getCatID()) + ".teleport")) {
						
						p.sendMessage(this.instance.getMessage("shopTeleportNotAllowed"));
						return;
					}
					
					p.teleport(this.instance.getAreaFileManager().getAreaSpawn(RentTypes.SHOP, id));
				}
			}
			
		//SHOWS ALL HOTELS FROM A CATEGORIES
		}else if (e.getView().getTitle().equalsIgnoreCase(this.instance.translateHexColorCodes(this.instance.manageFile().getString("GUI.categorySub.displayNameHotel")))) {

			e.setCancelled(true);

			if (e.getClickedInventory() == null)
				return;

			if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR)
				return;

			ItemStack item = e.getCurrentItem();

			if(this.instance.getMethodes().removeIDKeyFromItem(item).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "backItem"))) {
				
				p.closeInventory();
				p.openInventory(CategoryGUI.getCategoryGUI(this.instance, RentTypes.HOTEL));
				
			}else if(this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "nextSiteItem"))
					|| this.instance.getMethodes().removeSiteKeyFromItem(this.instance.getMethodes().removeIDKeyFromItem(item)).isSimilar(this.instance.getMethodes().getGUIItem("categorySub", "beforeSiteItem"))) {

				ItemMeta meta = item.getItemMeta();
				NamespacedKey siteKey = new NamespacedKey(this.instance, "Site");
				if (!meta.getPersistentDataContainer().has(siteKey, PersistentDataType.INTEGER))
					return;
					
				int site = meta.getPersistentDataContainer().get(siteKey, PersistentDataType.INTEGER);
				
				ItemStack firstItem = p.getOpenInventory().getTopInventory().getItem(0);
				if(firstItem == null || firstItem.getType() == Material.AIR)
					return;

				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

				RentTypeHandler rentHandler = this.instance.getMethodes().getTypeHandler(RentTypes.HOTEL, id);

				if (rentHandler == null)
					return;
				
				p.getOpenInventory().getTopInventory().setContents(CategoryGUI.getSubCategoryGUI(this.instance, RentTypes.HOTEL, rentHandler.getCatID(), site).getContents());
				
			}else {
				//HOTEL
				ItemMeta meta = item.getItemMeta();
				NamespacedKey key = new NamespacedKey(this.instance, "ID");
				
				if(!meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER))
					return;
				
				int id = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
				
				RentTypeHandler typeHandler = this.instance.getMethodes().getTypeHandler(RentTypes.HOTEL, id);
				if(typeHandler == null) return;
				
				if(e.isRightClick()) {
					
					//OPEN BUY MENU
					p.closeInventory();
					//this.instance.openId.put(uuid, id);
					p.openInventory(UserConfirmGUI.getHotelConfirmationGUI(this.instance, id));
					
				}else if(e.isLeftClick()) {
					//Teleport to Hotelroom
					
					CategoryHandler catHandler = this.instance.getMethodes().getCategory(RentTypes.HOTEL, typeHandler.getCatID());
					if(catHandler == null) return;
					
					if(this.instance.manageFile().isSet("Options.categorySettings.HotelCategory." + String.valueOf(catHandler.getCatID()) + ".teleport") &&
							!this.instance.manageFile().getBoolean("Options.categorySettings.HotelCategory." + String.valueOf(catHandler.getCatID()) + ".teleport")) {
						
						p.sendMessage(this.instance.getMessage("hotelTeleportNotAllowed"));
						return;
					}
					
					p.teleport(this.instance.getAreaFileManager().getAreaSpawn(RentTypes.HOTEL, id));
				}
			}
		}
	}
}
