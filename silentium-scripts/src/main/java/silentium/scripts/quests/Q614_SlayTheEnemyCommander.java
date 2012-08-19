/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.L2Party;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q614_SlayTheEnemyCommander extends Quest implements ScriptFile {
	private static final String qn = "Q614_SlayTheEnemyCommander";

	// Quest Items
	private static final int Tayr_Head = 7241;
	private static final int Wisdom_Feather = 7230;
	private static final int Varka_Alliance_Four = 7224;

	public Q614_SlayTheEnemyCommander(final int questId, final String name, final String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { Tayr_Head };

		addStartNpc(31377); // Ashas Varka Durai
		addTalkId(31377);

		addKillId(25302); // Tayr
	}

	public static void onLoad() {
		new Q614_SlayTheEnemyCommander(614, "Q614_SlayTheEnemyCommander", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31377-04.htm".equalsIgnoreCase(event)) {
			if (player.getAllianceWithVarkaKetra() <= -4 && st.getQuestItemsCount(Varka_Alliance_Four) > 0 && st.getQuestItemsCount(Wisdom_Feather) == 0) {
				if (player.getLevel() >= 75) {
					st.set("cond", "1");
					st.setState(QuestState.STARTED);
					st.playSound(QuestState.SOUND_ACCEPT);
				} else {
					htmltext = "31377-03.htm";
					st.exitQuest(true);
				}
			} else {
				htmltext = "31377-02.htm";
				st.exitQuest(true);
			}
		} else if ("31377-07.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(Tayr_Head) == 1) {
				st.takeItems(Tayr_Head, -1);
				st.giveItems(Wisdom_Feather, 1);
				st.addExpAndSp(10000, 0);
				st.playSound(QuestState.SOUND_FINISH);
				st.exitQuest(true);
			} else {
				htmltext = "31377-06.htm";
				st.set("cond", "1");
				st.playSound(QuestState.SOUND_ACCEPT);
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
				htmltext = "31377-01.htm";
				break;

			case QuestState.STARTED:
				htmltext = st.getQuestItemsCount(Tayr_Head) > 0 ? "31377-05.htm" : "31377-06.htm";
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final L2Party party = player.getParty();
		if (party != null) {
			for (final L2PcInstance partyMember : party.getPartyMembers()) {
				if (partyMember != null)
					rewardPlayer(partyMember);
			}
		} else
			rewardPlayer(player);

		return null;
	}

	private static void rewardPlayer(final L2PcInstance player) {
		if (player.getAllianceWithVarkaKetra() <= -4) {
			final QuestState st = player.getQuestState(qn);
			if (st.getInt("cond") == 1 && st.getQuestItemsCount(Varka_Alliance_Four) > 0) {
				st.set("cond", "2");
				st.giveItems(Tayr_Head, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		}
	}
}