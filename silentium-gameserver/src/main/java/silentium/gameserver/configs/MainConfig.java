/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class MainConfig extends ConfigEngine
{
	public static String LOGIN_BIND_ADDRESS;
	public static int REQUEST_ID;
	public static boolean ACCEPT_ALTERNATE_ID;
	public static String GAMESERVER_HOSTNAME;
	public static int PORT_GAME;
	public static String EXTERNAL_HOSTNAME;
	public static String INTERNAL_HOSTNAME;
	public static int GAME_SERVER_LOGIN_PORT;
	public static String GAME_SERVER_LOGIN_HOST;
	public static File DATAPACK_ROOT;

	public static enum IdFactoryType
	{
		BitSet, Stack
	}

	public static IdFactoryType IDFACTORY_TYPE;
	public static boolean BAD_ID_CHECKING;
	public static boolean SERVER_LIST_BRACKET;
	public static boolean SERVER_LIST_CLOCK;
	public static boolean SERVER_LIST_TESTSERVER;
	public static boolean SERVER_GMONLY;
	public static boolean TEST_SERVER;
	public static int DELETE_DAYS;
	public static int MAXIMUM_ONLINE_USERS;
	public static int MIN_PROTOCOL_REVISION;
	public static int MAX_PROTOCOL_REVISION;
	public static boolean JAIL_IS_PVP;
	public static int DEFAULT_PUNISH;
	public static int DEFAULT_PUNISH_PARAM;
	public static boolean AUTO_LOOT;
	public static boolean AUTO_LOOT_HERBS;
	public static boolean AUTO_LOOT_RAID;
	public static boolean LAZY_ITEMS_UPDATE;
	public static boolean ALLOW_DISCARDITEM;
	public static boolean MULTIPLE_ITEM_DROP;
	public static int AUTODESTROY_ITEM_AFTER;
	public static int HERB_AUTO_DESTROY_TIME;
	public static String PROTECTED_ITEMS;
	public static List<Integer> LIST_PROTECTED_ITEMS;
	public static boolean DESTROY_DROPPED_PLAYER_ITEM;
	public static boolean DESTROY_EQUIPABLE_PLAYER_ITEM;
	public static boolean SAVE_DROPPED_ITEM;
	public static boolean EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD;
	public static int SAVE_DROPPED_ITEM_INTERVAL;
	public static boolean CLEAR_DROPPED_ITEM_TABLE;
	public static float RATE_XP;
	public static float RATE_SP;
	public static float RATE_PARTY_XP;
	public static float RATE_PARTY_SP;
	public static float RATE_DROP_ADENA;
	public static float RATE_CONSUMABLE_COST;
	public static float RATE_DROP_ITEMS;
	public static float RATE_DROP_ITEMS_BY_RAID;
	public static float RATE_DROP_SPOIL;
	public static int RATE_DROP_MANOR;
	public static float RATE_QUEST_DROP;
	public static float RATE_QUEST_REWARD;
	public static float RATE_QUEST_REWARD_XP;
	public static float RATE_QUEST_REWARD_SP;
	public static float RATE_QUEST_REWARD_ADENA;
	public static float RATE_KARMA_EXP_LOST;
	public static float RATE_SIEGE_GUARDS_PRICE;
	public static int PLAYER_DROP_LIMIT;
	public static int PLAYER_RATE_DROP;
	public static int PLAYER_RATE_DROP_ITEM;
	public static int PLAYER_RATE_DROP_EQUIP;
	public static int PLAYER_RATE_DROP_EQUIP_WEAPON;
	public static int KARMA_DROP_LIMIT;
	public static int KARMA_RATE_DROP;
	public static int KARMA_RATE_DROP_ITEM;
	public static int KARMA_RATE_DROP_EQUIP;
	public static int KARMA_RATE_DROP_EQUIP_WEAPON;
	public static float PET_XP_RATE;
	public static int PET_FOOD_RATE;
	public static float SINEATER_XP_RATE;
	public static float RATE_DROP_COMMON_HERBS;
	public static float RATE_DROP_HP_HERBS;
	public static float RATE_DROP_MP_HERBS;
	public static float RATE_DROP_SPECIAL_HERBS;
	public static boolean ALLOW_FREIGHT;
	public static boolean ALLOW_WAREHOUSE;
	public static boolean ALLOW_WEAR;
	public static int WEAR_DELAY;
	public static int WEAR_PRICE;
	public static boolean ALLOW_LOTTERY;
	public static boolean ALLOW_RACE;
	public static boolean ALLOW_WATER;
	public static boolean ALLOWFISHING;
	public static boolean ALLOW_BOAT;
	public static boolean ALLOW_CURSED_WEAPONS;
	public static boolean ALLOW_MANOR;
	public static boolean ENABLE_FALLING_DAMAGE;
	public static boolean ALT_DEV_NO_QUESTS;
	public static boolean ALT_DEV_NO_SPAWNS;
	public static boolean DEVELOPER;
	public static boolean PACKET_HANDLER_DEBUG;
	public static boolean DEADLOCK_DETECTOR;
	public static long DEADLOCK_CHECK_INTERVAL;
	public static boolean RESTART_ON_DEADLOCK;
	public static boolean LOG_CHAT;
	public static boolean LOG_ITEMS;
	public static boolean GMAUDIT;
	public static boolean ENABLE_COMMUNITY_BOARD;
	public static String BBS_DEFAULT;
	public static int COORD_SYNCHRONIZE;
	public static int GEODATA;
	public static boolean FORCE_GEODATA;
	public static boolean GEODATA_CELLFINDING;
	public static String PATHFIND_BUFFERS;
	public static float LOW_WEIGHT;
	public static float MEDIUM_WEIGHT;
	public static float HIGH_WEIGHT;
	public static boolean ADVANCED_DIAGONAL_STRATEGY;
	public static float DIAGONAL_WEIGHT;
	public static int MAX_POSTFILTER_PASSES;
	public static boolean DEBUG_PATH;
	public static boolean L2WALKER_PROTECTION;
	public static boolean FORCE_INVENTORY_UPDATE;
	public static boolean AUTODELETE_INVALID_QUEST_DATA;
	public static boolean GAMEGUARD_ENFORCE;
	public static boolean SERVER_NEWS;
	public static int ZONE_TOWN;
	public static boolean DISABLE_TUTORIAL;
	public static int THREAD_P_EFFECTS = 6; // default 6
	public static int THREAD_P_GENERAL = 15; // default 15
	public static int GENERAL_PACKET_THREAD_CORE_SIZE = 4; // default 4
	public static int IO_PACKET_THREAD_CORE_SIZE = 2; // default 2
	public static int GENERAL_THREAD_CORE_SIZE = 4; // default 4
	public static int AI_MAX_THREAD = 10; // default 10
	public static boolean COUNT_PACKETS = false; // default false
	public static boolean DUMP_PACKET_COUNTS = false; // default false
	public static int DUMP_INTERVAL_SECONDS = 60; // default 60
	public static int MINIMUM_UPDATE_DISTANCE = 50; // default 50
	public static int MINIMUN_UPDATE_TIME = 500; // default 500
	public static int KNOWNLIST_FORGET_DELAY = 10000; // default 10000
	public static int PACKET_LIFETIME = 0; // default 0 (unlimited)
	public static boolean RESERVE_HOST_ON_LOGIN = false; // default false
	public static int MMO_SELECTOR_SLEEP_TIME = 20; // default 20
	public static int MMO_MAX_SEND_PER_PASS = 12; // default 12
	public static int MMO_MAX_READ_PER_PASS = 12; // default 12
	public static int MMO_HELPER_BUFFER_COUNT = 20; // default 20
	public static int CLIENT_PACKET_QUEUE_SIZE = 14; // default MMO_MAX_READ_PER_PASS + 2
	public static int CLIENT_PACKET_QUEUE_MAX_BURST_SIZE = 13; // default MMO_MAX_READ_PER_PASS + 1
	public static int CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = 80; // default 80
	public static int CLIENT_PACKET_QUEUE_MEASURE_INTERVAL = 5; // default 5
	public static int CLIENT_PACKET_QUEUE_MAX_AVERAGE_PACKETS_PER_SECOND = 40; // default 40
	public static int CLIENT_PACKET_QUEUE_MAX_FLOODS_PER_MIN = 2; // default 2
	public static int CLIENT_PACKET_QUEUE_MAX_OVERFLOWS_PER_MIN = 1; // default 1
	public static int CLIENT_PACKET_QUEUE_MAX_UNDERFLOWS_PER_MIN = 1; // default 1
	public static int CLIENT_PACKET_QUEUE_MAX_UNKNOWN_PER_MIN = 5; // default 5

	public static void load()
	{
		try (InputStream is = new FileInputStream(new File(SERVER_FILE)))
		{
			Properties server = new Properties();
			server.load(is);
			is.close();

			GAMESERVER_HOSTNAME = server.getProperty("GameserverHostname");
			PORT_GAME = Integer.parseInt(server.getProperty("GameserverPort", "7777"));

			EXTERNAL_HOSTNAME = server.getProperty("ExternalHostname", "*");
			INTERNAL_HOSTNAME = server.getProperty("InternalHostname", "*");

			GAME_SERVER_LOGIN_PORT = Integer.parseInt(server.getProperty("LoginPort", "9014"));
			GAME_SERVER_LOGIN_HOST = server.getProperty("LoginHost", "127.0.0.1");

			REQUEST_ID = Integer.parseInt(server.getProperty("RequestServerID", "0"));
			ACCEPT_ALTERNATE_ID = Boolean.parseBoolean(server.getProperty("AcceptAlternateID", "True"));

			DATAPACK_ROOT = new File(server.getProperty("DatapackRoot", ".")).getCanonicalFile();

			IDFACTORY_TYPE = IdFactoryType.valueOf(server.getProperty("IDFactory", "BitSet"));
			BAD_ID_CHECKING = Boolean.parseBoolean(server.getProperty("BadIdChecking", "True"));

			SERVER_LIST_BRACKET = Boolean.parseBoolean(server.getProperty("ServerListBrackets", "false"));
			SERVER_LIST_CLOCK = Boolean.parseBoolean(server.getProperty("ServerListClock", "false"));
			SERVER_GMONLY = Boolean.parseBoolean(server.getProperty("ServerGMOnly", "false"));
			TEST_SERVER = Boolean.parseBoolean(server.getProperty("TestServer", "false"));
			SERVER_LIST_TESTSERVER = Boolean.parseBoolean(server.getProperty("TestServer", "false"));

			DELETE_DAYS = Integer.parseInt(server.getProperty("DeleteCharAfterDays", "7"));
			MAXIMUM_ONLINE_USERS = Integer.parseInt(server.getProperty("MaximumOnlineUsers", "100"));
			MIN_PROTOCOL_REVISION = Integer.parseInt(server.getProperty("MinProtocolRevision", "730"));
			MAX_PROTOCOL_REVISION = Integer.parseInt(server.getProperty("MaxProtocolRevision", "746"));
			if (MIN_PROTOCOL_REVISION > MAX_PROTOCOL_REVISION)
				throw new Error("MinProtocolRevision is bigger than MaxProtocolRevision in server.properties.");

			JAIL_IS_PVP = Boolean.parseBoolean(server.getProperty("JailIsPvp", "True"));
			DEFAULT_PUNISH = Integer.parseInt(server.getProperty("DefaultPunish", "2"));
			DEFAULT_PUNISH_PARAM = Integer.parseInt(server.getProperty("DefaultPunishParam", "0"));

			AUTO_LOOT = Boolean.parseBoolean(server.getProperty("AutoLoot", "False"));
			AUTO_LOOT_HERBS = Boolean.parseBoolean(server.getProperty("AutoLootHerbs", "False"));
			AUTO_LOOT_RAID = Boolean.parseBoolean(server.getProperty("AutoLootRaid", "False"));

			LAZY_ITEMS_UPDATE = Boolean.parseBoolean(server.getProperty("LazyItemsUpdate", "False"));
			ALLOW_DISCARDITEM = Boolean.parseBoolean(server.getProperty("AllowDiscardItem", "True"));
			MULTIPLE_ITEM_DROP = Boolean.parseBoolean(server.getProperty("MultipleItemDrop", "True"));
			AUTODESTROY_ITEM_AFTER = Integer.parseInt(server.getProperty("AutoDestroyDroppedItemAfter", "0"));
			HERB_AUTO_DESTROY_TIME = Integer.parseInt(server.getProperty("AutoDestroyHerbTime", "15")) * 1000;
			PROTECTED_ITEMS = server.getProperty("ListOfProtectedItems");

			LIST_PROTECTED_ITEMS = new ArrayList<>();
			for (String id : PROTECTED_ITEMS.split(","))
				LIST_PROTECTED_ITEMS.add(Integer.parseInt(id));

			DESTROY_DROPPED_PLAYER_ITEM = Boolean.parseBoolean(server.getProperty("DestroyPlayerDroppedItem", "False"));
			DESTROY_EQUIPABLE_PLAYER_ITEM = Boolean.parseBoolean(server.getProperty("DestroyEquipableItem", "False"));
			SAVE_DROPPED_ITEM = Boolean.parseBoolean(server.getProperty("SaveDroppedItem", "False"));
			EMPTY_DROPPED_ITEM_TABLE_AFTER_LOAD = Boolean.parseBoolean(server.getProperty("EmptyDroppedItemTableAfterLoad", "False"));
			SAVE_DROPPED_ITEM_INTERVAL = Integer.parseInt(server.getProperty("SaveDroppedItemInterval", "0")) * 60000;
			CLEAR_DROPPED_ITEM_TABLE = Boolean.parseBoolean(server.getProperty("ClearDroppedItemTable", "False"));

			RATE_XP = Float.parseFloat(server.getProperty("RateXp", "1."));
			RATE_SP = Float.parseFloat(server.getProperty("RateSp", "1."));
			RATE_PARTY_XP = Float.parseFloat(server.getProperty("RatePartyXp", "1."));
			RATE_PARTY_SP = Float.parseFloat(server.getProperty("RatePartySp", "1."));
			RATE_DROP_ADENA = Float.parseFloat(server.getProperty("RateDropAdena", "1."));
			RATE_CONSUMABLE_COST = Float.parseFloat(server.getProperty("RateConsumableCost", "1."));
			RATE_DROP_ITEMS = Float.parseFloat(server.getProperty("RateDropItems", "1."));
			RATE_DROP_ITEMS_BY_RAID = Float.parseFloat(server.getProperty("RateRaidDropItems", "1."));
			RATE_DROP_SPOIL = Float.parseFloat(server.getProperty("RateDropSpoil", "1."));
			RATE_DROP_MANOR = Integer.parseInt(server.getProperty("RateDropManor", "1"));
			RATE_QUEST_DROP = Float.parseFloat(server.getProperty("RateQuestDrop", "1."));
			RATE_QUEST_REWARD = Float.parseFloat(server.getProperty("RateQuestReward", "1."));
			RATE_QUEST_REWARD_XP = Float.parseFloat(server.getProperty("RateQuestRewardXP", "1."));
			RATE_QUEST_REWARD_SP = Float.parseFloat(server.getProperty("RateQuestRewardSP", "1."));
			RATE_QUEST_REWARD_ADENA = Float.parseFloat(server.getProperty("RateQuestRewardAdena", "1."));
			RATE_KARMA_EXP_LOST = Float.parseFloat(server.getProperty("RateKarmaExpLost", "1."));
			RATE_SIEGE_GUARDS_PRICE = Float.parseFloat(server.getProperty("RateSiegeGuardsPrice", "1."));
			RATE_DROP_COMMON_HERBS = Float.parseFloat(server.getProperty("RateCommonHerbs", "1."));
			RATE_DROP_HP_HERBS = Float.parseFloat(server.getProperty("RateHpHerbs", "1."));
			RATE_DROP_MP_HERBS = Float.parseFloat(server.getProperty("RateMpHerbs", "1."));
			RATE_DROP_SPECIAL_HERBS = Float.parseFloat(server.getProperty("RateSpecialHerbs", "1."));
			PLAYER_DROP_LIMIT = Integer.parseInt(server.getProperty("PlayerDropLimit", "3"));
			PLAYER_RATE_DROP = Integer.parseInt(server.getProperty("PlayerRateDrop", "5"));
			PLAYER_RATE_DROP_ITEM = Integer.parseInt(server.getProperty("PlayerRateDropItem", "70"));
			PLAYER_RATE_DROP_EQUIP = Integer.parseInt(server.getProperty("PlayerRateDropEquip", "25"));
			PLAYER_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(server.getProperty("PlayerRateDropEquipWeapon", "5"));
			PET_XP_RATE = Float.parseFloat(server.getProperty("PetXpRate", "1."));
			PET_FOOD_RATE = Integer.parseInt(server.getProperty("PetFoodRate", "1"));
			SINEATER_XP_RATE = Float.parseFloat(server.getProperty("SinEaterXpRate", "1."));
			KARMA_DROP_LIMIT = Integer.parseInt(server.getProperty("KarmaDropLimit", "10"));
			KARMA_RATE_DROP = Integer.parseInt(server.getProperty("KarmaRateDrop", "70"));
			KARMA_RATE_DROP_ITEM = Integer.parseInt(server.getProperty("KarmaRateDropItem", "50"));
			KARMA_RATE_DROP_EQUIP = Integer.parseInt(server.getProperty("KarmaRateDropEquip", "40"));
			KARMA_RATE_DROP_EQUIP_WEAPON = Integer.parseInt(server.getProperty("KarmaRateDropEquipWeapon", "10"));

			ALLOW_FREIGHT = Boolean.parseBoolean(server.getProperty("AllowFreight", "True"));
			ALLOW_WAREHOUSE = Boolean.parseBoolean(server.getProperty("AllowWarehouse", "True"));
			ALLOW_WEAR = Boolean.parseBoolean(server.getProperty("AllowWear", "True"));
			WEAR_DELAY = Integer.parseInt(server.getProperty("WearDelay", "5"));
			WEAR_PRICE = Integer.parseInt(server.getProperty("WearPrice", "10"));
			ALLOW_LOTTERY = Boolean.parseBoolean(server.getProperty("AllowLottery", "True"));
			ALLOW_RACE = Boolean.parseBoolean(server.getProperty("AllowRace", "True"));
			ALLOW_WATER = Boolean.parseBoolean(server.getProperty("AllowWater", "True"));
			ALLOWFISHING = Boolean.parseBoolean(server.getProperty("AllowFishing", "False"));
			ALLOW_MANOR = Boolean.parseBoolean(server.getProperty("AllowManor", "True"));
			ALLOW_BOAT = Boolean.parseBoolean(server.getProperty("AllowBoat", "True"));
			ALLOW_CURSED_WEAPONS = Boolean.parseBoolean(server.getProperty("AllowCursedWeapons", "True"));

			String str = server.getProperty("EnableFallingDamage", "auto");
			ENABLE_FALLING_DAMAGE = "auto".equalsIgnoreCase(str) ? GEODATA > 0 : Boolean.parseBoolean(str);

			ALT_DEV_NO_QUESTS = Boolean.parseBoolean(server.getProperty("NoQuests", "False"));
			ALT_DEV_NO_SPAWNS = Boolean.parseBoolean(server.getProperty("NoSpawns", "False"));
			DEVELOPER = Boolean.parseBoolean(server.getProperty("Developer", "False"));
			PACKET_HANDLER_DEBUG = Boolean.parseBoolean(server.getProperty("PacketHandlerDebug", "False"));

			DEADLOCK_DETECTOR = Boolean.parseBoolean(server.getProperty("DeadLockDetector", "False"));
			DEADLOCK_CHECK_INTERVAL = Long.parseLong(server.getProperty("DeadLockCheckInterval", "20"));
			RESTART_ON_DEADLOCK = Boolean.parseBoolean(server.getProperty("RestartOnDeadlock", "False"));

			LOG_CHAT = Boolean.parseBoolean(server.getProperty("LogChat", "false"));
			LOG_ITEMS = Boolean.parseBoolean(server.getProperty("LogItems", "false"));
			GMAUDIT = Boolean.parseBoolean(server.getProperty("GMAudit", "False"));

			ENABLE_COMMUNITY_BOARD = Boolean.parseBoolean(server.getProperty("EnableCommunityBoard", "False"));
			BBS_DEFAULT = server.getProperty("BBSDefault", "_bbshome");

			COORD_SYNCHRONIZE = Integer.parseInt(server.getProperty("CoordSynchronize", "-1"));
			GEODATA = Integer.parseInt(server.getProperty("GeoData", "0"));
			FORCE_GEODATA = Boolean.parseBoolean(server.getProperty("ForceGeoData", "True"));

			GEODATA_CELLFINDING = Boolean.parseBoolean(server.getProperty("CellPathFinding", "False"));
			PATHFIND_BUFFERS = server.getProperty("PathFindBuffers", "100x6;128x6;192x6;256x4;320x4;384x4;500x2");
			LOW_WEIGHT = Float.parseFloat(server.getProperty("LowWeight", "0.5"));
			MEDIUM_WEIGHT = Float.parseFloat(server.getProperty("MediumWeight", "2"));
			HIGH_WEIGHT = Float.parseFloat(server.getProperty("HighWeight", "3"));
			ADVANCED_DIAGONAL_STRATEGY = Boolean.parseBoolean(server.getProperty("AdvancedDiagonalStrategy", "True"));
			DIAGONAL_WEIGHT = Float.parseFloat(server.getProperty("DiagonalWeight", "0.707"));
			MAX_POSTFILTER_PASSES = Integer.parseInt(server.getProperty("MaxPostfilterPasses", "3"));
			DEBUG_PATH = Boolean.parseBoolean(server.getProperty("DebugPath", "False"));

			L2WALKER_PROTECTION = Boolean.parseBoolean(server.getProperty("L2WalkerProtection", "False"));
			FORCE_INVENTORY_UPDATE = Boolean.parseBoolean(server.getProperty("ForceInventoryUpdate", "False"));
			AUTODELETE_INVALID_QUEST_DATA = Boolean.parseBoolean(server.getProperty("AutoDeleteInvalidQuestData", "False"));
			GAMEGUARD_ENFORCE = Boolean.parseBoolean(server.getProperty("GameGuardEnforce", "False"));
			ZONE_TOWN = Integer.parseInt(server.getProperty("ZoneTown", "0"));
			SERVER_NEWS = Boolean.parseBoolean(server.getProperty("ShowServerNews", "False"));
			DISABLE_TUTORIAL = Boolean.parseBoolean(server.getProperty("DisableTutorial", "False"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + SERVER_FILE + " file.");
		}
	}
}