package com.Belkar.DeathChests;


import org.bukkit.configuration.file.FileConfiguration;

public class Settings {
	// Period of the autosaver
	public int AUTOSAVE_PERIOD;

	public Settings(FileConfiguration config) {
		loadConfig(config);
	}

	/**Load the configuration from a file
	 * @param config File to read the settings from
	 */
	public void loadConfig(FileConfiguration config) {
		this.AUTOSAVE_PERIOD = config.getInt("general.autosavePeriod");
	}

	/**Write down the configs
	 * @param config The used configuration-file
	 */
	public void saveConfig(FileConfiguration config) {
		config.set("general.autosavePeriod", this.AUTOSAVE_PERIOD);
		
//		try {
//			config.save(config.getCurrentPath());
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
}
