package me.truemb.rentit.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.function.Consumer;

import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.main.Main;

public class CategoriesSQL {
	
	private Main instance;
	
	public CategoriesSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shop_categories + " (catID INT PRIMARY KEY, alias VARCHAR(100), size INT, costs DOUBLE, time VARCHAR(30))");
		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_hotel_categories + " (catID INT PRIMARY KEY, alias VARCHAR(100), costs DOUBLE, time VARCHAR(30))");
		
		//UPDATES
		sql.addColumn(sql.t_shop_categories, "alias", "VARCHAR(100)");
		sql.addColumn(sql.t_hotel_categories, "alias", "VARCHAR(100)");
	}
	
	
	public void updateShopCategory(int catID, int invSize, double costs, String time){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size, costs, time) VALUES ('" + catID + "','" + invSize + "', '" + costs + "', '" + time + "') "
					+ "ON CONFLICT(catID) DO UPDATE SET size='" + invSize + "', costs='" + costs + "', time='" + time + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size, costs, time) VALUES ('" + catID + "','" + invSize + "', '" + costs + "', '" + time + "') "
					+ "ON DUPLICATE KEY UPDATE size='" + invSize + "', costs='" + costs + "', time='" + time + "';");
	}
	
	public void updateHotelCategory(int catID, double costs, String time){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_hotel_categories + " (catID, costs, time) VALUES ('" + catID + "', '" + costs + "', '" + time + "') "
					+ "ON CONFLICT(catID) DO UPDATE SET costs='" + costs + "', time='" + time + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_hotel_categories + " (catID, costs, time) VALUES ('" + catID + "', '" + costs + "', '" + time + "') "
					+ "ON DUPLICATE KEY UPDATE costs='" + costs + "', time='" + time + "';");
	}
	
	public void setAlias(int catId, RentTypes type, String alias){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " SET alias='" + alias + "' WHERE catID='" + catId + "'");
	}

	public void setSize(int catID, int invSize){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size) VALUES ('" + catID + "','" + invSize + "') "
					+ "ON CONFLICT(catID) DO UPDATE SET size='" + invSize + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size) VALUES ('" + catID + "','" + invSize + "') "
					+ "ON DUPLICATE KEY UPDATE size='" + invSize + "';");
	}
	
	public void setCosts(int catID, RentTypes type, double costs){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, costs) VALUES ('" + catID + "', '" + costs + "') "
					+ "ON CONFLICT(catID) DO UPDATE SET costs='" + costs + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, costs) VALUES ('" + catID + "', '" + costs + "') "
					+ "ON DUPLICATE KEY UPDATE costs='" + costs + "';");
	}
	
	public void setTime(int catID, RentTypes type, String time){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, time) VALUES ('" + catID + "', '" + time + "') "
					+ "ON CONFLICT(catID) DO UPDATE SET time='" + time + "';");
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, time) VALUES ('" + catID + "', '" + time + "') "
					+ "ON DUPLICATE KEY UPDATE time='" + time + "';");
		
	}
	
	public void delete(RentTypes type, int catID){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " WHERE catID='" + catID + "'");
	}

	public void setupCategories() {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		for(RentTypes type : RentTypes.values()) {
			sql.prepareStatement("SELECT * FROM " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + ";", new Consumer<ResultSet>() {
	
				@Override
				public void accept(ResultSet rs) {
					try {
	
						int catAmount = 0;
						
						while (rs.next()) {
							
							catAmount++;
							
							int catID = rs.getInt("catID");
							String alias = rs.getString("alias") != null && !rs.getString("alias").equalsIgnoreCase("null") ? rs.getString("alias") : null;
							
							double costs = rs.getDouble("costs");
							String time = rs.getString("time");

							CategoryHandler handler = new CategoryHandler(catID, costs, time);
							handler.setAlias(alias);
							
							if(type.equals(RentTypes.SHOP)) {
								int size = rs.getInt("size");
								handler.setSize(size);
							}
							
							HashMap<Integer, CategoryHandler> hash = new HashMap<>();
							if(instance.catHandlers.containsKey(type))
								hash = instance.catHandlers.get(type);
							
							hash.put(catID, handler);
							instance.catHandlers.put(type, hash);
						}
						
						instance.getLogger().info(String.valueOf(catAmount) + " " + type.toString() + "-Categories are loaded.");
						return;
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}
}
