/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import java.util.HashMap;
import java.util.Map;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q004_LongliveThePaagrioLord extends Quest implements ScriptFile
{
	private final static String qn = "Q004_LongliveThePaagrioLord";

	private final static Map<Integer, Integer> npc_gifts = new HashMap<>();

	{
		npc_gifts.put(30585, 1542);
		npc_gifts.put(30566, 1541);
		npc_gifts.put(30562, 1543);
		npc_gifts.put(30560, 1544);
		npc_gifts.put(30559, 1545);
		npc_gifts.put(30587, 1546);
	}

	public Q004_LongliveThePaagrioLord(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { 1541, 1542, 1543, 1544, 1545, 1546 };

		addStartNpc(30578);
		addTalkId(30578, 30585, 30566, 30562, 30560, 30559, 30587);
	}

	public static void onLoad()
	{
		new Q004_LongliveThePaagrioLord(4, "Q004_LongliveThePaagrioLord", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30578-03.htm"))
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
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState())
		{
			case QuestState.CREATED:
				if (player.getRace().ordinal() != 3)
				{
					htmltext = "30578-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 2 && player.getLevel() <= 5)
					htmltext = "30578-02.htm";
				else
				{
					htmltext = "30578-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				int npcId = npc.getNpcId();
				if (npcId == 30578)
				{
					if (cond == 1)
						htmltext = "30578-04.htm";
					else if (cond == 2)
					{
						htmltext = "30578-06.htm";
						st.giveItems(4, 1);
						for (int item : npc_gifts.values())
							st.takeItems(item, -1);

						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(false);
					}
				}
				else
				{
					int i = npc_gifts.get(npcId);
					if (st.getQuestItemsCount(i) >= 1)
						htmltext = npcId + "-02.htm";
					else
					{
						st.giveItems(i, 1);
						htmltext = npcId + "-01.htm";

						int count = 0;
						for (int item : npc_gifts.values())
							count += st.getQuestItemsCount(item);

						if (count == 6)
						{
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						}
						else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}