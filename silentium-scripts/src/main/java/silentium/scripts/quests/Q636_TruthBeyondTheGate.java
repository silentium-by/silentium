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

public class Q636_TruthBeyondTheGate extends Quest
{
	private final static String qn = "Q636_TruthBeyondTheGate";

	// NPCs
	private static final int ELIYAH = 31329;
	private static final int FLAURON = 32010;

	// Reward
	private static final int MARK = 8064;

	public Q636_TruthBeyondTheGate(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(ELIYAH);
		addTalkId(ELIYAH, FLAURON);
	}

	public static void main(String[] args)
	{
		new Q636_TruthBeyondTheGate(636, "Q636_TruthBeyondTheGate", "The Truth Beyond the Gate");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31329-04.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("32010-02.htm"))
		{
			st.giveItems(MARK, 1);
			st.exitQuest(false);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getLevel() > 72)
					htmltext = "31329-02.htm";
				else
				{
					htmltext = "31329-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case ELIYAH:
						htmltext = "31329-05.htm";
						break;

					case FLAURON:
						if (st.hasQuestItems(MARK))
							htmltext = "32010-03.htm";
						else
							htmltext = "32010-01.htm";
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