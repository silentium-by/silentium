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

public class Q113_StatusOfTheBeaconTower extends Quest implements ScriptFile
{
	private static final String qn = "Q113_StatusOfTheBeaconTower";

	// NPCs
	private static final int MOIRA = 31979;
	private static final int TORRANT = 32016;

	// Item
	private static final int BOX = 8086;

	public Q113_StatusOfTheBeaconTower(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { BOX };

		addStartNpc(MOIRA);
		addTalkId(MOIRA, TORRANT);
	}

	public static void onLoad()
	{
		new Q113_StatusOfTheBeaconTower(113, "Q113_StatusOfTheBeaconTower", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31979-02.htm"))
		{
			st.set("cond", "1");
			st.giveItems(BOX, 1);
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32016-02.htm"))
		{
			st.takeItems(BOX, 1);
			st.rewardItems(57, 21578);
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
				if (player.getLevel() >= 40 && player.getLevel() <= 51)
					htmltext = "31979-01.htm";
				else
				{
					htmltext = "31979-00.htm";
					st.exitQuest(false);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case MOIRA:
						htmltext = "31979-03.htm";
						break;

					case TORRANT:
						if (st.getQuestItemsCount(BOX) == 1)
							htmltext = "32016-01.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}