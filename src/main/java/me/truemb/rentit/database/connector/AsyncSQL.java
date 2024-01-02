package me.truemb.rentit.database.connector;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import me.truemb.rentit.main.Main;
import me.truemb.rentit.utils.UTF8YamlConfiguration;

public class AsyncSQL {
	
	private ExecutorService executor;
	private DatabaseConnector sql;
	
	private Main instance;
	
	//TABLES

	public String t_shop_categories = "rentit_shopcategories";
	public String t_hotel_categories = "rentit_hotelcategories";
	public String t_hotels = "rentit_hotels";
	public String t_shops = "rentit_shops";
	public String t_shop_inv = "rentit_shopInv";
	public String t_shop_inv_new = "rentit_shopInventories";
	public String t_perms = "rentit_perms";
	public String t_settings = "rentit_psettings";
	
	
	public AsyncSQL(Main plugin) throws Exception {
		this.instance = plugin;
		
		UTF8YamlConfiguration config = this.instance.manageFile();
		String db = "Database.";

		String type = config.getString(db + "type");
		DatabaseStorage storage = DatabaseStorage.getStorageFromString(type);
		
		if(storage == null)
			throw new Exception("The Database Storage Type is invalid! '" + type + "'");
		

		this.executor = Executors.newCachedThreadPool();
		
		plugin.getLogger().info("{SQL} Connecting to " + type + " Database...");
		
		if (storage == DatabaseStorage.SQLITE) {
			File databaseFile = new File(plugin.getDataFolder(), "Database.db");
			this.sql = new DatabaseConnector(plugin.getLogger(), databaseFile);
		} else {
			String host = config.getString(db + "host");
			int port = config.getInt(db + "port");
			String user = config.getString(db + "user");
			String password = config.getString(db + "password");
			String database = config.getString(db + "database");
			boolean useSSL = config.getBoolean(db + "useSSL");
		
			this.sql = new DatabaseConnector(storage, host, port, user, password, database, useSSL);
		}
		
	}

	public void addColumn(String table, String name, String type) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.getDatabaseConnector().getDriver().getType() == DatabaseStorage.SQLITE) {
			sql.prepareStatement("Select * from pragma_table_info('" + table + "') WHERE name = '" + name + "';", new Consumer<ResultSet>() {
			
				@Override
				public void accept(ResultSet rs) {
					try {
						if(!rs.next()) //COLUMN DOESNT EXISTS
							sql.queryUpdate("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type + ";");
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		} else {
			//sql.queryUpdate("ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + name + " " + type + ";");
			sql.prepareStatement("SHOW COLUMNS FROM `" + table + "` LIKE '" + name + "'", new Consumer<ResultSet>() {
				
				@Override
				public void accept(ResultSet rs) {
					try {
						if(!rs.next()) //COLUMN DOESNT EXISTS
							sql.queryUpdate("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type + ";");
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public void queryUpdate(PreparedStatement statement) {
		this.executor.execute(() -> this.sql.queryUpdate(statement));
	}

	public void queryUpdate(String statement, String... args) {
		this.executor.execute(() -> this.sql.queryUpdate(statement, args));
	}
	
	public void prepareStatement(PreparedStatement statement, Consumer<ResultSet> consumer) {
		this.executor.execute(() -> {
			ResultSet result = this.sql.query(statement);
			new Thread(() -> consumer.accept(result)).start();
		});
	}

	public void prepareStatement(String statement, Consumer<ResultSet> consumer) {
		this.executor.execute(() -> {
			ResultSet result = this.sql.query(statement);
			new Thread(() -> consumer.accept(result)).start();
		});
	}
	
	public void prepareStatement(String statement, Consumer<ResultSet> consumer, String... args) {
		this.executor.execute(() -> {
			ResultSet result = this.sql.query(statement, args);
			new Thread(() -> consumer.accept(result)).start();
		});
	}
	
	public PreparedStatement prepare(String query) {
		try {
			return this.sql.getConnection().prepareStatement(query);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public DatabaseConnector getDatabaseConnector() {
		return this.sql;
	}
	
	public boolean isSqlLite() {
		return this.getDatabaseConnector().getDriver().getType() == DatabaseStorage.SQLITE;
	}

}