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

public class Q020_BringUpWithLove extends Quest {
	private static final String qn = "Q020_BringUpWithLove";

	// Item
	private static final int JEWEL_OF_INNOCENCE = 7185;

	// Reward
	private static final int ADENA = 57;

	// NPC
	private static final int TUNATUN = 31537;

	public Q020_BringUpWithLove(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { JEWEL_OF_INNOCENCE };

		addStartNpc(TUNATUN);
		addTalkId(TUNATUN);
	}

	public static void main(final String... args) {
		new Q020_BringUpWithLove(20, "Q020_BringUpWithLove", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31537-09.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31537-12.htm".equalsIgnoreCase(event)) {
			st.takeItems(JEWEL_OF_INNOCENCE, -1);
			st.rewardItems(ADENA, 68500);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				htmltext = player.getLevel() >= 65 ? "31537-01.htm" : "31537-02.htm";
				break;

			case QuestState.STARTED:
				htmltext = st.getQuestItemsCount(JEWEL_OF_INNOCENCE) >= 1 ? "31537-11.htm" : "31537-10.htm";
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}