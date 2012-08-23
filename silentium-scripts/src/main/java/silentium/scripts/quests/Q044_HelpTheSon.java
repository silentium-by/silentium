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

public class Q044_HelpTheSon extends Quest implements ScriptFile {
	private static final String qn = "Q044_HelpTheSon";

	// Npcs
	private static final int LUNDY = 30827;
	private static final int DRIKUS = 30505;

	// Items
	private static final int WORK_HAMMER = 168;
	private static final int GEMSTONE_FRAGMENT = 7552;
	private static final int GEMSTONE = 7553;
	private static final int PET_TICKET = 7585;

	// Monsters
	private static final int MAILLE_GUARD = 20921;
	private static final int MAILLE_SCOUT = 20920;

	public Q044_HelpTheSon(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { GEMSTONE_FRAGMENT, GEMSTONE };

		addStartNpc(LUNDY);
		addTalkId(LUNDY, DRIKUS);

		addKillId(MAILLE_GUARD, MAILLE_SCOUT);
	}

	public static void onLoad() {
		new Q044_HelpTheSon(44, "Q044_HelpTheSon", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30827-01.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30827-03.htm".equalsIgnoreCase(event) && st.getQuestItemsCount(WORK_HAMMER) >= 1) {
			st.set("cond", "2");
			st.takeItems(WORK_HAMMER, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30827-05.htm".equalsIgnoreCase(event) && st.getQuestItemsCount(GEMSTONE_FRAGMENT) >= 30) {
			st.takeItems(GEMSTONE_FRAGMENT, 30);
			st.giveItems(GEMSTONE, 1);
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30505-06.htm".equalsIgnoreCase(event) && st.getQuestItemsCount(GEMSTONE) == 1) {
			st.takeItems(GEMSTONE, 1);
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30827-07.htm".equalsIgnoreCase(event)) {
			st.giveItems(PET_TICKET, 1);
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
				if (player.getLevel() >= 24)
					htmltext = "30827-00.htm";
				else {
					htmltext = "<html><body>This quest can only be taken by characters that have a minimum level of 24. Return when you are more experienced.</body></html>";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case LUNDY:
						if (cond == 1)
							htmltext = st.getQuestItemsCount(WORK_HAMMER) == 0 ? "30827-01a.htm" : "30827-02.htm";
						else if (cond == 2)
							htmltext = "30827-03a.htm";
						else if (cond == 3)
							htmltext = "30827-04.htm";
						else if (cond == 4)
							htmltext = "30827-05a.htm";
						else if (cond == 5)
							htmltext = "30827-06.htm";
						break;

					case DRIKUS:
						if (cond == 4 && st.getQuestItemsCount(GEMSTONE) >= 1)
							htmltext = "30505-05.htm";
						else if (cond == 5)
							htmltext = "30505-06a.htm";
						break;
				}
				break;

			case QuestState.COMPLETED:
				htmltext = Quest.getAlreadyCompletedMsg();
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("cond") == 2)
			if (st.dropAlwaysQuestItems(GEMSTONE_FRAGMENT, 1, 30))
				st.set("cond", "3");

		return null;
	}
}