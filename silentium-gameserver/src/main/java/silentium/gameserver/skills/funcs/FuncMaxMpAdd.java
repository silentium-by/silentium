/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.funcs;

import silentium.gameserver.skills.Env;
import silentium.gameserver.skills.Stats;
import silentium.gameserver.skills.basefuncs.Func;
import silentium.gameserver.templates.chars.L2PcTemplate;

public class FuncMaxMpAdd extends Func
{
	static final FuncMaxMpAdd _fmma_instance = new FuncMaxMpAdd();

	public static Func getInstance()
	{
		return _fmma_instance;
	}

	private FuncMaxMpAdd()
	{
		super(Stats.MAX_MP, 0x10, null);
	}

	@Override
	public void calc(Env env)
	{
		L2PcTemplate t = (L2PcTemplate) env.player.getTemplate();
		int lvl = env.player.getLevel() - t.classBaseLevel;

		// This is to prevent Players having only 1 MP
		if (lvl < 0)
			lvl = 0;

		double mpmod = t.lvlMpMod * lvl;
		double mpmax = (t.lvlMpAdd + mpmod) * lvl;
		double mpmin = (t.lvlMpAdd * lvl) + mpmod;
		env.value += (mpmax + mpmin) / 2;
	}
}