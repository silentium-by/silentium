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

public class Q262_TradeWithTheIvoryTower extends Quest implements ScriptFile
{
	private final static String qn = "Q262_TradeWithTheIvoryTower";

	// NPC
	private static final int Vollodos = 30137;

	// Item
	private static final int FUNGUS_SAC = 707;

	public Q262_TradeWithTheIvoryTower(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { FUNGUS_SAC };

		addStartNpc(Vollodos);
		addTalkId(Vollodos);

		addKillId(20400, 20007);
	}

	public static void onLoad()
	{
		new Q262_TradeWithTheIvoryTower(262, "Q262_TradeWithTheIvoryTower", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30137-03.htm"))
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
				if (player.getLevel() >= 8 && player.getLevel() <= 16)
					htmltext = "30137-02.htm";
				else
				{
					htmltext = "30137-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(FUNGUS_SAC) < 10)
					htmltext = "30137-04.htm";
				else
				{
					htmltext = "30137-05.htm";
					st.takeItems(FUNGUS_SAC, -1);
					st.rewardItems(57, 3000);
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
		{
			if (Rnd.get(10) < ((npc.getNpcId() == 20400) ? 4 : 3))
			{
				st.giveItems(FUNGUS_SAC, 1);

				if (st.getQuestItemsCount(FUNGUS_SAC) < 10)
					st.playSound(QuestState.SOUND_ITEMGET);
				else
				{
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}