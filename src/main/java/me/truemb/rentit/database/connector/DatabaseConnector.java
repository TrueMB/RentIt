package me.truemb.rentit.database.connector;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

public class DatabaseConnector {

	private DatabaseDriver driver;
	private String host, user, password, database;
	private int port;
	private boolean useSSL;

	private Connection conn;

	public DatabaseConnector(Logger logger, File file) throws Exception {
		
		this.driver = new DatabaseDriverSQLite(logger, file);
		this.openConnection();
	}
	
	public DatabaseConnector(DatabaseStorage storage, String host, int port, String user, String password, String database, boolean useSSL) throws Exception {
		
		switch (storage) {
			case MARIADB:
				this.driver = new DatabaseDriverMariaDB();
				break;
			case MYSQL:
				this.driver = new DatabaseDriverMySQL();
				break;
			default:
				break;
		}
		
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.database = database;
		this.useSSL = useSSL;

		this.openConnection();
	}

	public void queryUpdate(String query, String... args) {
		checkConnection();
		try {
			PreparedStatement statement = this.getConnection().prepareStatement(query);
			for (int i = 0; i < args.length; i++) {
				statement.setString(i + 1, args[i]);
			}
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

	public ResultSet query(String query, String... args) {
		checkConnection();
		try {
			PreparedStatement statement = this.getConnection().prepareStatement(query);
			for (int i = 0; i < args.length; i++) {
				statement.setString(i + 1, args[i]);
			}
			return query(statement);
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
	
	public DatabaseDriver getDriver() {
		return this.driver;
	}

	public void checkConnection() {
		try {
			if (this.conn == null || !this.conn.isValid(10) || this.conn.isClosed())
				openConnection();
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void openConnection() throws ClassNotFoundException, SQLException {
		String driverS = this.driver.getDriverClass();
		if(driverS != null)
			Class.forName(driverS);
		
		if(this.getDriver().getType() == DatabaseStorage.SQLITE) {
			DatabaseDriverSQLite driver = (DatabaseDriverSQLite) this.getDriver();
			this.conn = DriverManager.getConnection("jdbc:" + this.driver.getJdbcIdentifier() + ":" + driver.getDatabaseFolder().toString());
		}else
			this.conn = DriverManager.getConnection("jdbc:" + this.driver.getJdbcIdentifier() + "://" + this.host + ":" + this.port + "/" + this.database + "?" + this.getDriver().getArguments() + "" + String.valueOf(this.useSSL), this.user, this.password);
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
