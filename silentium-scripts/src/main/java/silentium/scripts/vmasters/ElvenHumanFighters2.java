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
import silentium.gameserver.scripting.ScriptFile;

public class ElvenHumanFighters2 extends OccupationEngine implements ScriptFile
{
	public ElvenHumanFighters2(int id, String name, String descr)
	{
		super(id, name, descr);
		for (int i : new int[] { 30109, 30187, 30689, 30849, 30900, 31965, 32094 })
		{
			addStartNpc(i);
			addTalkId(i);
		}
	}

	public static void onLoad()
	{
		new ElvenHumanFighters2(-1, "ElvenHumanFighters2", "vmasters");
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 0 || player.getRace().ordinal() == 1)
		{
			if (player.getClassId().getId() == 19) // elven knight
				return "30120-01.htm";
			else if (player.getClassId().getId() == 4) // human knight
				return "30120-08.htm";
			else if (player.getClassId().getId() == 7) // rogue
				return "30120-15.htm";
			else if (player.getClassId().getId() == 22) // elven scout
				return "30120-22.htm";
			else if (player.getClassId().getId() == 1) // human warrior
				return "30120-29.htm";
			else if (player.getClassId().level() == 0) // first occupation change not made yet
				return "30120-76.htm";
			else if (player.getClassId().level() >= 2) // second/third occupation change already made
				return "30120-77.htm";
			else
				return "30120-78.htm"; // other conditions
		}
		else
			return "30120-78.htm"; // other races
	}
}