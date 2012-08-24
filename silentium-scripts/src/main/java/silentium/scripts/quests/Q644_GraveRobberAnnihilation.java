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

public class Q644_GraveRobberAnnihilation extends Quest implements ScriptFile {
	private static final String qn = "Q644_GraveRobberAnnihilation";

	// Item
	private static final int GOODS = 8088;

	// Rewards
	private static final Map<String, int[]> Rewards = new HashMap<>();

	static {
		Rewards.put("var", new int[] { 1865, 30 });
		Rewards.put("ask", new int[] { 1867, 40 });
		Rewards.put("ior", new int[] { 1869, 30 });
		Rewards.put("coa", new int[] { 1870, 30 });
		Rewards.put("cha", new int[] { 1871, 30 });
		Rewards.put("abo", new int[] { 1872, 40 });
	}

	// NPC
	private static final int KARUDA = 32017;

	public Q644_GraveRobberAnnihilation(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { GOODS };

		addStartNpc(KARUDA);
		addTalkId(KARUDA);

		addKillId(22003, 22004, 22005, 22006, 22008);
	}

	public static void onLoad() {
		new Q644_GraveRobberAnnihilation(644, "Q644_GraveRobberAnnihilation", "Grave Robber Annihilation", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("32017-02.htm".equalsIgnoreCase(event)) {
			st.setState(QuestState.STARTED);
			st.set("cond", "1");
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (Rewards.containsKey(event)) {
			if (st.getQuestItemsCount(GOODS) == 120) {
				htmltext = "32017-04.htm";
				st.takeItems(GOODS, -1);
				st.rewardItems(Rewards.get(event)[0], Rewards.get(event)[1]);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			} else
				htmltext = "32017-07.htm";
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
				htmltext = player.getLevel() >= 20 && player.getLevel() <= 33 ? "32017-01.htm" : "32017-06.htm";
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "32017-05.htm";
				else if (cond == 2) {
					htmltext = st.getQuestItemsCount(GOODS) == 120 ? "32017-03.htm" : "32017-07.htm";
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

		if (st.dropQuestItems(GOODS, 1, 120, 500000))
			st.set("cond", "2");

		return null;
	}
}