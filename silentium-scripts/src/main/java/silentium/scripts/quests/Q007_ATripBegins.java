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

public class Q007_ATripBegins extends Quest implements ScriptFile {
	private static final String qn = "Q007_ATripBegins";

	// NPCs
	private static final int MIRABEL = 30146;
	private static final int ARIEL = 30148;
	private static final int ASTERIOS = 30154;

	// Items
	private static final int ARIEL_RECO = 7572;

	// Rewards
	private static final int MARK_TRAVELER = 7570;
	private static final int SCROLL_GIRAN = 7559;

	public Q007_ATripBegins(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ARIEL_RECO };

		addStartNpc(MIRABEL);
		addTalkId(MIRABEL, ARIEL, ASTERIOS);
	}

	public static void onLoad() {
		new Q007_ATripBegins(7, "Q007_ATripBegins", "A Trip Begins", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30146-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30148-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.giveItems(ARIEL_RECO, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30154-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "3");
			st.takeItems(ARIEL_RECO, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30146-06.htm".equalsIgnoreCase(event)) {
			st.giveItems(MARK_TRAVELER, 1);
			st.rewardItems(SCROLL_GIRAN, 1);
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
				if (player.getRace().ordinal() != 1) {
					htmltext = "30146-01.htm";
					st.exitQuest(true);
				} else if (player.getLevel() >= 3 && player.getLevel() <= 10)
					htmltext = "30146-02.htm";
				else {
					htmltext = "30146-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case MIRABEL:
						if (cond == 1 || cond == 2)
							htmltext = "30146-04.htm";
						else if (cond == 3)
							htmltext = "30146-05.htm";
						break;

					case ARIEL:
						if (cond == 1)
							htmltext = "30148-01.htm";
						else if (cond == 2 && st.getQuestItemsCount(ARIEL_RECO) == 1)
							htmltext = "30148-03.htm";
						break;

					case ASTERIOS:
						htmltext = cond == 2 && st.getQuestItemsCount(ARIEL_RECO) == 1 ? "30154-01.htm" : "30154-03.htm";
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