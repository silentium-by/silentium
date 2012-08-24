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

public class Q345_MethodToRaiseTheDead extends Quest implements ScriptFile {
	private static final String qn = "Q345_MethodToRaiseTheDead";

	// Items
	private static final int VICTIMS_ARM_BONE = 4274;
	private static final int VICTIMS_THIGH_BONE = 4275;
	private static final int VICTIMS_SKULL = 4276;
	private static final int VICTIMS_RIB_BONE = 4277;
	private static final int VICTIMS_SPINE = 4278;
	private static final int USELESS_BONE_PIECES = 4280;
	private static final int POWDER_TO_SUMMON_DEAD_SOULS = 4281;

	private static final int[] CORPSE_PARTS = { VICTIMS_ARM_BONE, VICTIMS_THIGH_BONE, VICTIMS_SKULL, VICTIMS_RIB_BONE, VICTIMS_SPINE };

	// NPCs
	private static final int Xenovia = 30912;
	private static final int Dorothy = 30970;
	private static final int Orpheus = 30971;
	private static final int Medium_Jar = 30973;

	// Rewards
	private static final int BILL_OF_IASON_HEINE = 4310;
	private static final int IMPERIAL_DIAMOND = 3456;

	public Q345_MethodToRaiseTheDead(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { VICTIMS_ARM_BONE, VICTIMS_THIGH_BONE, VICTIMS_SKULL, VICTIMS_RIB_BONE, VICTIMS_SPINE, POWDER_TO_SUMMON_DEAD_SOULS, USELESS_BONE_PIECES };

		addStartNpc(Dorothy);
		addTalkId(Dorothy, Xenovia, Medium_Jar, Orpheus);

		addKillId(20789, 20791);
	}

	public static void onLoad() {
		new Q345_MethodToRaiseTheDead(345, "Q345_MethodToRaiseTheDead", "Method To Raise The Dead", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30970-03.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
		} else if ("30970-06.htm".equalsIgnoreCase(event)) {
			st.set("cond", "2");
			st.playSound(QuestState.SOUND_MIDDLE);
		} else if ("30912-04.htm".equalsIgnoreCase(event)) {
			if (player.getAdena() >= 1000) {
				if (st.getQuestItemsCount(VICTIMS_ARM_BONE) + st.getQuestItemsCount(VICTIMS_THIGH_BONE) + st.getQuestItemsCount(VICTIMS_SKULL) + st.getQuestItemsCount(VICTIMS_RIB_BONE) + st.getQuestItemsCount(VICTIMS_SPINE) == 5) {
					st.set("cond", "3");
					st.takeItems(57, 1000);
					htmltext = "30912-03.htm";
					st.giveItems(POWDER_TO_SUMMON_DEAD_SOULS, 1);
					st.playSound(QuestState.SOUND_ITEMGET);
				} else
					st.set("cond", "1");
			}
		} else if ("30973-04.htm".equalsIgnoreCase(event)) {
			if (st.getInt("cond") == 3) {
				if (st.getQuestItemsCount(POWDER_TO_SUMMON_DEAD_SOULS) + st.getQuestItemsCount(VICTIMS_ARM_BONE) + st.getQuestItemsCount(VICTIMS_THIGH_BONE) + st.getQuestItemsCount(VICTIMS_SKULL) + st.getQuestItemsCount(VICTIMS_RIB_BONE) + st.getQuestItemsCount(VICTIMS_SPINE) == 6) {
					final int chance = Rnd.get(3);
					if (chance == 0) {
						st.set("cond", "6");
						htmltext = "30973-02a.htm";
					} else if (chance == 1) {
						st.set("cond", "6");
						htmltext = "30973-02b.htm";
					} else {
						st.set("cond", "7");
						htmltext = "30973-02c.htm";
					}

					st.takeItems(POWDER_TO_SUMMON_DEAD_SOULS, -1);
					st.takeItems(VICTIMS_ARM_BONE, -1);
					st.takeItems(VICTIMS_THIGH_BONE, -1);
					st.takeItems(VICTIMS_SKULL, -1);
					st.takeItems(VICTIMS_RIB_BONE, -1);
					st.takeItems(VICTIMS_SPINE, -1);

					st.playSound(QuestState.SOUND_MIDDLE);
				} else {
					st.set("cond", "1");
					st.takeItems(POWDER_TO_SUMMON_DEAD_SOULS, -1);
				}
			}
		} else if ("30971-02a.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(USELESS_BONE_PIECES) > 0)
				htmltext = "30971-02.htm";
		} else if ("30971-03.htm".equalsIgnoreCase(event)) {
			if (st.getQuestItemsCount(USELESS_BONE_PIECES) > 0) {
				final int amount = st.getQuestItemsCount(USELESS_BONE_PIECES) * 104;
				st.takeItems(USELESS_BONE_PIECES, -1);
				st.rewardItems(57, amount);
			} else
				htmltext = "30971-02a.htm";
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
				if (player.getLevel() >= 35 && player.getLevel() <= 42)
					htmltext = "30970-01.htm";
				else {
					htmltext = "30970-00.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				switch (npc.getNpcId()) {
					case Dorothy:
						if (cond == 1) {
							htmltext = st.getQuestItemsCount(VICTIMS_ARM_BONE) + st.getQuestItemsCount(VICTIMS_THIGH_BONE) + st.getQuestItemsCount(VICTIMS_SKULL) + st.getQuestItemsCount(VICTIMS_RIB_BONE) + st.getQuestItemsCount(VICTIMS_SPINE) < 5 ? "30970-04.htm" : "30970-05.htm";
						} else if (cond == 2)
							htmltext = "30970-07.htm";
						else if (cond >= 3 && cond <= 5)
							htmltext = "30970-08.htm";
						else if (cond >= 6) {
							// Shared part between cond 6 and 7.
							final int amount = st.getQuestItemsCount(USELESS_BONE_PIECES) * 70;
							st.takeItems(USELESS_BONE_PIECES, -1);

							// Scaried little girl
							if (cond == 7) {
								htmltext = "30970-10.htm";
								st.rewardItems(57, 3040 + amount);

								// Reward can be either an Imperial Diamond or bills.
								if (Rnd.get(10) < 1)
									st.giveItems(IMPERIAL_DIAMOND, 1);
								else
									st.giveItems(BILL_OF_IASON_HEINE, 5);
							}
							// Friends of Dorothy
							else {
								htmltext = "30970-09.htm";
								st.rewardItems(57, 5390 + amount);
								st.giveItems(BILL_OF_IASON_HEINE, 3);
							}
							st.playSound(QuestState.SOUND_FINISH);
							st.exitQuest(true);
						}
						break;

					case Xenovia:
						if (cond == 2)
							htmltext = "30912-01.htm";
						else if (cond >= 3)
							htmltext = "30912-06.htm";
						break;

					case Medium_Jar:
						htmltext = "30973-01.htm";
						break;

					case Orpheus:
						htmltext = "30971-01.htm";
						break;
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

		if (st.getInt("cond") == 1) {
			if (Rnd.get(100) < 66) {
				st.giveItems(USELESS_BONE_PIECES, 1);
				st.playSound(QuestState.SOUND_ITEMGET);
			} else {
				final int randomPart = CORPSE_PARTS[Rnd.get(CORPSE_PARTS.length)];
				if (st.getQuestItemsCount(randomPart) == 0) {
					st.giveItems(randomPart, 1);
					st.playSound(QuestState.SOUND_MIDDLE);
				}
			}
		}

		return null;
	}
}