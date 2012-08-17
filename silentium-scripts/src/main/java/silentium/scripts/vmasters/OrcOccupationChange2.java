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

public class OrcOccupationChange2 extends OccupationEngine implements ScriptFile
{
	public OrcOccupationChange2(int id, String name, String descr)
	{
		super(id, name, descr);

		for (int npc : new int[] { 30513, 30681, 30704, 30865, 30913, 31288, 31326, 31977 })
		{
			addStartNpc(npc);
			addTalkId(npc);
		}
	}

	public static void onLoad()
	{
		new OrcOccupationChange2(-1, "OrcOccupationChange2", "vmasters");
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 3)
		{
			if (player.getClassId().getId() == 47)
				return "30513-01.htm";
			else if (player.getClassId().getId() == 45)
				return "30513-05.htm";
			else if (player.getClassId().getId() == 50)
				return "30513-09.htm";
			else if (player.getClassId().level() == 0)
				return "30513-33.htm";
			else if (player.getClassId().level() >= 2)
				return "30513-32.htm";
		}
		else
			return "30513-34.htm";
		return null;
	}
}
