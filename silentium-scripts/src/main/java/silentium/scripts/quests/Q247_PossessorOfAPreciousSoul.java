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

public class Q247_PossessorOfAPreciousSoul extends Quest implements ScriptFile
{
	private static final String qn = "Q247_PossessorOfAPreciousSoul";

	// NPCs
	private static final int CARADINE = 31740;
	private static final int LADY_OF_THE_LAKE = 31745;

	// Items
	private static final int CARADINE_LETTER_3 = 7679;
	private static final int NOBLESS_TIARA = 7694;

	public Q247_PossessorOfAPreciousSoul(int questId, String name, String descr)
	{
		super(questId, name, descr);

		addStartNpc(CARADINE);
		addTalkId(CARADINE, LADY_OF_THE_LAKE);
	}

	public static void onLoad()
	{
		new Q247_PossessorOfAPreciousSoul(247, "Q247_PossessorOfAPreciousSoul", "quests");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		// Caradine
		if (event.equalsIgnoreCase("31740-03.htm"))
		{
			st.set("cond", "1");
			st.takeItems(CARADINE_LETTER_3, 1);
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		}
		else if (event.equalsIgnoreCase("31740-05.htm"))
		{
			st.set("cond", "2");
			player.teleToLocation(143209, 43968, -3038);
		}
		// Lady of the lake
		else if (event.equalsIgnoreCase("31745-05.htm"))
		{
			player.setNoble(true, true);
			st.addExpAndSp(93836, 0);
			st.giveItems(NOBLESS_TIARA, 1);
			st.playSound(QuestState.SOUND_FANFARE);
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
				if (st.hasQuestItems(CARADINE_LETTER_3))
				{
					if (!player.isSubClassActive() || player.getLevel() < 75)
					{
						htmltext = "31740-02.htm";
						st.exitQuest(true);
					}
					else
						htmltext = "31740-01.htm";
				}
				break;

			case QuestState.STARTED:
				if (!player.isSubClassActive())
					break;

				int cond = st.getInt("cond");
				switch (npc.getNpcId())
				{
					case CARADINE:
						if (cond == 1)
							htmltext = "31740-04.htm";
						else if (cond == 2)
							htmltext = "31740-06.htm";
						break;

					case LADY_OF_THE_LAKE:
						if (cond == 2)
						{
							if (player.getLevel() < 75)
								htmltext = "31745-06.htm";
							else
								htmltext = "31745-01.htm";
						}
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = getAlreadyCompletedMsg();
				break;
		}
		return htmltext;
	}
}