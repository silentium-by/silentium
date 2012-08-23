/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q353_PowerOfDarkness extends Quest implements ScriptFile {
	private static final String qn = "Q353_PowerOfDarkness";

	// NPC
	private static final int GALMAN = 31044;

	// Item
	private static final int STONE = 5862;

	public Q353_PowerOfDarkness(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { STONE };

		addStartNpc(GALMAN);
		addTalkId(GALMAN);

		addKillId(20284, 20245, 20244, 20283);
	}

	public static void onLoad() {
		new Q353_PowerOfDarkness(353, "Q353_PowerOfDarkness", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31044-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31044-08.htm".equalsIgnoreCase(event)) {
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getLevel() >= 55 && player.getLevel() <= 60)
					htmltext = "31044-02.htm";
				else {
					htmltext = "31044-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int stones = st.getQuestItemsCount(STONE);
				if (stones == 0)
					htmltext = "31044-05.htm";
				else {
					htmltext = "31044-06.htm";
					st.takeItems(STONE, -1);
					st.rewardItems(57, 2500 + 230 * stones);
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

		if (st.isStarted() && Rnd.get(100) < 25) {
			st.giveItems(STONE, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}