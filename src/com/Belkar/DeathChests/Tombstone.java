package com.Belkar.DeathChests;

import java.io.Serializable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Tombstone implements Serializable {
	private static final long serialVersionUID = -6573214161254220289L;
	
	private String owner;
	private String worldName;
	private int[] chestPosition;
	private int[] chest2Position;
	private int[] signPosition;
	private boolean alreadyDroppedChests = false;
	
	public Tombstone(Player owner, String world, Vector chestPos, Vector chest2Pos, Vector signPos) {
		this.owner = owner.getName();
		this.worldName = world; 
		this.chestPosition = Utils.toArray(chestPos);
		this.chest2Position = Utils.toArray(chest2Pos);
		this.signPosition = Utils.toArray(signPos);	
		
		if (owner.hasPermission("deathchest.use.free")) {
			setDropChests();
		}
	}

	/**Returns the containing World of the DeathChest
	 */
	public String getWorld() {
		return this.worldName;
	}

	/**Returns the Owner of the DeathChest in form of a BukkitPlayer-Object
	 */
	public Player getOwner() {
		return Utils.getPlayer(owner);
	}

	/**Checks if the DeathChest is located at the specified position
	 * @param x X-Coordinate
	 * @param y Y-Coordinate
	 * @param z Z-Coordinate
	 */
	public boolean isChest(int x, int y, int z) {
		if (x == chestPosition[0] && y == chestPosition[1] && z == chestPosition[2]) {
			return true;
		}
		if (chest2Position != null && x == chest2Position[0] && y == chest2Position[1] && z == chest2Position[2]) {
			return true;
		}
		if (signPosition != null && x == signPosition[0] && y == signPosition[1] && z == signPosition[2]) {
			return true;
		}
		return false;
	}

	/**Converts the SignLocation into a usable Location
	 * @param world Containing World
	 * @return NULL if the DeathChest doesn't has a sign
	 */
	public Location getSignLoc(World world) {
		if (world == null)
			throw new NullPointerException();
		if (signPosition != null)
			return new Location(world, signPosition[0], signPosition[1], signPosition[2]);
		return null;
	}
	
	/**Converts the ChestLocation into a usable Location
	 * @param world Containing World
	 */
	public Location getChestLoc(World world) {
		if (world == null)
			throw new NullPointerException();
		return new Location(world, chestPosition[0], chestPosition[1], chestPosition[2]);
	}
	
	/**Converts the second ChestLocation into a usable Location
	 * @param world Containing World
	 * @return NULL if the DeathChest isn't a DoubleChest
	 */
	public Location getChest2Loc(World world) {
		if (world == null)
			throw new NullPointerException();
		if (chest2Position != null)
			return new Location(world, chest2Position[0], chest2Position[1], chest2Position[2]);
		return null;
	}

	/**Checks if the Chest has a NameSign
	 */
	public boolean hasSign() {
		return (signPosition != null);
	}

	/**Returns the name of the Owner
	 */
	public String getOwnerName() {
		return this.owner;
	}

	/**Converts the Position of the chest into a readable format
	 */
	public String getStringLoc() {
		return (new StringBuilder(this.worldName).append(" ").append(chestPosition[0]).append(",").append(chestPosition[1]).append(",").append(chestPosition[2])).toString();
	}

	/**Returns an ItemStack containing the chests of the DeathChest (use with DeathChest.removeChest())
	 */
	public ItemStack getChests() {
		int amount = 1;
		if (chest2Position != null) {
			amount = 2;
		}
		return new ItemStack(Material.CHEST, amount);
	}
	
	
	/**Returns the Inventory of the ChestBlocks
	 */
	public Inventory getInventory() {
		Block block = Bukkit.getWorld(worldName).getBlockAt(chestPosition[0], chestPosition[1], chestPosition[2]);
		return ((Chest) block.getState()).getInventory();
	}

	/**Checks if the DeathChest is a DoubleChest
	 */
	public boolean isDoubleChest() {
		return (chest2Position != null);
	}
	
	/**Sets that the Chest-ItemStack was already picked up
	 */
	public void setDropChests() {
		this.alreadyDroppedChests = true;
	}
	
	/**Check if the Chests were already picked up
	 */
	public boolean isDropped() {
		return this.alreadyDroppedChests;
	}
}
