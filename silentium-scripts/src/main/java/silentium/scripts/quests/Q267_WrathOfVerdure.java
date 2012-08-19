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

public class Q267_WrathOfVerdure extends Quest implements ScriptFile {
	private static final String qn = "Q267_WrathOfVerdure";

	// Items
	private static final int GOBLIN_CLUB = 1335;

	// Reward
	private static final int SILVERY_LEAF = 1340;

	// NPC
	private static final int TREANT_BREMEC = 31853;

	// Mob
	private static final int GOBLIN = 20325;

	public Q267_WrathOfVerdure(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { GOBLIN_CLUB };

		addStartNpc(TREANT_BREMEC);
		addTalkId(TREANT_BREMEC);

		addKillId(GOBLIN);
	}

	public static void onLoad() {
		new Q267_WrathOfVerdure(267, "Q267_WrathOfVerdure", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31853-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31853-06.htm".equalsIgnoreCase(event)) {
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
				if (player.getRace().ordinal() == 1) {
					if (player.getLevel() >= 4 && player.getLevel() <= 9)
						htmltext = "31853-02.htm";
					else {
						htmltext = "31853-01.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "31853-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int count = st.getQuestItemsCount(GOBLIN_CLUB);

				if (count > 0) {
					htmltext = "31853-05.htm";
					st.takeItems(GOBLIN_CLUB, -1);
					st.rewardItems(SILVERY_LEAF, count);
				} else
					htmltext = "31853-04.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 1 && Rnd.get(10) < 5) {
			st.giveItems(GOBLIN_CLUB, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}