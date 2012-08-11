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

public class Q649_ALooterAndARailroadMan extends Quest
{
	private final static String qn = "Q649_ALooterAndARailroadMan";

	// Item
	private static final int THIEF_GUILD_MARK = 8099;

	// NPC
	private static final int OBI = 32052;

	public Q649_ALooterAndARailroadMan(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { THIEF_GUILD_MARK };

		addStartNpc(OBI);
		addTalkId(OBI);

		addKillId(22017, 22018, 22019, 22021, 22022, 22023, 22024, 22026);
	}

	public static void main(String[] args)
	{
		new Q649_ALooterAndARailroadMan(649, "Q649_ALooterAndARailroadMan", "A Looter and a Railroad Man");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("32052-1.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32052-3.htm"))
		{
			if (st.getQuestItemsCount(THIEF_GUILD_MARK) < 200)
				htmltext = "32052-3a.htm";
			else
			{
				st.takeItems(THIEF_GUILD_MARK, -1);
				st.rewardItems(57, 21698);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
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
				if (player.getLevel() >= 30)
					htmltext = "32052-0.htm";
				else
				{
					htmltext = "32052-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(THIEF_GUILD_MARK) == 200)
					htmltext = "32052-2.htm";
				else
					htmltext = "32052-2a.htm";
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
			if (st.dropQuestItems(THIEF_GUILD_MARK, 1, 200, 800000))
				st.set("cond", "2");

		return null;
	}
}