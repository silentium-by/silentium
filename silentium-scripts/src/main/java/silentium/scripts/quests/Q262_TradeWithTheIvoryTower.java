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

public class Q262_TradeWithTheIvoryTower extends Quest implements ScriptFile {
	private static final String qn = "Q262_TradeWithTheIvoryTower";

	// NPC
	private static final int Vollodos = 30137;

	// Item
	private static final int FUNGUS_SAC = 707;

	public Q262_TradeWithTheIvoryTower(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { FUNGUS_SAC };

		addStartNpc(Vollodos);
		addTalkId(Vollodos);

		addKillId(20400, 20007);
	}

	public static void onLoad() {
		new Q262_TradeWithTheIvoryTower(262, "Q262_TradeWithTheIvoryTower", "Trade With The Ivory Tower", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30137-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getLevel() >= 8 && player.getLevel() <= 16)
					htmltext = "30137-02.htm";
				else {
					htmltext = "30137-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				if (st.getQuestItemsCount(FUNGUS_SAC) < 10)
					htmltext = "30137-04.htm";
				else {
					htmltext = "30137-05.htm";
					st.takeItems(FUNGUS_SAC, -1);
					st.rewardItems(57, 3000);
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

		if (st.getInt("cond") == 1) {
			if (Rnd.get(10) < (npc.getNpcId() == 20400 ? 4 : 3)) {
				st.giveItems(FUNGUS_SAC, 1);

				if (st.getQuestItemsCount(FUNGUS_SAC) < 10)
					st.playSound(QuestState.SOUND_ITEMGET);
				else {
					st.set("cond", "2");
					st.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}