/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q368_TrespassingIntoTheSacredArea extends Quest implements ScriptFile
{
	private static final String qn = "Q368_TrespassingIntoTheSacredArea";

	// NPC
	private static final int RESTINA = 30926;

	// Item
	private static final int FANG = 5881;

	public Q368_TrespassingIntoTheSacredArea(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { FANG };

		addStartNpc(RESTINA);
		addTalkId(RESTINA);
		addKillId(20794, 20795, 20796, 20797);
	}

	public static void onLoad()
	{
		new Q368_TrespassingIntoTheSacredArea(368, "Q368_TrespassingIntoTheSacredArea", "Trespassing into the Sacred Area");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30926-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30926-05.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getLevel() >= 36 && player.getLevel() <= 48)
					htmltext = "30926-01.htm";
				else
				{
					htmltext = "30926-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int fangs = st.getQuestItemsCount(FANG);
				if (fangs == 0)
					htmltext = "30926-03.htm";
				else
				{
					int reward = 250 * fangs + (fangs > 10 ? 5730 : 2000);
					htmltext = "30926-04.htm";
					st.takeItems(5881, -1);
					st.rewardItems(57, reward);
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		int chance = (int) (33 * MainConfig.RATE_QUEST_DROP);
		int numItems = chance / 100;
		chance = chance % 100;

		if (Rnd.get(100) < chance)
			numItems++;

		if (numItems > 0)
		{
			st.giveItems(FANG, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}