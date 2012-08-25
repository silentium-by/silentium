/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.ServerType;
import silentium.commons.database.DatabaseFactory;
import silentium.commons.database.DatabaseTuning;
import silentium.commons.network.mmocore.SelectorConfig;
import silentium.commons.network.mmocore.SelectorThread;
import silentium.commons.utils.DeadLockDetector;
import silentium.commons.utils.IPv4Filter;
import silentium.commons.utils.Util;
import silentium.gameserver.board.Manager.ForumsBBSManager;
import silentium.gameserver.configs.ConfigEngine;
import silentium.gameserver.configs.EventsConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.crest.CrestCache;
import silentium.gameserver.data.html.HtmCache;
import silentium.gameserver.data.xml.*;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.geo.pathfinding.PathFinding;
import silentium.gameserver.handler.*;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.instancemanager.*;
import silentium.gameserver.model.*;
import silentium.gameserver.model.entity.Hero;
import silentium.gameserver.model.entity.MonsterRace;
import silentium.gameserver.model.entity.TvTManager;
import silentium.gameserver.model.entity.sevensigns.SevenSigns;
import silentium.gameserver.model.entity.sevensigns.SevenSignsFestival;
import silentium.gameserver.model.olympiad.Olympiad;
import silentium.gameserver.model.olympiad.OlympiadGameManager;
import silentium.gameserver.network.L2GameClient;
import silentium.gameserver.network.L2GamePacketHandler;
import silentium.gameserver.scripting.L2ScriptEngineManager;
import silentium.gameserver.tables.*;
import silentium.gameserver.taskmanager.KnownListUpdateTaskManager;
import silentium.gameserver.taskmanager.TaskManager;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;

public class GameServer {
	private static final Logger _log = LoggerFactory.getLogger(GameServer.class.getName());

	private final SelectorThread<L2GameClient> _selectorThread;
	private final L2GamePacketHandler _gamePacketHandler;
	private final DeadLockDetector _deadDetectThread;
	public static GameServer gameServer;
	private final LoginServerThread _loginThread;
	public static final Calendar dateTimeServerStarted = Calendar.getInstance();

	public SelectorThread<L2GameClient> getSelectorThread() {
		return _selectorThread;
	}

	public GameServer() throws Exception {
		gameServer = this;

		IdFactory.getInstance();
		ThreadPoolManager.getInstance();

		new File(MainConfig.DATAPACK_ROOT, "data/crests").mkdirs();

		Util.printSection("World");
		GameTimeController.getInstance();
		L2World.getInstance();
		MapRegionData.getInstance();
		Announcements.getInstance();

		Util.printSection("Skills");
		SkillTable.getInstance();
		SkillTreeData.getInstance();

		Util.printSection("Items");
		ItemTable.getInstance();
		SummonItemsData.getInstance();
		TradeController.getInstance();
		L2Multisell.getInstance();
		RecipeController.getInstance();
		ArmorSetsData.getInstance();
		FishData.getInstance();
		SpellbookData.getInstance();

		Util.printSection("Augments");
		AugmentationData.getInstance();

		Util.printSection("Characters");
		AccessLevelsData.getInstance();
		AdminCommandAccessRightsData.getInstance();
		CharTemplateData.getInstance();
		CharNameTable.getInstance();
		GmListTable.getInstance();
		RaidBossPointsManager.getInstance();

		Util.printSection("Community server");
		if (MainConfig.ENABLE_COMMUNITY_BOARD) // Forums has to be loaded before clan data
			ForumsBBSManager.getInstance().initRoot();
		else
			_log.info("Community server is disabled.");

		Util.printSection("Clans");
		ClanTable.getInstance();
		AuctionManager.getInstance();
		ClanHallManager.getInstance();

		Util.printSection("Geodata");
		GeoData.getInstance();
		if (MainConfig.GEODATA == 2)
			PathFinding.getInstance();

		Util.printSection("Zones");
		ZoneManager.getInstance();

		Util.printSection("World Bosses");
		_log.info("GrandBossManager: Loaded " + GrandBossManager.getInstance().size() + " GrandBosses instances.");
		GrandBossManager.getInstance().initZones();

		Util.printSection("Castles");
		CastleManager.getInstance().load();

		Util.printSection("Seven Signs");
		SevenSigns.getInstance().spawnSevenSignsNPC();
		SevenSignsFestival.getInstance();

		Util.printSection("Sieges");
		SiegeManager.getSieges();
		MercTicketManager.getInstance();

		Util.printSection("Manor Manager");
		CastleManorManager.getInstance();
		L2Manor.getInstance();

		Util.printSection("NPCs");
		HerbDropData.getInstance();
		PetDataTable.getInstance();
		NpcTable.getInstance();
		NpcWalkerRoutesData.getInstance();
		DoorData.getInstance();
		StaticObjectsData.load();
		SpawnTable.getInstance();
		RaidBossSpawnManager.getInstance();
		DayNightSpawnManager.getInstance().trim().notifyChangeMode();
		DimensionalRiftManager.getInstance();

		Util.printSection("Olympiads & Heroes");
		OlympiadGameManager.getInstance();
		Olympiad.getInstance();
		Hero.getInstance();

		Util.printSection("Four Sepulchers");
		FourSepulchersManager.getInstance().init();

		Util.printSection("Cache");
		HtmCache.getInstance();
		CrestCache.load();
		TeleportLocationData.getInstance();
		PartyMatchWaitingList.getInstance();
		PartyMatchRoomList.getInstance();
		PetitionManager.getInstance();
		HennaData.getInstance();
		HelperBuffData.getInstance();
		CursedWeaponsManager.getInstance();

		Util.printSection("Quests & Scripts");
		QuestManager.getInstance();
		BoatManager.getInstance();

		L2ScriptEngineManager.getInstance().initializeScripts();

		QuestManager.getInstance().report();

		if (MainConfig.SAVE_DROPPED_ITEM)
			ItemsOnGroundManager.getInstance();

		if (MainConfig.AUTODESTROY_ITEM_AFTER > 0 || MainConfig.HERB_AUTO_DESTROY_TIME > 0)
			ItemsAutoDestroy.getInstance();

		MonsterRace.getInstance();

		Util.printSection("Handlers");
		_log.info("AutoSpawnHandler: Loaded " + AutoSpawnHandler.getInstance().size() + " handlers.");
		_log.info("AutoChatHandler: Loaded " + AutoChatHandler.getInstance().size() + " handlers.");
		_log.info("AdminCommandHandler: Loaded " + AdminCommandHandler.getInstance().size() + " handlers.");
		_log.info("ChatHandler: Loaded " + ChatHandler.getInstance().size() + " handlers.");
		_log.info("ItemHandler: Loaded " + ItemHandler.getInstance().size() + " handlers.");
		_log.info("SkillHandler: Loaded " + SkillHandler.getInstance().size() + " handlers.");
		_log.info("UserCommandHandler: Loaded " + UserCommandHandler.getInstance().size() + " handlers.");
		_log.info("VoicedCommandHandler: Loaded " + VoicedCommandHandler.getInstance().size() + " handlers.");

		if (EventsConfig.ALLOW_WEDDING)
			CoupleManager.getInstance();

		Util.printSection("System");
		TaskManager.getInstance();

		Runtime.getRuntime().addShutdownHook(Shutdown.getInstance());
		ForumsBBSManager.getInstance();
		_log.info("IdFactory: Free ObjectIDs remaining: " + IdFactory.getInstance().size());

		TvTManager.getInstance();
		KnownListUpdateTaskManager.getInstance();
		MovieMakerManager.getInstance();

		if (MainConfig.DEADLOCK_DETECTOR) {
			_log.info("Deadlock detector is enabled. Timer: " + MainConfig.DEADLOCK_CHECK_INTERVAL + "s.");
			_deadDetectThread = new DeadLockDetector(MainConfig.DEADLOCK_CHECK_INTERVAL);
			_deadDetectThread.setDaemon(true);
			_deadDetectThread.start();
		} else {
			_log.info("Deadlock detector is disabled.");
			_deadDetectThread = null;
		}

		System.gc();

		_log.info("Gameserver have started.");
		_log.info("Maximum allowed players: " + MainConfig.MAXIMUM_ONLINE_USERS);

		Util.printSection("Login");
		_loginThread = LoginServerThread.getInstance();
		_loginThread.start();

		final SelectorConfig sc = new SelectorConfig();
		sc.MAX_READ_PER_PASS = MainConfig.MMO_MAX_READ_PER_PASS;
		sc.MAX_SEND_PER_PASS = MainConfig.MMO_MAX_SEND_PER_PASS;
		sc.SLEEP_TIME = MainConfig.MMO_SELECTOR_SLEEP_TIME;
		sc.HELPER_BUFFER_COUNT = MainConfig.MMO_HELPER_BUFFER_COUNT;

		_gamePacketHandler = new L2GamePacketHandler();
		_selectorThread = new SelectorThread<>(sc, _gamePacketHandler, _gamePacketHandler, _gamePacketHandler, new IPv4Filter());

		InetAddress bindAddress = null;
		if (!MainConfig.GAMESERVER_HOSTNAME.equals("*")) {
			try {
				bindAddress = InetAddress.getByName(MainConfig.GAMESERVER_HOSTNAME);
			} catch (UnknownHostException e1) {
				_log.error("WARNING: The GameServer bind address is invalid, using all available IPs. Reason: " + e1.getMessage(), e1);
			}
		}

		try {
			_selectorThread.openServerSocket(bindAddress, MainConfig.PORT_GAME);
		} catch (IOException e) {
			_log.error("FATAL: Failed to open server socket. Reason: " + e.getMessage(), e);
			System.exit(1);
		}
		_selectorThread.start();
	}

	public static void main(String[] args) throws Exception {
		ServerType.SERVER_TYPE = ServerType.GAMESERVER;

		// Create log folder
		final File logFolder = new File("./log");
		logFolder.mkdir();

		Util.printSection("Main");
		_log.info("Developers: Silentium");
		_log.info("https://silentium.by");


		Util.printSection("Configuration");
		ConfigEngine.init();

		// Factories
		XMLDocumentFactory.getInstance();

		Util.printSection("Database");
		DatabaseFactory.init();
		DatabaseTuning.start();

		gameServer = new GameServer();
	}
}
