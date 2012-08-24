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

public class Q166_MassOfDarkness extends Quest implements ScriptFile {
	private static final String qn = "Q166_MassOfDarkness";

	// NPCs
	private static final int UNDRIAS = 30130;
	private static final int IRIA = 30135;
	private static final int DORANKUS = 30139;
	private static final int TRUDY = 30143;

	// Items
	private static final int UNDRIAS_LETTER = 1088;
	private static final int CEREMONIAL_DAGGER = 1089;
	private static final int DREVIANT_WINE = 1090;
	private static final int GARMIELS_SCRIPTURE = 1091;
	private static final int ADENA = 57;

	public Q166_MassOfDarkness(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { UNDRIAS_LETTER, CEREMONIAL_DAGGER, DREVIANT_WINE, GARMIELS_SCRIPTURE };

		addStartNpc(UNDRIAS);
		addTalkId(UNDRIAS, IRIA, DORANKUS, TRUDY);
	}

	public static void onLoad() {
		new Q166_MassOfDarkness(166, "Q166_MassOfDarkness", "Mass Of Darkness", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30130-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(UNDRIAS_LETTER, 1);
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
				if (player.getRace().ordinal() == 2) {
					if (player.getLevel() >= 2 && player.getLevel() <= 5)
						htmltext = "30130-03.htm";
					else {
						htmltext = "30130-02.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30130-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case UNDRIAS:
						if (cond == 1)
							htmltext = "30130-05.htm";
						else if (cond == 2) {
							htmltext = "30130-06.htm";
							st.takeItems(CEREMONIAL_DAGGER, 1);
							st.takeItems(DREVIANT_WINE, 1);
							st.takeItems(GARMIELS_SCRIPTURE, 1);
							st.takeItems(UNDRIAS_LETTER, 1);
							st.rewardItems(ADENA, 500);
							st.addExpAndSp(500, 0);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;

					case IRIA:
						if (st.getQuestItemsCount(CEREMONIAL_DAGGER) == 0) {
							st.giveItems(CEREMONIAL_DAGGER, 1);
							htmltext = "30135-01.htm";
						} else
							htmltext = "30135-02.htm";
						break;

					case DORANKUS:
						if (st.getQuestItemsCount(DREVIANT_WINE) == 0) {
							st.giveItems(DREVIANT_WINE, 1);
							htmltext = "30139-01.htm";
						} else
							htmltext = "30139-02.htm";
						break;

					case TRUDY:
						if (st.getQuestItemsCount(GARMIELS_SCRIPTURE) == 0) {
							st.giveItems(GARMIELS_SCRIPTURE, 1);
							htmltext = "30143-01.htm";
						} else
							htmltext = "30143-02.htm";
						break;
				}
				if (cond == 1 && st.getQuestItemsCount(CEREMONIAL_DAGGER) + st.getQuestItemsCount(DREVIANT_WINE) + st.getQuestItemsCount(GARMIELS_SCRIPTURE) >= 3) {
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}