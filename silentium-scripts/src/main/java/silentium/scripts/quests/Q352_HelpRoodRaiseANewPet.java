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

public class Q352_HelpRoodRaiseANewPet extends Quest implements ScriptFile
{
	private static final String qn = "Q352_HelpRoodRaiseANewPet";

	// NPCs
	private static final int ROOD = 31067;

	// Items
	private static final int LIENRIK_EGG_1 = 5860;
	private static final int LIENRIK_EGG_2 = 5861;

	public Q352_HelpRoodRaiseANewPet(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { LIENRIK_EGG_1, LIENRIK_EGG_2 };

		addStartNpc(ROOD);
		addTalkId(ROOD);

		addKillId(20786, 20787, 21644, 21645);
	}

	public static void onLoad()
	{
		new Q352_HelpRoodRaiseANewPet(352, "Q352_HelpRoodRaiseANewPet", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31067-04.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31067-09.htm"))
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
				if (player.getLevel() >= 39 && player.getLevel() <= 44)
					htmltext = "31067-01.htm";
				else
				{
					htmltext = "31067-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int eggs1 = st.getQuestItemsCount(LIENRIK_EGG_1);
				int eggs2 = st.getQuestItemsCount(LIENRIK_EGG_2);

				if (eggs1 + eggs2 == 0)
					htmltext = "31067-05.htm";
				else
				{
					int reward = 2000;
					if (eggs1 > 0 && eggs2 == 0)
					{
						htmltext = "31067-06.htm";
						reward += eggs1 * 34;

						st.takeItems(LIENRIK_EGG_1, -1);
						st.rewardItems(57, reward);
					}
					else if (eggs1 == 0 && eggs2 > 0)
					{
						htmltext = "31067-08.htm";
						reward += eggs2 * 1025;

						st.takeItems(LIENRIK_EGG_2, -1);
						st.rewardItems(57, reward);
					}
					else if (eggs1 > 0 && eggs2 > 0)
					{
						htmltext = "31067-08.htm";
						reward += (eggs1 * 34) + (eggs2 * 1025) + 2000;

						st.takeItems(LIENRIK_EGG_1, -1);
						st.takeItems(LIENRIK_EGG_2, -1);
						st.rewardItems(57, reward);
					}
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
			st.giveItems((Rnd.get(100) < 3) ? LIENRIK_EGG_2 : LIENRIK_EGG_1, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}