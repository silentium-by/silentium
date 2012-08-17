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

public class Q112_WalkOfFate extends Quest implements ScriptFile
{
	private static final String qn = "Q112_WalkOfFate";

	// NPCs
	private final static int LIVINA = 30572;
	private final static int KARUDA = 32017;

	// Rewards
	private final static int ADENA = 57;
	private final static int ENCHANT_D = 956;

	public Q112_WalkOfFate(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(LIVINA);
		addTalkId(LIVINA, KARUDA);
	}

	public static void onLoad()
	{
		new Q112_WalkOfFate(112, "Q112_WalkOfFate", "Walk of Fate");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30572-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32017-02.htm"))
		{
			st.rewardItems(ADENA, 4665);
			st.giveItems(ENCHANT_D, 1);
			st.exitQuest(false);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getLevel() >= 20 && player.getLevel() <= 36)
					htmltext = "30572-01.htm";
				else
				{
					htmltext = "30572-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case LIVINA:
						htmltext = "30572-03.htm";
						break;

					case KARUDA:
						htmltext = "32017-01.htm";
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