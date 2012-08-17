package com.Belkar.DeathChests;


import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

public class Settings {

	/**Duration in Seconds how long the timeout lasts.
	 */
	public static int TIMEOUT;
	
	/**Period of the autosaver
	 */
	public static int AUTOSAVE_PERIOD;
	
	/**Defines if empty DeathChests should be picked up when they got emptied
	 */
	public static boolean PICKUP_EMPTY_CHESTS;
	/**The time in seconds between emptying the chest and finally picking it up/putting it into the inventory
	 */
	public static long EMPTY_TIMEOUT = 0;

	private static String pluginName;

	private static String version;

//	public Settings(FileConfiguration config) {
//		loadConfig(config);
//	}

	/**Load the configuration from a file
	 * @param config File to read the settings from
	 */
	public static void loadConfig(FileConfiguration config) {
		AUTOSAVE_PERIOD = config.getInt("general.autosavePeriod");
		TIMEOUT = config.getInt("general.timeout");
		PICKUP_EMPTY_CHESTS = config.getBoolean("general.pickupEmpty");
		EMPTY_TIMEOUT = config.getInt("general.emptyTimeout");
	}

	/**Write down the configs
	 * @param config The used configuration-file
	 */
	public static void saveConfig(FileConfiguration config) {
		config.set("general.autosavePeriod", AUTOSAVE_PERIOD);
		config.set("general.timeout", TIMEOUT);
		config.set("general.pickupEmpty", PICKUP_EMPTY_CHESTS);
		config.set("general.emptyTimeout", EMPTY_TIMEOUT);
		
//		try {
//			config.save(config.getCurrentPath());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	public static String getPluginName() {
		if (Settings.pluginName == null) {
			pluginName = DeathChests.class.getName();
			int index = pluginName.lastIndexOf(".");
			if (index > 0)
				pluginName = pluginName.substring(index + 1);
		}
		return Settings.pluginName;
	}

	public static String getVersion() {
		if (Settings.version == null) {
			version = Bukkit.getPluginManager().getPlugin(getPluginName()).getDescription().getVersion();
		}
		return Settings.version;
	}
}
