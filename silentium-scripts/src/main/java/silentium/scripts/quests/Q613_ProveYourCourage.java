/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;

public class Q613_ProveYourCourage extends Quest
{
	private static final String qn = "Q613_ProveYourCourage";

	// Items
	private static final int Hekaton_Head = 7240;
	private static final int Valor_Feather = 7229;
	private static final int Varka_Alliance_Three = 7223;

	public Q613_ProveYourCourage(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { Hekaton_Head };

		addStartNpc(31377); // Ashas Varka Durai
		addTalkId(31377);

		addKillId(25299); // Hekaton
	}

	public static void main(String[] args)
	{
		new Q613_ProveYourCourage(613, "Q613_ProveYourCourage", "Prove your courage!");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31377-04.htm"))
		{
			if (player.getAllianceWithVarkaKetra() <= -3 && st.getQuestItemsCount(Varka_Alliance_Three) > 0 && st.getQuestItemsCount(Valor_Feather) == 0)
			{
				if (player.getLevel() >= 75)
				{
					st.set("cond", "1");
					st.setState(QuestState.STARTED);
					st.playSound(QuestState.SOUND_ACCEPT);
				}
				else
				{
					htmltext = "31377-03.htm";
					st.exitQuest(true);
				}
			}
			else
			{
				htmltext = "31377-02.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("31377-07.htm"))
		{
			if (st.getQuestItemsCount(Hekaton_Head) == 1)
			{
				st.takeItems(Hekaton_Head, -1);
				st.giveItems(Valor_Feather, 1);
				st.addExpAndSp(10000, 0);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
			{
				htmltext = "31377-06.htm";
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
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
				htmltext = "31377-01.htm";
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(Hekaton_Head) == 1)
					htmltext = "31377-05.htm";
				else
					htmltext = "31377-06.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		final L2Party party = player.getParty();
		if (party != null)
		{
			for (L2PcInstance partyMember : party.getPartyMembers())
			{
				if (partyMember != null)
					rewardPlayer(partyMember);
			}
		}
		else
			rewardPlayer(player);

		return null;
	}

	private static void rewardPlayer(L2PcInstance player)
	{
		if (player.getAllianceWithVarkaKetra() <= -3)
		{
			QuestState st = player.getQuestState(qn);
			if (st.getInt("cond") == 1 && st.getQuestItemsCount(Varka_Alliance_Three) > 0)
			{
				st.set("cond", "2");
				st.giveItems(Hekaton_Head, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
	}
}