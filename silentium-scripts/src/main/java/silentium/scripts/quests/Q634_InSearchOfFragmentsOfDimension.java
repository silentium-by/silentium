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

public class Q634_InSearchOfFragmentsOfDimension extends Quest implements ScriptFile {
	private static final String qn = "Q634_InSearchOfFragmentsOfDimension";

	// Items
	private static final int DIMENSION_FRAGMENT = 7079;

	public Q634_InSearchOfFragmentsOfDimension(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		// Dimensional Gate Keepers.
		for (int i = 31494; i < 31508; i++) {
			addStartNpc(i);
			addTalkId(i);
		}

		// All mobs.
		for (int i = 21208; i < 21256; i++)
			addKillId(i);
	}

	public static void onLoad() {
		new Q634_InSearchOfFragmentsOfDimension(634, "Q634_InSearchOfFragmentsOfDimension", "In Search Of Fragments Of Dimension", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("02.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("05.htm".equalsIgnoreCase(event)) {
			st.playSound(QuestState.SOUND_FINISH);
			st.exitQuest(true);
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
				if (st.getPlayer().getLevel() >= 20)
					htmltext = "01.htm";
				else {
					htmltext = "01a.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				htmltext = "03.htm";
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

		int itemMultiplier = (int) (80 * MainConfig.RATE_QUEST_DROP) / 1000;
		final int chance = (int) (80 * MainConfig.RATE_QUEST_DROP) % 1000;

		if (Rnd.get(1000) < chance)
			itemMultiplier++;

		final int numItems = (int) (itemMultiplier * (npc.getLevel() * 0.15 + 1.6));
		if (numItems > 0) {
			st.giveItems(DIMENSION_FRAGMENT, numItems);
			st.playSound(QuestState.SOUND_ITEMGET);
		}

		return null;
	}
}