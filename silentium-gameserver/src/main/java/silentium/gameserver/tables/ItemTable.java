/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.tables;

import javolution.util.FastMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.Item;
import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2ItemInstance.ItemLocation;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.L2Attackable;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.skills.SkillsEngine;
import silentium.gameserver.templates.item.*;
import silentium.gameserver.utils.LoggingUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class ItemTable
{
	private static Logger _log = LoggerFactory.getLogger(ItemTable.class.getName());
	private static Logger _logItems = LoggerFactory.getLogger("item");

	public static final Map<String, Integer> _materials = new FastMap<>();
	public static final Map<String, Integer> _crystalTypes = new FastMap<>();
	public static final Map<String, Integer> _slots = new FastMap<>();
	public static final Map<String, L2ArmorType> _armorTypes = new FastMap<>();
	public static final Map<String, L2WeaponType> _weaponTypes = new FastMap<>();

	private L2Item[] _allTemplates;
	private final Map<Integer, L2Armor> _armors;
	private final Map<Integer, L2EtcItem> _etcItems;
	private final Map<Integer, L2Weapon> _weapons;

	static
	{
		_materials.put("adamantaite", L2Item.MATERIAL_ADAMANTAITE);
		_materials.put("blood_steel", L2Item.MATERIAL_BLOOD_STEEL);
		_materials.put("bone", L2Item.MATERIAL_BONE);
		_materials.put("bronze", L2Item.MATERIAL_BRONZE);
		_materials.put("cloth", L2Item.MATERIAL_CLOTH);
		_materials.put("chrysolite", L2Item.MATERIAL_CHRYSOLITE);
		_materials.put("cobweb", L2Item.MATERIAL_COBWEB);
		_materials.put("cotton", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("crystal", L2Item.MATERIAL_CRYSTAL);
		_materials.put("damascus", L2Item.MATERIAL_DAMASCUS);
		_materials.put("dyestuff", L2Item.MATERIAL_DYESTUFF);
		_materials.put("fine_steel", L2Item.MATERIAL_FINE_STEEL);
		_materials.put("gold", L2Item.MATERIAL_GOLD);
		_materials.put("horn", L2Item.MATERIAL_HORN);
		_materials.put("leather", L2Item.MATERIAL_LEATHER);
		_materials.put("liquid", L2Item.MATERIAL_LIQUID);
		_materials.put("mithril", L2Item.MATERIAL_MITHRIL);
		_materials.put("oriharukon", L2Item.MATERIAL_ORIHARUKON);
		_materials.put("paper", L2Item.MATERIAL_PAPER);
		_materials.put("scale_of_dragon", L2Item.MATERIAL_SCALE_OF_DRAGON);
		_materials.put("seed", L2Item.MATERIAL_SEED);
		_materials.put("silver", L2Item.MATERIAL_SILVER);
		_materials.put("steel", L2Item.MATERIAL_STEEL);
		_materials.put("wood", L2Item.MATERIAL_WOOD);

		_crystalTypes.put("s", L2Item.CRYSTAL_S);
		_crystalTypes.put("a", L2Item.CRYSTAL_A);
		_crystalTypes.put("b", L2Item.CRYSTAL_B);
		_crystalTypes.put("c", L2Item.CRYSTAL_C);
		_crystalTypes.put("d", L2Item.CRYSTAL_D);
		_crystalTypes.put("none", L2Item.CRYSTAL_NONE);

		// weapon types
		for (L2WeaponType type : L2WeaponType.values())
			_weaponTypes.put(type.toString(), type);

		// armor types
		for (L2ArmorType type : L2ArmorType.values())
			_armorTypes.put(type.toString(), type);

		_slots.put("chest", L2Item.SLOT_CHEST);
		_slots.put("fullarmor", L2Item.SLOT_FULL_ARMOR);
		_slots.put("alldress", L2Item.SLOT_ALLDRESS);
		_slots.put("head", L2Item.SLOT_HEAD);
		_slots.put("hair", L2Item.SLOT_HAIR);
		_slots.put("face", L2Item.SLOT_FACE);
		_slots.put("hairall", L2Item.SLOT_HAIRALL);
		_slots.put("underwear", L2Item.SLOT_UNDERWEAR);
		_slots.put("back", L2Item.SLOT_BACK);
		_slots.put("neck", L2Item.SLOT_NECK);
		_slots.put("legs", L2Item.SLOT_LEGS);
		_slots.put("feet", L2Item.SLOT_FEET);
		_slots.put("gloves", L2Item.SLOT_GLOVES);
		_slots.put("chest,legs", L2Item.SLOT_CHEST | L2Item.SLOT_LEGS);
		_slots.put("rhand", L2Item.SLOT_R_HAND);
		_slots.put("lhand", L2Item.SLOT_L_HAND);
		_slots.put("lrhand", L2Item.SLOT_LR_HAND);
		_slots.put("rear;lear", L2Item.SLOT_R_EAR | L2Item.SLOT_L_EAR);
		_slots.put("rfinger;lfinger", L2Item.SLOT_R_FINGER | L2Item.SLOT_L_FINGER);
		_slots.put("none", L2Item.SLOT_NONE);
		_slots.put("wolf", L2Item.SLOT_WOLF); // for wolf
		_slots.put("hatchling", L2Item.SLOT_HATCHLING); // for hatchling
		_slots.put("strider", L2Item.SLOT_STRIDER); // for strider
		_slots.put("babypet", L2Item.SLOT_BABYPET); // for babypet
	}

	/**
	 * Returns instance of ItemTable
	 *
	 * @return ItemTable
	 */
	public static ItemTable getInstance()
	{
		return SingletonHolder._instance;
	}

	/**
	 * Returns a new object Item
	 *
	 * @return
	 */
	public Item newItem()
	{
		return new Item();
	}

	/**
	 * Constructor.
	 */
	protected ItemTable()
	{
		_armors = new FastMap<>();
		_etcItems = new FastMap<>();
		_weapons = new FastMap<>();
		load();
	}

	private void load()
	{
		int highest = 0;

		for (L2Item item : SkillsEngine.getInstance().loadItems())
		{
			if (highest < item.getItemId())
				highest = item.getItemId();

			if (item instanceof L2EtcItem)
				_etcItems.put(item.getItemId(), (L2EtcItem) item);
			else if (item instanceof L2Armor)
				_armors.put(item.getItemId(), (L2Armor) item);
			else
				_weapons.put(item.getItemId(), (L2Weapon) item);
		}
		buildFastLookupTable(highest);
	}

	/**
	 * Builds a variable in which all items are putting in in function of their ID.
	 *
	 * @param size
	 */
	private void buildFastLookupTable(int size)
	{
		// Create a FastLookUp Table called _allTemplates of size : value of the highest item ID
		_log.info("ItemTable: Highest used itemID : " + size);

		_allTemplates = new L2Item[size + 1];

		// Insert armor item in Fast Look Up Table
		for (L2Armor item : _armors.values())
			_allTemplates[item.getItemId()] = item;

		// Insert weapon item in Fast Look Up Table
		for (L2Weapon item : _weapons.values())
			_allTemplates[item.getItemId()] = item;

		// Insert etcItem item in Fast Look Up Table
		for (L2EtcItem item : _etcItems.values())
			_allTemplates[item.getItemId()] = item;
	}

	/**
	 * Returns the item corresponding to the item ID
	 *
	 * @param id
	 *            : int designating the item
	 * @return L2Item
	 */
	public L2Item getTemplate(int id)
	{
		if (id >= _allTemplates.length)
			return null;

		return _allTemplates[id];
	}

	/**
	 * Create the L2ItemInstance corresponding to the Item Identifier and quantitiy add logs the activity.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Create and Init the L2ItemInstance corresponding to the Item Identifier and quantity</li> <li>Add the L2ItemInstance
	 * object to _allObjects of L2world</li> <li>Logs Item creation according to log settings</li><BR>
	 * <BR>
	 *
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param itemId
	 *            : int Item Identifier of the item to be created
	 * @param count
	 *            : int Quantity of items to be created for stackable items
	 * @param actor
	 *            : L2PcInstance Player requesting the item creation
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 * @return L2ItemInstance corresponding to the new item
	 */
	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor, L2Object reference)
	{
		// Create and Init the L2ItemInstance corresponding to the Item Identifier
		L2ItemInstance item = new L2ItemInstance(IdFactory.getInstance().getNextId(), itemId);

		if (process.equalsIgnoreCase("loot"))
		{
			ScheduledFuture<?> itemLootShedule;
			if (reference instanceof L2Attackable && ((L2Attackable) reference).isRaid()) // loot privilege for raids
			{
				L2Attackable raid = (L2Attackable) reference;
				// if in CommandChannel and was killing a World/RaidBoss
				if (raid.getFirstCommandChannelAttacked() != null && !MainConfig.AUTO_LOOT_RAID)
				{
					item.setOwnerId(raid.getFirstCommandChannelAttacked().getChannelLeader().getObjectId());
					itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), 300000);
					item.setItemLootShedule(itemLootShedule);
				}
			}
			else if (!MainConfig.AUTO_LOOT)
			{
				item.setOwnerId(actor.getObjectId());
				itemLootShedule = ThreadPoolManager.getInstance().scheduleGeneral(new ResetOwner(item), 15000);
				item.setItemLootShedule(itemLootShedule);
			}
		}

		_log.debug("ItemTable: Item created  oid:" + item.getObjectId() + " itemid:" + itemId);

		// Add the L2ItemInstance object to _allObjects of L2world
		L2World.getInstance().storeObject(item);

		// Set Item parameters
		if (item.isStackable() && count > 1)
			item.setCount(count);

		if (MainConfig.LOG_ITEMS)
			LoggingUtils.logItem(_logItems, "CREATE: ", process, item, actor.getName(), reference);

		return item;
	}

	public L2ItemInstance createItem(String process, int itemId, int count, L2PcInstance actor)
	{
		return createItem(process, itemId, count, actor, null);
	}

	/**
	 * Returns a dummy (fr = factice) item.<BR>
	 * <BR>
	 * <U><I>Concept :</I></U><BR>
	 * Dummy item is created by setting the ID of the object in the world at null value
	 *
	 * @param itemId
	 *            : int designating the item
	 * @return L2ItemInstance designating the dummy item created
	 */
	public L2ItemInstance createDummyItem(int itemId)
	{
		L2Item item = getTemplate(itemId);
		if (item == null)
			return null;

		L2ItemInstance temp = new L2ItemInstance(0, item);
		return temp;
	}

	/**
	 * Destroys the L2ItemInstance.<BR>
	 * <BR>
	 * <B><U> Actions</U> :</B><BR>
	 * <BR>
	 * <li>Sets L2ItemInstance parameters to be unusable</li> <li>Removes the L2ItemInstance object to _allObjects of L2world</li>
	 * <li>Logs Item delettion according to log settings</li><BR>
	 * <BR>
	 *
	 * @param process
	 *            : String Identifier of process triggering this action
	 * @param item
	 *            : L2ItemInstance The instance of object to delete
	 * @param actor
	 *            : L2PcInstance Player requesting the item destroy
	 * @param reference
	 *            : L2Object Object referencing current action like NPC selling item or previous item in transformation
	 */
	public void destroyItem(String process, L2ItemInstance item, L2PcInstance actor, L2Object reference)
	{
		synchronized (item)
		{
			item.setCount(0);
			item.setOwnerId(0);
			item.setLocation(ItemLocation.VOID);
			item.setLastChange(L2ItemInstance.REMOVED);

			L2World.getInstance().removeObject(item);
			IdFactory.getInstance().releaseId(item.getObjectId());

			if (MainConfig.LOG_ITEMS)
				LoggingUtils.logItem(_logItems, "DELETE: ", process, item, actor.getName(), reference);

			// if it's a pet control item, delete the pet as well
			if (PetDataTable.isPetCollar(item.getItemId()))
			{
				try (Connection con = DatabaseFactory.getConnection())
				{
					PreparedStatement statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id=?");
					statement.setInt(1, item.getObjectId());
					statement.execute();
					statement.close();
				}
				catch (Exception e)
				{
					_log.warn("could not delete pet objectid:", e);
				}
			}
		}
	}

	public void reload()
	{
		_armors.clear();
		_etcItems.clear();
		_weapons.clear();

		load();
	}

	protected static class ResetOwner implements Runnable
	{
		L2ItemInstance _item;

		public ResetOwner(L2ItemInstance item)
		{
			_item = item;
		}

		@Override
		public void run()
		{
			_item.setOwnerId(0);
			_item.setItemLootShedule(null);
		}
	}

	private static class SingletonHolder
	{
		protected static final ItemTable _instance = new ItemTable();
	}
}
