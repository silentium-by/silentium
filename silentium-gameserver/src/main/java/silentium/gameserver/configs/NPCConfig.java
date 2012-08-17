/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.configs;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.StringTokenizer;

public final class NPCConfig extends ConfigEngine
{
	public static boolean CHAMPION_ENABLE;
	public static int CHAMPION_FREQUENCY;
	public static int CHAMP_MIN_LVL;
	public static int CHAMP_MAX_LVL;
	public static int CHAMPION_HP;
	public static int CHAMPION_REWARDS;
	public static int CHAMPION_ADENAS_REWARDS;
	public static float CHAMPION_HP_REGEN;
	public static float CHAMPION_ATK;
	public static float CHAMPION_SPD_ATK;
	public static int CHAMPION_REWARD;
	public static int CHAMPION_REWARD_ID;
	public static int CHAMPION_REWARD_QTY;

	public static boolean ALLOW_CLASS_MASTERS;
	public static ClassMasterSettings CLASS_MASTER_SETTINGS;
	public static boolean ALLOW_ENTIRE_TREE;
	public static boolean ANNOUNCE_MAMMON_SPAWN;
	public static boolean ALT_GAME_MOB_ATTACK_AI;
	public static boolean ALT_MOB_AGRO_IN_PEACEZONE;
	public static boolean ALT_GAME_FREE_TELEPORT;
	public static boolean SHOW_NPC_LVL;
	public static boolean SHOW_NPC_CREST;
	public static boolean SHOW_SUMMON_CREST;

	public static boolean WYVERN_ALLOW_UPGRADER;
	public static int WYVERN_REQUIRED_LEVEL;
	public static int WYVERN_REQUIRED_CRYSTALS;

	public static double RAID_HP_REGEN_MULTIPLIER;
	public static double RAID_MP_REGEN_MULTIPLIER;
	public static double RAID_DEFENCE_MULTIPLIER;
	public static float RAID_MIN_RESPAWN_MULTIPLIER;
	public static float RAID_MAX_RESPAWN_MULTIPLIER;
	public static double RAID_MINION_RESPAWN_TIMER;
	public static boolean RAID_DISABLE_CURSE;
	public static int RAID_CHAOS_TIME;
	public static int GRAND_CHAOS_TIME;
	public static int MINION_CHAOS_TIME;

	public static int SPAWN_INTERVAL_AQ;
	public static int RANDOM_SPAWN_TIME_AQ;
	public static int SPAWN_INTERVAL_BAIUM;
	public static int RANDOM_SPAWN_TIME_BAIUM;
	public static int SPAWN_INTERVAL_CORE;
	public static int RANDOM_SPAWN_TIME_CORE;
	public static int SPAWN_INTERVAL_FRINTEZZA;
	public static int RANDOM_SPAWN_TIME_FRINTEZZA;
	public static int SPAWN_INTERVAL_ORFEN;
	public static int RANDOM_SPAWN_TIME_ORFEN;
	public static int SPAWN_INTERVAL_ZAKEN;
	public static int RANDOM_SPAWN_TIME_ZAKEN;
	public static int SPAWN_INTERVAL_ANTHARAS;
	public static int RANDOM_SPAWN_TIME_ANTHARAS;
	public static int WAIT_TIME_ANTHARAS;
	public static int SPAWN_INTERVAL_VALAKAS;
	public static int RANDOM_SPAWN_TIME_VALAKAS;
	public static int WAIT_TIME_VALAKAS;

	public static boolean GUARD_ATTACK_AGGRO_MOB;
	public static int MAX_DRIFT_RANGE;
	public static boolean MOVE_BASED_KNOWNLIST;
	public static long KNOWNLIST_UPDATE_INTERVAL;
	public static int MIN_NPC_ANIMATION;
	public static int MAX_NPC_ANIMATION;
	public static int MIN_MONSTER_ANIMATION;
	public static int MAX_MONSTER_ANIMATION;

	public static boolean GRIDS_ALWAYS_ON;
	public static int GRID_NEIGHBOR_TURNON_TIME;
	public static int GRID_NEIGHBOR_TURNOFF_TIME;

	public static void load()
	{
		try (InputStream is = new FileInputStream(new File(NPCS_FILE)))
		{
			Properties npcs = new Properties();
			npcs.load(is);
			is.close();

			CHAMPION_ENABLE = Boolean.parseBoolean(npcs.getProperty("ChampionEnable", "False"));
			CHAMPION_FREQUENCY = Integer.parseInt(npcs.getProperty("ChampionFrequency", "0"));
			CHAMP_MIN_LVL = Integer.parseInt(npcs.getProperty("ChampionMinLevel", "20"));
			CHAMP_MAX_LVL = Integer.parseInt(npcs.getProperty("ChampionMaxLevel", "60"));
			CHAMPION_HP = Integer.parseInt(npcs.getProperty("ChampionHp", "7"));
			CHAMPION_HP_REGEN = Float.parseFloat(npcs.getProperty("ChampionHpRegen", "1."));
			CHAMPION_REWARDS = Integer.parseInt(npcs.getProperty("ChampionRewards", "8"));
			CHAMPION_ADENAS_REWARDS = Integer.parseInt(npcs.getProperty("ChampionAdenasRewards", "1"));
			CHAMPION_ATK = Float.parseFloat(npcs.getProperty("ChampionAtk", "1."));
			CHAMPION_SPD_ATK = Float.parseFloat(npcs.getProperty("ChampionSpdAtk", "1."));
			CHAMPION_REWARD = Integer.parseInt(npcs.getProperty("ChampionRewardItem", "0"));
			CHAMPION_REWARD_ID = Integer.parseInt(npcs.getProperty("ChampionRewardItemID", "6393"));
			CHAMPION_REWARD_QTY = Integer.parseInt(npcs.getProperty("ChampionRewardItemQty", "1"));

			ALLOW_CLASS_MASTERS = Boolean.parseBoolean(npcs.getProperty("AllowClassMasters", "False"));
			ALLOW_ENTIRE_TREE = Boolean.parseBoolean(npcs.getProperty("AllowEntireTree", "False"));
			if (ALLOW_CLASS_MASTERS)
				CLASS_MASTER_SETTINGS = new ClassMasterSettings(npcs.getProperty("ConfigClassMaster"));

			ALT_GAME_FREE_TELEPORT = Boolean.parseBoolean(npcs.getProperty("AltFreeTeleporting", "False"));
			ANNOUNCE_MAMMON_SPAWN = Boolean.parseBoolean(npcs.getProperty("AnnounceMammonSpawn", "True"));
			ALT_GAME_MOB_ATTACK_AI = Boolean.parseBoolean(npcs.getProperty("AltGameMobAttackAI", "False"));
			ALT_MOB_AGRO_IN_PEACEZONE = Boolean.parseBoolean(npcs.getProperty("AltMobAgroInPeaceZone", "True"));
			SHOW_NPC_LVL = Boolean.parseBoolean(npcs.getProperty("ShowNpcLevel", "False"));
			SHOW_NPC_CREST = Boolean.parseBoolean(npcs.getProperty("ShowNpcCrest", "False"));
			SHOW_SUMMON_CREST = Boolean.parseBoolean(npcs.getProperty("ShowSummonCrest", "False"));

			WYVERN_ALLOW_UPGRADER = Boolean.parseBoolean(npcs.getProperty("AllowWyvernUpgrader", "True"));
			WYVERN_REQUIRED_LEVEL = Integer.parseInt(npcs.getProperty("RequiredStriderLevel", "55"));
			if (WYVERN_REQUIRED_LEVEL > 80 && WYVERN_REQUIRED_LEVEL < 1) // Sanity check
				WYVERN_REQUIRED_LEVEL = 55;
			WYVERN_REQUIRED_CRYSTALS = Integer.parseInt(npcs.getProperty("RequiredCrystalsNumber", "10"));

			RAID_HP_REGEN_MULTIPLIER = Double.parseDouble(npcs.getProperty("RaidHpRegenMultiplier", "100")) / 100;
			RAID_MP_REGEN_MULTIPLIER = Double.parseDouble(npcs.getProperty("RaidMpRegenMultiplier", "100")) / 100;
			RAID_DEFENCE_MULTIPLIER = Double.parseDouble(npcs.getProperty("RaidDefenceMultiplier", "100")) / 100;
			RAID_MINION_RESPAWN_TIMER = Integer.parseInt(npcs.getProperty("RaidMinionRespawnTime", "300000"));
			RAID_MIN_RESPAWN_MULTIPLIER = Float.parseFloat(npcs.getProperty("RaidMinRespawnMultiplier", "1.0"));
			RAID_MAX_RESPAWN_MULTIPLIER = Float.parseFloat(npcs.getProperty("RaidMaxRespawnMultiplier", "1.0"));

			RAID_DISABLE_CURSE = Boolean.parseBoolean(npcs.getProperty("DisableRaidCurse", "False"));
			RAID_CHAOS_TIME = Integer.parseInt(npcs.getProperty("RaidChaosTime", "10"));
			GRAND_CHAOS_TIME = Integer.parseInt(npcs.getProperty("GrandChaosTime", "10"));
			MINION_CHAOS_TIME = Integer.parseInt(npcs.getProperty("MinionChaosTime", "10"));

			WAIT_TIME_ANTHARAS = Integer.parseInt(npcs.getProperty("AntharasWaitTime", "30"));
			if (WAIT_TIME_ANTHARAS < 3 || WAIT_TIME_ANTHARAS > 60)
				WAIT_TIME_ANTHARAS = 30;
			WAIT_TIME_ANTHARAS = WAIT_TIME_ANTHARAS * 60000;

			WAIT_TIME_VALAKAS = Integer.parseInt(npcs.getProperty("ValakasWaitTime", "30"));
			if (WAIT_TIME_VALAKAS < 3 || WAIT_TIME_VALAKAS > 60)
				WAIT_TIME_VALAKAS = 30;
			WAIT_TIME_VALAKAS = WAIT_TIME_VALAKAS * 60000;

			SPAWN_INTERVAL_ANTHARAS = Integer.parseInt(npcs.getProperty("IntervalOfAntharasSpawn", "264"));
			if (SPAWN_INTERVAL_ANTHARAS < 1 || SPAWN_INTERVAL_ANTHARAS > 480)
				SPAWN_INTERVAL_ANTHARAS = 192;
			SPAWN_INTERVAL_ANTHARAS = SPAWN_INTERVAL_ANTHARAS * 3600000;

			RANDOM_SPAWN_TIME_ANTHARAS = Integer.parseInt(npcs.getProperty("RandomOfAntharasSpawn", "72"));
			if (RANDOM_SPAWN_TIME_ANTHARAS < 1 || RANDOM_SPAWN_TIME_ANTHARAS > 192)
				RANDOM_SPAWN_TIME_ANTHARAS = 145;
			RANDOM_SPAWN_TIME_ANTHARAS = RANDOM_SPAWN_TIME_ANTHARAS * 3600000;

			SPAWN_INTERVAL_VALAKAS = Integer.parseInt(npcs.getProperty("IntervalOfValakasSpawn", "264"));
			if (SPAWN_INTERVAL_VALAKAS < 1 || SPAWN_INTERVAL_VALAKAS > 480)
				SPAWN_INTERVAL_VALAKAS = 192;
			SPAWN_INTERVAL_VALAKAS = SPAWN_INTERVAL_VALAKAS * 3600000;

			RANDOM_SPAWN_TIME_VALAKAS = Integer.parseInt(npcs.getProperty("RandomOfValakasSpawn", "72"));
			if (RANDOM_SPAWN_TIME_VALAKAS < 1 || RANDOM_SPAWN_TIME_VALAKAS > 192)
				RANDOM_SPAWN_TIME_VALAKAS = 145;
			RANDOM_SPAWN_TIME_VALAKAS = RANDOM_SPAWN_TIME_VALAKAS * 3600000;

			SPAWN_INTERVAL_BAIUM = Integer.parseInt(npcs.getProperty("IntervalOfBaiumSpawn", "168"));
			if (SPAWN_INTERVAL_BAIUM < 1 || SPAWN_INTERVAL_BAIUM > 480)
				SPAWN_INTERVAL_BAIUM = 121;
			SPAWN_INTERVAL_BAIUM = SPAWN_INTERVAL_BAIUM * 3600000;

			RANDOM_SPAWN_TIME_BAIUM = Integer.parseInt(npcs.getProperty("RandomOfBaiumSpawn", "48"));
			if (RANDOM_SPAWN_TIME_BAIUM < 1 || RANDOM_SPAWN_TIME_BAIUM > 192)
				RANDOM_SPAWN_TIME_BAIUM = 8;
			RANDOM_SPAWN_TIME_BAIUM = RANDOM_SPAWN_TIME_BAIUM * 3600000;

			SPAWN_INTERVAL_CORE = Integer.parseInt(npcs.getProperty("IntervalOfCoreSpawn", "60"));
			if (SPAWN_INTERVAL_CORE < 1 || SPAWN_INTERVAL_CORE > 480)
				SPAWN_INTERVAL_CORE = 27;
			SPAWN_INTERVAL_CORE = SPAWN_INTERVAL_CORE * 3600000;

			RANDOM_SPAWN_TIME_CORE = Integer.parseInt(npcs.getProperty("RandomOfCoreSpawn", "24"));
			if (RANDOM_SPAWN_TIME_CORE < 1 || RANDOM_SPAWN_TIME_CORE > 192)
				RANDOM_SPAWN_TIME_CORE = 47;
			RANDOM_SPAWN_TIME_CORE = RANDOM_SPAWN_TIME_CORE * 3600000;

			SPAWN_INTERVAL_ORFEN = Integer.parseInt(npcs.getProperty("IntervalOfOrfenSpawn", "48"));
			if (SPAWN_INTERVAL_ORFEN < 1 || SPAWN_INTERVAL_ORFEN > 480)
				SPAWN_INTERVAL_ORFEN = 28;
			SPAWN_INTERVAL_ORFEN = SPAWN_INTERVAL_ORFEN * 3600000;

			RANDOM_SPAWN_TIME_ORFEN = Integer.parseInt(npcs.getProperty("RandomOfOrfenSpawn", "20"));
			if (RANDOM_SPAWN_TIME_ORFEN < 1 || RANDOM_SPAWN_TIME_ORFEN > 192)
				RANDOM_SPAWN_TIME_ORFEN = 41;
			RANDOM_SPAWN_TIME_ORFEN = RANDOM_SPAWN_TIME_ORFEN * 3600000;

			SPAWN_INTERVAL_AQ = Integer.parseInt(npcs.getProperty("IntervalOfQueenAntSpawn", "36"));
			if (SPAWN_INTERVAL_AQ < 1 || SPAWN_INTERVAL_AQ > 480)
				SPAWN_INTERVAL_AQ = 19;
			SPAWN_INTERVAL_AQ = SPAWN_INTERVAL_AQ * 3600000;

			RANDOM_SPAWN_TIME_AQ = Integer.parseInt(npcs.getProperty("RandomOfQueenAntSpawn", "17"));
			if (RANDOM_SPAWN_TIME_AQ < 1 || RANDOM_SPAWN_TIME_AQ > 192)
				RANDOM_SPAWN_TIME_AQ = 35;
			RANDOM_SPAWN_TIME_AQ = RANDOM_SPAWN_TIME_AQ * 3600000;

			SPAWN_INTERVAL_ZAKEN = Integer.parseInt(npcs.getProperty("IntervalOfZakenSpawn", "19"));
			if (SPAWN_INTERVAL_ZAKEN < 1 || SPAWN_INTERVAL_ZAKEN > 480)
				SPAWN_INTERVAL_ZAKEN = 19;
			SPAWN_INTERVAL_ZAKEN = SPAWN_INTERVAL_ZAKEN * 3600000;

			RANDOM_SPAWN_TIME_ZAKEN = Integer.parseInt(npcs.getProperty("RandomOfZakenSpawn", "35"));
			if (RANDOM_SPAWN_TIME_ZAKEN < 1 || RANDOM_SPAWN_TIME_ZAKEN > 192)
				RANDOM_SPAWN_TIME_ZAKEN = 35;
			RANDOM_SPAWN_TIME_ZAKEN = RANDOM_SPAWN_TIME_ZAKEN * 3600000;

			SPAWN_INTERVAL_FRINTEZZA = Integer.parseInt(npcs.getProperty("IntervalOfFrintezzaSpawn", "48"));
			if (SPAWN_INTERVAL_FRINTEZZA < 1 || SPAWN_INTERVAL_FRINTEZZA > 480)
				SPAWN_INTERVAL_FRINTEZZA = 121;
			SPAWN_INTERVAL_FRINTEZZA = SPAWN_INTERVAL_FRINTEZZA * 3600000;

			RANDOM_SPAWN_TIME_FRINTEZZA = Integer.parseInt(npcs.getProperty("RandomOfFrintezzaSpawn", "8"));
			if (RANDOM_SPAWN_TIME_FRINTEZZA < 1 || RANDOM_SPAWN_TIME_FRINTEZZA > 192)
				RANDOM_SPAWN_TIME_FRINTEZZA = 8;
			RANDOM_SPAWN_TIME_FRINTEZZA = RANDOM_SPAWN_TIME_FRINTEZZA * 3600000;

			GUARD_ATTACK_AGGRO_MOB = Boolean.parseBoolean(npcs.getProperty("GuardAttackAggroMob", "False"));
			MAX_DRIFT_RANGE = Integer.parseInt(npcs.getProperty("MaxDriftRange", "300"));
			MOVE_BASED_KNOWNLIST = Boolean.parseBoolean(npcs.getProperty("MoveBasedKnownlist", "False"));
			KNOWNLIST_UPDATE_INTERVAL = Long.parseLong(npcs.getProperty("KnownListUpdateInterval", "1250"));
			MIN_NPC_ANIMATION = Integer.parseInt(npcs.getProperty("MinNPCAnimation", "10"));
			MAX_NPC_ANIMATION = Integer.parseInt(npcs.getProperty("MaxNPCAnimation", "20"));
			MIN_MONSTER_ANIMATION = Integer.parseInt(npcs.getProperty("MinMonsterAnimation", "5"));
			MAX_MONSTER_ANIMATION = Integer.parseInt(npcs.getProperty("MaxMonsterAnimation", "20"));

			GRIDS_ALWAYS_ON = Boolean.parseBoolean(npcs.getProperty("GridsAlwaysOn", "False"));
			GRID_NEIGHBOR_TURNON_TIME = Integer.parseInt(npcs.getProperty("GridNeighborTurnOnTime", "1"));
			GRID_NEIGHBOR_TURNOFF_TIME = Integer.parseInt(npcs.getProperty("GridNeighborTurnOffTime", "90"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + NPCS_FILE + " file.");
		}
	}

	public static class ClassMasterSettings
	{
		private final TIntObjectHashMap<TIntIntHashMap> _claimItems;
		private final TIntObjectHashMap<TIntIntHashMap> _rewardItems;
		private final TIntObjectHashMap<Boolean> _allowedClassChange;

		public ClassMasterSettings(String _configLine)
		{
			_claimItems = new TIntObjectHashMap<>(3);
			_rewardItems = new TIntObjectHashMap<>(3);
			_allowedClassChange = new TIntObjectHashMap<>(3);
			if (_configLine != null)
				parseConfigLine(_configLine.trim());
		}

		private void parseConfigLine(String _configLine)
		{
			StringTokenizer st = new StringTokenizer(_configLine, ";");

			while (st.hasMoreTokens())
			{
				// get allowed class change
				int job = Integer.parseInt(st.nextToken());

				_allowedClassChange.put(job, true);

				TIntIntHashMap _items = new TIntIntHashMap();
				// parse items needed for class change
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");

					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						int _itemId = Integer.parseInt(st3.nextToken());
						int _quantity = Integer.parseInt(st3.nextToken());
						_items.put(_itemId, _quantity);
					}
				}

				_claimItems.put(job, _items);

				_items = new TIntIntHashMap();
				// parse gifts after class change
				if (st.hasMoreTokens())
				{
					StringTokenizer st2 = new StringTokenizer(st.nextToken(), "[],");

					while (st2.hasMoreTokens())
					{
						StringTokenizer st3 = new StringTokenizer(st2.nextToken(), "()");
						int _itemId = Integer.parseInt(st3.nextToken());
						int _quantity = Integer.parseInt(st3.nextToken());
						_items.put(_itemId, _quantity);
					}
				}

				_rewardItems.put(job, _items);
			}
		}

		public boolean isAllowed(int job)
		{
			if (_allowedClassChange == null)
				return false;
			if (_allowedClassChange.containsKey(job))
				return _allowedClassChange.get(job);

			return false;
		}

		public TIntIntHashMap getRewardItems(int job)
		{
			if (_rewardItems.containsKey(job))
				return _rewardItems.get(job);

			return null;
		}

		public TIntIntHashMap getRequireItems(int job)
		{
			if (_claimItems.containsKey(job))
				return _claimItems.get(job);

			return null;
		}
	}
}
