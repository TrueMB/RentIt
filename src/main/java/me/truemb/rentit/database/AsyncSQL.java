package me.truemb.rentit.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.bukkit.Bukkit;

import me.truemb.rentit.main.Main;

public class AsyncSQL {
	
	private ExecutorService executor;
	private MySQL mySql;
	private SqlLite sqlLite;
	
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
		
		String db = "Database.";
		if (this.instance.getConfig().getString(db + "host").equalsIgnoreCase("ipaddress")) {
		
			this.sqlLite = new SqlLite(new File(plugin.getDataFolder(), "Database.db"));
			this.executor = Executors.newCachedThreadPool();

			this.instance.getLogger().info("Using SQLLite since MySQL was not set up.");
		} else {
			String host = this.instance.getConfig().getString(db + "host");
			int port = this.instance.getConfig().getInt(db + "port");
			String user = this.instance.getConfig().getString(db + "user");
			String password = this.instance.getConfig().getString(db + "password");
			String database = this.instance.getConfig().getString(db + "database");
			boolean useSSL = this.instance.getConfig().getBoolean(db + "useSSL");
		
			this.mySql = new MySQL(host, port, user, password, database, useSSL);
			this.executor = Executors.newCachedThreadPool();
		}
	}

	public void addColumn(String table, String name, String type) {
		AsyncSQL sql = this.instance.getAsyncSQL();
		
		if(sql.isSqlLite()) {
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
	
	public boolean isSqlLite() {
		return this.mySql == null;
	}

	public void queryUpdate(PreparedStatement statement) {
		if(this.mySql != null)
			this.executor.execute(() -> this.mySql.queryUpdate(statement));
		else
			this.executor.execute(() -> this.sqlLite.queryUpdate(statement));
				
	}

	public void queryUpdate(String statement) {
		if(this.mySql != null)
			this.executor.execute(() -> this.mySql.queryUpdate(statement));
		else
			this.executor.execute(() -> this.sqlLite.queryUpdate(statement));
	}
	
	public void prepareStatement(PreparedStatement statement, Consumer<ResultSet> consumer) {
		if(this.mySql != null)
			this.executor.execute(() -> {
				ResultSet result = this.mySql.query(statement);
				Bukkit.getScheduler().runTaskAsynchronously(this.instance, () -> consumer.accept(result));
			});
		else
			this.executor.execute(() -> {
				ResultSet result = this.sqlLite.query(statement);
				Bukkit.getScheduler().runTaskAsynchronously(this.instance, () -> consumer.accept(result));
			});
	}

	public void prepareStatement(String statement, Consumer<ResultSet> consumer) {
		if(this.mySql != null)
			this.executor.execute(() -> {
				ResultSet result = this.mySql.query(statement);
				Bukkit.getScheduler().runTaskAsynchronously(this.instance, () -> consumer.accept(result));
			});
		else
			this.executor.execute(() -> {
				ResultSet result = this.sqlLite.query(statement);
				Bukkit.getScheduler().runTaskAsynchronously(this.instance, () -> consumer.accept(result));
			});
	}

	public PreparedStatement prepare(String query) {
		try {
			if(this.mySql != null)
				return this.mySql.getConnection().prepareStatement(query);
			else
				return this.sqlLite.getConnection().prepareStatement(query);
				
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public MySQL getMySQL() {
		return this.mySql;
	}
	
	public SqlLite getSqlLite() {
		return this.sqlLite;
	}

	public static class MySQL {

		private String host, user, password, database;
		private int port;
		private boolean useSSL;

		private Connection conn;

		public MySQL(String host, int port, String user, String password, String database, boolean useSSL) throws Exception {
			this.host = host;
			this.port = port;
			this.user = user;
			this.password = password;
			this.database = database;
			this.useSSL = useSSL;

			this.openConnection();
		}

		public void queryUpdate(String query) {
			checkConnection();
			try (PreparedStatement statement = conn.prepareStatement(query)) {
				queryUpdate(statement);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void queryUpdate(PreparedStatement statement) {
			checkConnection();
			try {
				statement.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					statement.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public ResultSet query(String query) {
			checkConnection();
			try {
				return query(conn.prepareStatement(query));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public ResultSet query(PreparedStatement statement) {
			checkConnection();
			try {
				return statement.executeQuery();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public Connection getConnection() {
			return this.conn;
		}

		public void checkConnection() {
			try {
				if (this.conn == null || !this.conn.isValid(10) || this.conn.isClosed())
					openConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Connection openConnection() throws Exception {
			//Class.forName("com.mysql.jdbc.Driver");
			return this.conn = DriverManager.getConnection("jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useSSL=" + String.valueOf(this.useSSL), this.user, this.password);
		}
		
		public void closeRessources(ResultSet rs, PreparedStatement st){
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {}
			}
			if(st != null){
				try {
					st.close();
				} catch (SQLException e) {}
			}
		}

		public void closeConnection() {
			try {
				this.conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				this.conn = null;
			}
		}
	}

	public static class SqlLite {

		private Connection connection;
		private File dbFile;

		public SqlLite(File dbFile) throws Exception {
			this.dbFile = dbFile;
			this.openConnection();
		}

		public void queryUpdate(String query) {
			checkConnection();
			try (PreparedStatement statement = connection.prepareStatement(query)) {
				queryUpdate(statement);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void queryUpdate(PreparedStatement statement) {
			checkConnection();
			try {
				statement.executeUpdate();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					statement.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public ResultSet query(String query) {
			checkConnection();
			try {
				return query(connection.prepareStatement(query));
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public ResultSet query(PreparedStatement statement) {
			checkConnection();
			try {
				return statement.executeQuery();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		public Connection getConnection() {
			return this.connection;
		}

		public void checkConnection() {
			try {
				if (this.connection == null || !this.connection.isValid(10) || this.connection.isClosed())
					openConnection();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public Connection openConnection() throws Exception {
		        File dataFolder = this.dbFile;
		        if (!dataFolder.exists()){
		            try {
		                dataFolder.createNewFile();
		            } catch (IOException e) {
		               System.err.println("File write error: " + dataFolder.getAbsolutePath());
		            }
		        }
		        try {
		            if(connection != null && !connection.isClosed()){
		                return connection;
		            }
		            
		            connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder);
		            return connection;
		        } catch (SQLException ex) {
		        	System.err.println("SQLite exception on initialize\n" + ex.getStackTrace());
		        }
		        return null;
		}
		
		public void closeRessources(ResultSet rs, PreparedStatement st){
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {}
			}
			if(st != null){
				try {
					st.close();
				} catch (SQLException e) {}
			}
		}

		public void closeConnection() {
			try {
				this.connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				this.connection = null;
			}
		}
	}
}