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

public class Q019_GoToThePastureland extends Quest implements ScriptFile {
	private static final String qn = "Q019_GoToThePastureland";

	// Items
	private static final int YoungWildBeastMeat = 7547;
	private static final int Adena = 57;

	// NPCs
	private static final int Vladimir = 31302;
	private static final int Tunatun = 31537;

	public Q019_GoToThePastureland(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { YoungWildBeastMeat };

		addStartNpc(Vladimir);
		addTalkId(Vladimir, Tunatun);
	}

	public static void onLoad() {
		new Q019_GoToThePastureland(19, "Q019_GoToThePastureland", "Go To The Pastureland", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31302-01.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.giveItems(YoungWildBeastMeat, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("019_finish".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(YoungWildBeastMeat) == 1) {
				htmltext = "31537-01.htm";
				st.takeItems(YoungWildBeastMeat, 1);
				st.rewardItems(Adena, 30000);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			} else
				htmltext = "31537-02.htm";
		}
		return htmltext;
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				htmltext = player.getLevel() >= 63 ? "31302-00.htm" : "31302-03.htm";
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case Vladimir:
						htmltext = "31302-02.htm";
						break;

					case Tunatun:
						htmltext = "31537-00.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}
}