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

public class Q009_IntoTheCityOfHumans extends Quest implements ScriptFile
{
	private static final String qn = "Q009_IntoTheCityOfHumans";

	// NPCs
	public final int PETUKAI = 30583;
	public final int TANAPI = 30571;
	public final int TAMIL = 30576;

	// Rewards
	public final int MARK_OF_TRAVELER = 7570;
	public final int SCROLL_OF_ESCAPE_GIRAN = 7126;

	public Q009_IntoTheCityOfHumans(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(PETUKAI);
		addTalkId(PETUKAI, TANAPI, TAMIL);
	}

	public static void onLoad()
	{
		new Q009_IntoTheCityOfHumans(9, "Q009_IntoTheCityOfHumans", "Into the City of Humans");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30583-01.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("30571-01.htm"))
		{
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		}
		else if (event.equalsIgnoreCase("30576-01.htm"))
		{
			st.giveItems(MARK_OF_TRAVELER, 1);
			st.rewardItems(SCROLL_OF_ESCAPE_GIRAN, 1);
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
				if ((player.getLevel() >= 3 && player.getLevel() <= 10) && player.getRace().ordinal() == 3)
					htmltext = "30583-00.htm";
				else
					htmltext = "30583-00a.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case PETUKAI:
						if (cond == 1)
							htmltext = "30583-01a.htm";
						break;

					case TANAPI:
						if (cond == 1)
							htmltext = "30571-00.htm";
						else if (cond == 2)
							htmltext = "30571-01a.htm";
						break;

					case TAMIL:
						if (cond == 2)
							htmltext = "30576-00.htm";
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