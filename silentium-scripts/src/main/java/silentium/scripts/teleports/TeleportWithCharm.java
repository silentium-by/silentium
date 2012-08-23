/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.teleports;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.quest.Quest;
import silentium.gameserver.model.quest.QuestState;
import silentium.gameserver.scripting.ScriptFile;

public class TeleportWithCharm extends Quest implements ScriptFile {
	private static final int WHIRPY = 30540;
	private static final int TAMIL = 30576;

	private static final int ORC_GATEKEEPER_CHARM = 1658;
	private static final int DWARF_GATEKEEPER_TOKEN = 1659;

	public static void onLoad() {
		new TeleportWithCharm(-1, "TeleportWithCharm", "Teleport With Charm", "teleports");
	}

	public TeleportWithCharm(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		addStartNpc(WHIRPY, TAMIL);
		addTalkId(WHIRPY, TAMIL);
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		final QuestState st = player.getQuestState(getName());
		String htmltext = "";

		final int npcId = npc.getNpcId();
		if (npcId == WHIRPY) {
			if (st.getQuestItemsCount(DWARF_GATEKEEPER_TOKEN) >= 1) {
				st.takeItems(DWARF_GATEKEEPER_TOKEN, 1);
				player.teleToLocation(-80826, 149775, -3043);
			} else
				htmltext = "30540-01.htm";
		} else if (npcId == TAMIL) {
			if (st.getQuestItemsCount(ORC_GATEKEEPER_CHARM) >= 1) {
				st.takeItems(ORC_GATEKEEPER_CHARM, 1);
				player.teleToLocation(-80826, 149775, -3043);
			} else
				htmltext = "30576-01.htm";
		}

		st.exitQuest(true);
		return htmltext;
	}
}