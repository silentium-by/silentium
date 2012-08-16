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

public class Q016_TheComingDarkness extends Quest {
	private final static String qn = "Q016_TheComingDarkness";

	// NPCs
	private static final int HIERARCH = 31517;
	private static final int EVIL_ALTAR_1 = 31512;
	private static final int EVIL_ALTAR_2 = 31513;
	private static final int EVIL_ALTAR_3 = 31514;
	private static final int EVIL_ALTAR_4 = 31515;
	private static final int EVIL_ALTAR_5 = 31516;

	// Item
	private static final int CRYSTAL_OF_SEAL = 7167;

	public Q016_TheComingDarkness(int questId, String name, String descr) {
		super(questId, name, descr);

		addStartNpc(HIERARCH);
		addTalkId(HIERARCH, EVIL_ALTAR_1, EVIL_ALTAR_2, EVIL_ALTAR_3, EVIL_ALTAR_4, EVIL_ALTAR_5);
	}

	public static void main(String[] args) {
		new Q016_TheComingDarkness(16, "Q016_TheComingDarkness", "The Coming Darkness");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31517-2.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(CRYSTAL_OF_SEAL, 5);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("31512-1.htm")) {
			st.set("cond", "2");
			st.takeItems(CRYSTAL_OF_SEAL, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equalsIgnoreCase("31513-1.htm")) {
			st.set("cond", "3");
			st.takeItems(CRYSTAL_OF_SEAL, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equalsIgnoreCase("31514-1.htm")) {
			st.set("cond", "4");
			st.takeItems(CRYSTAL_OF_SEAL, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equalsIgnoreCase("31515-1.htm")) {
			st.set("cond", "5");
			st.takeItems(CRYSTAL_OF_SEAL, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equalsIgnoreCase("31516-1.htm")) {
			st.set("cond", "6");
			st.takeItems(CRYSTAL_OF_SEAL, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = Quest.getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 62)
					htmltext = "31517-0.htm";
				else {
					htmltext = "31517-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case HIERARCH:
						if (cond >= 1 && cond <= 5) {
							if (st.getQuestItemsCount(CRYSTAL_OF_SEAL) == 0) {
								htmltext = "31517-3a.htm";
								st.exitQuest(true);
							} else
								htmltext = "31517-3.htm";
						} else if (cond == 6) {
							htmltext = "31517-4.htm";
							st.addExpAndSp(221958, 0);
							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case EVIL_ALTAR_1:
						if (cond == 1) {
							if (st.getQuestItemsCount(CRYSTAL_OF_SEAL) > 0)
								htmltext = "31512-0.htm";
							else
								htmltext = "altar_nocrystal.htm";
						} else if (cond > 1)
							htmltext = "31512-2.htm";
						break;

					case EVIL_ALTAR_2:
						if (cond == 2) {
							if (st.getQuestItemsCount(CRYSTAL_OF_SEAL) > 0)
								htmltext = "31513-0.htm";
							else
								htmltext = "altar_nocrystal.htm";
						} else if (cond > 2)
							htmltext = "31513-2.htm";
						break;

					case EVIL_ALTAR_3:
						if (cond == 3) {
							if (st.getQuestItemsCount(CRYSTAL_OF_SEAL) > 0)
								htmltext = "31514-0.htm";
							else
								htmltext = "altar_nocrystal.htm";
						} else if (cond > 3)
							htmltext = "31514-2.htm";
						break;

					case EVIL_ALTAR_4:
						if (cond == 4) {
							if (st.getQuestItemsCount(CRYSTAL_OF_SEAL) > 0)
								htmltext = "31515-0.htm";
							else
								htmltext = "altar_nocrystal.htm";
						} else if (cond > 4)
							htmltext = "31515-2.htm";
						break;

					case EVIL_ALTAR_5:
						if (cond == 5) {
							if (st.getQuestItemsCount(CRYSTAL_OF_SEAL) > 0)
								htmltext = "31516-0.htm";
							else
								htmltext = "altar_nocrystal.htm";
						} else if (cond > 5)
							htmltext = "31516-2.htm";
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