/*
 * This program is free software: you can redistribute it &&/or modify it under the terms of the GNU General Public License as published by the
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

public class Q047_IntoTheDarkForest extends Quest implements ScriptFile {
	private static final String qn = "Q047_IntoTheDarkForest";

	private static final int GALLADUCCIS_ORDER_DOCUMENT_ID_1 = 7563;
	private static final int GALLADUCCIS_ORDER_DOCUMENT_ID_2 = 7564;
	private static final int GALLADUCCIS_ORDER_DOCUMENT_ID_3 = 7565;
	private static final int MAGIC_SWORD_HILT_ID = 7568;
	private static final int GEMSTONE_POWDER_ID = 7567;
	private static final int PURIFIED_MAGIC_NECKLACE_ID = 7566;
	private static final int MARK_OF_TRAVELER_ID = 7570;
	private static final int SCROLL_OF_ESCAPE_SPECIAL = 7556;
	private static final int RACE = 2;

	public Q047_IntoTheDarkForest(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);
		addStartNpc(30097);
		addTalkId(30097, 30094, 30090, 30116);
		questItemIds = new int[] { GALLADUCCIS_ORDER_DOCUMENT_ID_1, GALLADUCCIS_ORDER_DOCUMENT_ID_2, GALLADUCCIS_ORDER_DOCUMENT_ID_3, MAGIC_SWORD_HILT_ID, GEMSTONE_POWDER_ID, PURIFIED_MAGIC_NECKLACE_ID };
	}

	public static void onLoad() {
		new Q047_IntoTheDarkForest(47, "Q047_IntoTheDarkForest", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "1") {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound("ItemSound.quest_accept");
			st.giveItems(GALLADUCCIS_ORDER_DOCUMENT_ID_1, 1);
			htmltext = "30097-03.htm";
		} else if (event == "2") {
			st.set("cond", "2");
			st.takeItems(GALLADUCCIS_ORDER_DOCUMENT_ID_1, 1);
			st.giveItems(MAGIC_SWORD_HILT_ID, 1);
			htmltext = "30094-02.htm";
		} else if (event == "3") {
			st.set("cond", "3");
			st.takeItems(MAGIC_SWORD_HILT_ID, 1);
			st.giveItems(GALLADUCCIS_ORDER_DOCUMENT_ID_2, 1);
			htmltext = "30097-06.htm";
		} else if (event == "4") {
			st.set("cond", "4");
			st.takeItems(GALLADUCCIS_ORDER_DOCUMENT_ID_2, 1);
			st.giveItems(GEMSTONE_POWDER_ID, 1);
			htmltext = "30090-02.htm";
		} else if (event == "5") {
			st.set("cond", "5");
			st.takeItems(GEMSTONE_POWDER_ID, 1);
			st.giveItems(GALLADUCCIS_ORDER_DOCUMENT_ID_3, 1);
			htmltext = "30097-09.htm";
		} else if (event == "6") {
			st.set("cond", "6");
			st.takeItems(GALLADUCCIS_ORDER_DOCUMENT_ID_3, 1);
			st.giveItems(PURIFIED_MAGIC_NECKLACE_ID, 1);
			htmltext = "30116-02.htm";
		} else if (event == "7") {
			st.giveItems(SCROLL_OF_ESCAPE_SPECIAL, 1);
			st.takeItems(PURIFIED_MAGIC_NECKLACE_ID, 1);
			st.takeItems(MARK_OF_TRAVELER_ID, -1);
			htmltext = "30097-12.htm";
			st.unset("cond");
			st.setState(QuestState.COMPLETED);
			st.playSound("ItemSound.quest_finish");
		}
		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		final int npcId = npc.getNpcId();
		final int id = st.getState();
		if (id == QuestState.CREATED) {
			st.set("cond", "0");
			if (player.getRace().ordinal() == RACE && st.getQuestItemsCount(MARK_OF_TRAVELER_ID) > 0)
				htmltext = "30097-02.htm";
			else {
				htmltext = "30097-01.htm";
				st.exitQuest(true);
			}
		} else if (id == QuestState.STARTED) {
			if (npcId == 30094 && st.getInt("cond") == 1)
				htmltext = "30094-01.htm";
			else if (npcId == 30094 && st.getInt("cond") == 2)
				htmltext = "30094-03.htm";
			else if (npcId == 30090 && st.getInt("cond") == 3)
				htmltext = "30090-01.htm";
			else if (npcId == 30090 && st.getInt("cond") == 4)
				htmltext = "30090-03.htm";
			else if (npcId == 30116 && st.getInt("cond") == 5)
				htmltext = "30116-01.htm";
			else if (npcId == 30116 && st.getInt("cond") == 6)
				htmltext = "30116-03.htm";
		} else if (npcId == 30097 && id == QuestState.COMPLETED)
			htmltext = "<html><body>I can't supply you with another Scroll of Escape. Sorry traveller.</body></html>";
		else if (npcId == 30097 && st.getInt("cond") == 1)
			htmltext = "30097-04.htm";
		else if (npcId == 30097 && st.getInt("cond") == 2)
			htmltext = "30097-05.htm";
		else if (npcId == 30097 && st.getInt("cond") == 3)
			htmltext = "30097-07.htm";
		else if (npcId == 30097 && st.getInt("cond") == 4)
			htmltext = "30097-08.htm";
		else if (npcId == 30097 && st.getInt("cond") == 5)
			htmltext = "30097-10.htm";
		else if (npcId == 30097 && st.getInt("cond") == 6)
			htmltext = "30097-11.htm";

		return htmltext;
	}
}