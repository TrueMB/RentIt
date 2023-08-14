package me.truemb.rentit.database.connector;

public enum DatabaseStorage {

	SQLITE,
	MARIADB,
	MYSQL;

	public static DatabaseStorage getStorageFromString(String s) {
		if(s == null)
			return null;
		
		for(DatabaseStorage action : DatabaseStorage.values())
			if(action.toString().equalsIgnoreCase(s))
				return action;
				
		return null;
	}
}
