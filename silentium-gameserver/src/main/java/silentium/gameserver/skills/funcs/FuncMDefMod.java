/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.funcs;

import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.itemcontainer.Inventory;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncMDefMod extends Func
{
	static final FuncMDefMod _fpa_instance = new FuncMDefMod();

	public static Func getInstance()
	{
		return _fpa_instance;
	}

	private FuncMDefMod()
	{
		super(Stats.MAGIC_DEFENCE, 0x20, null);
	}

	@Override
	public void calc(Env env)
	{
		if (env.player instanceof L2PetInstance)
			return;

		if (env.player instanceof L2PcInstance)
		{
			L2PcInstance p = (L2PcInstance) env.player;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
				env.value -= 5;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
				env.value -= 5;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
				env.value -= 9;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
				env.value -= 9;
			if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
				env.value -= 13;
		}

		env.value *= Formulas.MENbonus[env.player.getMEN()] * env.player.getLevelMod();
	}
}