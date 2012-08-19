/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.commons.utils.Rnd;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;
import silentium.gameserver.utils.Util;

import java.util.HashMap;
import java.util.Map;

public class Q617_GatherTheFlames extends Quest implements ScriptFile {
	private static final String qn = "Q617_GatherTheFlames";

	// NPCs
	private static final int HILDA = 31271;
	private static final int VULCAN = 31539;
	private static final int ROONEY = 32049;

	// Items
	private static final int TORCH = 7264;

	// Droplist
	private static final Map<Integer, Integer> droplist = new HashMap<>();

	static {
		droplist.put(21381, 51);
		droplist.put(21653, 51);
		droplist.put(21387, 53);
		droplist.put(21655, 53);
		droplist.put(21390, 56);
		droplist.put(21656, 69);
		droplist.put(21389, 55);
		droplist.put(21388, 53);
		droplist.put(21383, 51);
		droplist.put(21392, 56);
		droplist.put(21382, 60);
		droplist.put(21654, 52);
		droplist.put(21384, 64);
		droplist.put(21394, 51);
		droplist.put(21395, 56);
		droplist.put(21385, 52);
		droplist.put(21391, 55);
		droplist.put(21393, 58);
		droplist.put(21657, 57);
		droplist.put(21386, 52);
		droplist.put(21652, 49);
		droplist.put(21378, 49);
		droplist.put(21376, 48);
		droplist.put(21377, 48);
		droplist.put(21379, 59);
		droplist.put(21380, 49);
	}

	// Rewards
	private static final int[] reward = { 6881, 6883, 6885, 6887, 6891, 6893, 6895, 6897, 6899, 7580 };

	public Q617_GatherTheFlames(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { TORCH };

		addStartNpc(VULCAN, HILDA);
		addTalkId(VULCAN, HILDA, ROONEY);

		for (final int mobs : droplist.keySet())
			addKillId(mobs);
	}

	public static void onLoad() {
		new Q617_GatherTheFlames(617, "Q617_GatherTheFlames", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31539-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31271-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31539-05.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(TORCH) >= 1000) {
				htmltext = "31539-07.htm";
				st.takeItems(TORCH, 1000);
				st.giveItems(reward[Rnd.get(reward.length)], 1);
			}
		} else if ("31539-08.htm".equalsIgnoreCase(event)) {
			st.takeItems(TORCH, -1);
			st.exitQuest(true);
		} else if (Util.isDigit(event)) {
			if (st.getQuestItemsCount(TORCH) >= 1200) {
				htmltext = "32049-03.htm";
				st.takeItems(TORCH, 1200);
				st.giveItems(Integer.valueOf(event), 1);
			} else
				htmltext = "32049-02.htm";
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
				switch (npc.getNpcId()) {
					case VULCAN:
						if (player.getLevel() >= 74)
							htmltext = "31539-01.htm";
						else {
							htmltext = "31539-02.htm";
							st.exitQuest(true);
						}
						break;

					case HILDA:
						if (player.getLevel() >= 74)
							htmltext = "31271-02.htm";
						else {
							htmltext = "31271-01.htm";
							st.exitQuest(true);
						}
						break;
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case VULCAN:
						htmltext = st.getQuestItemsCount(TORCH) >= 1000 ? "31539-04.htm" : "31539-05.htm";
						break;

					case HILDA:
						htmltext = "31271-04.htm";
						break;

					case ROONEY:
						htmltext = st.getQuestItemsCount(TORCH) >= 1200 ? "32049-01.htm" : "32049-02.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2PcInstance partyMember = getRandomPartyMemberState(player, npc, QuestState.STARTED);
		if (partyMember == null)
			return null;

		final QuestState st = partyMember.getQuestState(qn);

		if (droplist.containsKey(npc.getNpcId())) {
			final int count = st.getQuestItemsCount(TORCH);
			final int probability = droplist.get(npc.getNpcId());
			int chance = (int) (probability * MainConfig.RATE_QUEST_DROP);
			int numItems = chance / 100;
			chance %= 100;

			if (Rnd.get(100) < chance)
				numItems++;

			if (numItems > 0) {
				if ((count + numItems) / 100 > count / 100)
					st.playSound(QuestState.SOUND_MIDDLE);
				else
					st.playSound(QuestState.SOUND_ITEMGET);

				st.giveItems(TORCH, numItems);
			}
		}

		return null;
	}
}