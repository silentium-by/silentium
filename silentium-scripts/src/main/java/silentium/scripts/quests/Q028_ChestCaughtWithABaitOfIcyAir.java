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

public class Q028_ChestCaughtWithABaitOfIcyAir extends Quest {
	private static final String qn = "Q028_ChestCaughtWithABaitOfIcyAir";

	// NPCs
	private final static int OFulle = 31572;
	private final static int Kiki = 31442;

	// Items
	private final static int BigYellowTreasureChest = 6503;
	private final static int KikisLetter = 7626;
	private final static int ElvenRing = 881;

	public Q028_ChestCaughtWithABaitOfIcyAir(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { KikisLetter };

		addStartNpc(OFulle);
		addTalkId(OFulle, Kiki);
	}

	public static void main(String[] args) {
		new Q028_ChestCaughtWithABaitOfIcyAir(28, "Q028_ChestCaughtWithABaitOfIcyAir", "Chest caught with a bait of icy air");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("31572-04.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("31572-07.htm")) {
			if (st.getQuestItemsCount(BigYellowTreasureChest) == 1) {
				st.set("cond", "2");
				st.takeItems(BigYellowTreasureChest, 1);
				st.giveItems(KikisLetter, 1);
			} else
				htmltext = "31572-08.htm";
		} else if (event.equalsIgnoreCase("31442-02.htm")) {
			if (st.getQuestItemsCount(KikisLetter) == 1) {
				htmltext = "31442-02.htm";
				st.takeItems(KikisLetter, 1);
				st.giveItems(ElvenRing, 1);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			} else
				htmltext = ("31442-03.htm");
		}

		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 36 && player.getLevel() <= 38) {
					QuestState st2 = player.getQuestState("Q051_OFullesSpecialBait");
					if (st2 != null) {
						if (st2.isCompleted())
							htmltext = "31572-01.htm";
						else {
							htmltext = "31572-02.htm";
							st.exitQuest(true);
						}
					} else {
						htmltext = "31572-03.htm";
						st.exitQuest(true);
					}
				} else
					htmltext = "31572-02.htm";
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case OFulle:
						if (cond == 1) {
							htmltext = ("31572-05.htm");
							if (st.getQuestItemsCount(BigYellowTreasureChest) == 0)
								htmltext = ("31572-06.htm");
						} else if (cond == 2)
							htmltext = ("31572-09.htm");
						break;

					case Kiki:
						if (cond == 2)
							htmltext = ("31442-01.htm");
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