/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q023_LidiasHeart extends Quest
{
	private final static String qn = "Q023_LidiasHeart";

	// NPCs
	private static final int Innocentin = 31328;
	private static final int BrokenBookshelf = 31526;
	private static final int GhostofvonHellmann = 31524;
	private static final int Tombstone = 31523;
	private static final int Violet = 31386;
	private static final int Box = 31530;

	// NPC instance
	private L2Npc ghost = null;

	// Items
	private static final int MapForestofDeadman = 7063;
	private static final int SilverKey = 7149;
	private static final int LidiaHairPin = 7148;
	private static final int LidiaDiary = 7064;
	private static final int SilverSpear = 7150;

	public Q023_LidiasHeart(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { MapForestofDeadman, SilverKey, LidiaDiary, SilverSpear };

		addStartNpc(Innocentin);
		addTalkId(Innocentin, BrokenBookshelf, GhostofvonHellmann, Violet, Box, Tombstone);
	}

	public static void main(String[] args)
	{
		new Q023_LidiasHeart(23, "Q023_LidiasHeart", "Lidia's Heart");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31328-02.htm"))
		{
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
			st.setState(QuestState.STARTED);

			st.giveItems(MapForestofDeadman, 1);
			st.giveItems(SilverKey, 1);
		}
		else if (event.equalsIgnoreCase("31328-06.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31526-05.htm"))
		{
			if (st.getQuestItemsCount(LidiaHairPin) == 0)
			{
				st.giveItems(LidiaHairPin, 1);
				if (st.getQuestItemsCount(LidiaDiary) >= 1)
				{
					st.set("cond", "4");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		else if (event.equalsIgnoreCase("31526-11.htm"))
		{
			if (st.getQuestItemsCount(LidiaDiary) == 0)
			{
				st.giveItems(LidiaDiary, 1);
				if (st.getQuestItemsCount(LidiaHairPin) >= 1)
				{
					st.set("cond", "4");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		else if (event.equalsIgnoreCase("31328-11.htm"))
		{
			if (st.getInt("cond") < 5)
			{
				st.set("cond", "5");
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		}
		else if (event.equalsIgnoreCase("31328-19.htm"))
		{
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31524-04.htm"))
		{
			st.set("cond", "7");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(LidiaDiary, -1);
		}
		else if (event.equalsIgnoreCase("31523-02.htm"))
		{
			if (ghost == null)
			{
				ghost = st.addSpawn(31524, 51432, -54570, -3136, 60000);
				ghost.broadcastNpcSay("Who awoke me?");
				st.startQuestTimer("ghost_cleanup", 58000);
			}
		}
		else if (event.equalsIgnoreCase("31523-05.htm"))
		{
			// Don't launch twice the same task...
			if (st.getQuestTimer("tomb_digger") == null)
				st.startQuestTimer("tomb_digger", 10000);
		}
		else if (event.equalsIgnoreCase("tomb_digger"))
		{
			st.set("cond", "8");
			htmltext = "31523-06.htm";
			st.playSound(QuestState.SOUND_MIDDLE);
			st.giveItems(SilverKey, 1);
		}
		else if (event.equalsIgnoreCase("31530-02.htm"))
		{
			st.set("cond", "10");
			st.playSound(QuestState.SOUND_MIDDLE);
			st.takeItems(SilverKey, -1);
			st.giveItems(SilverSpear, 1);
		}
		else if (event.equalsIgnoreCase("ghost_cleanup"))
		{
			ghost = null;
			return null;
		}
		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				QuestState st2 = player.getQuestState("Q022_TragedyInVonHellmannForest");
				if (st2 != null && st2.isCompleted())
				{
					if (player.getLevel() >= 64)
						htmltext = "31328-01.htm";
					else
					{
						htmltext = "31328-00a.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "31328-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Innocentin:
						if (cond == 1)
							htmltext = "31328-03.htm";
						else if (cond == 2)
							htmltext = "31328-07.htm";
						else if (cond == 4)
							htmltext = "31328-08.htm";
						else if (cond >= 6)
							htmltext = "31328-21.htm";
						break;

					case BrokenBookshelf:
						if (cond == 2)
						{
							htmltext = "31526-00.htm";
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 3)
						{
							if (st.getQuestItemsCount(LidiaHairPin) == 0 && st.getQuestItemsCount(LidiaDiary) == 0)
								htmltext = "31526-02.htm";
							else if (st.getQuestItemsCount(LidiaHairPin) != 0 && st.getQuestItemsCount(LidiaDiary) == 0)
								htmltext = "31526-06.htm";
							else if (st.getQuestItemsCount(LidiaHairPin) == 0 && st.getQuestItemsCount(LidiaDiary) != 0)
								htmltext = "31526-12.htm";
						}
						else if (cond >= 4)
							htmltext = "31526-13.htm";
						break;

					case GhostofvonHellmann:
						if (cond == 6)
							htmltext = "31524-01.htm";
						else if (cond >= 7)
							htmltext = "31524-05.htm";
						break;

					case Tombstone:
						if (cond == 6)
						{
							if (ghost != null)
								htmltext = "31523-03.htm";
							else
								htmltext = "31523-01.htm";
						}
						else if (cond == 7)
							htmltext = "31523-04.htm";
						else if (cond >= 8)
							htmltext = "31523-06.htm";
						break;

					case Violet:
						if (cond == 8)
						{
							st.set("cond", "9");
							htmltext = "31386-01.htm";
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else if (cond == 9)
							htmltext = "31386-02.htm";
						else if (cond == 10)
						{
							if (st.getQuestItemsCount(SilverSpear) > 0)
							{
								htmltext = "31386-03.htm";
								st.takeItems(SilverSpear, -1);
								st.rewardItems(57, 100000);
								st.exitQuest(false);
								st.playSound(QuestState.SOUND_FINISH);
							}
							else
							{
								st.set("cond", "9");
								htmltext = "31386-02.htm";
							}
						}
						break;

					case Box:
						if (cond == 9)
							htmltext = "31530-01.htm";
						else if (cond == 10)
							htmltext = "31530-03.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				if (npc.getNpcId() == Violet)
					htmltext = "31386-04.htm";
				else
					htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}