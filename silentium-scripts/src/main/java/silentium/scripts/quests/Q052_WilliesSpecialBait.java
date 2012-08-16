/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q052_WilliesSpecialBait extends Quest implements ScriptFile {
	private static final String qn = "Q052_WilliesSpecialBait";

	// NPC
	private final static int WILLIE = 31574;

	// Item
	private final static int TARLK_EYE = 7623;

	// Reward
	private final static int EARTH_FISHING_LURE = 7612;

	// Monster
	private final static int TARLK_BASILISK = 20573;

	public Q052_WilliesSpecialBait(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { TARLK_EYE };

		addStartNpc(WILLIE);
		addTalkId(WILLIE);

		addKillId(TARLK_BASILISK);
	}

	public static void onLoad() {
		new Q052_WilliesSpecialBait(52, "Q052_WilliesSpecialBait", "Willie's Special Bait");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31574-03.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("31574-07.htm") && st.getQuestItemsCount(TARLK_EYE) == 100) {
			htmltext = "31574-06.htm";
			st.rewardItems(EARTH_FISHING_LURE, 4);
			st.takeItems(TARLK_EYE, 100);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 48 && player.getLevel() <= 50)
					htmltext = "31574-01.htm";
				else {
					htmltext = "31574-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(TARLK_EYE) == 100)
					htmltext = "31574-04.htm";
				else
					htmltext = "31574-05.htm";
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 1)
			if (st.dropQuestItems(TARLK_EYE, 1, 100, 300000))
				st.set("cond", "2");

		return null;
	}
}