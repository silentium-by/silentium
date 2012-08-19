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

public class Q013_ParcelDelivery extends Quest {
	private static final String qn = "Q013_ParcelDelivery";

	// NPCs
	private static final int FUNDIN = 31274;
	private static final int VULCAN = 31539;

	// Item
	private static final int PACKAGE = 7263;

	public Q013_ParcelDelivery(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { PACKAGE };

		addStartNpc(FUNDIN);
		addTalkId(FUNDIN, VULCAN);
	}

	public static void main(final String... args) {
		new Q013_ParcelDelivery(13, "Q013_ParcelDelivery", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31274-2.htm".equalsIgnoreCase(event)) {
			if (st.getInt("cond") == 0) {
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.giveItems(PACKAGE, 1);
				st.playSound(QuestState.SOUND_ACCEPT);
			}
		} else if ("31539-1.htm".equalsIgnoreCase(event)) {
			if (st.getInt("cond") == 1) {
				if (st.getQuestItemsCount(PACKAGE) >= 1) {
					st.takeItems(PACKAGE, 1);
					st.rewardItems(57, 82656);
					st.exitQuest(false);
					st.playSound(QuestState.SOUND_FINISH);
				} else
					htmltext = "<html><body>Flame Blacksmith Vulcan:<br>You don't have the required items.</body></html>";
			}
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
				if (player.getLevel() < 74) {
					htmltext = "31274-1.htm";
					st.exitQuest(true);
				} else
					htmltext = "31274-0.htm";
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case FUNDIN:
						htmltext = "31274-2.htm";
						break;

					case VULCAN:
						htmltext = "31539-0.htm";
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