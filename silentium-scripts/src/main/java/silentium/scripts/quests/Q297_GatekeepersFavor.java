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

public class Q297_GatekeepersFavor extends Quest implements ScriptFile {
	private static final String qn = "Q297_GatekeepersFavor";

	// NPC
	private static final int WIRPHY = 30540;

	// Item
	private static final int STARSTONE = 1573;

	// Reward
	private static final int GATEKEEPER_TOKEN = 1659;

	// Monster
	private static final int WHINSTONE_GOLEM = 20521;

	public Q297_GatekeepersFavor(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { STARSTONE };

		addStartNpc(WIRPHY);
		addTalkId(WIRPHY);
		addKillId(WHINSTONE_GOLEM);
	}

	public static void onLoad() {
		new Q297_GatekeepersFavor(297, "Q297_GatekeepersFavor", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30540-03.htm".equalsIgnoreCase(event)) {
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
				if (player.getLevel() >= 15 && player.getLevel() <= 21)
					htmltext = "30540-02.htm";
				else {
					htmltext = "30540-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30540-04.htm";
				else if (cond == 2) {
					if (st.getQuestItemsCount(STARSTONE) == 20) {
						htmltext = "30540-05.htm";
						st.takeItems(STARSTONE, 20);
						st.rewardItems(GATEKEEPER_TOKEN, 2);
						st.playSound(QuestState.SOUND_FINISH);
						st.exitQuest(true);
					} else
						htmltext = "30540-04.htm";
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
			if (st.dropQuestItems(STARSTONE, 1, 20, 500000))
				st.set("cond", "2");

		return null;
	}
}