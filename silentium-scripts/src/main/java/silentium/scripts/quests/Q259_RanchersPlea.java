/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.quests;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class Q259_RanchersPlea extends Quest implements ScriptFile {
	private final static String qn = "Q259_RanchersPlea";

	// NPCs
	private static final int EDMOND = 30497;
	private static final int MARIUS = 30405;

	// Monsters
	private static final int GIANT_SPIDER = 20103;
	private static final int TALON_SPIDER = 20106;
	private static final int BLADE_SPIDER = 20108;

	// Items
	private static final int GIANT_SPIDER_SKIN = 1495;

	// Rewards
	private static final int ADENA = 57;
	private static final int HEALING_POTION = 1061;
	private static final int WOODEN_ARROW = 17;

	public Q259_RanchersPlea(int questId, String name, String descr) {
		super(questId, name, descr);

		questItemIds = new int[] { GIANT_SPIDER_SKIN };

		addStartNpc(EDMOND);
		addTalkId(EDMOND, MARIUS);

		addKillId(GIANT_SPIDER, TALON_SPIDER, BLADE_SPIDER);
	}

	public static void onLoad() {
		new Q259_RanchersPlea(259, "Q259_RanchersPlea", "Rancher's Plea");
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		int count = st.getQuestItemsCount(GIANT_SPIDER_SKIN);

		if (event.equalsIgnoreCase("30497-03.htm")) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if (event.equalsIgnoreCase("30497-06.htm")) {
			st.exitQuest(true);
			st.playSound(QuestState.SOUND_FINISH);
		} else if (event.equalsIgnoreCase("30405-04.htm")) {
			if (count >= 10) {
				st.rewardItems(HEALING_POTION, 1);
				st.takeItems(GIANT_SPIDER_SKIN, 10);
			} else
				htmltext = "<html><body>Incorrect item count</body></html>";
		} else if (event.equalsIgnoreCase("30405-05.htm")) {
			if (count >= 10) {
				st.rewardItems(WOODEN_ARROW, 50);
				st.takeItems(GIANT_SPIDER_SKIN, 10);
			} else
				htmltext = "<html><body>Incorrect item count</body></html>";
		} else if (event.equalsIgnoreCase("30405-07.htm")) {
			if (count >= 10)
				htmltext = "30405-06.htm";
		}
		return htmltext;
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		QuestState st = player.getQuestState(qn);
		String htmltext = getNoQuestMsg();
		if (st == null)
			return htmltext;

		switch (st.getState()) {
			case QuestState.CREATED:
				if (player.getLevel() >= 15 && player.getLevel() <= 21)
					htmltext = "30497-02.htm";
				else {
					htmltext = "30497-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				switch (npc.getNpcId()) {
					case EDMOND:
						int count = st.getQuestItemsCount(GIANT_SPIDER_SKIN);

						if (count == 0)
							htmltext = "30497-04.htm";
						else {
							htmltext = "30497-05.htm";
							int amount = count * 25;

							if (count > 9)
								amount += 250;

							st.rewardItems(ADENA, amount);
							st.takeItems(GIANT_SPIDER_SKIN, -1);
						}
						break;

					case MARIUS:
						if (st.getQuestItemsCount(GIANT_SPIDER_SKIN) < 10)
							htmltext = "30405-01.htm";
						else
							htmltext = "30405-02.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onKill(L2Npc npc, L2PcInstance player, boolean isPet) {
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.isStarted()) {
			st.giveItems(GIANT_SPIDER_SKIN, 1);
			st.playSound(QuestState.SOUND_ITEMGET);
		}
		return null;
	}
}