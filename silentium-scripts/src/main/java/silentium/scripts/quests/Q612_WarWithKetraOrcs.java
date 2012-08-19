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
 * The onKill section of that quest is directly written on Q611.
 */
public class Q612_WarWithKetraOrcs extends Quest implements ScriptFile {
	private static final String qn = "Q612_WarWithKetraOrcs";

	// Items
	private static final int Seed = 7187;
	private static final int Molar = 7234;

	public Q612_WarWithKetraOrcs(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { Molar };

		addStartNpc(31377); // Ashas Varka Durai
		addTalkId(31377);
	}

	public static void onLoad() {
		new Q612_WarWithKetraOrcs(612, "Q612_WarWithKetraOrcs", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31377-03.htm".equalsIgnoreCase(event)) {
			if (player.getLevel() >= 74 && player.getAllianceWithVarkaKetra() <= -1) {
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
			} else {
				htmltext = "31377-02.htm";
				st.exitQuest(true);
			}
		} else if ("31377-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Molar) >= 100) {
				st.takeItems(Molar, 100);
				st.giveItems(Seed, 20);
				st.playSound(QuestState.SOUND_ITEMGET);
			} else
				htmltext = "31377-08.htm";
		} else if ("31377-09.htm".equalsIgnoreCase(event)) {
			st.takeItems(Molar, -1);
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
				htmltext = "31377-01.htm";
				break;

			case QuestState.STARTED:
				htmltext = st.getQuestItemsCount(Molar) > 0 ? "31377-04.htm" : "31377-05.htm";
				break;
		}

		return htmltext;
	}
}