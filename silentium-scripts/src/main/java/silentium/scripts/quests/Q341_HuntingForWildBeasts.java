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

public class Q341_HuntingForWildBeasts extends Quest implements ScriptFile {
	private static final String qn = "Q341_HuntingForWildBeasts";

	// NPC
	private static final int PANO = 30078;

	// Item
	private static final int BEAR_SKIN = 4259;

	public Q341_HuntingForWildBeasts(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { BEAR_SKIN };

		addStartNpc(PANO);
		addTalkId(PANO);

		// Red bear, brown bear, grizzly, Dion grizzly.
		addKillId(20203, 20021, 20310, 20143);
	}

	public static void onLoad() {
		new Q341_HuntingForWildBeasts(341, "Q341_HuntingForWildBeasts", "Hunting for Wild Beasts");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30078-02.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getLevel() >= 20 && player.getLevel() <= 24)
					htmltext = "30078-01.htm";
				else {
					htmltext = "30078-00.htm";
					st.exitQuest(false);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(BEAR_SKIN) >= 20) {
					htmltext = "30078-04.htm";
					st.takeItems(BEAR_SKIN, -1);
					st.rewardItems(57, 3710);
					st.playSound(QuestState.SOUND_FINISH);
					st.exitQuest(true);
				} else
					htmltext = "30078-03.htm";
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted())
			st.dropQuestItems(BEAR_SKIN, 1, 20, 400000);

		return null;
	}
}