/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q020_BringUpWithLove extends Quest
{
	private final static String qn = "Q020_BringUpWithLove";

	// Item
	private final static int JEWEL_OF_INNOCENCE = 7185;

	// Reward
	private final static int ADENA = 57;

	// NPC
	private final static int TUNATUN = 31537;

	public Q020_BringUpWithLove(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { JEWEL_OF_INNOCENCE };

		addStartNpc(TUNATUN);
		addTalkId(TUNATUN);
	}

	public static void main(String[] args)
	{
		new Q020_BringUpWithLove(20, "Q020_BringUpWithLove", "Bring Up With Love");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31537-09.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31537-12.htm"))
		{
			st.takeItems(JEWEL_OF_INNOCENCE, -1);
			st.rewardItems(ADENA, 68500);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getLevel() >= 65)
					htmltext = "31537-01.htm";
				else
					htmltext = "31537-02.htm";
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(JEWEL_OF_INNOCENCE) >= 1)
					htmltext = "31537-11.htm";
				else
					htmltext = "31537-10.htm";
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}