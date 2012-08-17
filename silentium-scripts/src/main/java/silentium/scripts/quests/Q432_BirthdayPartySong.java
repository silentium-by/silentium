/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q432_BirthdayPartySong extends Quest implements ScriptFile
{
	private static final String qn = "Q432_BirthdayPartySong";

	// NPC
	private static final int OCTAVIA = 31043;

	// Item
	private static final int RED_CRYSTAL = 7541;

	public Q432_BirthdayPartySong(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { RED_CRYSTAL };

		addStartNpc(OCTAVIA);
		addTalkId(OCTAVIA);

		addKillId(21103);
	}

	public static void onLoad()
	{
		new Q432_BirthdayPartySong(432, "Q432_BirthdayPartySong", "Birthday Party Song");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31043-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31043-06.htm"))
		{
			if (st.getQuestItemsCount(RED_CRYSTAL) == 50)
			{
				htmltext = "31043-05.htm";
				st.takeItems(RED_CRYSTAL, -1);
				st.rewardItems(7061, 25);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			}
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
				if (player.getLevel() >= 31 && player.getLevel() <= 36)
					htmltext = "31043-01.htm";
				else
				{
					htmltext = "31043-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(RED_CRYSTAL) < 50)
					htmltext = "31043-03.htm";
				else
					htmltext = "31043-04.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet)
	{
		L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;

		QuestState st = partyMember.getQuestState(qn);

		if (st.dropQuestItems(RED_CRYSTAL, 1, 50, 330000))
			st.set("cond", "2");

		return null;
	}
}