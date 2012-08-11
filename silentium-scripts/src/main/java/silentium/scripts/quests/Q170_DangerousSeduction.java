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

public class Q170_DangerousSeduction extends Quest
{
	private final static String qn = "Q170_DangerousSeduction";

	// Item
	private static final int NIGHTMARE_CRYSTAL = 1046;

	// NPC
	private static final int VELLIOR = 30305;

	// Mob
	private static final int MERKENIS = 27022;

	public Q170_DangerousSeduction(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { NIGHTMARE_CRYSTAL };

		addStartNpc(VELLIOR);
		addTalkId(VELLIOR);

		addKillId(MERKENIS);
	}

	public static void main(String[] args)
	{
		new Q170_DangerousSeduction(170, "Q170_DangerousSeduction", "Dangerous Seduction");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30305-04.htm"))
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
				if (player.getRace().ordinal() == 2)
				{
					if (player.getLevel() >= 21 && player.getLevel() <= 26)
						htmltext = "30305-03.htm";
					else
					{
						htmltext = "30305-02.htm";
						st.exitQuest(true);
					}
				}
				else
				{
					htmltext = "30305-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(NIGHTMARE_CRYSTAL) > 0)
				{
					htmltext = "30305-06.htm";
					st.takeItems(NIGHTMARE_CRYSTAL, -1);
					st.rewardItems(57, 102680);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(false);
				}
				else
					htmltext = "30305-05.htm";
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
			st.set("cond", "2");
			st.giveItems(NIGHTMARE_CRYSTAL, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}

		return null;
	}
}