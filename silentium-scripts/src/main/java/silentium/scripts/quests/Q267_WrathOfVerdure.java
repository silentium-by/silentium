/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q267_WrathOfVerdure extends Quest implements ScriptFile
{
	private final static String qn = "Q267_WrathOfVerdure";

	// Items
	private static final int GOBLIN_CLUB = 1335;

	// Reward
	private static final int SILVERY_LEAF = 1340;

	// NPC
	private static final int TREANT_BREMEC = 31853;

	// Mob
	private static final int GOBLIN = 20325;

	public Q267_WrathOfVerdure(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { GOBLIN_CLUB };

		addStartNpc(TREANT_BREMEC);
		addTalkId(TREANT_BREMEC);

		addKillId(GOBLIN);
	}

	public static void onLoad()
	{
		new Q267_WrathOfVerdure(267, "Q267_WrathOfVerdure", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31853-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31853-06.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getRace().ordinal() == 1)
				{
					if (player.getLevel() >= 4 && player.getLevel() <= 9)
						htmltext = "31853-02.htm";
					else
					{
						htmltext = "31853-01.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "31853-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int count = st.getQuestItemsCount(GOBLIN_CLUB);

				if (count > 0)
				{
					htmltext = "31853-05.htm";
					st.takeItems(GOBLIN_CLUB, -1);
					st.rewardItems(SILVERY_LEAF, count);
				}
				else
					htmltext = "31853-04.htm";
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

		if (st.getInt("cond") == 1 && Rnd.get(10) < 5)
		{
			st.giveItems(GOBLIN_CLUB, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}