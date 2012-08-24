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

public class Q303_CollectArrowheads extends Quest implements ScriptFile {
	private static final String qn = "Q303_CollectArrowheads";

	// NPC
	private static final int MINIA = 30029;

	// Item
	private static final int ORCISH_ARROWHEAD = 963;

	public Q303_CollectArrowheads(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ORCISH_ARROWHEAD };

		addStartNpc(MINIA);
		addTalkId(MINIA);

		addKillId(20361);
	}

	public static void onLoad() {
		new Q303_CollectArrowheads(303, "Q303_CollectArrowheads", "Collect Arrowheads", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30029-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getLevel() >= 10 && player.getLevel() <= 14)
					htmltext = "30029-02.htm";
				else {
					htmltext = "30029-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(ORCISH_ARROWHEAD) < 10)
					htmltext = "30029-04.htm";
				else {
					htmltext = "30029-05.htm";
					st.takeItems(ORCISH_ARROWHEAD, -1);
					st.rewardItems(57, 1000);
					st.addExpAndSp(2000, 0);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
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

		if (st.getInt("cond") == 1)
			if (st.dropQuestItems(ORCISH_ARROWHEAD, 1, 10, 400000))
				st.set("cond", "2");

		return null;
	}
}