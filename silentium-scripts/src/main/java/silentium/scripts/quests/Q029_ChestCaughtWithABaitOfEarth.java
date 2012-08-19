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

public class Q029_ChestCaughtWithABaitOfEarth extends Quest implements ScriptFile {
	private static final String qn = "Q029_ChestCaughtWithABaitOfEarth";

	// NPCs
	private static final int Willie = 31574;
	private static final int Anabel = 30909;

	// Items
	private static final int SmallPurpleTreasureChest = 6507;
	private static final int SmallGlassBox = 7627;
	private static final int PlatedLeatherGloves = 2455;

	public Q029_ChestCaughtWithABaitOfEarth(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { SmallGlassBox };

		addStartNpc(Willie);
		addTalkId(Willie, Anabel);
	}

	public static void onLoad() {
		new Q029_ChestCaughtWithABaitOfEarth(29, "Q029_ChestCaughtWithABaitOfEarth", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31574-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31574-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(SmallPurpleTreasureChest) == 1) {
				st.set("cond", "2");
				st.takeItems(SmallPurpleTreasureChest, 1);
				st.giveItems(SmallGlassBox, 1);
			} else
				htmltext = "31574-08.htm";
		} else if ("30909-02.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(SmallGlassBox) == 1) {
				htmltext = "30909-02.htm";
				st.takeItems(SmallGlassBox, 1);
				st.giveItems(PlatedLeatherGloves, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			} else
				htmltext = "30909-03.htm";
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
				if (player.getLevel() >= 48 && player.getLevel() <= 50) {
					final QuestState st2 = player.getQuestState("Q052_WilliesSpecialBait");
					if (st2 != null) {
						if (st2.isCompleted())
							htmltext = "31574-01.htm";
						else {
							htmltext = "31574-02.htm";
							st.exitQuest(true);
						}
					} else {
						htmltext = "31574-03.htm";
						st.exitQuest(true);
					}
				} else
					htmltext = "31574-02.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Willie:
						if (cond == 1) {
							htmltext = "31574-05.htm";
							if (st.getQuestItemsCount(SmallPurpleTreasureChest) == 0)
								htmltext = "31574-06.htm";
						} else if (cond == 2)
							htmltext = "31574-09.htm";
						break;

					case Anabel:
						if (cond == 2)
							htmltext = "30909-01.htm";
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