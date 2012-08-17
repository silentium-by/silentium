/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q607_ProveYourCourage extends Quest implements ScriptFile
{
	private static final String qn = "Q607_ProveYourCourage";

	// Items
	private static final int Shadith_Head = 7235;
	private static final int Valor_Totem = 7219;
	private static final int Ketra_Alliance_Three = 7213;

	public Q607_ProveYourCourage(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { Shadith_Head };

		addStartNpc(31370); // Kadun Zu Ketra
		addTalkId(31370);

		addKillId(25309); // Shadith
	}

	public static void onLoad()
	{
		new Q607_ProveYourCourage(607, "Q607_ProveYourCourage", "Prove your courage!");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31370-04.htm"))
		{
			if (player.getAllianceWithVarkaKetra() >= 3 && st.getQuestItemsCount(Ketra_Alliance_Three) > 0 && st.getQuestItemsCount(Valor_Totem) == 0)
			{
				if (player.getLevel() >= 75)
				{
					st.set("cond", "1");
					st.setState(QuestState.STARTED);
					st.playSound(QuestState.SOUND_ACCEPT);
				}
				else
				{
					htmltext = "31370-03.htm";
					st.exitQuest(true);
				}
			}
			else
			{
				htmltext = "31370-02.htm";
				st.exitQuest(true);
			}
		}
		else if (event.equalsIgnoreCase("31370-07.htm"))
		{
			if (st.getQuestItemsCount(Shadith_Head) == 1)
			{
				st.takeItems(Shadith_Head, -1);
				st.giveItems(Valor_Totem, 1);
				st.addExpAndSp(10000, 0);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
			else
			{
				htmltext = "31370-06.htm";
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
				htmltext = "31370-01.htm";
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(Shadith_Head) == 1)
					htmltext = "31370-05.htm";
				else
					htmltext = "31370-06.htm";
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
		if (player.getAllianceWithVarkaKetra() >= 3)
		{
			QuestState st = player.getQuestState(qn);
			if (st.getInt("cond") == 1 && st.getQuestItemsCount(Ketra_Alliance_Three) > 0)
			{
				st.set("cond", "2");
				st.giveItems(Shadith_Head, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
	}
}