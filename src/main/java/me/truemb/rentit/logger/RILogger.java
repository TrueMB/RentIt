package me.truemb.rentit.logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Logger for TempFly relevant debugging
 */
public class RILogger {
	
	private final String loggerName = "RentItTransaction";
	private final String logFileName = "RentItTransaction.log";
	
	private final int fileSize = 31457280; //30 MB
	
	private Logger logger = Logger.getLogger(this.loggerName);
	
	public RILogger(String path) {
		try {
			FileHandler handler = new FileHandler(path + "/" + this.logFileName, this.fileSize, 1, true);
			
			handler.setFormatter(new SimpleFormatter());
			this.logger.addHandler(handler);
			this.logger.setUseParentHandlers(false);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}
	
	public Logger getLogger() {
		return this.logger;
	}

}
