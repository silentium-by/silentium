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

public class Q319_ScentOfDeath extends Quest implements ScriptFile {
	private static final String qn = "Q319_ScentOfDeath";

	// NPC
	private static final int MINALESS = 30138;

	// Item
	private static final int ZOMBIE_SKIN = 1045;

	public Q319_ScentOfDeath(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { ZOMBIE_SKIN };

		addStartNpc(MINALESS);
		addTalkId(MINALESS);

		addKillId(20015, 20020);
	}

	public static void onLoad() {
		new Q319_ScentOfDeath(319, "Q319_ScentOfDeath", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30138-04.htm".equalsIgnoreCase(event)) {
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
				if (player.getLevel() >= 11 && player.getLevel() <= 18)
					htmltext = "30138-03.htm";
				else {
					htmltext = "30138-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(ZOMBIE_SKIN) == 5) {
					htmltext = "30138-06.htm";
					st.takeItems(ZOMBIE_SKIN, 5);
					st.rewardItems(57, 3350);
					st.rewardItems(1060, 1);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				} else
					htmltext = "30138-05.htm";
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
			if (st.dropQuestItems(ZOMBIE_SKIN, 1, 5, 300000))
				st.set("cond", "2");

		return null;
	}
}