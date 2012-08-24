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

public class Q299_GatherIngredientsForPie extends Quest implements ScriptFile {
	private static final String qn = "Q299_GatherIngredientsForPie";

	// NPCs
	private static final int LARA = 30063;
	private static final int BRIGHT = 30466;
	private static final int EMILY = 30620;

	// Items
	private static final int FRUIT_BASKET = 7136;
	private static final int AVELLAN_SPICE = 7137;
	private static final int HONEY_POUCH = 7138;

	// Monsters
	private static final int WASP_WORKER = 20934;
	private static final int WASP_LEADER = 20935;

	public Q299_GatherIngredientsForPie(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { FRUIT_BASKET, AVELLAN_SPICE, HONEY_POUCH };

		addStartNpc(EMILY);
		addTalkId(EMILY, LARA, BRIGHT);

		addKillId(WASP_WORKER, WASP_LEADER);
	}

	public static void onLoad() {
		new Q299_GatherIngredientsForPie(299, "Q299_GatherIngredientsForPie", "Gather Ingredients For Pie", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30620-1.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30620-3.htm".equalsIgnoreCase(event)) {
			st.set("cond", "3");
			st.takeItems(HONEY_POUCH, -1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30063-1.htm".equalsIgnoreCase(event)) {
			st.set("cond", "4");
			st.giveItems(AVELLAN_SPICE, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30620-5.htm".equalsIgnoreCase(event)) {
			st.set("cond", "5");
			st.takeItems(AVELLAN_SPICE, -1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30466-1.htm".equalsIgnoreCase(event)) {
			st.set("cond", "6");
			st.giveItems(FRUIT_BASKET, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30620-7a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(FRUIT_BASKET) >= 1) {
				htmltext = "30620-7.htm";
				st.takeItems(FRUIT_BASKET, -1);
				st.rewardItems(57, 25000);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			} else
				st.set("cond", "5");
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
				if (player.getLevel() >= 34 && player.getLevel() <= 40)
					htmltext = "30620-0.htm";
				else {
					htmltext = "30620-0a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case EMILY:
						if (cond == 1)
							htmltext = "30620-1a.htm";
						else if (cond == 2) {
							if (st.getQuestItemsCount(HONEY_POUCH) >= 100)
								htmltext = "30620-2.htm";
							else {
								htmltext = "30620-2a.htm";
								st.exitQuest(true);
							}
						} else if (cond == 3)
							htmltext = "30620-3a.htm";
						else if (cond == 4) {
							if (st.getQuestItemsCount(AVELLAN_SPICE) >= 1)
								htmltext = "30620-4.htm";
							else {
								htmltext = "30620-4a.htm";
								st.exitQuest(true);
							}
						} else if (cond == 5)
							htmltext = "30620-5a.htm";
						else if (cond == 6)
							htmltext = "30620-6.htm";
						break;

					case LARA:
						if (cond == 3)
							htmltext = "30063-0.htm";
						else if (cond >= 4)
							htmltext = "30063-1a.htm";
						break;

					case BRIGHT:
						if (cond == 5)
							htmltext = "30466-0.htm";
						else if (cond >= 6)
							htmltext = "30466-1a.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMember(player, npc, "1");
		if (partyMember != null)
			partyMember.getQuestState(qn).dropQuestItems(HONEY_POUCH, 1, 100, 500000);

		return null;
	}
}