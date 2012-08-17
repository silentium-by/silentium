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

public class Q155_FindSirWindawood extends Quest implements ScriptFile
{
	private final static String qn = "Q155_FindSirWindawood";

	// Items
	private static final int OFFICIAL_LETTER = 1019;
	private static final int HASTE_POTION = 734;

	// NPCs
	private static final int ABELLOS = 30042;
	private static final int WINDAWOOD = 30311;

	public Q155_FindSirWindawood(int questId, String name, String descr)
	{
		super(questId, name, descr);

		questItemIds = new int[] { OFFICIAL_LETTER };

		addStartNpc(ABELLOS);
		addTalkId(WINDAWOOD, ABELLOS);
	}

	public static void onLoad()
	{
		new Q155_FindSirWindawood(155, "Q155_FindSirWindawood", "Find Sir Windawood");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30042-02.htm"))
		{
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(OFFICIAL_LETTER, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getLevel() >= 3 && player.getLevel() <= 6)
					htmltext = "30042-01.htm";
				else
				{
					htmltext = "30042-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId())
				{
					case ABELLOS:
						htmltext = "30042-03.htm";
						break;

					case WINDAWOOD:
						if (st.getQuestItemsCount(OFFICIAL_LETTER) == 1)
						{
							htmltext = "30311-01.htm";
							st.takeItems(OFFICIAL_LETTER, -1);
							st.rewardItems(HASTE_POTION, 1);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
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