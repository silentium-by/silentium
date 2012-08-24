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

public class Q156_MillenniumLove extends Quest implements ScriptFile {
	private static final String qn = "Q156_MillenniumLove";

	// Items
	private static final int RYLITHS_LETTER = 1022;
	private static final int THEONS_DIARY = 1023;

	// NPCs
	private static final int LILITH = 30368;
	private static final int BAENEDES = 30369;

	public Q156_MillenniumLove(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { RYLITHS_LETTER, THEONS_DIARY };

		addStartNpc(LILITH);
		addTalkId(LILITH, BAENEDES);
	}

	public static void onLoad() {
		new Q156_MillenniumLove(156, "Q156_MillenniumLove", "Millennium Love", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30368-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.giveItems(RYLITHS_LETTER, 1);
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30369-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.takeItems(RYLITHS_LETTER, -1);
			st.giveItems(THEONS_DIARY, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30369-03.htm".equalsIgnoreCase(event)) {
			st.takeItems(RYLITHS_LETTER, -1);
			st.addExpAndSp(3000, 0);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 15 && player.getLevel() <= 19)
					htmltext = "30368-01.htm";
				else {
					htmltext = "30368-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case LILITH:
						if (st.getQuestItemsCount(RYLITHS_LETTER) == 1)
							htmltext = "30368-05.htm";
						else if (st.getQuestItemsCount(THEONS_DIARY) == 1) {
							htmltext = "30368-06.htm";
							st.takeItems(THEONS_DIARY, -1);
							st.giveItems(5250, 1);
							st.addExpAndSp(3000, 0);
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(false);
						}
						break;

					case BAENEDES:
						if (st.getQuestItemsCount(RYLITHS_LETTER) == 1)
							htmltext = "30369-01.htm";
						else if (st.getQuestItemsCount(THEONS_DIARY) == 1)
							htmltext = "30369-04.htm";
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