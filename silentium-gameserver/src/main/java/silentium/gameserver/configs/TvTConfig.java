package silentium.gameserver.configs;

import gnu.trove.map.hash.TIntIntHashMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import silentium.commons.utils.StringUtil;

public final class TvTConfig extends ConfigEngine
{
	public static boolean TVT_EVENT_ENABLED;
	public static String[] TVT_EVENT_INTERVAL;
	public static int TVT_EVENT_PARTICIPATION_TIME;
	public static int TVT_EVENT_RUNNING_TIME;
	public static int TVT_EVENT_PARTICIPATION_NPC_ID;
	public static int[] TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
	public static int[] TVT_EVENT_PARTICIPATION_FEE = new int[2];
	public static int TVT_EVENT_MIN_PLAYERS_IN_TEAMS;
	public static int TVT_EVENT_MAX_PLAYERS_IN_TEAMS;
	public static int TVT_EVENT_RESPAWN_TELEPORT_DELAY;
	public static int TVT_EVENT_START_LEAVE_TELEPORT_DELAY;
	public static String TVT_EVENT_TEAM_1_NAME;
	public static int[] TVT_EVENT_TEAM_1_COORDINATES = new int[3];
	public static String TVT_EVENT_TEAM_2_NAME;
	public static int[] TVT_EVENT_TEAM_2_COORDINATES = new int[3];
	public static List<int[]> TVT_EVENT_REWARDS;
	public static boolean TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED;
	public static boolean TVT_EVENT_SCROLL_ALLOWED;
	public static boolean TVT_EVENT_POTIONS_ALLOWED;
	public static boolean TVT_EVENT_SUMMON_BY_ITEM_ALLOWED;
	public static List<Integer> TVT_DOORS_IDS_TO_OPEN;
	public static List<Integer> TVT_DOORS_IDS_TO_CLOSE;
	public static boolean TVT_REWARD_TEAM_TIE;
	public static byte TVT_EVENT_MIN_LVL;
	public static byte TVT_EVENT_MAX_LVL;
	public static int TVT_EVENT_EFFECTS_REMOVAL;
	public static TIntIntHashMap TVT_EVENT_FIGHTER_BUFFS;
	public static TIntIntHashMap TVT_EVENT_MAGE_BUFFS;
	public static boolean TVT_ALLOW_VOICED_COMMAND;

	public static void load()
	{
		try (InputStream is = new FileInputStream(new File(TVT_FILE)))
		{
			Properties tvt = new Properties();
			tvt.load(is);
			is.close();

			TVT_EVENT_ENABLED = Boolean.parseBoolean(tvt.getProperty("TvTEventEnabled", "false"));
			TVT_EVENT_INTERVAL = tvt.getProperty("TvTEventInterval", "20:00").split(",");
			TVT_EVENT_PARTICIPATION_TIME = Integer.parseInt(tvt.getProperty("TvTEventParticipationTime", "3600"));
			TVT_EVENT_RUNNING_TIME = Integer.parseInt(tvt.getProperty("TvTEventRunningTime", "1800"));
			TVT_EVENT_PARTICIPATION_NPC_ID = Integer.parseInt(tvt.getProperty("TvTEventParticipationNpcId", "0"));

			if (TVT_EVENT_PARTICIPATION_NPC_ID == 0)
			{
				TVT_EVENT_ENABLED = false;
				log.warn("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcId");
			}
			else
			{
				String[] propertySplit = tvt.getProperty("TvTEventParticipationNpcCoordinates", "0,0,0").split(",");
				if (propertySplit.length < 3)
				{
					TVT_EVENT_ENABLED = false;
					log.warn("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationNpcCoordinates");
				}
				else
				{
					TVT_EVENT_REWARDS = new ArrayList<int[]>();
					TVT_DOORS_IDS_TO_OPEN = new ArrayList<Integer>();
					TVT_DOORS_IDS_TO_CLOSE = new ArrayList<Integer>();
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES = new int[4];
					TVT_EVENT_TEAM_1_COORDINATES = new int[3];
					TVT_EVENT_TEAM_2_COORDINATES = new int[3];
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
					TVT_EVENT_PARTICIPATION_NPC_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
					if (propertySplit.length == 4)
						TVT_EVENT_PARTICIPATION_NPC_COORDINATES[3] = Integer.parseInt(propertySplit[3]);
					TVT_EVENT_MIN_PLAYERS_IN_TEAMS = Integer.parseInt(tvt.getProperty("TvTEventMinPlayersInTeams", "1"));
					TVT_EVENT_MAX_PLAYERS_IN_TEAMS = Integer.parseInt(tvt.getProperty("TvTEventMaxPlayersInTeams", "20"));
					TVT_EVENT_MIN_LVL = (byte) Integer.parseInt(tvt.getProperty("TvTEventMinPlayerLevel", "1"));
					TVT_EVENT_MAX_LVL = (byte) Integer.parseInt(tvt.getProperty("TvTEventMaxPlayerLevel", "80"));
					TVT_EVENT_RESPAWN_TELEPORT_DELAY = Integer.parseInt(tvt.getProperty("TvTEventRespawnTeleportDelay", "20"));
					TVT_EVENT_START_LEAVE_TELEPORT_DELAY = Integer.parseInt(tvt.getProperty("TvTEventStartLeaveTeleportDelay", "20"));
					TVT_EVENT_EFFECTS_REMOVAL = Integer.parseInt(tvt.getProperty("TvTEventEffectsRemoval", "0"));
					TVT_ALLOW_VOICED_COMMAND = Boolean.parseBoolean(tvt.getProperty("TvTAllowVoicedInfoCommand", "false"));
					TVT_EVENT_TEAM_1_NAME = tvt.getProperty("TvTEventTeam1Name", "Team1");
					propertySplit = tvt.getProperty("TvTEventTeam1Coordinates", "0,0,0").split(",");
					if (propertySplit.length < 3)
					{
						TVT_EVENT_ENABLED = false;
						log.warn("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam1Coordinates");
					}
					else
					{
						TVT_EVENT_TEAM_1_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
						TVT_EVENT_TEAM_1_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
						TVT_EVENT_TEAM_1_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
						TVT_EVENT_TEAM_2_NAME = tvt.getProperty("TvTEventTeam2Name", "Team2");
						propertySplit = tvt.getProperty("TvTEventTeam2Coordinates", "0,0,0").split(",");
						if (propertySplit.length < 3)
						{
							TVT_EVENT_ENABLED = false;
							log.warn("TvTEventEngine[Config.load()]: invalid config property -> TvTEventTeam2Coordinates");
						}
						else
						{
							TVT_EVENT_TEAM_2_COORDINATES[0] = Integer.parseInt(propertySplit[0]);
							TVT_EVENT_TEAM_2_COORDINATES[1] = Integer.parseInt(propertySplit[1]);
							TVT_EVENT_TEAM_2_COORDINATES[2] = Integer.parseInt(propertySplit[2]);
							propertySplit = tvt.getProperty("TvTEventParticipationFee", "0,0").split(",");
							try
							{
								TVT_EVENT_PARTICIPATION_FEE[0] = Integer.parseInt(propertySplit[0]);
								TVT_EVENT_PARTICIPATION_FEE[1] = Integer.parseInt(propertySplit[1]);
							}
							catch (NumberFormatException nfe)
							{
								if (propertySplit.length > 0)
									log.warn("TvTEventEngine[Config.load()]: invalid config property -> TvTEventParticipationFee");
							}
							propertySplit = tvt.getProperty("TvTEventReward", "57,100000").split(";");
							for (String reward : propertySplit)
							{
								String[] rewardSplit = reward.split(",");
								if (rewardSplit.length != 2)
									log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"", reward, "\""));
								else
								{
									try
									{
										TVT_EVENT_REWARDS.add(new int[] { Integer.parseInt(rewardSplit[0]), Integer.parseInt(rewardSplit[1]) });
									}
									catch (NumberFormatException nfe)
									{
										if (!reward.isEmpty())
											log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventReward \"", reward, "\""));
									}
								}
							}

							TVT_EVENT_TARGET_TEAM_MEMBERS_ALLOWED = Boolean.parseBoolean(tvt.getProperty("TvTEventTargetTeamMembersAllowed", "true"));
							TVT_EVENT_SCROLL_ALLOWED = Boolean.parseBoolean(tvt.getProperty("TvTEventScrollsAllowed", "false"));
							TVT_EVENT_POTIONS_ALLOWED = Boolean.parseBoolean(tvt.getProperty("TvTEventPotionsAllowed", "false"));
							TVT_EVENT_SUMMON_BY_ITEM_ALLOWED = Boolean.parseBoolean(tvt.getProperty("TvTEventSummonByItemAllowed", "false"));
							TVT_REWARD_TEAM_TIE = Boolean.parseBoolean(tvt.getProperty("TvTRewardTeamTie", "false"));
							propertySplit = tvt.getProperty("TvTDoorsToOpen", "").split(";");
							for (String door : propertySplit)
							{
								try
								{
									TVT_DOORS_IDS_TO_OPEN.add(Integer.parseInt(door));
								}
								catch (NumberFormatException nfe)
								{
									if (!door.isEmpty())
										log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToOpen \"", door, "\""));
								}
							}

							propertySplit = tvt.getProperty("TvTDoorsToClose", "").split(";");
							for (String door : propertySplit)
							{
								try
								{
									TVT_DOORS_IDS_TO_CLOSE.add(Integer.parseInt(door));
								}
								catch (NumberFormatException nfe)
								{
									if (!door.isEmpty())
										log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTDoorsToClose \"", door, "\""));
								}
							}

							propertySplit = tvt.getProperty("TvTEventFighterBuffs", "").split(";");
							if (!propertySplit[0].isEmpty())
							{
								TVT_EVENT_FIGHTER_BUFFS = new TIntIntHashMap(propertySplit.length);
								for (String skill : propertySplit)
								{
									String[] skillSplit = skill.split(",");
									if (skillSplit.length != 2)
										log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventFighterBuffs \"", skill, "\""));
									else
									{
										try
										{
											TVT_EVENT_FIGHTER_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
										}
										catch (NumberFormatException nfe)
										{
											if (!skill.isEmpty())
												log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventFighterBuffs \"", skill, "\""));
										}
									}
								}
							}

							propertySplit = tvt.getProperty("TvTEventMageBuffs", "").split(";");
							if (!propertySplit[0].isEmpty())
							{
								TVT_EVENT_MAGE_BUFFS = new TIntIntHashMap(propertySplit.length);
								for (String skill : propertySplit)
								{
									String[] skillSplit = skill.split(",");
									if (skillSplit.length != 2)
										log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventMageBuffs \"", skill, "\""));
									else
									{
										try
										{
											TVT_EVENT_MAGE_BUFFS.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
										}
										catch (NumberFormatException nfe)
										{
											if (!skill.isEmpty())
												log.warn(StringUtil.concat("TvTEventEngine[Config.load()]: invalid config property -> TvTEventMageBuffs \"", skill, "\""));
										}
									}
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new Error("Server failed to load " + TVT_FILE + " file.");
		}
	}
}
