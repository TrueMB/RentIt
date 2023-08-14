package me.truemb.rentit.database.connector;

public class DatabaseDriverMySQL extends DatabaseDriver{

	@Override
	public DatabaseStorage getType() {
		return DatabaseStorage.MYSQL;
	}

	@Override
	public String getDriverClass() {
		return "com.mysql.cj.jdbc.Driver";
	}

	@Override
	public String getJdbcIdentifier() {
		return "mysql";
	}
	
	@Override
	public String getArguments() {
		return "autoReconnect=true&failOverReadOnly=false&maxReconnects=10&useSSL="; //after = will be the Config useSSL Value!
	}

}
