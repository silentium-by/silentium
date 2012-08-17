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

public class Q626_ADarkTwilight extends Quest implements ScriptFile
{
	private final static String qn = "Q626_ADarkTwilight";

	// Items
	private final static int Adena = 57;
	private final static int BloodOfSaint = 7169;

	// NPC
	private final static int Hierarch = 31517;

	public Q626_ADarkTwilight(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { BloodOfSaint };

		addStartNpc(Hierarch);
		addTalkId(Hierarch);

		addKillId(21520, 21523, 21524, 21526, 21529, 21530, 21531, 21532, 21535, 21536, 21539, 21540);
	}

	public static void onLoad()
	{
		new Q626_ADarkTwilight(626, "Q626_ADarkTwilight", "A Dark Twilight");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31517-03.htm"))
		{
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("reward1"))
		{
			if (st.getQuestItemsCount(BloodOfSaint) == 300)
			{
				htmltext = "31517-07.htm";
				st.takeItems(BloodOfSaint, 300);
				st.addExpAndSp(162773, 12500);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "31517-08.htm";
		}
		else if (event.equalsIgnoreCase("reward2"))
		{
			if (st.getQuestItemsCount(BloodOfSaint) == 300)
			{
				htmltext = "31517-07.htm";
				st.takeItems(BloodOfSaint, 300);
				st.rewardItems(Adena, 100000);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
			else
				htmltext = "31517-08.htm";
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
				if (player.getLevel() >= 60 && player.getLevel() <= 71)
					htmltext = "31517-01.htm";
				else
				{
					htmltext = "31517-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				if (cond == 1 && st.getQuestItemsCount(BloodOfSaint) < 300)
					htmltext = "31517-05.htm";
				else if (cond == 2)
					htmltext = "31517-04.htm";
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
			if (st.dropAlwaysQuestItems(BloodOfSaint, 1, 300))
				st.set("cond", "2");

		return null;
	}
}