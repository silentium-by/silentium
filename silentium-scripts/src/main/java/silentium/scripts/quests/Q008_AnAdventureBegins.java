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

public class Q008_AnAdventureBegins extends Quest implements ScriptFile
{
	private static final String qn = "Q008_AnAdventureBegins";

	// NPCs
	private final static int JASMINE = 30134;
	private final static int ROSELYN = 30355;
	private final static int HARNE = 30144;

	// Items
	private final static int ROSELYN_NOTE = 7573;

	// Rewards
	private final static int SCROLL_ESCAPE = 7559;
	private final static int MARK_TRAVELER = 7570;

	public Q008_AnAdventureBegins(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { ROSELYN_NOTE };

		addStartNpc(JASMINE);
		addTalkId(JASMINE, ROSELYN, HARNE);
	}

	public static void onLoad()
	{
		new Q008_AnAdventureBegins(8, "Q008_AnAdventureBegins", "An Adventure Begins");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30134-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30355-02.htm"))
		{
			st.set("cond", "2");
			st.giveItems(ROSELYN_NOTE, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30144-02.htm"))
		{
			st.set("cond", "3");
			st.takeItems(ROSELYN_NOTE, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30134-06.htm"))
		{
			st.giveItems(MARK_TRAVELER, 1);
			st.rewardItems(SCROLL_ESCAPE, 1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if ((player.getLevel() >= 3 && player.getLevel() <= 10) && player.getRace().ordinal() == 2)
					htmltext = "30134-02.htm";
				else
				{
					htmltext = "30134-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case JASMINE:
						if (cond == 1 || cond == 2)
							htmltext = "30134-04.htm";
						else if (cond == 3)
							htmltext = "30134-05.htm";
						break;

					case ROSELYN:
						if (st.getQuestItemsCount(ROSELYN_NOTE) == 0)
							htmltext = "30355-01.htm";
						else
							htmltext = "30355-03.htm";
						break;

					case HARNE:
						if (cond == 2 && st.getQuestItemsCount(ROSELYN_NOTE) == 1)
							htmltext = "30144-01.htm";
						else if (cond == 3)
							htmltext = "30144-03.htm";
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