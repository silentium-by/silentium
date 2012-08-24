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

public class Q167_DwarvenKinship extends Quest implements ScriptFile {
	private static final String qn = "Q167_DwarvenKinship";

	// Items
	private static final int CARLON_LETTER = 1076;
	private static final int NORMANS_LETTER = 1106;
	private static final int ADENA = 57;

	// NPCs
	private static final int CARLON = 30350;
	private static final int NORMAN = 30210;
	private static final int HAPROCK = 30255;

	public Q167_DwarvenKinship(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { CARLON_LETTER, NORMANS_LETTER };

		addStartNpc(CARLON);
		addTalkId(CARLON, HAPROCK, NORMAN);
	}

	public static void onLoad() {
		new Q167_DwarvenKinship(167, "Q167_DwarvenKinship", "Dwarven Kinship", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30350-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.giveItems(CARLON_LETTER, 1);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30255-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.takeItems(CARLON_LETTER, 1);
			st.giveItems(NORMANS_LETTER, 1);
			st.rewardItems(ADENA, 2000);
		} else if ("30255-04.htm".equalsIgnoreCase(event)) {
			st.takeItems(CARLON_LETTER, 1);
			st.rewardItems(ADENA, 3000);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
		} else if ("30210-02.htm".equalsIgnoreCase(event)) {
			st.takeItems(NORMANS_LETTER, 1);
			st.rewardItems(ADENA, 20000);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 15)
					htmltext = "30350-03.htm";
				else {
					htmltext = "30350-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case CARLON:
						if (cond == 1)
							htmltext = "30350-05.htm";
						break;

					case HAPROCK:
						if (cond == 1)
							htmltext = "30255-01.htm";
						else if (cond == 2)
							htmltext = "30255-05.htm";
						break;

					case NORMAN:
						if (cond == 2)
							htmltext = "30210-01.htm";
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