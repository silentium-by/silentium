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

public class Q356_DigUpTheSeaOfSpores extends Quest implements ScriptFile {
	private static final String qn = "Q356_DigUpTheSeaOfSpores";

	// Items
	private static final int HERB_SPORE = 5866;
	private static final int CARN_SPORE = 5865;

	// NPC
	private static final int GAUEN = 30717;

	// Monsters
	private static final int ROTTING_TREE = 20558;
	private static final int SPORE_ZOMBIE = 20562;

	public Q356_DigUpTheSeaOfSpores(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { HERB_SPORE, CARN_SPORE };

		addStartNpc(GAUEN);
		addTalkId(GAUEN);

		addKillId(ROTTING_TREE, SPORE_ZOMBIE);
	}

	public static void onLoad() {
		new Q356_DigUpTheSeaOfSpores(356, "Q356_DigUpTheSeaOfSpores", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30717-06.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30717-17.htm".equalsIgnoreCase(event)) {
			st.takeItems(HERB_SPORE, 50);
			st.takeItems(CARN_SPORE, 50);
			st.rewardItems(57, 20950);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		} else if ("30717-14.htm".equalsIgnoreCase(event)) {
			st.takeItems(HERB_SPORE, 50);
			st.takeItems(CARN_SPORE, 50);
			st.addExpAndSp(35000, 2600);
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		} else if ("30717-12.htm".equalsIgnoreCase(event)) {
			st.takeItems(HERB_SPORE, 50);
			st.addExpAndSp(24500, 0);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30717-13.htm".equalsIgnoreCase(event)) {
			st.takeItems(CARN_SPORE, 50);
			st.addExpAndSp(0, 1820);
			st.playSound(QuestState.SOUND_MIDDLE);
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
				htmltext = player.getLevel() >= 43 && player.getLevel() <= 51 ? "30717-02.htm" : "30717-01.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30717-07.htm";
				else if (cond == 2) {
					if (st.getQuestItemsCount(HERB_SPORE) >= 50)
						htmltext = "30717-08.htm";
					else htmltext = st.getQuestItemsCount(CARN_SPORE) >= 50 ? "30717-09.htm" : "30717-07.htm";
				} else if (cond == 3)
					htmltext = "30717-10.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		final int cond = st.getInt("cond");
		if (st.isStarted() && cond < 3 && Rnd.get(10) < 5) {
			switch (npc.getNpcId()) {
				case ROTTING_TREE:
					if (st.getQuestItemsCount(HERB_SPORE) < 50) {
						st.giveItems(HERB_SPORE, 1);

						if (cond == 2 && st.getQuestItemsCount(CARN_SPORE) >= 50 && st.getQuestItemsCount(HERB_SPORE) >= 50) {
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 1 && (st.getQuestItemsCount(CARN_SPORE) >= 50 || st.getQuestItemsCount(HERB_SPORE) >= 50)) {
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case SPORE_ZOMBIE:
					if (st.getQuestItemsCount(CARN_SPORE) < 50) {
						st.giveItems(CARN_SPORE, 1);

						if (cond == 2 && st.getQuestItemsCount(CARN_SPORE) >= 50 && st.getQuestItemsCount(HERB_SPORE) >= 50) {
							st.set("cond", "3");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 1 && (st.getQuestItemsCount(CARN_SPORE) >= 50 || st.getQuestItemsCount(HERB_SPORE) >= 50)) {
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