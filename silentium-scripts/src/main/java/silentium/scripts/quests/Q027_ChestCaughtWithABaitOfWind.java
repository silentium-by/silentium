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

public class Q027_ChestCaughtWithABaitOfWind extends Quest implements ScriptFile {
	private static final String qn = "Q027_ChestCaughtWithABaitOfWind";

	// NPCs
	private static final int Lanosco = 31570;
	private static final int Shaling = 31442;

	// Items
	private static final int LargeBlueTreasureChest = 6500;
	private static final int StrangeBlueprint = 7625;
	private static final int BlackPearlRing = 880;

	public Q027_ChestCaughtWithABaitOfWind(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { StrangeBlueprint };

		addStartNpc(Lanosco);
		addTalkId(Lanosco, Shaling);
	}

	public static void onLoad() {
		new Q027_ChestCaughtWithABaitOfWind(27, "Q027_ChestCaughtWithABaitOfWind", "Ches tCaught With A Bait Of Wind", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31570-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31570-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(LargeBlueTreasureChest) == 1) {
				st.set("cond", "2");
				st.takeItems(LargeBlueTreasureChest, 1);
				st.giveItems(StrangeBlueprint, 1);
			} else
				htmltext = "31570-08.htm";
		} else if ("31434-02.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(StrangeBlueprint) == 1) {
				htmltext = "31434-02.htm";
				st.takeItems(StrangeBlueprint, 1);
				st.giveItems(BlackPearlRing, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			} else
				htmltext = "31434-03.htm";
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
				if (player.getLevel() >= 27 && player.getLevel() <= 29) {
					final QuestState st2 = player.getQuestState("Q050_LanoscosSpecialBait");
					if (st2 != null) {
						if (st2.isCompleted())
							htmltext = "31570-01.htm";
						else {
							htmltext = "31570-02.htm";
							st.exitQuest(true);
						}
					} else {
						htmltext = "31570-03.htm";
						st.exitQuest(true);
					}
				} else
					htmltext = "31570-02.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Lanosco:
						if (cond == 1) {
							htmltext = "31570-05.htm";
							if (st.getQuestItemsCount(LargeBlueTreasureChest) == 0)
								htmltext = "31570-06.htm";
						} else if (cond == 2)
							htmltext = "31570-09.htm";
						break;

					case Shaling:
						if (cond == 2)
							htmltext = "31434-01.htm";
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