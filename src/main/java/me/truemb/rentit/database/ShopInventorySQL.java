package me.truemb.rentit.database;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import org.bukkit.inventory.ItemStack;

import me.truemb.rentit.gui.UserShopGUI;
import me.truemb.rentit.handler.RentTypeHandler;
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
		sql.prepareStatement("SHOW TABLES LIKE " + sql.t_shop_inv + ";", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				//IMPORTING DATA
				sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " SELECT ID, 1 AS site, sellInv, buyInv FROM " + sql.t_shop_inv + ";");
				//Delete old data
				sql.queryUpdate("DROP TABLE " + sql.t_shop_inv + ";");
			}
		});
		
	}
	
	public void updateSellInv(int shopId, int site, ItemStack[] contents){
		AsyncSQL sql = this.instance.getAsyncSQL();
		String contentsS = contents != null ? InventoryUtils.itemStackArrayToBase64(contents) : null;

		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES ('" + shopId + "', '" + site + "', '" + contentsS + "', '" + null + "') "
					+ "ON CONFLICT(ID, site) DO UPDATE SET sellInv='" + contentsS + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES ('" + shopId + "', '" + site + "', '" + contentsS + "', '" + null + "') "
					+ "ON DUPLICATE KEY UPDATE sellInv='" + contentsS + "';");
	}
	
	public void updateBuyInv(int shopId, int site, ItemStack[] contents){
		AsyncSQL sql = this.instance.getAsyncSQL();
		String contentsS = contents != null ? InventoryUtils.itemStackArrayToBase64(contents) : null;

		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES ('" + shopId + "', '" + site + "', '" + null + "', '" + contentsS + "') "
					+ "ON CONFLICT(ID, site) DO UPDATE SET buyInv='" + contentsS + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_inv_new + " (ID, site, sellInv, buyInv) VALUES ('" + shopId + "', '" + site + "', '" + null + "', '" + contentsS + "') "
					+ "ON DUPLICATE KEY UPDATE buyInv='" + contentsS + "';");
	}
	
	public void setupShopInventories(RentTypeHandler handler) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		int id = handler.getID();
		
		sql.prepareStatement("SELECT * FROM " + sql.t_shop_inv_new + " WHERE ID='" + id + "';", new Consumer<ResultSet>() {

			@Override
			public void accept(ResultSet rs) {
				try {

					while (rs.next()) {
						
						int site = rs.getInt("site");
						String sellInv = rs.getString("sellInv");
						String buyInv = rs.getString("buyInv");
						
						ItemStack[] sellContents = !sellInv.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(sellInv) : null;
						ItemStack[] buyContents = !buyInv.equalsIgnoreCase("null") ? InventoryUtils.itemStackArrayFromBase64(buyInv) : null;
						
						handler.setSellInv(site, UserShopGUI.getSellInv(instance, id, site, sellContents));
						handler.setBuyInv(site, UserShopGUI.getBuyInv(instance, id, site, buyContents));
						return;
					}

					//NO ENTRY FOUND
					handler.setSellInv(1, UserShopGUI.getSellInv(instance, id, 1, null));
					handler.setBuyInv(1, UserShopGUI.getBuyInv(instance, id, 1, null));
				} catch (SQLException | IOException e) {
					e.printStackTrace();
				}
			}
		});
	}
	
}
