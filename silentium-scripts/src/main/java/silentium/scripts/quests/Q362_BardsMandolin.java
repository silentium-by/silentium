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

public class Q362_BardsMandolin extends Quest implements ScriptFile {
	private static final String qn = "Q362_BardsMandolin";

	// Items
	private static final int SWAN_FLUTE = 4316;
	private static final int SWAN_LETTER = 4317;

	// NPCs
	private static final int SWAN = 30957;
	private static final int NANARIN = 30956;
	private static final int GALION = 30958;
	private static final int WOODROW = 30837;

	public Q362_BardsMandolin(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { SWAN_FLUTE, SWAN_LETTER };

		addStartNpc(SWAN);
		addTalkId(SWAN, NANARIN, GALION, WOODROW);
	}

	public static void onLoad() {
		new Q362_BardsMandolin(362, "Q362_BardsMandolin", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30957-3.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30957-7.htm".equalsIgnoreCase(event) || "30957-8.htm".equalsIgnoreCase(event)) {
			st.rewardItems(57, 10000);
			st.giveItems(4410, 1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (st.getPlayer().getLevel() >= 15)
					htmltext = "30957-1.htm";
				else {
					htmltext = "30957-2.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case SWAN:
						if (cond == 1 || cond == 2)
							htmltext = "30957-4.htm";
						else if (cond == 3) {
							htmltext = "30957-5.htm";
							st.set("cond", "4");
							st.giveItems(SWAN_LETTER, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 4)
							htmltext = "30957-5a.htm";
						else if (cond == 5)
							htmltext = "30957-6.htm";
						break;

					case WOODROW:
						if (cond == 1) {
							htmltext = "30837-1.htm";
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 2)
							htmltext = "30837-2.htm";
						else if (cond > 2)
							htmltext = "30837-3.htm";
						break;

					case GALION:
						if (cond == 2) {
							htmltext = "30958-1.htm";
							st.set("cond", "3");
							st.giveItems(SWAN_FLUTE, 1);
							st.playSound(QuestState.SOUND_ITEMGET);
						} else if (cond >= 3)
							htmltext = "30958-2.htm";
						break;

					case NANARIN:
						if (cond == 4) {
							htmltext = "30956-1.htm";
							st.set("cond", "5");
							st.takeItems(SWAN_FLUTE, 1);
							st.takeItems(SWAN_LETTER, 1);
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 5)
							htmltext = "30956-2.htm";
						break;
				}
				break;
		}

		return htmltext;
	}
}