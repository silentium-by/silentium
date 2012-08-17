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

public class Q006_StepIntoTheFuture extends Quest implements ScriptFile
{
	private static final String qn = "Q006_StepIntoTheFuture";

	// NPCs
	private final static int ROXXY = 30006;
	private final static int BAULRO = 30033;
	private final static int SIR_COLLIN = 30311;

	// Items
	private final static int BAULRO_LETTER = 7571;

	// Rewards
	private final static int MARK_TRAVELER = 7570;
	private final static int SCROLL_GIRAN = 7559;

	public Q006_StepIntoTheFuture(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { BAULRO_LETTER };

		addStartNpc(ROXXY);
		addTalkId(ROXXY, BAULRO, SIR_COLLIN);
	}

	public static void onLoad()
	{
		new Q006_StepIntoTheFuture(6, "Q006_StepIntoTheFuture", "Step into the Future");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30006-03.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30033-02.htm"))
		{
			st.set("cond", "2");
			st.giveItems(BAULRO_LETTER, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30311-02.htm"))
		{
			st.set("cond", "3");
			st.takeItems(BAULRO_LETTER, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30006-06.htm"))
		{
			st.giveItems(MARK_TRAVELER, 1);
			st.rewardItems(SCROLL_GIRAN, 1);
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
				if (player.getRace().ordinal() != 0)
				{
					htmltext = "30006-01.htm";
					st.exitQuest(true);
				}
				else if (player.getLevel() >= 3 && player.getLevel() <= 10)
					htmltext = "30006-02.htm";
				else
				{
					htmltext = "30006-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case ROXXY:
						if (cond == 1 || cond == 2)
							htmltext = "30006-04.htm";
						else if (cond == 3)
							htmltext = "30006-05.htm";
						break;

					case BAULRO:
						if (cond == 1)
							htmltext = "30033-01.htm";
						else if (cond == 2 && st.getQuestItemsCount(BAULRO_LETTER) == 1)
							htmltext = "30033-03.htm";
						else
							htmltext = "30033-04.htm";
						break;

					case SIR_COLLIN:
						if (cond < 3 && st.getQuestItemsCount(BAULRO_LETTER) == 0)
							htmltext = "30311-03.htm";
						if (cond == 2 && st.getQuestItemsCount(BAULRO_LETTER) == 1)
							htmltext = "30311-01.htm";
						else
							htmltext = "30311-03a.htm";
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