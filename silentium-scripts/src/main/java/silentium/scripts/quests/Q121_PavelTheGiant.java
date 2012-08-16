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

public class Q121_PavelTheGiant extends Quest implements ScriptFile {
	private static final String qn = "Q121_PavelTheGiant";

	// NPCs
	private final static int NEWYEAR = 31961;
	private final static int YUMI = 32041;

	public Q121_PavelTheGiant(int questId, String name, String descr) {
		super(questId, name, descr);

		addStartNpc(NEWYEAR);
		addTalkId(NEWYEAR, YUMI);
	}

	public static void onLoad() {
		new Q121_PavelTheGiant(121, "Q121_PavelTheGiant", "Pavel the Giant");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31961-2.htm")) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("32041-2.htm")) {
			st.playSound(QuestState.SOUND_FINISH);
			st.addExpAndSp(10000, 0);
			st.setState(QuestState.COMPLETED);
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = getNoQuestMsg();
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 46)
					htmltext = "31961-1.htm";
				else {
					htmltext = "31961-1a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case NEWYEAR:
						htmltext = "31961-2a.htm";
						break;

					case YUMI:
						htmltext = "32041-1.htm";
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