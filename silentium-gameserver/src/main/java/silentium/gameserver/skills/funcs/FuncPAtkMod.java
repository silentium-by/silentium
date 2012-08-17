/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.funcs;

import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncPAtkMod extends Func
{
	static final FuncPAtkMod _fpa_instance = new FuncPAtkMod();

	public static Func getInstance()
	{
		return _fpa_instance;
	}

	private FuncPAtkMod()
	{
		super(Stats.POWER_ATTACK, 0x30, null);
	}

	@Override
	public void calc(Env env)
	{
		if (env.player instanceof L2PetInstance)
			return;

		env.value *= Formulas.STRbonus[env.player.getSTR()] * env.player.getLevelMod();
	}
}