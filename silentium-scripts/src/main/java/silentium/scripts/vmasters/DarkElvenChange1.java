/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.vmasters;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.scripting.ScriptFile;

public class DarkElvenChange1 extends OccupationEngine implements ScriptFile {
	public DarkElvenChange1(int id, String name, String descr) {
		super(id, name, descr);

		for (int i : new int[] { 30290, 30297, 30462, 32160 }) {
			addStartNpc(i);
			addTalkId(i);
		}
	}

	public static void onLoad() {
		new DarkElvenChange1(-1, "DarkElvenChange1", "vmasters");
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player) {
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 2) {
			if (player.getClassId().level() == 1) // first occupation change already made
				return npc.getNpcId() + "-32.htm";
			else if (player.getClassId().level() >= 2) // second/third occupation change already made
				return npc.getNpcId() + "-31.htm";
			else if (player.getClassId().getId() == 31) // DE Fighter
				return npc.getNpcId() + "-01.htm";
			else if (player.getClassId().getId() == 38) // DE Mystic
				return npc.getNpcId() + "-08.htm";
		} else
			return npc.getNpcId() + "-33.htm"; // other races
		return null;
	}
}