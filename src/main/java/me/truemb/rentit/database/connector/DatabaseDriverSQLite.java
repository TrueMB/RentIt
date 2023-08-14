package me.truemb.rentit.database.connector;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class DatabaseDriverSQLite extends DatabaseDriver{
	
	private Logger logger;
	private File dbFile;
	
	public DatabaseDriverSQLite(Logger logger, File databaseFile) {
		this.logger = logger;
		this.dbFile = databaseFile;
	}

	@Override
	public DatabaseStorage getType() {
		return DatabaseStorage.SQLITE;
	}

	@Override
	public String getDriverClass() {
        return null;
	}

	@Override
	public String getJdbcIdentifier() {
		return "sqlite";
	}
	
	@Override
	public String getArguments() {
		return null;
	}
	
	public File getDatabaseFolder() {
		File dataFolder = this.dbFile;
	    if (!dataFolder.exists()){
	        try {
	            dataFolder.createNewFile();
	        } catch (IOException e) {
	           this.logger.warning("File write error: " + dataFolder.getAbsolutePath());
	        }
	    }
        return dataFolder;
	}
}
