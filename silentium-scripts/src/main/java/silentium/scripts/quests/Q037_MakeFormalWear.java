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

public class Q037_MakeFormalWear extends Quest implements ScriptFile {
	private static final String qn = "Q037_MakeFormalWear";

	// NPCs
	private static final int ALEXIS = 30842;
	private static final int LEIKAR = 31520;
	private static final int JEREMY = 31521;
	private static final int MIST = 31627;

	// Items
	private static final int MYSTERIOUS_CLOTH = 7076;
	private static final int JEWEL_BOX = 7077;
	private static final int SEWING_KIT = 7078;
	private static final int DRESS_SHOES_BOX = 7113;
	private static final int SIGNET_RING = 7164;
	private static final int ICE_WINE = 7160;
	private static final int BOX_OF_COOKIES = 7159;

	// Reward
	private static final int FORMAL_WEAR = 6408;

	public Q037_MakeFormalWear(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { SIGNET_RING, ICE_WINE, BOX_OF_COOKIES };

		addStartNpc(ALEXIS);
		addTalkId(ALEXIS, LEIKAR, JEREMY, MIST);
	}

	public static void onLoad() {
		new Q037_MakeFormalWear(37, "Q037_MakeFormalWear", "Make Formal Wear");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if (event.equalsIgnoreCase("30842-1.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("31520-1.htm")) {
			st.giveItems(SIGNET_RING, 1);
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equalsIgnoreCase("31521-1.htm")) {
			st.takeItems(SIGNET_RING, 1);
			st.giveItems(ICE_WINE, 1);
			st.set("cond", "3");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equalsIgnoreCase("31627-1.htm")) {
			st.takeItems(ICE_WINE, 1);
			st.set("cond", "4");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equals("31521-3.htm")) {
			st.giveItems(BOX_OF_COOKIES, 1);
			st.set("cond", "5");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equals("31520-3.htm")) {
			st.takeItems(BOX_OF_COOKIES, 1);
			st.set("cond", "6");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equals("31520-5.htm")) {
			st.takeItems(MYSTERIOUS_CLOTH, 1);
			st.takeItems(JEWEL_BOX, 1);
			st.takeItems(SEWING_KIT, 1);
			st.set("cond", "7");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if (event.equals("31520-7.htm")) {
			st.takeItems(DRESS_SHOES_BOX, 1);
			st.giveItems(FORMAL_WEAR, 1);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(false);
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
				if (player.getLevel() >= 60)
					htmltext = "30842-0.htm";
				else {
					htmltext = "30842-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case ALEXIS:
						if (cond == 1)
							htmltext = "30842-2.htm";
						break;

					case LEIKAR:
						if (cond == 1)
							htmltext = "31520-0.htm";
						else if (cond == 2)
							htmltext = "31520-1a.htm";
						else if (cond == 5 || cond == 6) {
							if (st.getQuestItemsCount(MYSTERIOUS_CLOTH) > 0 && st.getQuestItemsCount(JEWEL_BOX) > 0 && st.getQuestItemsCount(SEWING_KIT) > 0)
								htmltext = "31520-4.htm";
							else if (st.getQuestItemsCount(BOX_OF_COOKIES) > 0)
								htmltext = "31520-2.htm";
							else
								htmltext = "31520-3a.htm";
						} else if (cond == 7) {
							if (st.getQuestItemsCount(DRESS_SHOES_BOX) > 0)
								htmltext = "31520-6.htm";
							else
								htmltext = "31520-5a.htm";
						}
						break;

					case JEREMY:
						if (st.getQuestItemsCount(SIGNET_RING) > 0)
							htmltext = "31521-0.htm";
						else if (cond == 3)
							htmltext = "31521-1a.htm";
						else if (cond == 4)
							htmltext = "31521-2.htm";
						else if (cond >= 5)
							htmltext = "31521-3a.htm";
						break;

					case MIST:
						if (cond == 3)
							htmltext = "31627-0.htm";
						else if (cond >= 4)
							htmltext = "31627-2.htm";
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