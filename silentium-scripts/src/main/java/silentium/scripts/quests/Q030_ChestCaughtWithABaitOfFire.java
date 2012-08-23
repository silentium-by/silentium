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

public class Q030_ChestCaughtWithABaitOfFire extends Quest implements ScriptFile {
	private static final String qn = "Q030_ChestCaughtWithABaitOfFire";

	// NPCs
	private static final int Linnaeus = 31577;
	private static final int Rukal = 30629;

	// Items
	private static final int RedTreasureBox = 6511;
	private static final int MusicalScore = 7628;
	private static final int NecklaceOfProtection = 916;

	public Q030_ChestCaughtWithABaitOfFire(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { MusicalScore };

		addStartNpc(Linnaeus);
		addTalkId(Linnaeus, Rukal);
	}

	public static void onLoad() {
		new Q030_ChestCaughtWithABaitOfFire(30, "Q030_ChestCaughtWithABaitOfFire", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31577-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31577-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(RedTreasureBox) == 1) {
				st.set("cond", "2");
				st.takeItems(RedTreasureBox, 1);
				st.giveItems(MusicalScore, 1);
			} else
				htmltext = "31577-08.htm";
		} else if ("30629-02.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(MusicalScore) == 1) {
				htmltext = "30629-02.htm";
				st.takeItems(MusicalScore, 1);
				st.giveItems(NecklaceOfProtection, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			} else
				htmltext = "30629-03.htm";
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
				if (player.getLevel() >= 60 && player.getLevel() <= 62) {
					final QuestState st2 = player.getQuestState("Q053_LinnaeusSpecialBait");
					if (st2 != null) {
						if (st2.isCompleted())
							htmltext = "31577-01.htm";
						else {
							htmltext = "31577-02.htm";
							st.exitQuest(true);
						}
					} else {
						htmltext = "31577-03.htm";
						st.exitQuest(true);
					}
				} else
					htmltext = "31577-02.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Linnaeus:
						if (cond == 1) {
							htmltext = "31577-05.htm";
							if (st.getQuestItemsCount(RedTreasureBox) == 0)
								htmltext = "31577-06.htm";
						} else if (cond == 2)
							htmltext = "31577-09.htm";
						break;

					case Rukal:
						if (cond == 2)
							htmltext = "30629-01.htm";
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