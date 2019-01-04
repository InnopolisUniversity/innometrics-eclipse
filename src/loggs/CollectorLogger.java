package loggs;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class CollectorLogger extends Logger {
	private static Logger loggger = null;
	
	protected CollectorLogger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
		// TODO Auto-generated constructor stub
	}
	
	public static Logger getLogger() {
		if (loggger == null) {
			loggger = Logger.getLogger("EclipseCollectorLog");
			FileHandler fh;  

		    try {   
		    	String filePath = System.getProperty("user.dir") + "logs/EclipseCollectorLogFile.log";
		        fh = new FileHandler(filePath);  
		        loggger.addHandler(fh);
		        SimpleFormatter formatter = new SimpleFormatter();  
		        fh.setFormatter(formatter);  

		    } catch (SecurityException e) {  
		        e.printStackTrace();  
		    } catch (IOException e) {  
		        e.printStackTrace();  
		    }  
		}
		return loggger;
	}
}
