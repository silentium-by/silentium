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

public class Q015_SweetWhispers extends Quest {
	private static final String qn = "Q015_SweetWhispers";

	// NPCs
	private static final int Vladimir = 31302;
	private static final int Hierarch = 31517;
	private static final int MysteriousNecromancer = 31518;

	public Q015_SweetWhispers(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		addStartNpc(Vladimir);
		addTalkId(Vladimir, Hierarch, MysteriousNecromancer);
	}

	public static void main(final String... args) {
		new Q015_SweetWhispers(15, "Q015_SweetWhispers", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31302-01.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31518-01.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("31517-01.htm".equalsIgnoreCase(event)) {
			st.addExpAndSp(60217, 0);
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
				htmltext = player.getLevel() >= 60 ? "31302-00.htm" : "31302-00a.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Vladimir:
						if (cond >= 1)
							htmltext = "31302-01a.htm";
						break;

					case MysteriousNecromancer:
						if (cond == 1)
							htmltext = "31518-00.htm";
						else if (cond == 2)
							htmltext = "31518-01a.htm";
						break;

					case Hierarch:
						if (cond == 2)
							htmltext = "31517-00.htm";
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