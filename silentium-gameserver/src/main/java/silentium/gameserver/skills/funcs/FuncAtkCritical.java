/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.funcs;

import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.L2Summon;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncAtkCritical extends Func
{
	static final FuncAtkCritical _fac_instance = new FuncAtkCritical();

	public static Func getInstance()
	{
		return _fac_instance;
	}

	private FuncAtkCritical()
	{
		super(Stats.CRITICAL_RATE, 0x09, null);
	}

	@Override
	public void calc(Env env)
	{
		L2Character p = env.player;
		if (p instanceof L2Summon)
			env.value = 40;
		else if (p instanceof L2PcInstance && p.getActiveWeaponInstance() == null)
			env.value = 40;
		else
		{
			env.value *= Formulas.DEXbonus[p.getDEX()];
			env.value *= 10;
		}
		env.baseValue = env.value;
	}
}
