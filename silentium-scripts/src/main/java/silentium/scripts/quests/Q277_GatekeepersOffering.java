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

public class Q277_GatekeepersOffering extends Quest implements ScriptFile {
	private static final String qn = "Q277_GatekeepersOffering";

	// NPC
	private static final int TAMIL = 30576;

	// Item
	private static final int STARSTONE = 1572;

	// Reward
	private static final int GATEKEEPER_CHARM = 1658;

	// Monster
	private static final int GRAYSTONE_GOLEM = 20333;

	public Q277_GatekeepersOffering(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(TAMIL);
		addTalkId(TAMIL);

		addKillId(GRAYSTONE_GOLEM);
	}

	public static void onLoad() {
		new Q277_GatekeepersOffering(277, "Q277_GatekeepersOffering", "Gatekeepers Offering", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30576-03.htm".equalsIgnoreCase(event)) {
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
					htmltext = "30576-02.htm";
				else {
					htmltext = "30576-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1 && st.getQuestItemsCount(STARSTONE) < 20)
					htmltext = "30576-04.htm";
				else if (cond == 2 && st.getQuestItemsCount(STARSTONE) >= 20) {
					htmltext = "30576-05.htm";
					st.takeItems(STARSTONE, -1);
					st.rewardItems(GATEKEEPER_CHARM, 2);
					st.exitQuest(true);
					st.playSound(QuestState.SOUND_FINISH);
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
			if (st.dropQuestItems(STARSTONE, 1, 20, 200000))
				st.set("cond", "2");

		return null;
	}
}