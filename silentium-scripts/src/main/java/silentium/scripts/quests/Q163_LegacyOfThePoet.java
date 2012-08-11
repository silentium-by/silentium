/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q163_LegacyOfThePoet extends Quest
{
	private final static String qn = "Q163_LegacyOfThePoet";

	// NPC
	private static final int STARDEN = 30220;

	// Items
	private static final int RUMIELS_POEM_1 = 1038;
	private static final int RUMIELS_POEM_2 = 1039;
	private static final int RUMIELS_POEM_3 = 1040;
	private static final int RUMIELS_POEM_4 = 1041;

	// Reward
	private static final int ADENA = 57;

	public Q163_LegacyOfThePoet(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { RUMIELS_POEM_1, RUMIELS_POEM_2, RUMIELS_POEM_3, RUMIELS_POEM_4 };

		addStartNpc(STARDEN);
		addTalkId(STARDEN);

		addKillId(20372, 20373);
	}

	public static void main(String[] args)
	{
		new Q163_LegacyOfThePoet(163, "Q163_LegacyOfThePoet", "Legacy of the Poet");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30220-07.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getRace().ordinal() == 2)
				{
					htmltext = "30220-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 11 && player.getLevel() <= 15)
					htmltext = "30220-03.htm";
				else
				{
					htmltext = "30220-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(RUMIELS_POEM_1) == 1 && st.getQuestItemsCount(RUMIELS_POEM_2) == 1 && st.getQuestItemsCount(RUMIELS_POEM_3) == 1 && st.getQuestItemsCount(RUMIELS_POEM_4) == 1)
				{
					htmltext = "30220-09.htm";
					st.takeItems(RUMIELS_POEM_1, 1);
					st.takeItems(RUMIELS_POEM_2, 1);
					st.takeItems(RUMIELS_POEM_3, 1);
					st.takeItems(RUMIELS_POEM_4, 1);
					st.rewardItems(ADENA, 13890);
					st.exitQuest(false);
					st.playSound(QuestState.SOUND_FINISH);
				}
				else
					htmltext = "30220-08.htm";
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 1)
		{
			if (!st.hasQuestItems(RUMIELS_POEM_1) && Rnd.get(10) == 0)
			{
				st.giveItems(RUMIELS_POEM_1, 1);

				if (st.hasQuestItems(RUMIELS_POEM_2) && st.hasQuestItems(RUMIELS_POEM_3) && st.hasQuestItems(RUMIELS_POEM_4))
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
			else if (!st.hasQuestItems(RUMIELS_POEM_2) && Rnd.get(10) > 7)
			{
				st.giveItems(RUMIELS_POEM_2, 1);

				if (st.hasQuestItems(RUMIELS_POEM_1) && st.hasQuestItems(RUMIELS_POEM_3) && st.hasQuestItems(RUMIELS_POEM_4))
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
			else if (!st.hasQuestItems(RUMIELS_POEM_3) && Rnd.get(10) > 7)
			{
				st.giveItems(RUMIELS_POEM_3, 1);

				if (st.hasQuestItems(RUMIELS_POEM_1) && st.hasQuestItems(RUMIELS_POEM_2) && st.hasQuestItems(RUMIELS_POEM_4))
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
			else if (!st.hasQuestItems(RUMIELS_POEM_4) && Rnd.get(10) > 5)
			{
				st.giveItems(RUMIELS_POEM_4, 1);

				if (st.hasQuestItems(RUMIELS_POEM_1) && st.hasQuestItems(RUMIELS_POEM_2) && st.hasQuestItems(RUMIELS_POEM_3))
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				else
					st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
		return null;
	}
}