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

public class Q615_MagicalPowerOfFire_Part1 extends Quest implements ScriptFile {
	private static final String qn = "Q615_MagicalPowerOfFire_Part1";

	// NPCs
	private static final int NARAN = 31378;
	private static final int UDAN = 31379;
	private static final int ASEFA_BOX = 31559;
	private static final int EYE = 31684;

	// Items
	private static final int KEY = 1661;
	private static final int STOLEN_RED_TOTEM = 7242;
	private static final int RED_TOTEM = 7243;
	private static final int DIVINE_STONE = 7081;

	public Q615_MagicalPowerOfFire_Part1(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { STOLEN_RED_TOTEM };

		addStartNpc(NARAN);
		addTalkId(NARAN, UDAN, ASEFA_BOX);

		// IDs aggro ranges to avoid, else quest is automatically failed.
		addAggroRangeEnterId(21350, 21351, 21353, 21354, 21355, 21357, 21358, 21360, 21361, 21362, 21369, 21370, 21364, 21365, 21366, 21368, 21371, 21372, 21373, 21374, 21375);
	}

	public static void onLoad() {
		new Q615_MagicalPowerOfFire_Part1(615, "Q615_MagicalPowerOfFire_Part1", "Magical Power Of Fire 1", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("31378-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.set("spawned", "0");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("31559-03.htm".equalsIgnoreCase(event)) {
			// You have been discovered ; quest is failed.
			if (st.getInt("spawned") == 1)
				htmltext = "31559-04.htm";
				// No Thief's Key in inventory.
			else if (st.getQuestItemsCount(KEY) == 0)
				htmltext = "31559-02.htm";
			else {
				st.set("cond", "3");
				st.takeItems(KEY, 1);
				st.giveItems(STOLEN_RED_TOTEM, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			}
		} else if ("UdanEyeDespawn".equalsIgnoreCase(event)) {
			npc.broadcastNpcSay("I'll be waiting for your return.");
			return null;
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
				if (player.getLevel() >= 74 && player.getAllianceWithVarkaKetra() <= -2)
					htmltext = "31378-01.htm";
				else {
					htmltext = "31378-02.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case NARAN:
						htmltext = "31378-04.htm";
						break;

					case UDAN:
						if (cond == 1) {
							htmltext = "31379-01.htm";
							st.set("cond", "2");
							st.playSound(QuestState.SOUND_MIDDLE);
						} else if (cond == 2) {
							if (st.getInt("spawned") == 0)
								htmltext = "31379-02.htm";
							else {
								htmltext = "31379-03.htm";
								st.set("spawned", "0");
								st.playSound(QuestState.SOUND_MIDDLE);
							}
						} else if (cond == 3 && st.getQuestItemsCount(STOLEN_RED_TOTEM) >= 1) {
							htmltext = "31379-04.htm";

							st.takeItems(STOLEN_RED_TOTEM, 1);
							st.giveItems(RED_TOTEM, 1);
							st.giveItems(DIVINE_STONE, 1);

							st.unset("spawned");
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(true);
						}
						break;

					case ASEFA_BOX:
						if (cond == 2)
							htmltext = "31559-01.htm";
						else if (cond == 3)
							htmltext = "31559-05.htm";
						break;
				}
				break;
		}

		return htmltext;
	}

	@Override
	public String onAggroRangeEnter(final L2Npc npc, final L2PcInstance player, final boolean isPet) {
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;

		if (st.getInt("spawned") == 0 && st.getInt("cond") == 2) {
			// Put "spawned" flag to 1 to avoid to spawn another.
			st.set("spawned", "1");

			// Spawn Udan's eye.
			final L2Npc udanEye = st.addSpawn(EYE, player, 10000);
			if (udanEye != null) {
				st.startQuestTimer("UdanEyeDespawn", 9000, udanEye);
				udanEye.broadcastNpcSay("You cannot escape Udan's Eye!");
				st.playSound(QuestState.SOUND_GIVEUP);
			}
		}

		return null;
	}
}