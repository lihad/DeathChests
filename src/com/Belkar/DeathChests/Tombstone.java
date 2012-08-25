package com.Belkar.DeathChests;

import javax.management.modelmbean.XMLParseException;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Tombstone {	
	private Player owner;
	private String worldName;
	private int[] chestPosition;
	private int[] chest2Position;
	private int[] signPosition;
	private boolean alreadyDroppedChests = false;
	private long timestamp = -1;
	
	private int savedXp = 0;
	
	public Tombstone(Player owner, String world, Vector chestPos, Vector chest2Pos, Vector signPos, int xp) {
//		this.owner = owner.getName();
		this.owner = owner;
		this.worldName = world; 
		this.chestPosition = Utils.toArray(chestPos);
		this.chest2Position = Utils.toArray(chest2Pos);
		this.signPosition = Utils.toArray(signPos);
		
		this.savedXp = xp;
		
		if (!owner.hasPermission("deathchest.use.noTimeout")) {
			this.timestamp = System.currentTimeMillis();
		}
	}

	public Tombstone(Element node, long currentTimestamp) throws XMLParseException {
		this.owner = Utils.getPlayer(node.getAttribute("owner"));
		if (!owner.hasPermission("deathchest.use.noTimeout")) {
			this.timestamp = currentTimestamp - Long.parseLong(node.getAttribute("existingTime"));
		}
		this.savedXp = Integer.getInteger(node.getAttribute("xp"));
		this.alreadyDroppedChests = Boolean.getBoolean(node.getAttribute("dropped"));
		
		Element positionsNode = (Element) node.getElementsByTagName("positions").item(0);
		{
			this.worldName = positionsNode.getAttribute("world");
			int x, y, z;
			{
				Element chestPos = (Element) positionsNode.getElementsByTagName("chestPos").item(0);
				x = Integer.parseInt(chestPos.getAttribute("x"));
				y = Integer.parseInt(chestPos.getAttribute("y"));
				z = Integer.parseInt(chestPos.getAttribute("z"));
				
				this.chestPosition = new int[] { x, y, z };
			}
			{
				NodeList list = positionsNode.getElementsByTagName("chest2Pos");
				if (list.getLength() > 0) {
					Element chest2Pos = (Element) list.item(0);
					x = Integer.parseInt(chest2Pos.getAttribute("x"));
					y = Integer.parseInt(chest2Pos.getAttribute("y"));
					z = Integer.parseInt(chest2Pos.getAttribute("z"));
					
					this.chest2Position = new int[] { x, y, z };
				}
			}
			{				
				NodeList list = positionsNode.getElementsByTagName("signPos");
				if (list.getLength() > 0) {
					Element signPos = (Element) list.item(0);
					x = Integer.parseInt(signPos.getAttribute("x"));
					y = Integer.parseInt(signPos.getAttribute("y"));
					z = Integer.parseInt(signPos.getAttribute("z"));
					
					this.signPosition = new int[] { x, y, z };
				}
			}
		}
		
		if (owner == null || worldName == null || chestPosition == null)
			throw new XMLParseException("Important fields are missing! Did you modify this file?");
	}

	/**Returns the containing World of the DeathChest
	 */
	public String getWorld() {
		return this.worldName;
	}

	/**Returns the Owner of the DeathChest in form of a BukkitPlayer-Object
	 */
	public Player getOwner() {
//		return Utils.getPlayer(owner);
		return owner;
	}

	/**Checks if the DeathChest is located at the specified position
	 * @param x X-Coordinate
	 * @param y Y-Coordinate
	 * @param z Z-Coordinate
	 */
	public boolean isChest(int x, int y, int z, String world) {
		if (this.worldName.equalsIgnoreCase(world)) {
			if (x == chestPosition[0] && y == chestPosition[1] && z == chestPosition[2]) {
				return true;
			}
			if (chest2Position != null && x == chest2Position[0] && y == chest2Position[1] && z == chest2Position[2]) {
				return true;
			}
			if (signPosition != null && x == signPosition[0] && y == signPosition[1] && z == signPosition[2]) {
				return true;
			}
		}
		return false;
	}
	
	/**Same as isChest(int, int, int, String) but with a Location
	 * @param Location to check
	 * @return true if there is a this chest
	 */
	public boolean isChest(Location loc) {
		return isChest(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), loc.getWorld().getName());
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
		return this.owner.getName();
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

	
	/**Returns the amount of XP that is saved in the chest
	 */
	public int getXp() {
		return savedXp;
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
	
	/**Checks if the timeout has already ran out
	 */
	public boolean isTimedOut() {
		if (Settings.TIMEOUT == 0)
			return false;
//		System.out.println("[DEBUG] timestamp = " + this.timestamp);
//		System.out.println("[DEBUG] TIMEOUT = " + Settings.TIMEOUT * 1000);
//		System.out.println("[DEBUG] current = " + System.currentTimeMillis());
		if (this.timestamp + ((long)(Settings.TIMEOUT * 1000)) < System.currentTimeMillis())
			return true;
		return false;
	}

	public Node createXmlNode(Element rootNode, Document xmlDoc, long currentTimestamp) {
		rootNode.setAttribute("owner", this.owner.getName());
		rootNode.setAttribute("xp", Integer.toString(savedXp));

		Element positions = xmlDoc.createElement("positions");
		positions.setAttribute("world", worldName);
		{
			Element chestPos = xmlDoc.createElement("chestPos");
			chestPos.setAttribute("x", Integer.toString(chestPosition[0]));
			chestPos.setAttribute("y", Integer.toString(chestPosition[1]));
			chestPos.setAttribute("z", Integer.toString(chestPosition[2]));
			positions.appendChild(chestPos);
			if (isDoubleChest()) {
				Element chest2Pos = xmlDoc.createElement("chest2Pos");
				chest2Pos.setAttribute("x", Integer.toString(chest2Position[0]));
				chest2Pos.setAttribute("y", Integer.toString(chest2Position[1]));
				chest2Pos.setAttribute("z", Integer.toString(chest2Position[2]));
				positions.appendChild(chest2Pos);
			}
			if (hasSign()) {
				Element signPos = xmlDoc.createElement("signPos");
				signPos.setAttribute("x", Integer.toString(signPosition[0]));
				signPos.setAttribute("y", Integer.toString(signPosition[1]));
				signPos.setAttribute("z", Integer.toString(signPosition[2]));
				positions.appendChild(signPos);
			}
		}
				
		rootNode.appendChild(positions);
		
		rootNode.setAttribute("dropped", Boolean.toString(alreadyDroppedChests));
		if (timestamp > 0) {
			rootNode.setAttribute("existingTime", Long.toString(currentTimestamp - timestamp));
		} else {
			rootNode.setAttribute("existingTime", Long.toString(timestamp));
		}
		
		return rootNode;
	}
}
