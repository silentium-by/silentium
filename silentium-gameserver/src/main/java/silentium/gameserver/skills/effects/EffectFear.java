/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.skills.effects;

import silentium.gameserver.ai.CtrlIntention;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.geo.GeoData;
import silentium.gameserver.model.CharEffectList;
import silentium.gameserver.model.L2CharPosition;
import silentium.gameserver.model.L2Effect;
import silentium.gameserver.model.Location;
import silentium.gameserver.model.actor.L2Npc;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.actor.instance.L2PetInstance;
import silentium.gameserver.model.actor.instance.L2SiegeFlagInstance;
import silentium.gameserver.model.actor.instance.L2SiegeSummonInstance;
import silentium.gameserver.skills.Env;
import silentium.gameserver.templates.skills.L2EffectType;

/**
 * @author littlecrow Implementation of the Fear Effect
 */
public class EffectFear extends L2Effect
{
	public static final int FEAR_RANGE = 500;

	private int _dX = -1;
	private int _dY = -1;

	public EffectFear(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.FEAR;
	}

	/** Notify started */
	@Override
	public boolean onStart()
	{
		if (getEffected() instanceof L2PcInstance && getEffector() instanceof L2PcInstance)
		{
			switch (getSkill().getId())
			{
				case 1376:
				case 1169:
				case 65:
				case 1092:
				case 98:
				case 1272:
				case 1381:
				case 763:
					break;
				default:
					return false;
			}
		}

		if (getEffected() instanceof L2Npc || getEffected() instanceof L2SiegeFlagInstance || getEffected() instanceof L2SiegeSummonInstance)
			return false;

		if (!getEffected().isAfraid())
		{
			getEffected().abortAttack();
			getEffected().abortCast();
			getEffected().stopMove(null);

			if (getEffected().getX() > getEffector().getX())
				_dX = 1;
			if (getEffected().getY() > getEffector().getY())
				_dY = 1;

			getEffected().startFear();
			onActionTime();
			return true;
		}
		return false;
	}

	/** Notify exited */
	@Override
	public void onExit()
	{
		getEffected().stopFear(false);
	}

	@Override
	public boolean onActionTime()
	{
		int posX = getEffected().getX();
		int posY = getEffected().getY();
		int posZ = getEffected().getZ();

		if (getEffected().getX() > getEffector().getX())
			_dX = 1;
		if (getEffected().getY() > getEffector().getY())
			_dY = 1;

		posX += _dX * FEAR_RANGE;
		posY += _dY * FEAR_RANGE;

		if (MainConfig.GEODATA > 0)
		{
			Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), posX, posY, posZ);
			posX = destiny.getX();
			posY = destiny.getY();
		}

		if (!(getEffected() instanceof L2PetInstance))
			getEffected().setRunning();

		getEffected().getAI().setIntention(CtrlIntention.AI_INTENTION_MOVE_TO, new L2CharPosition(posX, posY, posZ, 0));
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see silentium.gameserver.model.L2Effect#getEffectFlags()
	 */
	@Override
	public int getEffectFlags()
	{
		return CharEffectList.EFFECT_FLAG_FEAR;
	}
}