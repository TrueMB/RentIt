package me.truemb.rentit.database;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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
						
						ItemStack[] sellContents = sellInvS != null && !sellInvS.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(sellInvS) : null;
						ItemStack[] buyContents = buyInvS != null && !buyInvS.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(buyInvS) : null;
						
						//Adding next Site Button
						if(sellInv != null && sellContents != null)
							sellInv.setItem(sellInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));
						
						if(buyInv != null && buyContents != null)
							buyInv.setItem(buyInv.getSize() - 1, instance.getMethodes().getGUIItem("ShopBuyAndSell", "nextSiteItem", id));

						//Adding before Site Button
						if(sellInv != null)
							sellInv.setItem(sellInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
						
						if(buyInv != null)
							buyInv.setItem(buyInv.getSize() - 9, instance.getMethodes().getGUIItem("ShopBuyAndSell", "beforeSiteItem", id));
						
						ShopInventoryBuilder sellBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.SELL);
						sellInv = UserShopGUI.getInventory(instance, sellBuilder);
						
						ShopInventoryBuilder buyBuilder = new ShopInventoryBuilder(null, handler, ShopInventoryType.BUY);
						buyInv = UserShopGUI.getInventory(instance, buyBuilder);
						
						CategoryHandler catHandler = instance.getMethodes().getCategory(RentTypes.SHOP, handler.getCatID());
						
						if(catHandler.getMaxSite() > 1) {
							if(sellContents != null)
								for(int i = 0; i < sellInv.getSize(); i++)
									if(sellContents.length > i)
										sellInv.setItem(i, sellContents[i]);
							
							if(buyContents != null)
								for(int i = 0; i < buyInv.getSize(); i++)
									if(buyContents.length > i)
										buyInv.setItem(i, buyContents[i]);
						}else {
							if(sellContents != null)
								sellInv.setContents(sellContents);
							if(buyContents != null)
								buyInv.setContents(buyContents);
						}
						
						handler.setInventory(ShopInventoryType.SELL, site, sellInv);
						handler.setInventory(ShopInventoryType.BUY, site, buyInv);
						return;
					}
					
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
