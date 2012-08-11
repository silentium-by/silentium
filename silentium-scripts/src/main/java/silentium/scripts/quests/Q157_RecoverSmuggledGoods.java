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

public class Q157_RecoverSmuggledGoods extends Quest
{
	private static final String qn = "Q157_RecoverSmuggledGoods";

	// NPC
	private final static int WILFORD = 30005;

	// Monster
	private final static int TOAD = 20121;

	// Item
	private final static int ADAMANTITE_ORE = 1024;

	// Reward
	private final static int BUCKLER = 20;

	public Q157_RecoverSmuggledGoods(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { ADAMANTITE_ORE };

		addStartNpc(WILFORD);
		addTalkId(WILFORD);

		addKillId(TOAD);
	}

	public static void main(String[] args)
	{
		new Q157_RecoverSmuggledGoods(157, "Q157_RecoverSmuggledGoods", "Recover Smuggled Goods");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30005-05.htm"))
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
				if (player.getLevel() >= 5 && player.getLevel() <= 9)
					htmltext = "30005-03.htm";
				else
				{
					htmltext = "30005-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1 && st.getQuestItemsCount(ADAMANTITE_ORE) < 20)
					htmltext = "30005-06.htm";
				else if (cond == 2 && st.getQuestItemsCount(ADAMANTITE_ORE) >= 20)
				{
					htmltext = "30005-07.htm";
					st.takeItems(ADAMANTITE_ORE, 20);
					st.giveItems(BUCKLER, 1);
					st.exitQuest(false);
					st.playSound(QuestState.SOUND_FINISH);
				}
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
			if (st.dropAlwaysQuestItems(ADAMANTITE_ORE, 1, 20))
				st.set("cond", "2");

		return null;
	}
}