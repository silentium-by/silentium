/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.funcs;

import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Formulas;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncMaxCpMul extends Func
{
	static final FuncMaxCpMul _fmcm_instance = new FuncMaxCpMul();

	public static Func getInstance()
	{
		return _fmcm_instance;
	}

	private FuncMaxCpMul()
	{
		super(Stats.MAX_CP, 0x20, null);
	}

	@Override
	public void calc(Env env)
	{
		L2PcInstance p = (L2PcInstance) env.player;
		env.value *= Formulas.CONbonus[p.getCON()];
	}
}