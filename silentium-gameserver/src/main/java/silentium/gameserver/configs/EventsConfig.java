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
import java.util.Properties;

import silentium.commons.utils.StringUtil;

public final class EventsConfig extends ConfigEngine
{
	public static int ALT_OLY_START_TIME;
	public static int ALT_OLY_MIN;
	public static long ALT_OLY_CPERIOD;
	public static long ALT_OLY_BATTLE;
	public static long ALT_OLY_WPERIOD;
	public static long ALT_OLY_VPERIOD;
	public static int ALT_OLY_WAIT_TIME;
	public static int ALT_OLY_START_POINTS;
	public static int ALT_OLY_WEEKLY_POINTS;
	public static int ALT_OLY_MIN_MATCHES;
	public static int ALT_OLY_CLASSED;
	public static int ALT_OLY_NONCLASSED;
	public static int[][] ALT_OLY_CLASSED_REWARD;
	public static int[][] ALT_OLY_NONCLASSED_REWARD;
	public static int ALT_OLY_COMP_RITEM;
	public static int ALT_OLY_GP_PER_POINT;
	public static int ALT_OLY_HERO_POINTS;
	public static int ALT_OLY_RANK1_POINTS;
	public static int ALT_OLY_RANK2_POINTS;
	public static int ALT_OLY_RANK3_POINTS;
	public static int ALT_OLY_RANK4_POINTS;
	public static int ALT_OLY_RANK5_POINTS;
	public static int ALT_OLY_MAX_POINTS;
	public static int ALT_OLY_DIVIDER_CLASSED;
	public static int ALT_OLY_DIVIDER_NON_CLASSED;
	public static boolean ALT_OLY_ANNOUNCE_GAMES;
	public static boolean ALT_GAME_REQUIRE_CLAN_CASTLE;
	public static boolean ALT_GAME_CASTLE_DAWN;
	public static boolean ALT_GAME_CASTLE_DUSK;
	public static int ALT_FESTIVAL_MIN_PLAYER;
	public static int ALT_MAXIMUM_PLAYER_CONTRIB;
	public static long ALT_FESTIVAL_MANAGER_START;
	public static long ALT_FESTIVAL_LENGTH;
	public static long ALT_FESTIVAL_CYCLE_LENGTH;
	public static long ALT_FESTIVAL_FIRST_SPAWN;
	public static long ALT_FESTIVAL_FIRST_SWARM;
	public static long ALT_FESTIVAL_SECOND_SPAWN;
	public static long ALT_FESTIVAL_SECOND_SWARM;
	public static long ALT_FESTIVAL_CHEST_SPAWN;
	public static boolean ALT_SEVENSIGNS_LAZY_UPDATE;
	public static int FS_TIME_ATTACK;
	public static int FS_TIME_COOLDOWN;
	public static int FS_TIME_ENTRY;
	public static int FS_TIME_WARMUP;
	public static int FS_PARTY_MEMBER_COUNT;
	public static int RIFT_MIN_PARTY_SIZE;
	public static int RIFT_SPAWN_DELAY;
	public static int RIFT_MAX_JUMPS;
	public static int RIFT_AUTO_JUMPS_TIME_MIN;
	public static int RIFT_AUTO_JUMPS_TIME_MAX;
	public static int RIFT_ENTER_COST_RECRUIT;
	public static int RIFT_ENTER_COST_SOLDIER;
	public static int RIFT_ENTER_COST_OFFICER;
	public static int RIFT_ENTER_COST_CAPTAIN;
	public static int RIFT_ENTER_COST_COMMANDER;
	public static int RIFT_ENTER_COST_HERO;
	public static float RIFT_BOSS_ROOM_TIME_MUTIPLY;
	public static boolean ALLOW_WEDDING;
	public static int WEDDING_PRICE;
	public static boolean WEDDING_SAMESEX;
	public static boolean WEDDING_FORMALWEAR;
	public static int ALT_LOTTERY_PRIZE;
	public static int ALT_LOTTERY_TICKET_PRICE;
	public static float ALT_LOTTERY_5_NUMBER_RATE;
	public static float ALT_LOTTERY_4_NUMBER_RATE;
	public static float ALT_LOTTERY_3_NUMBER_RATE;
	public static int ALT_LOTTERY_2_AND_1_NUMBER_PRIZE;

	public static void load()
	{
		try (InputStream is = new FileInputStream(new File(EVENTS_FILE)))
		{
			Properties events = new Properties();
			events.load(is);
			is.close();

			ALT_OLY_START_TIME = Integer.parseInt(events.getProperty("AltOlyStartTime", "18"));
			ALT_OLY_MIN = Integer.parseInt(events.getProperty("AltOlyMin", "00"));
			ALT_OLY_CPERIOD = Long.parseLong(events.getProperty("AltOlyCPeriod", "21600000"));
			ALT_OLY_BATTLE = Long.parseLong(events.getProperty("AltOlyBattle", "180000"));
			ALT_OLY_WPERIOD = Long.parseLong(events.getProperty("AltOlyWPeriod", "604800000"));
			ALT_OLY_VPERIOD = Long.parseLong(events.getProperty("AltOlyVPeriod", "86400000"));
			ALT_OLY_WAIT_TIME = Integer.parseInt(events.getProperty("AltOlyWaitTime", "30"));
			ALT_OLY_START_POINTS = Integer.parseInt(events.getProperty("AltOlyStartPoints", "18"));
			ALT_OLY_WEEKLY_POINTS = Integer.parseInt(events.getProperty("AltOlyWeeklyPoints", "3"));
			ALT_OLY_MIN_MATCHES = Integer.parseInt(events.getProperty("AltOlyMinMatchesToBeClassed", "5"));
			ALT_OLY_CLASSED = Integer.parseInt(events.getProperty("AltOlyClassedParticipants", "5"));
			ALT_OLY_NONCLASSED = Integer.parseInt(events.getProperty("AltOlyNonClassedParticipants", "9"));
			ALT_OLY_CLASSED_REWARD = parseItemsList(events.getProperty("AltOlyClassedReward", "6651,50"));
			ALT_OLY_NONCLASSED_REWARD = parseItemsList(events.getProperty("AltOlyNonClassedReward", "6651,30"));
			ALT_OLY_COMP_RITEM = Integer.parseInt(events.getProperty("AltOlyCompRewItem", "6651"));
			ALT_OLY_GP_PER_POINT = Integer.parseInt(events.getProperty("AltOlyGPPerPoint", "1000"));
			ALT_OLY_HERO_POINTS = Integer.parseInt(events.getProperty("AltOlyHeroPoints", "300"));
			ALT_OLY_RANK1_POINTS = Integer.parseInt(events.getProperty("AltOlyRank1Points", "100"));
			ALT_OLY_RANK2_POINTS = Integer.parseInt(events.getProperty("AltOlyRank2Points", "75"));
			ALT_OLY_RANK3_POINTS = Integer.parseInt(events.getProperty("AltOlyRank3Points", "55"));
			ALT_OLY_RANK4_POINTS = Integer.parseInt(events.getProperty("AltOlyRank4Points", "40"));
			ALT_OLY_RANK5_POINTS = Integer.parseInt(events.getProperty("AltOlyRank5Points", "30"));
			ALT_OLY_MAX_POINTS = Integer.parseInt(events.getProperty("AltOlyMaxPoints", "10"));
			ALT_OLY_DIVIDER_CLASSED = Integer.parseInt(events.getProperty("AltOlyDividerClassed", "3"));
			ALT_OLY_DIVIDER_NON_CLASSED = Integer.parseInt(events.getProperty("AltOlyDividerNonClassed", "3"));
			ALT_OLY_ANNOUNCE_GAMES = Boolean.parseBoolean(events.getProperty("AltOlyAnnounceGames", "True"));

			ALT_GAME_REQUIRE_CLAN_CASTLE = Boolean.parseBoolean(events.getProperty("AltRequireClanCastle", "False"));
			ALT_GAME_CASTLE_DAWN = Boolean.parseBoolean(events.getProperty("AltCastleForDawn", "True"));
			ALT_GAME_CASTLE_DUSK = Boolean.parseBoolean(events.getProperty("AltCastleForDusk", "True"));
			ALT_FESTIVAL_MIN_PLAYER = Integer.parseInt(events.getProperty("AltFestivalMinPlayer", "5"));
			ALT_MAXIMUM_PLAYER_CONTRIB = Integer.parseInt(events.getProperty("AltMaxPlayerContrib", "1000000"));
			ALT_FESTIVAL_MANAGER_START = Long.parseLong(events.getProperty("AltFestivalManagerStart", "120000"));
			ALT_FESTIVAL_LENGTH = Long.parseLong(events.getProperty("AltFestivalLength", "1080000"));
			ALT_FESTIVAL_CYCLE_LENGTH = Long.parseLong(events.getProperty("AltFestivalCycleLength", "2280000"));
			ALT_FESTIVAL_FIRST_SPAWN = Long.parseLong(events.getProperty("AltFestivalFirstSpawn", "120000"));
			ALT_FESTIVAL_FIRST_SWARM = Long.parseLong(events.getProperty("AltFestivalFirstSwarm", "300000"));
			ALT_FESTIVAL_SECOND_SPAWN = Long.parseLong(events.getProperty("AltFestivalSecondSpawn", "540000"));
			ALT_FESTIVAL_SECOND_SWARM = Long.parseLong(events.getProperty("AltFestivalSecondSwarm", "720000"));
			ALT_FESTIVAL_CHEST_SPAWN = Long.parseLong(events.getProperty("AltFestivalChestSpawn", "900000"));
			ALT_SEVENSIGNS_LAZY_UPDATE = Boolean.parseBoolean(events.getProperty("AltSevenSignsLazyUpdate", "True"));

			FS_TIME_ATTACK = Integer.parseInt(events.getProperty("TimeOfAttack", "50"));
			FS_TIME_COOLDOWN = Integer.parseInt(events.getProperty("TimeOfCoolDown", "5"));
			FS_TIME_ENTRY = Integer.parseInt(events.getProperty("TimeOfEntry", "3"));
			FS_TIME_WARMUP = Integer.parseInt(events.getProperty("TimeOfWarmUp", "2"));
			FS_PARTY_MEMBER_COUNT = Integer.parseInt(events.getProperty("NumberOfNecessaryPartyMembers", "4"));

			if (FS_TIME_ATTACK <= 0)
				FS_TIME_ATTACK = 50;
			if (FS_TIME_COOLDOWN <= 0)
				FS_TIME_COOLDOWN = 5;
			if (FS_TIME_ENTRY <= 0)
				FS_TIME_ENTRY = 3;
			if (FS_TIME_WARMUP <= 0)
				FS_TIME_WARMUP = 2;
			if (FS_PARTY_MEMBER_COUNT <= 0)
				FS_PARTY_MEMBER_COUNT = 4;

			RIFT_MIN_PARTY_SIZE = Integer.parseInt(events.getProperty("RiftMinPartySize", "2"));
			RIFT_MAX_JUMPS = Integer.parseInt(events.getProperty("MaxRiftJumps", "4"));
			RIFT_SPAWN_DELAY = Integer.parseInt(events.getProperty("RiftSpawnDelay", "10000"));
			RIFT_AUTO_JUMPS_TIME_MIN = Integer.parseInt(events.getProperty("AutoJumpsDelayMin", "480"));
			RIFT_AUTO_JUMPS_TIME_MAX = Integer.parseInt(events.getProperty("AutoJumpsDelayMax", "600"));
			RIFT_ENTER_COST_RECRUIT = Integer.parseInt(events.getProperty("RecruitCost", "18"));
			RIFT_ENTER_COST_SOLDIER = Integer.parseInt(events.getProperty("SoldierCost", "21"));
			RIFT_ENTER_COST_OFFICER = Integer.parseInt(events.getProperty("OfficerCost", "24"));
			RIFT_ENTER_COST_CAPTAIN = Integer.parseInt(events.getProperty("CaptainCost", "27"));
			RIFT_ENTER_COST_COMMANDER = Integer.parseInt(events.getProperty("CommanderCost", "30"));
			RIFT_ENTER_COST_HERO = Integer.parseInt(events.getProperty("HeroCost", "33"));
			RIFT_BOSS_ROOM_TIME_MUTIPLY = Float.parseFloat(events.getProperty("BossRoomTimeMultiply", "1.0"));

			ALLOW_WEDDING = Boolean.parseBoolean(events.getProperty("AllowWedding", "False"));
			WEDDING_PRICE = Integer.parseInt(events.getProperty("WeddingPrice", "1000000"));
			WEDDING_SAMESEX = Boolean.parseBoolean(events.getProperty("WeddingAllowSameSex", "False"));
			WEDDING_FORMALWEAR = Boolean.parseBoolean(events.getProperty("WeddingFormalWear", "True"));

			ALT_LOTTERY_PRIZE = Integer.parseInt(events.getProperty("AltLotteryPrize", "50000"));
			ALT_LOTTERY_TICKET_PRICE = Integer.parseInt(events.getProperty("AltLotteryTicketPrice", "2000"));
			ALT_LOTTERY_5_NUMBER_RATE = Float.parseFloat(events.getProperty("AltLottery5NumberRate", "0.6"));
			ALT_LOTTERY_4_NUMBER_RATE = Float.parseFloat(events.getProperty("AltLottery4NumberRate", "0.2"));
			ALT_LOTTERY_3_NUMBER_RATE = Float.parseFloat(events.getProperty("AltLottery3NumberRate", "0.2"));
			ALT_LOTTERY_2_AND_1_NUMBER_PRIZE = Integer.parseInt(events.getProperty("AltLottery2and1NumberPrize", "200"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + EVENTS_FILE + " file.");
		}
	}

	private static int[][] parseItemsList(String line)
	{
		final String[] propertySplit = line.split(";");
		if (propertySplit.length == 0)
			return null;

		int i = 0;
		String[] valueSplit;
		final int[][] result = new int[propertySplit.length][];
		for (String value : propertySplit)
		{
			valueSplit = value.split(",");
			if (valueSplit.length != 2)
			{
				log.warn(StringUtil.concat("parseItemsList[MainConfig.load()]: invalid entry -> \"", valueSplit[0], "\", should be itemId,itemNumber"));
				return null;
			}

			result[i] = new int[2];
			try
			{
				result[i][0] = Integer.parseInt(valueSplit[0]);
			}
			catch (NumberFormatException e)
			{
				log.warn(StringUtil.concat("parseItemsList[MainConfig.load()]: invalid itemId -> \"", valueSplit[0], "\""));
				return null;
			}

			try
			{
				result[i][1] = Integer.parseInt(valueSplit[1]);
			}
			catch (NumberFormatException e)
			{
				log.warn(StringUtil.concat("parseItemsList[MainConfig.load()]: invalid item number -> \"", valueSplit[1], "\""));
				return null;
			}
			i++;
		}
		return result;
	}
}