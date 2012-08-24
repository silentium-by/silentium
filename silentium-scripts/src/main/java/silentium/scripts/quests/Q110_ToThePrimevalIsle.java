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

public class Q110_ToThePrimevalIsle extends Quest implements ScriptFile {
	private static final String qn = "Q110_ToThePrimevalIsle";

	// NPCs
	private static final int ANTON = 31338;
	private static final int MARQUEZ = 32113;

	// Item
	private static final int ANCIENT_BOOK = 8777;

	// Reward
	private static final int ADENA = 57;

	public Q110_ToThePrimevalIsle(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ANCIENT_BOOK };

		addStartNpc(ANTON);
		addTalkId(ANTON, MARQUEZ);
	}

	public static void onLoad() {
		new Q110_ToThePrimevalIsle(110, "Q110_ToThePrimevalIsle", "To The Primeval Isle", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31338-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(ANCIENT_BOOK, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32113-03.htm".equalsIgnoreCase(event) && st.getQuestItemsCount(ANCIENT_BOOK) == 1) {
			st.playSound(QuestState.SOUND_FINISH);
			st.rewardItems(ADENA, 169380);
			st.takeItems(ANCIENT_BOOK, 1);
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
				if (player.getLevel() >= 75)
					htmltext = "31338-01.htm";
				else {
					htmltext = "31338-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case ANTON:
						htmltext = "31338-01c.htm";
						break;

					case MARQUEZ:
						htmltext = "32113-01.htm";
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