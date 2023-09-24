package me.truemb.rentit.database;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import me.truemb.rentit.database.connector.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.enums.ShopInventoryType;
import me.truemb.rentit.gui.UserShopGUI;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.handler.RentTypeHandler;
import me.truemb.rentit.inventory.ShopInventoryBuilder;
import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.InventoryUtils;

public class ShopInventorySQL {
	
	private Main instance;

	public ShopInventorySQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		//OLD sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shop_inv + " (ID INT PRIMARY KEY, sellInv LONGTEXT, buyInv LONGTEXT)");
		
		
		//Create new Table
		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shop_inv_new + " (ID INT, site INT, sellInv LONGTEXT, buyInv LONGTEXT, PRIMARY KEY (ID, site));");
		
		//CHECK IF OLD DATA EXISTS
		String sqlLiteStatement = "PRAGMA table_list(" + sql.t_shop_inv + ");";
		String mysqlStatement = "SHOW TABLES LIKE '" + sql.t_shop_inv + "';";
		sql.prepareStatement(sql.isSqlLite() ? sqlLiteStatement : mysqlStatement, new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				//IMPORTING DATA
				try {
					if(rs.next()) {
						if(sql.isSqlLite()) { //SQLLITE
							sql.queryUpdate("INSERT OR REPLACE INTO " + sql.t_shop_inv_new + " SELECT ID, 1 AS site, sellInv, buyInv FROM " + sql.t_shop_inv + ";");
							
							sql.getDatabaseConnector().closeConnection(); //Closes the SQLite DB, results in committing changes.
							sql.getDatabaseConnector().openConnection();
							
						}else //MYSQL
							sql.queryUpdate("INSERT IGNORE INTO " + sql.t_shop_inv_new + " SELECT ID, 1 AS site, sellInv, buyInv FROM " + sql.t_shop_inv + ";");
						
						//Delete old data
						sql.queryUpdate("DROP TABLE " + sql.t_shop_inv + ";");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		
	}
	
	public void resetInventories(int shopId){
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shop_inv_new + " SET sellInv = null, buyInv = null WHERE ID='" + shopId + "';");
	}
	
	public void updateInventory(int shopId, ShopInventoryType type, int site, ItemStack[] contents){
		AsyncSQL sql = this.instance.getAsyncSQL();
		String contentsS = contents != null ? InventoryUtils.itemStackArrayToBase64(contents) : null;

		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES ('" + shopId + "', '" + site + "', '" + (type == ShopInventoryType.SELL ? contentsS : null) + "', '" + (type == ShopInventoryType.BUY ? contentsS : null) + "') "
					+ "ON CONFLICT(ID, site) DO UPDATE SET " + type.toString().toLowerCase() + "Inv='" + contentsS + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES ('" + shopId + "', '" + site + "', '" + (type == ShopInventoryType.SELL ? contentsS : null) + "', '" + (type == ShopInventoryType.BUY ? contentsS : null) + "') "
					+ "ON DUPLICATE KEY UPDATE " + type.toString().toLowerCase() + "Inv='" + contentsS + "';");
	}
	
	public void setupShopInventories(RentTypeHandler handler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		int id = handler.getID();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shop_inv_new + " WHERE ID='" + id + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					Inventory sellInv = null;
					Inventory buyInv = null;
					
					while (rs.next()) {
						
						int site = rs.getInt("site");
						String sellInvS = rs.getString("sellInv");
						String buyInvS = rs.getString("buyInv");
						
						CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.SHOP, handler.getCatID());
						
						boolean skipSellInv = false;
						boolean skipBuyInv = false;
						
						ItemStack[] sellContents = sellInvS != null && !sellInvS.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(sellInvS) : null;
						ItemStack[] buyContents = buyInvS != null && !buyInvS.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(buyInvS) : null;

						//Add Items one site before, if still space
						if(catHandler != null) {
							//Sell Inventory
							if(sellInv != null && sellContents != null) {
								ItemStack[] sellContentsClone = sellContents.clone();
								skipSellInv = true; //Items getting checked, if there is still space in the inventory before
								
								outer: for(int i = 0; i < sellContentsClone.length; i++) {
									ItemStack item = sellContentsClone[i];
									if(item != null && item.getType() != Material.AIR && !item.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING)) {
										
										boolean foundFreeSlot = false;
										for(int slot = 0; slot < sellInv.getSize() - 9; slot++) {
											ItemStack temp = sellInv.getItem(slot);
											
											if(temp == null || temp.getType() == Material.AIR) {
												sellInv.setItem(slot, item);
												sellContents[i] = null; //Removes the Items from the next Site
												foundFreeSlot = true;
												break; //Move to next Item
											}
										}
											
										if(!foundFreeSlot) {
											skipSellInv = false;
											break outer;
										}
									}else
										sellContents[i] = null; //Could be guiItem -> remove
								}
							}
							
							//Buy Inventory
							if(buyInv != null && buyContents != null) {
								ItemStack[] buyContentsClone = buyContents.clone();
								skipBuyInv = true; //Items getting checked, if there is still space in the inventory before
								
								outer: for(int i = 0; i < buyContentsClone.length; i++) {
									ItemStack item = buyContentsClone[i];
									if(item != null && item.getType() != Material.AIR && !item.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING)) {
										boolean foundFreeSlot = false;
										for(int slot = 0; slot < buyInv.getSize() - 9; slot++) {
											ItemStack temp = buyInv.getItem(slot);
												
											if(temp == null || temp.getType() == Material.AIR) {
												buyInv.setItem(slot, item);
												foundFreeSlot = true;
												break;
											}
										}

										if(!foundFreeSlot) {
											skipBuyInv = false;
											break outer;
										}
									}else
										buyContents[i] = null; //Could be guiItem -> remove
								}
							}
						}
						
						//Adding next Site Button
						if(sellInv != null && sellContents != null)
							sellInv.setItem(sellInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
						
						if(buyInv != null && buyContents != null)
							buyInv.setItem(buyInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
						
						if(!skipSellInv) {
							ShopInventoryBuilder sellBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.SELL);
							sellBuilder.setSite(site);
							sellInv = UserShopGUI.getInventory(instance, sellBuilder);
						}

						if(!skipBuyInv) {
							ShopInventoryBuilder buyBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.BUY);
							buyBuilder.setSite(site);
							buyInv = UserShopGUI.getInventory(instance, buyBuilder);
						}
						
						//Adding before Site Button
						if(sellInv != null && site > 1)
							sellInv.setItem(sellInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
						
						if(buyInv != null && site > 1)
							buyInv.setItem(buyInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
						
						//Sets the items to the new Site Inventory
						if(catHandler != null) {
							if(catHandler.getMaxSite() > 1) {
								if(sellContents != null)
									for(int i = 0; i < sellInv.getSize(); i++)
										if(sellContents.length > i) {
											ItemStack temp = sellContents[i];
											if(temp != null && !temp.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING)) {
												sellInv.setItem(i, temp);
											}
										}
								
								if(buyContents != null)
									for(int i = 0; i < buyInv.getSize(); i++)
										if(buyContents.length > i) {
											ItemStack temp = buyContents[i];
											if(temp != null && !temp.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING))
												buyInv.setItem(i, temp);
										}
							}else {
								if(sellContents != null)
									sellInv.setContents(sellContents);
								if(buyContents != null)
									buyInv.setContents(buyContents);
							}
						}
						
						handler.setInventory(ShopInventoryType.SELL, site, sellInv);
						handler.setInventory(ShopInventoryType.BUY, site, buyInv);
					}
					
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
