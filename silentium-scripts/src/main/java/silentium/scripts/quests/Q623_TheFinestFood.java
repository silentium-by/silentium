/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q623_TheFinestFood extends Quest implements ScriptFile {
	private static final String qn = "Q623_TheFinestFood";

	// Items
	private static final int LEAF = 7199;
	private static final int MEAT = 7200;
	private static final int HORN = 7201;

	// NPC
	private static final int JEREMY = 31521;

	// Monsters
	private static final int FLAVA = 21316;
	private static final int BUFFALO = 21315;
	private static final int ANTELOPE = 21318;

	public Q623_TheFinestFood(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { LEAF, MEAT, HORN };

		addStartNpc(JEREMY);
		addTalkId(JEREMY);

		addKillId(FLAVA, BUFFALO, ANTELOPE);
	}

	public static void onLoad() {
		new Q623_TheFinestFood(623, "Q623_TheFinestFood", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31521-02.htm".equalsIgnoreCase(event)) {
			if (player.getLevel() >= 71 && player.getLevel() <= 78) {
				st.set("cond", "1");
				st.setState(QuestState.STARTED);
				st.playSound(QuestState.SOUND_ACCEPT);
			} else
				htmltext = "31521-03.htm";
		} else if ("31521-05.htm".equalsIgnoreCase(event)) {
			st.takeItems(LEAF, -1);
			st.takeItems(MEAT, -1);
			st.takeItems(HORN, -1);

			final int luck = Rnd.get(100);
			if (luck < 11) {
				st.rewardItems(57, 25000);
				st.giveItems(6849, 1);
			} else if (luck < 23) {
				st.rewardItems(57, 65000);
				st.giveItems(6847, 1);
			} else if (luck < 33) {
				st.rewardItems(57, 25000);
				st.giveItems(6851, 1);
			} else {
				st.rewardItems(57, 73000);
				st.addExpAndSp(230000, 18250);
			}

			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				htmltext = "31521-01.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "31521-06.htm";
				else if (cond == 2) {
					htmltext = st.getQuestItemsCount(LEAF) >= 100 && st.getQuestItemsCount(MEAT) >= 100 && st.getQuestItemsCount(HORN) >= 100 ? "31521-04.htm" : "31521-07.htm";
				}
				break;
		}
		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember == null)
			return null;

		final QuestState st = partyMember.getQuestState(qn);

		if (Rnd.get(100) < 66) {
			switch (npc.getNpcId()) {
				case FLAVA:
					if (st.getQuestItemsCount(LEAF) < 100) {
						st.giveItems(LEAF, 1);
						if (st.getQuestItemsCount(LEAF) >= 100 && st.getQuestItemsCount(MEAT) >= 100 && st.getQuestItemsCount(HORN) >= 100) {
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case BUFFALO:
					if (st.getQuestItemsCount(MEAT) < 100) {
						st.giveItems(MEAT, 1);
						if (st.getQuestItemsCount(LEAF) >= 100 && st.getQuestItemsCount(MEAT) >= 100 && st.getQuestItemsCount(HORN) >= 100) {
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case ANTELOPE:
					if (st.getQuestItemsCount(HORN) < 100) {
						st.giveItems(HORN, 1);
						if (st.getQuestItemsCount(LEAF) >= 100 && st.getQuestItemsCount(MEAT) >= 100 && st.getQuestItemsCount(HORN) >= 100) {
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}

		return null;
	}
}