package me.truemb.rentit.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.function.Consumer;

import me.truemb.rentit.database.connector.AsyncSQL;
import me.truemb.rentit.enums.RentTypes;
import me.truemb.rentit.handler.CategoryHandler;
import me.truemb.rentit.main.Main;

public class CategoriesSQL {
	
	private Main instance;
	
	public CategoriesSQL(Main plugin){
		this.instance = plugin;
		AsyncSQL sql = this.instance.getAsyncSQL();

		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_shop_categories + " (catID INT PRIMARY KEY, alias VARCHAR(100), size INT, maxSite INT, costs DOUBLE, time VARCHAR(30))");
		sql.queryUpdate("CREATE TABLE IF NOT EXISTS " + sql.t_hotel_categories + " (catID INT PRIMARY KEY, alias VARCHAR(100), costs DOUBLE, time VARCHAR(30))");
		
		//UPDATES
		sql.addColumn(sql.t_shop_categories, "alias", "VARCHAR(100)");
		sql.addColumn(sql.t_shop_categories, "maxSite", "INT");
		sql.addColumn(sql.t_hotel_categories, "alias", "VARCHAR(100)");
	}
	
	
	public void updateShopCategory(int catID, int invSize, int maxSite, double costs, String time){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size, maxSite, costs, time) VALUES (?,?,?,?,?) "
					+ "ON CONFLICT(catID) DO UPDATE SET size=?, maxSite=?, costs=?, time=?;", 
					String.valueOf(catID), String.valueOf(invSize), String.valueOf(maxSite), String.valueOf(costs), time, 
					String.valueOf(invSize), String.valueOf(maxSite), String.valueOf(costs), time);
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size, maxSite, costs, time) VALUES (?,?,?,?,?) "
					+ "ON DUPLICATE KEY UPDATE size=?, maxSite=?, costs=?, time=?;", 
					String.valueOf(catID), String.valueOf(invSize), String.valueOf(maxSite), String.valueOf(costs), time, 
					String.valueOf(invSize), String.valueOf(maxSite), String.valueOf(costs), time);
	}
	
	public void updateHotelCategory(int catID, double costs, String time){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_hotel_categories + " (catID, costs, time) VALUES (?,?,?) "
					+ "ON CONFLICT(catID) DO UPDATE SET costs=?, time=?;", 
					String.valueOf(catID), String.valueOf(costs), time, 
					String.valueOf(costs), time);
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_hotel_categories + " (catID, costs, time) VALUES (?,?,?) "
					+ "ON DUPLICATE KEY UPDATE costs=?, time=?;", 
					String.valueOf(catID), String.valueOf(costs), time, 
					String.valueOf(costs), time);
	}
	
	public void setAlias(int catId, RentTypes type, String alias){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("UPDATE " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " SET alias=? WHERE catID=?;", 
				alias, String.valueOf(catId));
	}

	public void setSize(int catId, int invSize){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size) VALUES (?,?) "
					+ "ON CONFLICT(catID) DO UPDATE SET size=?;", 
					String.valueOf(catId), String.valueOf(invSize), String.valueOf(invSize));
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, size) VALUES (?,?) "
					+ "ON DUPLICATE KEY UPDATE size=?;", 
					String.valueOf(catId), String.valueOf(invSize), String.valueOf(invSize));
	}
	
	public void setMaxSite(int catId, int maxSite){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, maxSite) VALUES (?,?) "
					+ "ON CONFLICT(catID) DO UPDATE SET maxSite=?;", 
					String.valueOf(catId), String.valueOf(maxSite), String.valueOf(maxSite));
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + sql.t_shop_categories + " (catID, maxSite) VALUES (?,?) "
					+ "ON DUPLICATE KEY UPDATE maxSite=?;", 
					String.valueOf(catId), String.valueOf(maxSite), String.valueOf(maxSite));
	}
	
	public void setCosts(int catId, RentTypes type, double costs){
		AsyncSQL sql = this.instance.getAsyncSQL();
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, costs) VALUES (?,?) "
					+ "ON CONFLICT(catID) DO UPDATE SET costs=?;", 
					String.valueOf(catId), String.valueOf(costs), String.valueOf(costs));
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, costs) VALUES (?,?) "
					+ "ON DUPLICATE KEY UPDATE costs=?;", 
					String.valueOf(catId), String.valueOf(costs), String.valueOf(costs));
	}
	
	public void setTime(int catId, RentTypes type, String time){
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) //SQLLITE
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, time) VALUES (?,?) "
					+ "ON CONFLICT(catID) DO UPDATE SET time=?;", 
					String.valueOf(catId), time, time);
		else //MYSQL
			sql.queryUpdate("INSERT INTO " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " (catID, time) VALUES (?,?) "
					+ "ON DUPLICATE KEY UPDATE time=?;", 
					String.valueOf(catId), time, time);
		
	}
	
	public void delete(RentTypes type, int catId){
		
		AsyncSQL sql = this.instance.getAsyncSQL();
		sql.queryUpdate("DELETE FROM " + (type.equals(RentTypes.SHOP) ? sql.t_shop_categories : sql.t_hotel_categories) + " WHERE catID=?;", String.valueOf(catId));
	}

	public void setupCategories(Consumer<Boolean> c) {
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
								
								int maxSite = rs.getInt("maxSite");
								if(maxSite <= 0)
									maxSite = 1;
								handler.setMaxSite(maxSite);
							}
							
							HashMap<Integer, CategoryHandler> hash = new HashMap<>();
							if(instance.catHandlers.containsKey(type))
								hash = instance.catHandlers.get(type);
							
							hash.put(catID, handler);
							instance.catHandlers.put(type, hash);
						}
						
						instance.getLogger().info(String.valueOf(catAmount) + " " + type.toString() + "-Categories are loaded.");
					} catch (SQLException e) {
						e.printStackTrace();
						c.accept(false);
						return;
					}
				}
			});
		}

		c.accept(true);
	}
}
