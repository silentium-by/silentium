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

public class OrcOccupationChange1 extends OccupationEngine
{
	public OrcOccupationChange1(int id, String name, String descr)
	{
		super(id, name, descr);

		for (int i : new int[] { 30500, 30505, 30508, 32150 })
		{
			addStartNpc(i);
			addTalkId(i);
		}
	}

	public static void main(String[] args)
	{
		new OrcOccupationChange1(-1, "OrcOccupationChange1", "vmasters");
	}

	@Override
	public String onTalk(L2Npc npc, L2PcInstance player)
	{
		if (player.isSubClassActive())
			return null;
		if (player.getRace().ordinal() == 3)
		{
			if (player.getClassId().level() == 1) // first occupation change already made
				return npc.getNpcId() + "-21.htm";
			else if (player.getClassId().level() >= 2) // second/third occupation change already made
				return npc.getNpcId() + "-22.htm";
			else if (player.getClassId().getId() == 44) // Orc Fighter
				return npc.getNpcId() + "-01.htm";
			else if (player.getClassId().getId() == 49) // Orc Mystic
				return npc.getNpcId() + "-06.htm";
		}
		else
			return npc.getNpcId() + "-23.htm"; // other races
		return null;
	}
}