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

public class Q011_SecretMeetingWithKetraOrcs extends Quest implements ScriptFile {
	private static final String qn = "Q011_SecretMeetingWithKetraOrcs";

	// Npcs
	private static final int CADMON = 31296;
	private static final int LEON = 31256;
	private static final int WAHKAN = 31371;

	// Items
	private static final int MUNITIONS_BOX = 7231;

	public Q011_SecretMeetingWithKetraOrcs(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { MUNITIONS_BOX };

		addStartNpc(CADMON);
		addTalkId(CADMON, LEON, WAHKAN);
	}

	public static void onLoad() {
		new Q011_SecretMeetingWithKetraOrcs(11, "Q011_SecretMeetingWithKetraOrcs", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31296-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31256-02.htm".equalsIgnoreCase(event)) {
			st.giveItems(MUNITIONS_BOX, 1);
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31371-02.htm".equalsIgnoreCase(event)) {
			st.takeItems(MUNITIONS_BOX, 1);
			st.addExpAndSp(79787, 0);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		}

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
				if (player.getLevel() >= 74)
					htmltext = "31296-01.htm";
				else {
					htmltext = "31296-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case CADMON:
						if (cond == 1)
							htmltext = "31296-04.htm";
						break;

					case LEON:
						if (cond == 1)
							htmltext = "31256-01.htm";
						else if (cond == 2)
							htmltext = "31256-03.htm";
						break;

					case WAHKAN:
						if (cond == 2 && st.getQuestItemsCount(MUNITIONS_BOX) > 0)
							htmltext = "31371-01.htm";
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