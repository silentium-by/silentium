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
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;

public class FuncHennaCON extends Func
{
	static final FuncHennaCON _fh_instance = new FuncHennaCON();

	public static Func getInstance()
	{
		return _fh_instance;
	}

	private FuncHennaCON()
	{
		super(Stats.STAT_CON, 0x10, null);
	}

	@Override
	public void calc(Env env)
	{
		L2PcInstance pc = (L2PcInstance) env.player;
		if (pc != null)
			env.value += pc.getHennaStatCON();
	}
}