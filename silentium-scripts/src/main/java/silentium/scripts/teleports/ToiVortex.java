/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://l2j.ru/>.
 */
package silentium.scripts.teleports;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

/**
 * @author Demon
 */

public class ToiVortex extends Quest implements ScriptFile {
	private final static int DIMENSION_VORTEX_1 = 30952;
	private final static int DIMENSION_VORTEX_2 = 30953;
	private final static int DIMENSION_VORTEX_3 = 30954;
	private final static int NPC_EXIT = 29055;

	private final static int GREEN_DIMENSION_STONE = 4401;
	private final static int BLUE_DIMENSION_STONE = 4402;
	private final static int RED_DIMENSION_STONE = 4403;

	public static void onLoad() {
		new ToiVortex(-1, "ToiVortex", "teleports");
	}

	public ToiVortex(int questId, String name, String descr) {
		super(questId, name, descr);
		addStartNpc(DIMENSION_VORTEX_1, DIMENSION_VORTEX_2, DIMENSION_VORTEX_3, NPC_EXIT);
		addTalkId(DIMENSION_VORTEX_1, DIMENSION_VORTEX_2, DIMENSION_VORTEX_3, NPC_EXIT);
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		int npcId = npc.getNpcId();
		int chance = st.getRandom(3);

		if (npcId == DIMENSION_VORTEX_1 || npcId == DIMENSION_VORTEX_2) {
			if (st.getQuestItemsCount(RED_DIMENSION_STONE) >= 1) {
				st.takeItems(RED_DIMENSION_STONE, 1);
				player.teleToLocation(118558, 16659, 5987);
			} else
				htmltext = "1.htm";
		}

		if (npcId == DIMENSION_VORTEX_2 || npcId == DIMENSION_VORTEX_3) {
			if (st.getQuestItemsCount(GREEN_DIMENSION_STONE) >= 1) {
				st.takeItems(GREEN_DIMENSION_STONE, 1);
				player.teleToLocation(110930, 15963, -4378);
			} else
				htmltext = "1.htm";
		}

		if (npcId == DIMENSION_VORTEX_1 || npcId == DIMENSION_VORTEX_3) {
			if (st.getQuestItemsCount(BLUE_DIMENSION_STONE) >= 1) {
				st.takeItems(BLUE_DIMENSION_STONE, 1);
				player.teleToLocation(114097, 19935, 935);
			} else
				htmltext = "1.htm";
		}

		if (npcId == NPC_EXIT) {
			if (chance == 0) {
				int x = 108784 + st.getRandom(100);
				int y = 16000 + st.getRandom(100);
				int z = -4928;
				player.teleToLocation(x, y, z);
			} else if (chance == 1) {
				int x = 113824 + st.getRandom(100);
				int y = 10448 + st.getRandom(100);
				int z = -5164;
				player.teleToLocation(x, y, z);
			} else {
				int x = 115488 + st.getRandom(100);
				int y = 22096 + st.getRandom(100);
				int z = -5168;
				player.teleToLocation(x, y, z);
			}
		}

		st.exitQuest(true);
		return htmltext;
	}
}