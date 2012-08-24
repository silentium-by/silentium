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

public class Q260_HuntTheOrcs extends Quest implements ScriptFile {
	private static final String qn = "Q260_HuntTheOrcs";

	// NPC
	private static final int RAYEN = 30221;

	// Items
	private static final int ORC_AMULET = 1114;
	private static final int ORCS_NECKLACE = 1115;

	// Monsters
	private static final int KABOO_ORC = 20468;
	private static final int KABOO_ORC_ARCHER = 20469;
	private static final int KABOO_ORC_GRUNT = 20470;
	private static final int KABOO_ORC_FIGHTER = 20471;
	private static final int KABOO_ORC_FIGHTER_LEADER = 20472;
	private static final int KABOO_ORC_FIGHTER_LIEUTENANT = 20473;

	public Q260_HuntTheOrcs(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { ORC_AMULET, ORCS_NECKLACE };

		addStartNpc(RAYEN);
		addTalkId(RAYEN);

		addKillId(KABOO_ORC, KABOO_ORC_ARCHER, KABOO_ORC_GRUNT, KABOO_ORC_FIGHTER, KABOO_ORC_FIGHTER_LEADER, KABOO_ORC_FIGHTER_LIEUTENANT);
	}

	public static void onLoad() {
		new Q260_HuntTheOrcs(260, "Q260_HuntTheOrcs", "Hunt The Orcs", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30221-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30221-06.htm".equalsIgnoreCase(event)) {
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_FINISH);
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
				if (player.getRace().ordinal() == 1) {
					if (player.getLevel() >= 6 && player.getLevel() <= 16)
						htmltext = "30221-02.htm";
					else {
						htmltext = "30221-01.htm";
						st.exitQuest(true);
					}
				} else {
					htmltext = "30221-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int amulet = st.getQuestItemsCount(ORC_AMULET);
				final int necklace = st.getQuestItemsCount(ORCS_NECKLACE);

				if (amulet == 0 && necklace == 0)
					htmltext = "30221-04.htm";
				else {
					htmltext = "30221-05.htm";
					st.takeItems(ORC_AMULET, -1);
					st.takeItems(ORCS_NECKLACE, -1);
					st.giveItems(57, amulet * 5 + necklace * 15);
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted() && Rnd.get(10) < 4) {
			switch (npc.getNpcId()) {
				case KABOO_ORC:
				case KABOO_ORC_GRUNT:
				case KABOO_ORC_ARCHER:
					st.giveItems(ORC_AMULET, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
					break;

				case KABOO_ORC_FIGHTER:
				case KABOO_ORC_FIGHTER_LEADER:
				case KABOO_ORC_FIGHTER_LIEUTENANT:
					st.giveItems(ORCS_NECKLACE, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
					break;
			}
		}
		return null;
	}
}