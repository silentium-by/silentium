/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q329_CuriosityOfADwarf extends Quest
{
	private static final String qn = "Q329_CuriosityOfADwarf";

	// NPC
	private static final int Rolento = 30437;

	// Items
	private static final int Golem_Heartstone = 1346;
	private static final int Broken_Heartstone = 1365;

	public Q329_CuriosityOfADwarf(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(Rolento);
		addTalkId(Rolento);

		addKillId(20083, 20085); // Granite golem, Puncher
	}

	public static void main(String[] args)
	{
		new Q329_CuriosityOfADwarf(329, "Q329_CuriosityOfADwarf", "Curiosity of a Dwarf");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30437-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30437-06.htm"))
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
				if (player.getLevel() >= 33 && player.getLevel() <= 38)
					htmltext = "30437-02.htm";
				else
				{
					htmltext = "30437-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int golem = st.getQuestItemsCount(Golem_Heartstone);
				int broken = st.getQuestItemsCount(Broken_Heartstone);

				if (golem + broken == 0)
					htmltext = "30437-04.htm";
				else
				{
					htmltext = "30437-05.htm";
					st.takeItems(Golem_Heartstone, -1);
					st.takeItems(Broken_Heartstone, -1);
					st.rewardItems(57, broken * 50 + golem * 1000 + ((golem + broken > 10) ? 1183 : 0));
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

		if (st.isStarted())
		{
			int chance = Rnd.get(100);
			if (chance < 15)
			{
				st.giveItems(Golem_Heartstone, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
			else if (chance < 65)
			{
				st.giveItems(Broken_Heartstone, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}

		return null;
	}
}