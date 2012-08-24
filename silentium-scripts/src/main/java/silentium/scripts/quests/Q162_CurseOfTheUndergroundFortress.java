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

public class Q162_CurseOfTheUndergroundFortress extends Quest implements ScriptFile {
	private static final String qn = "Q162_CurseOfTheUndergroundFortress";

	// NPC
	private static final int Unoren = 30147;

	// Monsters
	private static final int Shade_Horror = 20033;
	private static final int Dark_Terror = 20345;
	private static final int Mist_Terror = 20371;
	private static final int Dungeon_Skeleton_Archer = 20463;
	private static final int Dungeon_Skeleton = 20464;
	private static final int Dread_Soldier = 20504;

	// Items
	private static final int BONE_FRAGMENT = 1158;
	private static final int ELF_SKULL = 1159;

	// Rewards
	private static final int ADENA = 57;
	private static final int BONE_SHIELD = 625;

	public Q162_CurseOfTheUndergroundFortress(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		questItemIds = new int[] { BONE_FRAGMENT, ELF_SKULL };

		addStartNpc(Unoren);
		addTalkId(Unoren);

		addKillId(Shade_Horror, Dark_Terror, Mist_Terror, Dungeon_Skeleton_Archer, Dungeon_Skeleton, Dread_Soldier);
	}

	public static void onLoad() {
		new Q162_CurseOfTheUndergroundFortress(162, "Q162_CurseOfTheUndergroundFortress", "Curse Of The Underground Fortress", "quests");
	}

	@Override
	public String onAdvEvent(final String event, final L2Npc npc, final L2PcInstance player) {
		final String htmltext = event;
		final QuestState st = player.getQuestState(qn);
		if (st == null)
			return htmltext;

		if ("30147-04.htm".equalsIgnoreCase(event)) {
			st.set("cond", "1");
			st.setState(QuestState.STARTED);
			st.playSound(QuestState.SOUND_ACCEPT);
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
				if (player.getRace().ordinal() == 2) {
					htmltext = "30147-00.htm";
					st.exitQuest(true);
				} else if (player.getLevel() >= 12 && player.getLevel() <= 21)
					htmltext = "30147-02.htm";
				else {
					htmltext = "30147-01.htm";
					st.exitQuest(true);
				}
				break;

			case QuestState.STARTED:
				final int cond = st.getInt("cond");
				if (cond == 1)
					htmltext = "30147-05.htm";
				else if (cond == 2 && st.getQuestItemsCount(ELF_SKULL) >= 3 && st.getQuestItemsCount(BONE_FRAGMENT) >= 10) {
					htmltext = "30147-06.htm";
					st.takeItems(ELF_SKULL, -1);
					st.takeItems(BONE_FRAGMENT, -1);
					st.rewardItems(ADENA, 24000);
					st.giveItems(BONE_SHIELD, 1);
					st.exitQuest(false);
					st.playSound(QuestState.SOUND_FINISH);
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

		if (st.getInt("cond") == 1 && Rnd.get(4) == 1) {
			switch (npc.getNpcId()) {
				case Dungeon_Skeleton:
				case Dungeon_Skeleton_Archer:
				case Dread_Soldier:
					if (st.getQuestItemsCount(BONE_FRAGMENT) < 10) {
						st.giveItems(BONE_FRAGMENT, 1);

						if (st.getQuestItemsCount(BONE_FRAGMENT) >= 10 && st.getQuestItemsCount(ELF_SKULL) >= 3) {
							st.playSound(QuestState.SOUND_MIDDLE);
							st.set("cond", "2");
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;

				case Shade_Horror:
				case Dark_Terror:
				case Mist_Terror:
					if (st.getQuestItemsCount(ELF_SKULL) < 3) {
						st.giveItems(ELF_SKULL, 1);

						if (st.getQuestItemsCount(BONE_FRAGMENT) >= 10 && st.getQuestItemsCount(ELF_SKULL) >= 3) {
							st.playSound(QuestState.SOUND_MIDDLE);
							st.set("cond", "2");
						} else
							st.playSound(QuestState.SOUND_ITEMGET);
					}
					break;
			}
		}
		return null;
	}
}