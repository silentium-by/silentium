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

public class Q018_MeetingWithTheGoldenRam extends Quest
{
	private final static String qn = "Q018_MeetingWithTheGoldenRam";

	// Items
	private final static int Adena = 57;
	private final static int SupplyBox = 7245;

	// NPCs
	private final static int Donal = 31314;
	private final static int Daisy = 31315;
	private final static int Abercrombie = 31555;

	public Q018_MeetingWithTheGoldenRam(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { SupplyBox };

		addStartNpc(Donal);
		addTalkId(Donal, Daisy, Abercrombie);
	}

	public static void main(String[] args)
	{
		new Q018_MeetingWithTheGoldenRam(18, "Q018_MeetingWithTheGoldenRam", "Meeting with the Golden Ram");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31314-03.htm"))
		{
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31315-02.htm"))
		{
			st.set("cond", "2");
			st.giveItems(SupplyBox, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("31555-02.htm"))
		{
			st.takeItems(SupplyBox, 1);
			st.rewardItems(Adena, 15000);
			st.addExpAndSp(50000, 0);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 66 && player.getLevel() <= 76)
					htmltext = "31314-01.htm";
				else
				{
					htmltext = "31314-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case Donal:
						htmltext = "31314-04.htm";
						break;

					case Daisy:
						if (cond == 1)
							htmltext = "31315-01.htm";
						else if (cond == 2)
							htmltext = "31315-03.htm";
						break;

					case Abercrombie:
						if (cond == 2)
							htmltext = "31555-01.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}