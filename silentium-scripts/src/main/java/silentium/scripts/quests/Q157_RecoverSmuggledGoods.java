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

public class Q157_RecoverSmuggledGoods extends Quest implements ScriptFile {
	private static final String qn = "Q157_RecoverSmuggledGoods";

	// NPC
	private static final int WILFORD = 30005;

	// Monster
	private static final int TOAD = 20121;

	// Item
	private static final int ADAMANTITE_ORE = 1024;

	// Reward
	private static final int BUCKLER = 20;

	public Q157_RecoverSmuggledGoods(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { ADAMANTITE_ORE };

		addStartNpc(WILFORD);
		addTalkId(WILFORD);

		addKillId(TOAD);
	}

	public static void onLoad() {
		new Q157_RecoverSmuggledGoods(157, "Q157_RecoverSmuggledGoods", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30005-05.htm".equalsIgnoreCase(event)) {
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
				if (player.getLevel() >= 5 && player.getLevel() <= 9)
					htmltext = "30005-03.htm";
				else {
					htmltext = "30005-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1 && st.getQuestItemsCount(ADAMANTITE_ORE) < 20)
					htmltext = "30005-06.htm";
				else if (cond == 2 && st.getQuestItemsCount(ADAMANTITE_ORE) >= 20) {
					htmltext = "30005-07.htm";
					st.takeItems(ADAMANTITE_ORE, 20);
					st.giveItems(BUCKLER, 1);
					st.exitQuest(false);
					st.playSound(QuestState.SOUND_FINISH);
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
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
			if (st.dropAlwaysQuestItems(ADAMANTITE_ORE, 1, 20))
				st.set("cond", "2");

		return null;
	}
}