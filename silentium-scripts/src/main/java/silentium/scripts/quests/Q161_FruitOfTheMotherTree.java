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

public class Q161_FruitOfTheMotherTree extends Quest implements ScriptFile {
	private static final String qn = "Q161_FruitOfTheMotherTree";

	// NPCs
	private static final int ANDELLIA = 30362;
	private static final int THALIA = 30371;

	// Items
	private static final int ANDELLIA_LETTER = 1036;
	private static final int MOTHERTREE_FRUIT = 1037;

	// Reward
	private static final int ADENA = 57;

	public Q161_FruitOfTheMotherTree(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ANDELLIA_LETTER, MOTHERTREE_FRUIT };

		addStartNpc(ANDELLIA);
		addTalkId(ANDELLIA, THALIA);
	}

	public static void onLoad() {
		new Q161_FruitOfTheMotherTree(161, "Q161_FruitOfTheMotherTree", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30362-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(ANDELLIA_LETTER, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getRace().ordinal() == 1) {
					if (player.getLevel() >= 3 && player.getLevel() <= 7)
						htmltext = "30362-03.htm";
					else {
						htmltext = "30362-02.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30362-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case ANDELLIA:
						if (cond == 1)
							htmltext = "30362-05.htm";
						else if (cond == 2 && st.getQuestItemsCount(MOTHERTREE_FRUIT) == 1) {
							htmltext = "30362-06.htm";
							st.takeItems(MOTHERTREE_FRUIT, 1);
							st.rewardItems(ADENA, 1000);
							st.addExpAndSp(1000, 0);
							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case THALIA:
						if (cond == 1 && st.getQuestItemsCount(ANDELLIA_LETTER) == 1) {
							htmltext = "30371-01.htm";
							st.takeItems(ANDELLIA_LETTER, 1);
							st.giveItems(MOTHERTREE_FRUIT, 1);
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 2 && st.getQuestItemsCount(MOTHERTREE_FRUIT) == 1)
							htmltext = "30371-02.htm";
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