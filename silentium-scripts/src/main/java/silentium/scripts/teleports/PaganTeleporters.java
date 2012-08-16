/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.teleports;

import silentium.gameserver.data.xml.DoorData;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class PaganTeleporters extends Quest implements ScriptFile {
	// Items
	private final static int VISITOR_MARK = 8064;
	private final static int FADED_VISITOR_MARK = 8065;
	private final static int PAGAN_MARK = 8067;

	public static void onLoad() {
		new PaganTeleporters(-1, "PaganTeleporters", "teleports");
	}

	public PaganTeleporters(int questId, String name, String descr) {
		super(questId, name, descr);

		addStartNpc(32034, 32035, 32036, 32037, 32039, 32040);
		addTalkId(32034, 32035, 32036, 32037, 32039, 32040);
	}

	@Override
	public String onAdvEvent(String event, L2Npc npc, L2PcInstance player) {
		if (event.equalsIgnoreCase("Close_Door1")) {
			DoorData.getInstance().getDoor(19160001).closeMe();
		} else if (event.equalsIgnoreCase("Close_Door2")) {
			DoorData.getInstance().getDoor(19160010).closeMe();
			DoorData.getInstance().getDoor(19160011).closeMe();
		}
		return super.onAdvEvent(event, npc, player);
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		String htmltext = "";
		QuestState st = player.getQuestState(getName());
		if (st == null)
			return htmltext;

		switch (npc.getNpcId()) {
			case 32034:
				if (st.hasQuestItems(VISITOR_MARK) || st.hasQuestItems(FADED_VISITOR_MARK) || st.hasQuestItems(PAGAN_MARK)) {
					if (st.hasQuestItems(VISITOR_MARK)) {
						st.takeItems(VISITOR_MARK, 1);
						st.giveItems(FADED_VISITOR_MARK, 1);
					}

					DoorData.getInstance().getDoor(19160001).openMe();
					startQuestTimer("Close_Door1", 10000, npc, player);
					htmltext = "FadedMark.htm";
				} else {
					htmltext = "32034-1.htm";
					st.exitQuest(true);
				}
				break;

			case 32035:
				DoorData.getInstance().getDoor(19160001).openMe();
				startQuestTimer("Close_Door1", 10000, npc, player);
				htmltext = "FadedMark.htm";
				break;

			case 32036:
				if (!st.hasQuestItems(PAGAN_MARK))
					htmltext = "32036-1.htm";
				else {
					DoorData.getInstance().getDoor(19160010).openMe();
					DoorData.getInstance().getDoor(19160011).openMe();
					startQuestTimer("Close_Door2", 10000, npc, player);
					htmltext = "32036-2.htm";
				}
				break;

			case 32037:
				DoorData.getInstance().getDoor(19160010).openMe();
				DoorData.getInstance().getDoor(19160011).openMe();
				startQuestTimer("Close_Door2", 10000, npc, player);
				htmltext = "FadedMark.htm";
				break;

			case 32039:
				if (player.getLevel() < 73 || (st.getQuestItemsCount(VISITOR_MARK) == 0 && st.getQuestItemsCount(FADED_VISITOR_MARK) == 0 && st.getQuestItemsCount(PAGAN_MARK) == 0)) {
					st.exitQuest(true);
				} else {
					player.teleToLocation(-12766, -35840, -10856);
					htmltext = "";
				}
				break;

			case 32040:
				player.teleToLocation(34962, -49758, -763);
				htmltext = "";
				break;
		}
		return htmltext;
	}
}