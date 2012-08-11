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
import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncAtkAccuracy extends Func
{
	static final FuncAtkAccuracy _faa_instance = new FuncAtkAccuracy();

	public static Func getInstance()
	{
		return _faa_instance;
	}

	private FuncAtkAccuracy()
	{
		super(Stats.ACCURACY_COMBAT, 0x10, null);
	}

	@Override
	public void calc(Env env)
	{
		L2Character p = env.player;

		env.value += Math.sqrt(p.getDEX()) * 6;
		env.value += p.getLevel();

		if (p instanceof L2Summon)
			env.value += (p.getLevel() < 60) ? 4 : 5;
	}
}