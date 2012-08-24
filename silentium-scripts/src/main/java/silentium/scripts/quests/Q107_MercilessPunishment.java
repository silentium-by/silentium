/*
 * This program is free software: you can redistribute it &&/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)any later version. This program is distributed in the hope that
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

/**
 * @author Demon
 */

public class Q107_MercilessPunishment extends Quest implements ScriptFile {
	private static final String qn = "Q107_MercilessPunishment";

	private static final int HATOSS_ORDER1_ID = 1553;
	private static final int HATOSS_ORDER2_ID = 1554;
	private static final int HATOSS_ORDER3_ID = 1555;
	private static final int LETTER_TO_HUMAN_ID = 1557;
	private static final int LETTER_TO_DARKELF_ID = 1556;
	private static final int LETTER_TO_ELF_ID = 1558;
	private static final int BUTCHER_ID = 1510;
	private static final int LESSER_HEALING_ID = 1060;
	private static final int CRYSTAL_BATTLE = 4412;
	private static final int CRYSTAL_LOVE = 4413;
	private static final int CRYSTAL_SOLITUDE = 4414;
	private static final int CRYSTAL_FEAST = 4415;
	private static final int CRYSTAL_CELEBRATION = 4416;
	private static final int SOULSHOT_NO_GRADE_FOR_BEGINNERS_ID = 5789;

	public Q107_MercilessPunishment(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);
		addStartNpc(30568);
		addTalkId(30568, 30580);
		addKillId(27041);
		questItemIds = new int[] { HATOSS_ORDER2_ID, LETTER_TO_DARKELF_ID, LETTER_TO_HUMAN_ID, LETTER_TO_ELF_ID, HATOSS_ORDER1_ID, HATOSS_ORDER3_ID };
	}

	public static void onLoad() {
		new Q107_MercilessPunishment(-1, "Q107_MercilessPunishment", "Merciless Punishment", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "1") {
			st.set("id", "0");
			htmltext = "30568-03.htm";
			st.giveItems(HATOSS_ORDER1_ID, 1);
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound("ItemSound.quest_accept");
		} else if (event == "30568_1") {
			htmltext = "30568-06.htm";
			st.takeItems(HATOSS_ORDER2_ID, 1);
			st.takeItems(LETTER_TO_DARKELF_ID, 1);
			st.takeItems(LETTER_TO_HUMAN_ID, 1);
			st.takeItems(LETTER_TO_ELF_ID, 1);
			st.takeItems(HATOSS_ORDER1_ID, 1);
			st.takeItems(HATOSS_ORDER3_ID, 1);
			st.set("cond", "0");
			st.playSound("ItemSound.quest_giveup");
		} else if (event == "30568_2") {
			htmltext = "30568-07.htm";
			st.takeItems(HATOSS_ORDER1_ID, 1);
			if (st.getQuestItemsCount(HATOSS_ORDER2_ID) == 0)
				st.giveItems(HATOSS_ORDER2_ID, 1);
		} else if (event == "30568_3") {
			htmltext = "30568-06.htm";
			st.takeItems(HATOSS_ORDER1_ID, 1);
			st.takeItems(LETTER_TO_DARKELF_ID, 1);
			st.takeItems(LETTER_TO_HUMAN_ID, 1);
			st.takeItems(LETTER_TO_ELF_ID, 1);
			st.takeItems(HATOSS_ORDER2_ID, 1);
			st.takeItems(HATOSS_ORDER3_ID, 1);
			st.set("cond", "0");
			st.playSound("ItemSound.quest_giveup");
		} else if (event == "30568_4") {
			htmltext = "30568-09.htm";
			st.takeItems(HATOSS_ORDER2_ID, 1);
			if (st.getQuestItemsCount(HATOSS_ORDER3_ID) == 0)
				st.giveItems(HATOSS_ORDER3_ID, 1);
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		final int id = st.getState();
		final int npcId = npc.getNpcId();

		if (id == QuestState.CREATED) {
			st.setState(QuestState.STARTED);
			st.set("cond", "0");
			st.set("onlyone", "0");
			st.set("id", "0");
		}
		if (npcId == 30568 && st.getInt("cond") == 0 && st.getInt("onlyone") == 0) {
			if (st.getInt("cond") < 15) {
				if (player.getRace().ordinal() != 3) {
					htmltext = "30568-00.htm";
					st.exitQuest(true);
				} else if (player.getLevel() >= 12) {
					htmltext = "30568-02.htm";
					return htmltext;
				} else {
					htmltext = "30568-01.htm";
					st.exitQuest(true);
				}
			} else {
				htmltext = "30568-01.htm";
				st.exitQuest(true);
			}
		} else if (npcId == 30568 && st.getInt("cond") == 0 && st.getInt("onlyone") == 1)
			htmltext = "<html><body>This quest has already been completed.</body></html>";
		else if (npcId == 30568 && st.getInt("cond") == 1 && (st.getQuestItemsCount(HATOSS_ORDER1_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER2_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER3_ID) > 0) && st.getQuestItemsCount(LETTER_TO_ELF_ID) + st.getQuestItemsCount(LETTER_TO_HUMAN_ID) + st.getQuestItemsCount(LETTER_TO_DARKELF_ID) == 0)
			htmltext = "30568-04.htm";
		else if (npcId == 30568 && st.getInt("cond") == 1 && (st.getQuestItemsCount(HATOSS_ORDER1_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER2_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER3_ID) > 0) && st.getQuestItemsCount(LETTER_TO_ELF_ID) + st.getQuestItemsCount(LETTER_TO_HUMAN_ID) + st.getQuestItemsCount(LETTER_TO_DARKELF_ID) == 1)
			htmltext = "30568-05.htm";
		else if (npcId == 30568 && st.getInt("cond") == 1 && (st.getQuestItemsCount(HATOSS_ORDER1_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER2_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER3_ID) > 0) && st.getQuestItemsCount(LETTER_TO_ELF_ID) + st.getQuestItemsCount(LETTER_TO_HUMAN_ID) + st.getQuestItemsCount(LETTER_TO_DARKELF_ID) == 2)
			htmltext = "30568-08.htm";
		else if (npcId == 30568 && st.getInt("cond") == 1 && (st.getQuestItemsCount(HATOSS_ORDER1_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER2_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER3_ID) > 0) && st.getQuestItemsCount(LETTER_TO_ELF_ID) + st.getQuestItemsCount(LETTER_TO_HUMAN_ID) + st.getQuestItemsCount(LETTER_TO_DARKELF_ID) == 3 && st.getInt("onlyone") == 0) {
			if (st.getInt("id") != 107) {
				st.set("id", "107");
				htmltext = "30568-10.htm";
				st.takeItems(LETTER_TO_DARKELF_ID, 1);
				st.takeItems(LETTER_TO_HUMAN_ID, 1);
				st.takeItems(LETTER_TO_ELF_ID, 1);
				st.takeItems(HATOSS_ORDER3_ID, 1);
				st.giveItems(LESSER_HEALING_ID, 100);
				st.giveItems(BUTCHER_ID, 1);
				st.giveItems(CRYSTAL_BATTLE, 10);
				st.giveItems(CRYSTAL_LOVE, 10);
				st.giveItems(CRYSTAL_SOLITUDE, 10);
				st.giveItems(CRYSTAL_FEAST, 10);
				st.giveItems(CRYSTAL_CELEBRATION, 10);
				if (player.getLevel() < 25 && st.getInt("onlyone") == 0 && player.isNewbie())
					st.giveItems(SOULSHOT_NO_GRADE_FOR_BEGINNERS_ID, 7000);
				st.set("cond", "0");
				st.setState(QuestState.COMPLETED);
				st.playSound("ItemSound.quest_finish");
				st.set("onlyone", "1");
			}
		} else if (npcId == 30580 && st.getInt("cond") == 1 && id == QuestState.STARTED && (st.getQuestItemsCount(HATOSS_ORDER1_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER2_ID) > 0 || st.getQuestItemsCount(HATOSS_ORDER3_ID) > 0))
			htmltext = "30580-01.htm";

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		final int npcId = npc.getNpcId();
		if (npcId == 27041) {
			st.set("id", "0");
			if (st.getInt("cond") == 1) {
				if (st.getQuestItemsCount(HATOSS_ORDER1_ID) > 0 && st.getQuestItemsCount(LETTER_TO_HUMAN_ID) == 0) {
					st.giveItems(LETTER_TO_HUMAN_ID, 1);
					st.playSound("ItemSound.quest_itemget");
				}
				if (st.getQuestItemsCount(HATOSS_ORDER2_ID) > 0 && st.getQuestItemsCount(LETTER_TO_DARKELF_ID) == 0) {
					st.giveItems(LETTER_TO_DARKELF_ID, 1);
					st.playSound("ItemSound.quest_itemget");
				}
				if (st.getQuestItemsCount(HATOSS_ORDER3_ID) > 0 && st.getQuestItemsCount(LETTER_TO_ELF_ID) == 0) {
					st.giveItems(LETTER_TO_ELF_ID, 1);
					st.playSound("ItemSound.quest_itemget");
				}
			}
		}

		return null;
	}
}