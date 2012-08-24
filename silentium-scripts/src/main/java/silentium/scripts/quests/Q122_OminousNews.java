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

public class Q122_OminousNews extends Quest implements ScriptFile {
	private static final String qn = "Q122_OminousNews";

	// NPCs
	private static final int MOIRA = 31979;
	private static final int KARUDA = 32017;

	public Q122_OminousNews(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(MOIRA);
		addTalkId(MOIRA, KARUDA);
	}

	public static void onLoad() {
		new Q122_OminousNews(122, "Q122_OminousNews", "Ominous News", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31979-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("32017-02.htm".equalsIgnoreCase(event)) {
			st.rewardItems(57, 1695);
			st.exitQuest(false);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getLevel() >= 20 && player.getLevel() <= 36)
					htmltext = "31979-02.htm";
				else {
					htmltext = "31979-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case MOIRA:
						htmltext = "31979-03.htm";
						break;

					case KARUDA:
						htmltext = "32017-01.htm";
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