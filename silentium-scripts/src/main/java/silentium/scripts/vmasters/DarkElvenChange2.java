/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.scripts.vmasters;

import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;

public class DarkElvenChange2 extends OccupationEngine {
	public DarkElvenChange2(final int scriptId, final String name, final String dname, final String path) {
		super(scriptId, name, dname, path);

		for (final int i : new int[] { 31328, 30195, 30699, 30474, 31324, 30862, 30910, 31285, 31331, 31334, 31974, 32096 }) {
			addStartNpc(i);
			addTalkId(i);
		}
	}

	public static void onLoad() {
		new DarkElvenChange2(-1, "DarkElvenChange2", "", "vmasters");
	}

	@Override
	public String onTalk(final L2Npc npc, final L2PcInstance player) {
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 2) {
			if (player.getClassId().getId() == 32) // palus knight
				return "30474-01.htm";
			else if (player.getClassId().getId() == 42) // shillien oracle
				return "30474-08.htm";
			else if (player.getClassId().getId() == 35)
				return "30474-12.htm";
			else if (player.getClassId().getId() == 39) // dark wizard
				return "30474-19.htm";
			else if (player.getClassId().level() == 0) // first occupation change not made yet
				return "30474-55.htm";
			else return player.getClassId().level() >= 2 ? "30474-54.htm" : "30474-56.htm";
		} else
			return "30474-56.htm"; // other races
	}
}