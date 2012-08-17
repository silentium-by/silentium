/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.funcs;

import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncPDefMod extends Func
{
	static final FuncPDefMod _fpa_instance = new FuncPDefMod();

	public static Func getInstance()
	{
		return _fpa_instance;
	}

	private FuncPDefMod()
	{
		super(Stats.POWER_DEFENCE, 0x20, null);
	}

	@Override
	public void calc(Env env)
	{
		if (env.player instanceof L2PcInstance)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			boolean hasMagePDef = (p.getClassId().isMage() || p.getClassId().getId() == 0x31); // orc mystics are a special case

			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
				env.value -= 12;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST) != null)
				env.value -= hasMagePDef ? 15 : 31;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null)
				env.value -= hasMagePDef ? 8 : 18;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
				env.value -= 8;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
				env.value -= 7;
		}

		env.value *= env.player.getLevelMod();
	}
}