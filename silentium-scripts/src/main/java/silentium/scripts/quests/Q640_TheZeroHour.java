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

import java.util.HashMap;
import java.util.Map;

public class Q640_TheZeroHour extends Quest implements ScriptFile {
	private static final String qn = "Q640_TheZeroHour";

	// NPC
	private static final int KAHMAN = 31554;

	// Item
	private static final int FANG = 8085;

	private static final Map<String, int[]> REWARDS = new HashMap<>();

	static {
		REWARDS.put("1", new int[] { 12, 4042, 1 });
		REWARDS.put("2", new int[] { 6, 4043, 1 });
		REWARDS.put("3", new int[] { 6, 4044, 1 });
		REWARDS.put("4", new int[] { 81, 1887, 10 });
		REWARDS.put("5", new int[] { 33, 1888, 5 });
		REWARDS.put("6", new int[] { 30, 1889, 10 });
		REWARDS.put("7", new int[] { 150, 5550, 10 });
		REWARDS.put("8", new int[] { 131, 1890, 10 });
		REWARDS.put("9", new int[] { 123, 1893, 5 });
	}

	public Q640_TheZeroHour(final int questId, final String name, final String descr) {
		super(questId, name, descr);
		questItemIds = new int[] { FANG };

		addStartNpc(KAHMAN);
		addTalkId(KAHMAN);

		// All "spiked" stakatos types, except babies and cannibalistic followers.
		addKillId(22105, 22106, 22107, 22108, 22109, 22110, 22111, 22113, 22114, 22115, 22116, 22117, 22118, 22119, 22121);
	}

	public static void onLoad() {
		new Q640_TheZeroHour(640, "Q640_TheZeroHour", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31554-02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31554-05.htm".equalsIgnoreCase(event)) {
			if (!st.hasQuestItems(FANG))
				htmltext = "31554-06.htm";
		} else if ("31554-08.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
		} else if (REWARDS.containsKey(event)) {
			final int cost = REWARDS.get(event)[0];
			final int item = REWARDS.get(event)[1];
			final int amount = REWARDS.get(event)[2];

			if (st.getQuestItemsCount(FANG) >= cost) {
				st.takeItems(FANG, cost);
				st.rewardItems(item, amount);
				htmltext = "31554-09.htm";
			} else
				htmltext = "31554-06.htm";
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
				if (player.getLevel() >= 66) {
					final QuestState st2 = player.getQuestState("Q109_InSearchOfTheNest");
					htmltext = st2 != null && st2.isCompleted() ? "31554-01.htm" : "31554-10.htm";
				} else {
					htmltext = "31554-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = st.hasQuestItems(FANG) ? "31554-04.htm" : "31554-03.htm";
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

		st.giveItems(FANG, 1);
		st.playSound(QuestState.SOUND_ITEMGET);
		return null;
	}
}