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

public class Q109_InSearchOfTheNest extends Quest implements ScriptFile {
	private static final String qn = "Q109_InSearchOfTheNest";

	// NPCs
	private static final int PIERCE = 31553;
	private static final int KAHMAN = 31554;
	private static final int SCOUT_CORPSE = 32015;

	// Items
	private static final int SCOUT_MEMO = 8083;
	private static final int RECRUIT_BADGE = 7246;
	private static final int SOLDIER_BADGE = 7247;

	// Reward
	private static final int ADENA = 57;

	public Q109_InSearchOfTheNest(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { SCOUT_MEMO };

		addStartNpc(PIERCE);
		addTalkId(PIERCE, SCOUT_CORPSE, KAHMAN);
	}

	public static void onLoad() {
		new Q109_InSearchOfTheNest(109, "Q109_InSearchOfTheNest", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31553-01.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32015-02.htm".equalsIgnoreCase(event)) {
			st.giveItems(SCOUT_MEMO, 1);
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31553-03.htm".equalsIgnoreCase(event)) {
			st.takeItems(SCOUT_MEMO, 1);
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31554-02.htm".equalsIgnoreCase(event)) {
			st.rewardItems(ADENA, 5168);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		final QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				// Must worn one or other Golden Ram Badge in order to be accepted.
				if (player.getLevel() >= 66 && (st.getQuestItemsCount(RECRUIT_BADGE) > 0 || st.getQuestItemsCount(SOLDIER_BADGE) > 0))
					htmltext = "31553-00.htm";
				else {
					htmltext = "31553-00a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case PIERCE:
						if (cond == 1)
							htmltext = "31553-01a.htm";
						else if (cond == 2)
							htmltext = "31553-02.htm";
						else if (cond == 3)
							htmltext = "31553-03.htm";
						break;

					case SCOUT_CORPSE:
						if (cond == 1)
							htmltext = "32015-01.htm";
						else if (cond == 2)
							htmltext = "32015-02.htm";
						break;

					case KAHMAN:
						if (cond == 3)
							htmltext = "31554-01.htm";
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