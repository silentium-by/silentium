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

public class Q624_TheFinestIngredients_Part1 extends Quest implements ScriptFile {
	private static final String qn = "Q624_TheFinestIngredients_Part1";

	// Mobs
	private static final int NEPENTHES = 21319;
	private static final int ATROX = 21321;
	private static final int ATROXSPAWN = 21317;
	private static final int BANDERSNATCH = 21314;

	// Items
	private static final int TRUNK = 7202;
	private static final int FOOT = 7203;
	private static final int SPICE = 7204;

	// Rewards
	private static final int CRYSTAL = 7080;
	private static final int SAUCE = 7205;

	public Q624_TheFinestIngredients_Part1(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { TRUNK, FOOT, SPICE };

		addStartNpc(31521); // Jeremy
		addTalkId(31521);

		addKillId(NEPENTHES, ATROX, ATROXSPAWN, BANDERSNATCH);
	}

	public static void onLoad() {
		new Q624_TheFinestIngredients_Part1(624, "Q624_TheFinestIngredients_Part1", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31521-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31521-05.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(TRUNK) >= 50 && st.getQuestItemsCount(FOOT) >= 50 && st.getQuestItemsCount(SPICE) >= 50) {
				st.takeItems(TRUNK, -1);
				st.takeItems(FOOT, -1);
				st.takeItems(SPICE, -1);
				st.giveItems(CRYSTAL, 1);
				st.giveItems(SAUCE, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			} else {
				st.set("cond", "1");
				htmltext = "31521-07.htm";
			}
		}

		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = Quest.getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 73)
					htmltext = "31521-01.htm";
				else {
					htmltext = "31521-03.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "31521-06.htm";
				else if (cond == 2) {
					htmltext = st.getQuestItemsCount(TRUNK) >= 50 && st.getQuestItemsCount(FOOT) >= 50 && st.getQuestItemsCount(SPICE) >= 50 ? "31521-04.htm" : "31521-07.htm";
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 1) {
			switch (npc.getNpcId()) {
				case NEPENTHES:
					if (st.dropAlwaysQuestItems(TRUNK, 1, 50))
						if (st.getQuestItemsCount(FOOT) >= 50 && st.getQuestItemsCount(SPICE) >= 50)
							st.set("cond", "2");
					break;

				case ATROX:
				case ATROXSPAWN:
					if (st.dropAlwaysQuestItems(SPICE, 1, 50))
						if (st.getQuestItemsCount(TRUNK) >= 50 && st.getQuestItemsCount(FOOT) >= 50)
							st.set("cond", "2");
					break;

				case BANDERSNATCH:
					if (st.dropAlwaysQuestItems(FOOT, 1, 50))
						if (st.getQuestItemsCount(TRUNK) >= 50 && st.getQuestItemsCount(SPICE) >= 50)
							st.set("cond", "2");
					break;
			}
		}

		return null;
	}
}