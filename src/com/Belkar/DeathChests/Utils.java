package com.Belkar.DeathChests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.management.modelmbean.XMLParseException;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemInWorldManager;
import net.minecraft.server.MinecraftServer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class Utils {

	// Sign-DataValues of their rotation/orientation
	public static final byte SIGN_NORTH = 0x2;
	public static final byte SIGN_SOUTH = 0x3;
	public static final byte SIGN_WEST = 0x4;
	public static final byte SIGN_EAST = 0x5;
	private static final String DEATHCHEST_XML_TAG = "deathchest";
	
//	/**Save an Object to the FileSystem
//	 * @param obj Object to save
//	 * @param path Path where to save the Object
//	 */
//	public static <T extends Object> void save(T obj,String path) throws Exception
//	{
//		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
//		oos.writeObject(obj);
//		oos.flush();
//		oos.close();
//	}
//	
//	/**
//	 * @param path Path to read the Object from
//	 * @return Loaded Object
//	 */
//	@SuppressWarnings("unchecked")
//	public static <T extends Object> T load(String path) throws Exception
//	{
//		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
//		T result = (T)ois.readObject();
//		ois.close();
//		return result;
//	}

	/**Counts the used slots in an inventory
	 * @param inventory Inventory to count from
	 * @return Amount of used slots
	 */
	public static int getStacks(Inventory inventory) {
		int cnt = 0;
		
		ItemStack[] items = inventory.getContents();
		for (ItemStack itemStack : items) {
			if (itemStack != null && itemStack.getTypeId() > 0 && itemStack.getAmount() > 0) {
				cnt++;
			}
		}
		
		return cnt;
	}
	
	/**Converts a Vector into a int[3] ([x;y;z])
	 * @param vector Vector to convert
	 * @return Converted Vector
	 */
	public static int[] toArray(Vector vector) {
		if (vector == null)
			return null;
		return new int[]{vector.getBlockX(), vector.getBlockY(), vector.getBlockZ()};
	}
	
	/**Converts a int[3] back to a Vector (look at Utils.toArray(Vector))
	 * @param array Array to Convert
	 * @return Converted Array
	 */
	public static Vector toVector(int[] array) {
		return new Vector(array[0], array[1], array[2]);
	}
	
	/**Resolves the name of a player (ether Online or Offline) to a BukkitPlayer 
	 * @param name Name of the Player
	 * @return Instance of the Player or NULL if nothing found
	 */ // CREDITS TO: lishid (OpenInv Author)
	public static Player getPlayer(String name) {
        Player target = Bukkit.getServer().getPlayer(name);
        
        if (target == null)
        {
            try
            {
                // See if the player has data files
                // Default player folder
                File playerfolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "players");
                if (!playerfolder.exists())
                {
                    return null;
                }
                
                String playername = matchUser(Arrays.asList(playerfolder.listFiles()), name);
                if (playername == null)
                {
                    return null;
                }
                
                // Create an entity to load the player data
                final MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
                final EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), playername, new ItemInWorldManager(server.getWorldServer(0)));
                target = (entity == null) ? null : (Player) entity.getBukkitEntity();
                if (target != null)
                {
                    target.loadData();
                }
                else
                {
                    return null;
                }
            }
            catch (Exception e)
            {
                return null;
            }
        }
		return target;
	}
	
    /**Searches a Collection of FileNames and checks if the String is in it. If it is found, it returns the FILENAME (countains Case)
     * @param container FileNames to check
     * @param search String to search
     * @return String or NULL if nothing found
     */ // CREDITS TO: lishid (OpenInv Author)
    private static String matchUser(final Collection<File> container, final String search)
    {
        String found = null;
        if (search == null)
        {
            return found;
        }
        final String lowerSearch = search.toLowerCase();
        int delta = Integer.MAX_VALUE;
        for (final File file : container)
        {
            final String filename = file.getName();
            final String str = filename.substring(0, filename.length() - 4);
            if (!str.toLowerCase().startsWith(lowerSearch))
            {
                continue;
            }
            final int curDelta = str.length() - lowerSearch.length();
            if (curDelta < delta)
            {
                found = str;
                delta = curDelta;
            }
            if (curDelta == 0)
            {
                break;
            }
            
        }
        return found;
    }
    
	/**Compresses the Inventory-Array (removes empty slot! doesn't join stacks of items)
	 * @param contents Inventory-Array
	 * @return Compressed Collection
	 */
	public static Collection<ItemStack> compressInventor(ItemStack[] contents) {
		List<ItemStack> list = new LinkedList<>();
		
		for (ItemStack itemStack : contents) {
			if (itemStack != null && itemStack.getType() != Material.AIR) {
				list.add(itemStack);
			}
		}
		
		return list;
	}
	/**Compresses the Array but also converts it into an Array rather than a Collection (look at compressInventory(ItemStack[]))
	 * @param contents Inventory-Array
	 * @return Compressed Array
	 */
	public static ItemStack[] compressInventorArray(ItemStack[] contents) {
		return collectionToArray(compressInventor(contents));
	}
	
	/**Converts a Collection of ItemStack (Inventory) into an Array
	 * @param coll Collection
	 * @return Converted Array
	 */
	public static ItemStack[] collectionToArray(Collection<ItemStack> coll) {
		ItemStack[] items = new ItemStack[coll.size()];
		int i = 0;
		for (ItemStack itemStack : coll) {
			items[i] = itemStack;
			i++;
		}
		return items;
	}

	public static boolean saveTombstones(List<Tombstone> deathChests, String chestsPath) {
		long timestamp = System.currentTimeMillis();
		try {
			DocumentBuilderFactory factory =
	            DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();

	        // Create the writer
	        Document xmlDoc = builder.newDocument();
	        
	        Element rootNode = xmlDoc.createElement("deathchestList");
//	        String pluginName = Settings.getPluginName();
	        String version = Settings.getVersion();
	        rootNode.setAttribute("version", version);
	        xmlDoc.appendChild(rootNode);

	        for (Tombstone tombstone : deathChests) {
	        	rootNode.appendChild(tombstone.createXmlNode(xmlDoc.createElement(Utils.DEATHCHEST_XML_TAG), xmlDoc, timestamp));
			}
	        
	        // Put the XML file into domSource
	        DOMSource domSource = new DOMSource(xmlDoc);

	        // PrintStream will be responsible for writing
	        // the text data to the file
	        PrintStream ps = new PrintStream(chestsPath);
	        StreamResult fileWriter = new StreamResult(ps);

	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        Transformer transformer = transformerFactory.newTransformer();

	        // Finally save the file
	        transformer.transform(domSource, fileWriter);
	        
	        return true;
		} catch (ParserConfigurationException | TransformerException | FileNotFoundException ex) {
			System.err.println("Error while writing deathchest data:");
			ex.printStackTrace();
		}
		return false;
	}

	public static List<Tombstone> loadTombstone(String chestsPath) {
		LinkedList<Tombstone> deathChests = new LinkedList<>(); 
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	
	        //Start the parser
	        Document doc = builder.parse(new File(chestsPath));
	        
	        Element rootElement = doc.getDocumentElement();
	        
	        String version = rootElement.getAttribute("version");
	        if (!Settings.getVersion().equals(version)) {
	        	System.out.println("The version of the Deathchests-File changed. There are possible errors at loading them. (Probably not if your updated it)");
	        }
	        	        
	        long timestamp = System.currentTimeMillis();
	        
	        NodeList list = rootElement.getChildNodes();
	        int length = list.getLength();
	        for (int i = 0; i < length; i++) {
	        	try {
		        	Node node = list.item(i);
		        	String name = node.getNodeName(); 
		        	if (name.equalsIgnoreCase(Utils.DEATHCHEST_XML_TAG)) {
		        		deathChests.add(new Tombstone((Element)node, timestamp));
		        	}
	        	} catch (XMLParseException ex) {
	        		System.err.println("Corrupted deathchest! Skipping this one!");
	        		ex.printStackTrace();
	        	}
	        }
		} catch (IOException | ParserConfigurationException | SAXException ex) {
			System.err.println("Error while loading deathchest data:");
			ex.printStackTrace();
		}
		return deathChests;
	}
}
