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

public class Q316_DestroyPlagueCarriers extends Quest
{
	private static final String qn = "Q316_DestroyPlagueCarriers";

	// Items
	private static final int Wererat_Fang = 1042;
	private static final int Varool_Foulclaws_Fang = 1043;

	// Monsters
	private static final int Sukar_Wererat = 20040;
	private static final int Sukar_Wererat_Leader = 20047;
	private static final int Varool_Foulclaw = 27020;

	public Q316_DestroyPlagueCarriers(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { Wererat_Fang, Varool_Foulclaws_Fang };

		addStartNpc(30155); // Ellenia
		addTalkId(30155);

		addKillId(Sukar_Wererat, Sukar_Wererat_Leader, Varool_Foulclaw);
	}

	public static void main(String[] args)
	{
		new Q316_DestroyPlagueCarriers(316, "Q316_DestroyPlagueCarriers", "Destroy Plague Carriers");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30155-04.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30155-08.htm"))
		{
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (player.getRace().ordinal() != 1)
				{
					htmltext = "30155-00.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 18 && player.getLevel() <= 24)
					htmltext = "30155-03.htm";
				else
				{
					htmltext = "30155-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int rats = st.getQuestItemsCount(Wererat_Fang);
				int varool = st.getQuestItemsCount(Varool_Foulclaws_Fang);

				if (rats + varool == 0)
					htmltext = "30155-05.htm";
				else
				{
					htmltext = "30155-07.htm";
					st.takeItems(Wererat_Fang, -1);
					st.takeItems(Varool_Foulclaws_Fang, -1);
					st.rewardItems(57, rats * 30 + varool * 10000 + ((rats > 10) ? 5000 : 0));
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
			switch (npc.getNpcId())
			{
				case Sukar_Wererat:
				case Sukar_Wererat_Leader:
					if (Rnd.get(10) < 6)
					{
						st.giveItems(Wererat_Fang, 1);
						st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case Varool_Foulclaw:
					if (st.getQuestItemsCount(Varool_Foulclaws_Fang) == 0 && Rnd.get(10) == 0)
					{
						st.giveItems(Varool_Foulclaws_Fang, 1);
						st.playSound(QuestState.SOUND_MIDDLE);
					}
					break;
			}
		}

		return null;
	}
}