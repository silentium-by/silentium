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

import java.util.HashMap;
import java.util.Map;

public class Q038_DragonFangs extends Quest implements ScriptFile {
	private static final String qn = "Q038_DragonFangs";

	// Items
	private static final int FEATHER_ORNAMENT = 7173;
	private static final int TOOTH_OF_TOTEM = 7174;
	private static final int TOOTH_OF_DRAGON = 7175;
	private static final int LETTER_OF_IRIS = 7176;
	private static final int LETTER_OF_ROHMER = 7177;

	// NPCs
	private static final int LUIS = 30386;
	private static final int IRIS = 30034;
	private static final int ROHMER = 30344;

	// Reward { item, adena }
	private static final int[][] reward = { { 45, 5200 }, { 627, 1500 }, { 1123, 3200 }, { 605, 3200 } };

	// Droplist
	private static final Map<Integer, int[]> droplist = new HashMap<>();

	static {
		droplist.put(21100, new int[] { 1, FEATHER_ORNAMENT, 100, 100 });
		droplist.put(20357, new int[] { 1, FEATHER_ORNAMENT, 100, 100 });
		droplist.put(21101, new int[] { 6, TOOTH_OF_DRAGON, 50, 50 });
		droplist.put(20356, new int[] { 6, TOOTH_OF_DRAGON, 50, 50 });
	}

	public Q038_DragonFangs(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { FEATHER_ORNAMENT, TOOTH_OF_TOTEM, TOOTH_OF_DRAGON, LETTER_OF_IRIS, LETTER_OF_ROHMER };

		addStartNpc(LUIS);
		addTalkId(LUIS, IRIS, ROHMER);

		for (final int mob : droplist.keySet())
			addKillId(mob);
	}

	public static void onLoad() {
		new Q038_DragonFangs(38, "Q038_DragonFangs", "", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30386-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30386-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "3");
			st.takeItems(FEATHER_ORNAMENT, 100);
			st.giveItems(TOOTH_OF_TOTEM, 1);
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30034-02a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(TOOTH_OF_TOTEM) == 1) {
				htmltext = "30034-02.htm";
				st.set("cond", "4");
				st.takeItems(TOOTH_OF_TOTEM, 1);
				st.giveItems(LETTER_OF_IRIS, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if ("30344-02a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(LETTER_OF_IRIS) == 1) {
				htmltext = "30344-02.htm";
				st.set("cond", "5");
				st.takeItems(LETTER_OF_IRIS, 1);
				st.giveItems(LETTER_OF_ROHMER, 1);
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if ("30034-04a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(LETTER_OF_ROHMER) == 1) {
				st.takeItems(LETTER_OF_ROHMER, 1);
				htmltext = "30034-04.htm";
				st.set("cond", "6");
				st.playSound(QuestState.SOUND_MIDDLE);
			}
		} else if ("30034-06a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(TOOTH_OF_DRAGON) == 50) {
				final int position = Rnd.get(reward.length);

				htmltext = "30034-06.htm";
				st.takeItems(TOOTH_OF_DRAGON, 50);
				st.giveItems(reward[position][0], 1);
				st.giveItems(57, reward[position][1]);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(false);
			}
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
				if (player.getLevel() >= 19 && player.getLevel() <= 29)
					htmltext = "30386-01.htm";
				else {
					st.exitQuest(true);
					htmltext = "30386-01a.htm";
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case LUIS:
						if (cond == 1)
							htmltext = "30386-02a.htm";
						else if (cond == 2)
							htmltext = "30386-03.htm";
						else if (cond >= 3)
							htmltext = "30386-03a.htm";
						break;

					case IRIS:
						if (cond == 3)
							htmltext = "30034-01.htm";
						else if (cond == 4)
							htmltext = "30034-02b.htm";
						else if (cond == 5)
							htmltext = "30034-03.htm";
						else if (cond == 6)
							htmltext = "30034-05a.htm";
						else if (cond == 7)
							htmltext = "30034-05.htm";
						break;

					case ROHMER:
						if (cond == 4)
							htmltext = "30344-01.htm";
						else if (cond >= 5)
							htmltext = "30344-03.htm";
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

		final int npcId = npc.getNpcId();

		if (droplist.containsKey(npcId)) {
			final int cond = droplist.get(npcId)[0];
			final int item = droplist.get(npcId)[1];
			final int max = droplist.get(npcId)[2];
			final int chance = droplist.get(npcId)[3];

			if (st.getInt("cond") == cond && st.getQuestItemsCount(item) < max) {
				if (Rnd.get(100) < chance) {
					st.giveItems(item, 1);

					if (st.getQuestItemsCount(item) == max) {
						st.set("cond", String.valueOf(cond + 1));
						st.playSound(QuestState.SOUND_MIDDLE);
					} else
						st.playSound(QuestState.SOUND_ITEMGET);
				}
			}
		}

		return null;
	}
}