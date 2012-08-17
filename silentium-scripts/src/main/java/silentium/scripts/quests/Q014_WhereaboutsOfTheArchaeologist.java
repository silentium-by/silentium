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

public class Q014_WhereaboutsOfTheArchaeologist extends Quest
{
	private static final String qn = "Q014_WhereaboutsOfTheArchaeologist";

	// NPCs
	private static final int LIESEL = 31263;
	private static final int GHOST_OF_ADVENTURER = 31538;

	// Items
	private static final int LETTER = 7253;

	public Q014_WhereaboutsOfTheArchaeologist(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { LETTER };

		addStartNpc(LIESEL);
		addTalkId(LIESEL, GHOST_OF_ADVENTURER);
	}

	public static void main(String[] args)
	{
		new Q014_WhereaboutsOfTheArchaeologist(14, "Q014_WhereaboutsOfTheArchaeologist", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31263-2.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(LETTER, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31538-1.htm"))
		{
			if (st.getQuestItemsCount(LETTER) == 1)
			{
				st.takeItems(LETTER, 1);
				st.rewardItems(57, 113228);
				st.exitQuest(false);
				st.playSound(QuestState.SOUND_FINISH);
			}
			else
				htmltext = "<html><body>Ghost of Adventurer:<br>A letter, for me? Where did you put it?</body></html>";
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
				if (player.getLevel() < 74)
					htmltext = "31263-1.htm";
				else
				{
					htmltext = "31263-0.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case LIESEL:
						htmltext = "31263-2.htm";
						break;

					case GHOST_OF_ADVENTURER:
						if (st.getInt("cond") == 1)
							htmltext = "31538-0.htm";
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