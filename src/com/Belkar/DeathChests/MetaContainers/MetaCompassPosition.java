package com.Belkar.DeathChests.MetaContainers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class MetaCompassPosition implements MetadataValue {

	private Location position;

	public MetaCompassPosition(Location pos) {
		this.position = pos;
	}
	
	
	@Override
	public boolean asBoolean() {
		return false;
	}

	@Override
	public byte asByte() {
		return 0;
	}

	@Override
	public double asDouble() {
		return 0;
	}

	@Override
	public float asFloat() {
		return 0;
	}

	@Override
	public int asInt() {
		return 0;
	}

	@Override
	public long asLong() {
		return 0;
	}

	@Override
	public short asShort() {
		return 0;
	}

	@Override
	public String asString() {
		return null;
	}

	@Override
	public Plugin getOwningPlugin() {
		return Bukkit.getPluginManager().getPlugin("DeathChests");
	}

	@Override
	public void invalidate() {
	}

	@Override
	public Object value() {
		return this.position;
	}

}
