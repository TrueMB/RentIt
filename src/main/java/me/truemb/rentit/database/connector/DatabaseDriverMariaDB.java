package me.truemb.rentit.database.connector;

public class DatabaseDriverMariaDB extends DatabaseDriver{

	@Override
	public DatabaseStorage getType() {
		return DatabaseStorage.MARIADB;
	}

	@Override
	public String getDriverClass() {
        return "org.mariadb.jdbc.Driver";
	}

	@Override
	public String getJdbcIdentifier() {
		return "mariadb";
	}
	
	@Override
	public String getArguments() {
		return "autoReconnect=true&failOverReadOnly=false&maxReconnects=10&sslMode="; //after = will be the Config useSSL Value!
	}

}
