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

public class Q018_MeetingWithTheGoldenRam extends Quest implements ScriptFile {
	private static final String qn = "Q018_MeetingWithTheGoldenRam";

	// Items
	private static final int Adena = 57;
	private static final int SupplyBox = 7245;

	// NPCs
	private static final int Donal = 31314;
	private static final int Daisy = 31315;
	private static final int Abercrombie = 31555;

	public Q018_MeetingWithTheGoldenRam(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { SupplyBox };

		addStartNpc(Donal);
		addTalkId(Donal, Daisy, Abercrombie);
	}

	public static void onLoad() {
		new Q018_MeetingWithTheGoldenRam(18, "Q018_MeetingWithTheGoldenRam", "Meeting With The Golden Ram", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31314-03.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31315-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.giveItems(SupplyBox, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31555-02.htm".equalsIgnoreCase(event)) {
			st.takeItems(SupplyBox, 1);
			st.rewardItems(Adena, 15000);
			st.addExpAndSp(50000, 0);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 66 && player.getLevel() <= 76)
					htmltext = "31314-01.htm";
				else {
					htmltext = "31314-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Donal:
						htmltext = "31314-04.htm";
						break;

					case Daisy:
						if (cond == 1)
							htmltext = "31315-01.htm";
						else if (cond == 2)
							htmltext = "31315-03.htm";
						break;

					case Abercrombie:
						if (cond == 2)
							htmltext = "31555-01.htm";
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