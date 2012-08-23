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

public class Q049_TheRoadHome extends Quest implements ScriptFile {
	private static final String qn = "Q049_TheRoadHome";

	// NPCs
	private static final int GALLADUCCI = 30097;
	private static final int GENTLER = 30094;
	private static final int SANDRA = 30090;
	private static final int DUSTIN = 30116;

	// Items
	private static final int ORDER_DOCUMENT_1 = 7563;
	private static final int ORDER_DOCUMENT_2 = 7564;
	private static final int ORDER_DOCUMENT_3 = 7565;
	private static final int MAGIC_SWORD_HILT = 7568;
	private static final int GEMSTONE_POWDER = 7567;
	private static final int PURIFIED_MAGIC_NECKLACE = 7566;
	private static final int MARK_OF_TRAVELER = 7570;
	private static final int SCROLL_OF_ESCAPE_SPECIAL = 7558;

	public Q049_TheRoadHome(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ORDER_DOCUMENT_1, ORDER_DOCUMENT_2, ORDER_DOCUMENT_3, MAGIC_SWORD_HILT, GEMSTONE_POWDER, PURIFIED_MAGIC_NECKLACE };

		addStartNpc(GALLADUCCI);
		addTalkId(GALLADUCCI, GENTLER, SANDRA, DUSTIN);
	}

	public static void onLoad() {
		new Q049_TheRoadHome(49, "Q049_TheRoadHome", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30097-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
			st.giveItems(ORDER_DOCUMENT_1, 1);
		} else if ("30094-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.takeItems(ORDER_DOCUMENT_1, 1);
			st.giveItems(MAGIC_SWORD_HILT, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30097-06.htm".equalsIgnoreCase(event)) {
			st.set("cond", "3");
			st.takeItems(MAGIC_SWORD_HILT, 1);
			st.giveItems(ORDER_DOCUMENT_2, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30090-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "4");
			st.takeItems(ORDER_DOCUMENT_2, 1);
			st.giveItems(GEMSTONE_POWDER, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30097-09.htm".equalsIgnoreCase(event)) {
			st.set("cond", "5");
			st.takeItems(GEMSTONE_POWDER, 1);
			st.giveItems(ORDER_DOCUMENT_3, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30116-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "6");
			st.takeItems(ORDER_DOCUMENT_3, 1);
			st.giveItems(PURIFIED_MAGIC_NECKLACE, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30097-12.htm".equalsIgnoreCase(event)) {
			st.takeItems(PURIFIED_MAGIC_NECKLACE, 1);
			st.takeItems(MARK_OF_TRAVELER, -1);
			st.rewardItems(SCROLL_OF_ESCAPE_SPECIAL, 1);
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
				if (player.getRace().ordinal() == 4 && player.getLevel() >= 3 && player.getLevel() <= 10) {
					if (st.getQuestItemsCount(MARK_OF_TRAVELER) > 0)
						htmltext = "30097-02.htm";
					else {
						htmltext = "30097-01.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30097-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case GALLADUCCI:
						if (cond == 1)
							htmltext = "30097-04.htm";
						else if (cond == 2)
							htmltext = "30097-05.htm";
						else if (cond == 3)
							htmltext = "30097-07.htm";
						else if (cond == 4)
							htmltext = "30097-08.htm";
						else if (cond == 5)
							htmltext = "30097-10.htm";
						else if (cond == 6)
							htmltext = "30097-11.htm";
						break;

					case GENTLER:
						if (cond == 1)
							htmltext = "30094-01.htm";
						else if (cond >= 2)
							htmltext = "30094-03.htm";
						break;

					case SANDRA:
						if (cond == 3)
							htmltext = "30090-01.htm";
						else if (cond >= 4)
							htmltext = "30090-03.htm";
						break;

					case DUSTIN:
						if (cond == 5)
							htmltext = "30116-01.htm";
						else if (cond == 6)
							htmltext = "30116-03.htm";
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