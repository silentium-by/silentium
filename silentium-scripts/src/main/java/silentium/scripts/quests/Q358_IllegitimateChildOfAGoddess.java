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

public class Q358_IllegitimateChildOfAGoddess extends Quest implements ScriptFile
{
	private static final String qn = "Q358_IllegitimateChildOfAGoddess";

	// Item
	private static final int SCALE = 5868;

	// Reward
	private static final int REWARD[] = { 6329, 6331, 6333, 6335, 6337, 6339, 5364, 5366 };

	public Q358_IllegitimateChildOfAGoddess(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { SCALE };

		addStartNpc(30862); // Oltlin
		addTalkId(30862);

		addKillId(20672, 20673); // Trives, Falibati
	}

	public static void onLoad()
	{
		new Q358_IllegitimateChildOfAGoddess(358, "Q358_IllegitimateChildOfAGoddess", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30862-05.htm"))
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
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 63 && player.getLevel() <= 67)
					htmltext = "30862-02.htm";
				else
				{
					htmltext = "30862-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(SCALE) < 108)
					htmltext = "30862-06.htm";
				else
				{
					htmltext = "30862-07.htm";
					st.takeItems(SCALE, -1);
					st.giveItems(REWARD[Rnd.get(REWARD.length)], 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				}
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
			if (st.dropQuestItems(SCALE, 1, 108, 700000))
				st.set("cond", "2");

		return null;
	}
}