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
import me.truemb.rentit.utils.ShopItemManager;

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
		sql.queryUpdate("UPDATE " + sql.t_shop_inv_new + " SET sellInv=?, buyInv=? WHERE ID=?;", 
				null, null, String.valueOf(shopId));
	}
	
	public void resetInventories(int shopId, ShopInventoryType type){
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + sql.t_shop_inv_new + " SET " + type.toString().toLowerCase() + "Inv = null WHERE ID=?;",
				String.valueOf(shopId));
	}
	
	public void updateInventories(int shopId, ShopInventoryType type){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		RentTypeHandler handler = this.instance.getMethodes().getTypeHandler(RentTypes.SHOP, shopId);
		
		if(handler == null) 
			return;

		boolean multiSite = this.instance.getMethodes().getCategory(RentTypes.SHOP, handler.getCatID()).getMaxSite() > 1;
		this.resetInventories(shopId, type);

		int site = 0;
		for(Inventory invs : handler.getInventories(type)) {
			site++;
			
			//Remove GUI Items from Inventory
			ItemStack[] contents = null;
			if(multiSite) {
				contents = new ItemStack[invs.getSize() - 9];
				
				for(int i = 0; i < contents.length; i++)
					contents[i] = invs.getContents()[i];
			} else
				contents = invs.getContents();
			
			
			String contentsS = contents != null ? InventoryUtils.itemStackArrayToBase64(contents) : null;
	
			if(sql.isSqlLite()) //SQLLITE
				sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES (?,?,?,?) "
						+ "ON CONFLICT(ID, site) DO UPDATE SET " + type.toString().toLowerCase() + "Inv=?;",
					String.valueOf(shopId), String.valueOf(site), type == ShopInventoryType.SELL ? contentsS : null, type == ShopInventoryType.BUY ? contentsS : null, contentsS);
			else //MYSQL
				sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES (?,?,?,?) "
						+ "ON DUPLICATE KEY UPDATE " + type.toString().toLowerCase() + "Inv=?;",
						String.valueOf(shopId), String.valueOf(site), type == ShopInventoryType.SELL ? contentsS : null, type == ShopInventoryType.BUY ? contentsS : null, contentsS);
		}
	}
	
	public void setupShopInventories(RentTypeHandler handler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		int id = handler.getID();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shop_inv_new + " WHERE ID=?;", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {
					Inventory sellInv = null;
					Inventory buyInv = null;
					
					int siteSellCounter = 0;
					int siteBuyCounter = 0;
					
					boolean needsToUpdate = false;
					
					while (rs.next()) {
						
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
										for(int slot = 0; slot < (catHandler.getMaxSite() > 1 ? sellInv.getSize() - 9 : sellInv.getSize()); slot++) {
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
										for(int slot = 0; slot < (catHandler.getMaxSite() > 1 ? buyInv.getSize() - 9 : buyInv.getSize()); slot++) {
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

						if(!skipSellInv) {
							if(siteSellCounter < catHandler.getMaxSite()) {
								if(sellContents != null) {
									siteSellCounter++;
									
									//Adding next Site Button
									if(sellInv != null)
										sellInv.setItem(sellInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
									
									//Creating a new Shop Site
									ShopInventoryBuilder sellBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.SELL);
									sellBuilder.setSite(siteSellCounter);
									sellInv = UserShopGUI.getInventory(instance, sellBuilder);
									
									handler.setInventory(ShopInventoryType.SELL, siteSellCounter, sellInv);
									
									//Adding before Site Button
									if(sellInv != null && siteSellCounter > 1)
										sellInv.setItem(sellInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
									
	
									//Sets the items to the new Site Inventory
									if(catHandler != null) {
										outer: for(int i = 0; i < sellContents.length; i++) {
											if(sellContents.length > i) {
												ItemStack item = sellContents[i];
												if(item != null && item.getType() != Material.AIR && !item.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING)) {
													for(int slot = 0; slot < (catHandler.getMaxSite() > 1 ? sellInv.getSize() - 9 : sellInv.getSize()); slot++) {
														ItemStack temp = sellInv.getItem(slot);
															
														if(temp == null || temp.getType() == Material.AIR) {
															sellInv.setItem(slot, item);
															continue outer;
														}
													}
													
													//Items left, create another site inventory
													if(siteSellCounter < catHandler.getMaxSite()) {
														siteSellCounter++;
														
														//Adding next Site Button
														if(sellInv != null)
															sellInv.setItem(sellInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
														
														//Creating a new Shop Site
														sellBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.SELL);
														sellBuilder.setSite(siteSellCounter);
														sellInv = UserShopGUI.getInventory(instance, sellBuilder);
														
														handler.setInventory(ShopInventoryType.SELL, siteSellCounter, sellInv);
														
														//Adding before Site Button
														if(sellInv != null && siteSellCounter > 1)
															sellInv.setItem(sellInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
														
														//It's definitely the first item
														sellInv.setItem(0, item);
														
														//After this the new Inventory was created and it continues with the other items
													}else {
														
														needsToUpdate = true;
														
														//Adds the items, that are not shown, to the backup container
														ItemStack[] backupItems = new ItemStack[sellContents.length - i];
														for(int pos = 0; i < sellContents.length; pos++) {
															ItemStack backupItem = sellContents[i];
															if(backupItem != null)
																backupItems[pos] = ShopItemManager.removeShopItem(instance, backupItem);
															i++;
														}
														instance.getShopCacheFileManager().addShopBackup(handler.getOwnerUUID(), id, backupItems);
														break outer;
													}
												}
											}
										}
									}
								}
							}else if(sellContents != null){
								needsToUpdate = true;

								//Adds the items, that are not shown, to the backup container
								ItemStack[] backupItems = new ItemStack[sellContents.length];
								for(int pos = 0; pos < sellContents.length; pos++) {
									ItemStack backupItem = sellContents[pos];
									if(backupItem != null)
										backupItems[pos] = ShopItemManager.removeShopItem(instance, backupItem);
								}
								instance.getShopCacheFileManager().addShopBackup(handler.getOwnerUUID(), id, backupItems);
							}
						}

						if(!skipBuyInv && siteBuyCounter < catHandler.getMaxSite()) {
							if(buyContents != null) {
								siteBuyCounter++;
								
								//Adding next Site Button
								if(buyInv != null)
									buyInv.setItem(buyInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
	
								//Creating a new Shop Site
								ShopInventoryBuilder buyBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.BUY);
								buyBuilder.setSite(siteBuyCounter);
								buyInv = UserShopGUI.getInventory(instance, buyBuilder);
	
								handler.setInventory(ShopInventoryType.BUY, siteBuyCounter, buyInv);
								
								//Adding before Site Button
								if(buyInv != null && siteBuyCounter > 1)
									buyInv.setItem(buyInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
								
	
								//Sets the items to the new Site Inventory
								if(catHandler != null) {
									outer: for(int i = 0; i < (catHandler.getMaxSite() > 1 ? buyInv.getSize() - 9 : buyInv.getSize()); i++) {
										if(buyContents.length > i) {
											ItemStack item = buyContents[i];
											if(item != null && item.getType() != Material.AIR && !item.getItemMeta().getPersistentDataContainer().has(instance.guiItem, PersistentDataType.STRING)) {

												for(int slot = 0; slot < (catHandler.getMaxSite() > 1 ? buyInv.getSize() - 9 : buyInv.getSize()); slot++) {
													ItemStack temp = buyInv.getItem(slot);
														
													if(temp == null || temp.getType() == Material.AIR) {
														buyInv.setItem(slot, item);
														continue outer;
													}
												}
												

												if(siteBuyCounter < catHandler.getMaxSite()) {
													siteBuyCounter++;
													
													//Adding next Site Button
													if(buyInv != null)
														buyInv.setItem(buyInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
						
													//Creating a new Shop Site
													buyBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.BUY);
													buyBuilder.setSite(siteBuyCounter);
													buyInv = UserShopGUI.getInventory(instance, buyBuilder);
						
													handler.setInventory(ShopInventoryType.BUY, siteBuyCounter, buyInv);
													
													//Adding before Site Button
													if(buyInv != null && siteBuyCounter > 1)
														buyInv.setItem(buyInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));

													//It's definitely the first item
													buyInv.setItem(0, item);
													
													//After this the new Inventory was created and it continues with the other items
												}
											}
										}
									}
								}
							}
						}
					}
					
					//Updates Sell Inventories, because Items got moved to the backup container
					if(needsToUpdate) {
						updateInventories(id, ShopInventoryType.SELL);
					}
					
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		}, String.valueOf(id));
	}
	
}
