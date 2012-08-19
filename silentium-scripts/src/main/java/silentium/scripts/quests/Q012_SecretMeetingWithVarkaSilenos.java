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

public class Q012_SecretMeetingWithVarkaSilenos extends Quest {
	private static final String qn = "Q012_SecretMeetingWithVarkaSilenos";

	// NPCs
	private static final int CADMON = 31296;
	private static final int HELMUT = 31258;
	private static final int NARAN_ASHANUK = 31378;

	// Items
	private static final int MUNITIONS_BOX = 7232;

	public Q012_SecretMeetingWithVarkaSilenos(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { MUNITIONS_BOX };

		addStartNpc(CADMON);
		addTalkId(CADMON, HELMUT, NARAN_ASHANUK);
	}

	public static void main(final String... args) {
		new Q012_SecretMeetingWithVarkaSilenos(12, "Q012_SecretMeetingWithVarkaSilenos", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31296-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31258-02.htm".equalsIgnoreCase(event)) {
			st.giveItems(MUNITIONS_BOX, 1);
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31378-02.htm".equalsIgnoreCase(event)) {
			st.takeItems(MUNITIONS_BOX, 1);
			st.addExpAndSp(79761, 0);
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
				if (player.getLevel() >= 74)
					htmltext = "31296-01.htm";
				else {
					htmltext = "31296-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case CADMON:
						if (cond == 1)
							htmltext = "31296-04.htm";
						break;

					case HELMUT:
						if (cond == 1)
							htmltext = "31258-01.htm";
						else if (cond == 2)
							htmltext = "31258-03.htm";
						break;

					case NARAN_ASHANUK:
						if (cond == 2 && st.getQuestItemsCount(MUNITIONS_BOX) > 0)
							htmltext = "31378-01.htm";
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