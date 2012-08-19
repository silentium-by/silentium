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

/**
 * @author Demon
 */

public class Q024_InhabitantsOfTheForrestOfTheDead extends Quest {
	private static final String qn = "Q024_InhabitantsOfTheForrestOfTheDead";

	private static final int Dorian = 31389;
	private static final int Wizard = 31522;
	private static final int Tombstone = 31531;
	private static final int MaidOfLidia = 31532;

	private static final int Letter = 7065;
	private static final int Hairpin = 7148;
	private static final int Totem = 7151;
	private static final int Flower = 7152;
	private static final int SilverCross = 7153;
	private static final int BrokenSilverCross = 7154;
	private static final int SuspiciousTotem = 7156;

	public Q024_InhabitantsOfTheForrestOfTheDead(final int questId, final String name, final String descr) {
		super(questId, name, descr);
		addStartNpc(Dorian);
		addTalkId(Dorian, Tombstone, MaidOfLidia, Wizard);
		addKillId(21557, 21558, 21560, 21563, 21564, 21565, 21566, 21567);
		addAggroRangeEnterId(25332);
		questItemIds = new int[] { Flower, SilverCross, BrokenSilverCross, Letter, Hairpin, Totem };
	}

	public static void main(final String... args) {
		new Q024_InhabitantsOfTheForrestOfTheDead(-1, "Q024_InhabitantsOfTheForrestOfTheDead", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event == "31389-02.htm") {
			st.giveItems(Flower, 1);
			st.set("cond", "1");
			st.playSound("ItemSound.quest_accept");
			st.setState(QuestState.STARTED);
		} else if (event == "31389-11.htm") {
			st.set("cond", "3");
			st.playSound("ItemSound.quest_middle");
			st.giveItems(SilverCross, 1);
		} else if (event == "31389-16.htm")
			st.playSound("InterfaceSound.charstat_open_01");
		else if (event == "31389-17.htm") {
			st.takeItems(BrokenSilverCross, -1);
			st.giveItems(Hairpin, 1);
			st.set("cond", "5");
		} else if (event == "31522-03.htm")
			st.takeItems(Totem, -1);
		else if (event == "31522-07.htm")
			st.set("cond", "11");
		else if (event == "31522-19.htm") {
			st.giveItems(SuspiciousTotem, 1);
			st.addExpAndSp(242105, 22529);
			st.exitQuest(false);
			st.playSound("ItemSound.quest_finish");
		} else if (event == "31531-02.htm") {
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "2");
			st.takeItems(Flower, -1);
		} else if (event == "31532-04.htm") {
			st.playSound("ItemSound.quest_middle");
			st.giveItems(Letter, 1);
			st.set("cond", "6");
		} else if (event == "31532-06.htm") {
			st.takeItems(Hairpin, -1);
			st.takeItems(Letter, -1);
		} else if (event == "31532-16.htm") {
			st.playSound("ItemSound.quest_middle");
			st.set("cond", "9");
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
		final int state = st.getState();
		final int cond = st.getInt("cond");
		if (state == QuestState.COMPLETED) {
			htmltext = npcId == Wizard ? "31522-20.htm" : "<html><body>This quest has already been completed.</body></html>";
		}
		if (npcId == Dorian) {
			if (state == QuestState.CREATED) {
				final QuestState st2 = player.getQuestState("Q023_LidiasHeart");
				if (st2 != null) {
					htmltext = st2 != null && st2.isCompleted() && player.getLevel() >= 65 ? "31389-01.htm" : "31389-00.htm";
				} else
					htmltext = "31389-00.htm";
			} else if (cond == 1)
				htmltext = "31389-03.htm";
			else if (cond == 2)
				htmltext = "31389-04.htm";
			else if (cond == 3)
				htmltext = "31389-12.htm";
			else if (cond == 4)
				htmltext = "31389-13.htm";
			else if (cond == 5)
				htmltext = "31389-18.htm";
		} else if (npcId == Tombstone) {
			if (cond == 1) {
				st.playSound("AmdSound.d_wind_loot_02");
				htmltext = "31531-01.htm";
			} else if (cond == 2)
				htmltext = "31531-03.htm";
		} else if (npcId == MaidOfLidia) {
			if (cond == 5)
				htmltext = "31532-01.htm";
			else if (cond == 6) {
				htmltext = st.getQuestItemsCount(Letter) > 0 && st.getQuestItemsCount(Hairpin) > 0 ? "31532-05.htm" : "31532-07.htm";
			} else if (cond == 9)
				htmltext = "31532-16.htm";
		} else if (npcId == Wizard) {
			if (cond == 10)
				htmltext = "31522-01.htm";
			else if (cond == 11)
				htmltext = "31522-08.htm";
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getState() != QuestState.STARTED)
			return null;

		final int npcId = npc.getNpcId();
		if (st.getQuestItemsCount(Totem) < 0 && st.getInt("cond") == 9) {
			if ((npcId == 21557 || npcId == 21558 || npcId == 21560 || npcId == 21563 || npcId == 21564 || npcId == 21565 || npcId == 21566 || npcId == 21567) && st.getRandom(100) <= 30) {
				st.giveItems(Totem, 1);
				st.set("cond", "10");
				st.playSound("ItemSound.quest_middle");
			}
		}

		return null;
	}
}