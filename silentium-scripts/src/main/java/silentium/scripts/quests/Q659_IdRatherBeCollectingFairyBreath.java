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

public class Q659_IdRatherBeCollectingFairyBreath extends Quest implements ScriptFile {
	private static final String qn = "Q659_IdRatherBeCollectingFairyBreath";

	// NPCs
	private static final int GALATEA = 30634;

	// Item
	private static final int FAIRY_BREATH = 8286;

	// Monsters
	private static final int SOBBING_WIND = 21023;
	private static final int BABBLING_WIND = 21024;
	private static final int GIGGLING_WIND = 21025;

	public Q659_IdRatherBeCollectingFairyBreath(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { FAIRY_BREATH };

		addStartNpc(GALATEA);
		addTalkId(GALATEA);
		addKillId(GIGGLING_WIND, BABBLING_WIND, SOBBING_WIND);
	}

	public static void onLoad() {
		new Q659_IdRatherBeCollectingFairyBreath(659, "Q659_IdRatherBeCollectingFairyBreath", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30634-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30634-06.htm".equalsIgnoreCase(event)) {
			final int count = st.getQuestItemsCount(FAIRY_BREATH);
			if (count > 0) {
				st.takeItems(FAIRY_BREATH, count);
				if (count < 10)
					st.rewardItems(57, count * 50);
				else
					st.rewardItems(57, count * 50 + 5365);
			}
		} else if ("30634-08.htm".equalsIgnoreCase(event))
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
				if (player.getLevel() >= 26)
					htmltext = "30634-02.htm";
				else {
					htmltext = "30634-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = !st.hasQuestItems(FAIRY_BREATH) ? "30634-04.htm" : "30634-05.htm";
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted() && Rnd.get(10) < 9) {
			st.giveItems(FAIRY_BREATH, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}