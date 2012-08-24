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

public class Q661_MakingTheHarvestGroundsSafe extends Quest implements ScriptFile {
	private static final String qn = "Q661_MakingTheHarvestGroundsSafe";

	// NPC
	private static final int NORMAN = 30210;

	// Items
	private static final int STING_OF_GIANT_PB = 8283;
	private static final int CLOUDY_GEM = 8284;
	private static final int TALON_OF_YA = 8285;

	// Reward
	private static final int ADENA = 57;

	// Monsters
	private static final int GIANT_PB = 21095;
	private static final int CLOUDY_BEAST = 21096;
	private static final int YOUNG_ARANEID = 21097;

	public Q661_MakingTheHarvestGroundsSafe(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { STING_OF_GIANT_PB, CLOUDY_GEM, TALON_OF_YA };

		addStartNpc(NORMAN);
		addTalkId(NORMAN);

		addKillId(GIANT_PB, CLOUDY_BEAST, YOUNG_ARANEID);
	}

	public static void onLoad() {
		new Q661_MakingTheHarvestGroundsSafe(661, "Q661_MakingTheHarvestGroundsSafe", "Making The Harvest Grounds Safe", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30210-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30210-04.htm".equalsIgnoreCase(event)) {
			final int item1 = st.getQuestItemsCount(STING_OF_GIANT_PB);
			final int item2 = st.getQuestItemsCount(CLOUDY_GEM);
			final int item3 = st.getQuestItemsCount(TALON_OF_YA);
			int sum = 0;

			sum = item1 * 57 + item2 * 56 + item3 * 60;

			if (item1 + item2 + item3 >= 10)
				sum += 2871;

			st.takeItems(STING_OF_GIANT_PB, item1);
			st.takeItems(CLOUDY_GEM, item2);
			st.takeItems(TALON_OF_YA, item3);
			st.rewardItems(ADENA, sum);
		} else if ("30210-06.htm".equalsIgnoreCase(event))
			st.exitQuest(true);

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
				if (player.getLevel() >= 21)
					htmltext = "30210-01.htm";
				else {
					htmltext = "30210-01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = st.hasQuestItems(STING_OF_GIANT_PB) || st.hasQuestItems(CLOUDY_GEM) || st.hasQuestItems(TALON_OF_YA) ? "30210-03.htm" : "30210-05.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted() && Rnd.get(10) < 5) {
			switch (npc.getNpcId()) {
				case GIANT_PB:
					st.giveItems(STING_OF_GIANT_PB, 1);
					break;
				case CLOUDY_BEAST:
					st.giveItems(CLOUDY_GEM, 1);
					break;
				case YOUNG_ARANEID:
					st.giveItems(TALON_OF_YA, 1);
					break;
			}
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}