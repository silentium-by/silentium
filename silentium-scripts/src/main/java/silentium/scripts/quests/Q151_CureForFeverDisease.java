/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q151_CureForFeverDisease extends Quest implements ScriptFile {
	private final static String qn = "Q151_CureForFeverDisease";

	// Items
	private static final int POISON_SAC = 703;
	private static final int FEVER_MEDICINE = 704;

	// NPCs
	private static final int ELIAS = 30050;
	private static final int YOHANES = 30032;

	public Q151_CureForFeverDisease(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { FEVER_MEDICINE, POISON_SAC };

		addStartNpc(ELIAS);
		addTalkId(ELIAS, YOHANES);

		addKillId(20103, 20106, 20108);
	}

	public static void onLoad() {
		new Q151_CureForFeverDisease(151, "Q151_CureForFeverDisease", "Cure for Fever Disease");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30050-03.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getLevel() >= 15 && player.getLevel() <= 21)
					htmltext = "30050-02.htm";
				else {
					htmltext = "30050-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case ELIAS:
						if (cond == 1)
							htmltext = "30050-04.htm";
						else if (cond == 2)
							htmltext = "30050-05.htm";
						else if (cond == 3) {
							htmltext = "30050-06.htm";
							st.takeItems(FEVER_MEDICINE, 1);
							st.giveItems(102, 1);
							st.exitQuest(false);
							st.playSound(QuestState.SOUND_FINISH);
						}
						break;

					case YOHANES:
						if (cond == 2) {
							htmltext = "30032-01.htm";
							st.set("cond", "3");
							st.takeItems(POISON_SAC, 1);
							st.giveItems(FEVER_MEDICINE, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 3)
							htmltext = "30032-02.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 1 && Rnd.get(5) == 0) {
			st.set("cond", "2");
			st.giveItems(POISON_SAC, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}