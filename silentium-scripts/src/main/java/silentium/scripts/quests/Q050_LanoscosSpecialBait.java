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
import silentium.gameserver.scripting.ScriptFile;

public class Q050_LanoscosSpecialBait extends Quest implements ScriptFile
{
	private static final String qn = "Q050_LanoscosSpecialBait";

	// NPC
	private final static int LANOSCO = 31570;

	// Item
	private final static int ESSENCE_OF_WIND = 7621;

	// Reward
	private final static int WIND_FISHING_LURE = 7610;

	// Monster
	private final static int SINGING_WIND = 21026;

	public Q050_LanoscosSpecialBait(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { ESSENCE_OF_WIND };

		addStartNpc(LANOSCO);
		addTalkId(LANOSCO);

		addKillId(SINGING_WIND);
	}

	public static void onLoad()
	{
		new Q050_LanoscosSpecialBait(50, "Q050_LanoscosSpecialBait", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31570-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31570-07.htm") && st.getQuestItemsCount(ESSENCE_OF_WIND) == 100)
		{
			htmltext = "31570-06.htm";
			st.rewardItems(WIND_FISHING_LURE, 4);
			st.takeItems(ESSENCE_OF_WIND, 100);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 27 && player.getLevel() <= 29)
					htmltext = "31570-01.htm";
				else
				{
					htmltext = "31570-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(ESSENCE_OF_WIND) == 100)
					htmltext = "31570-04.htm";
				else
					htmltext = "31570-05.htm";
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
			if (st.dropQuestItems(ESSENCE_OF_WIND, 1, 100, 300000))
				st.set("cond", "2");

		return null;
	}
}