package mc.alk.arena.util;

import java.util.logging.Logger;

import org.bukkit.Bukkit;

public class Log {
	public static final boolean debug = true;
	
	private static Logger log = Bukkit.getLogger() != null ? Bukkit.getLogger() : Logger.getLogger("Arena");

	public static void info(String msg){
		if (log != null)
			log.info(colorChat(msg));
		else 
			System.out.println(colorChat(msg));
	}
	public static void warn(String msg){
		if (log != null)
			log.warning(colorChat(msg));
		else 
			System.err.println(colorChat(msg));
	}
	public static void err(String msg){
		if (log != null)
			log.severe(colorChat(msg));
		else 
			System.err.println(colorChat(msg));
	}
	
    public static String colorChat(String msg) {
        return msg.replaceAll("&", Character.toString((char) 167));
    }
    public static void debug(String msg){
    	System.out.println(msg);
    }
}
