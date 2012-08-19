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

/**
 * The onKill section of that quest is directly written on Q605.
 */
public class Q606_WarWithVarkaSilenos extends Quest implements ScriptFile {
	private static final String qn = "Q606_WarWithVarkaSilenos";

	// Items
	private static final int Horn = 7186;
	private static final int Mane = 7233;

	public Q606_WarWithVarkaSilenos(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { Mane };

		addStartNpc(31370); // Kadun Zu Ketra
		addTalkId(31370);
	}

	public static void onLoad() {
		new Q606_WarWithVarkaSilenos(606, "Q606_WarWithVarkaSilenos", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31370-03.htm".equalsIgnoreCase(event)) {
			if (player.getLevel() >= 74 && player.getAllianceWithVarkaKetra() >= 1) {
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
			} else {
				htmltext = "31370-02.htm";
				st.exitQuest(true);
			}
		} else if ("31370-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Mane) >= 100) {
				st.takeItems(Mane, 100);
				st.giveItems(Horn, 20);
				st.playSound(QuestState.SOUND_ITEMGET);
			} else
				htmltext = "31370-08.htm";
		} else if ("31370-09.htm".equalsIgnoreCase(event)) {
			st.takeItems(Mane, -1);
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
				htmltext = "31370-01.htm";
				break;

			case QuestState.STARTED:
				htmltext = st.getQuestItemsCount(Mane) > 0 ? "31370-04.htm" : "31370-05.htm";
				break;
		}

		return htmltext;
	}
}