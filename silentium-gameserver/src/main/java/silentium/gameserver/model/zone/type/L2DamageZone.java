/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model.zone.type;

import java.util.Collection;
import java.util.concurrent.Future;

import silentium.gameserver.ThreadPoolManager;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.model.actor.L2Character;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.model.entity.Castle;
import silentium.gameserver.model.zone.L2ZoneType;
import silentium.gameserver.network.serverpackets.EtcStatusUpdate;
import silentium.gameserver.skills.Stats;

/**
 * A damage zone
 * 
 * @author durgus
 */
public class L2DamageZone extends L2ZoneType
{
	private int _damageHPPerSec;
	private Future<?> _task;

	private int _castleId;
	private Castle _castle;

	private int _startTask;
	private int _reuseTask;

	public L2DamageZone(int id)
	{
		super(id);

		_damageHPPerSec = 100; // setup default damage
		setTargetType("L2Playable"); // default only playable

		// Setup default start / reuse time
		_startTask = 10;
		_reuseTask = 5000;

		// no castle by default
		_castleId = 0;
		_castle = null;
	}

	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dmgSec"))
			_damageHPPerSec = Integer.parseInt(value);
		else if (name.equals("castleId"))
			_castleId = Integer.parseInt(value);
		else if (name.equalsIgnoreCase("initialDelay"))
			_startTask = Integer.parseInt(value);
		else if (name.equalsIgnoreCase("reuse"))
			_reuseTask = Integer.parseInt(value);
		else
			super.setParameter(name, value);
	}

	@Override
	protected void onEnter(L2Character character)
	{
		if (_task == null && _damageHPPerSec != 0)
		{
			L2PcInstance player = character.getActingPlayer();

			// Castle zone, siege and no defender
			if (getCastle() != null)
				if (!(getCastle().getSiege().getIsInProgress() && player != null && player.getSiegeState() != 2))
					return;

			synchronized (this)
			{
				if (_task == null)
					_task = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(new ApplyDamage(this), _startTask, _reuseTask);
			}
		}

		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_DANGERAREA, true);
			character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
		}
	}

	@Override
	protected void onExit(L2Character character)
	{
		if (_characterList.isEmpty() && _task != null)
			stopTask();

		if (character instanceof L2PcInstance)
		{
			character.setInsideZone(L2Character.ZONE_DANGERAREA, false);
			if (!character.isInsideZone(L2Character.ZONE_DANGERAREA))
				character.sendPacket(new EtcStatusUpdate((L2PcInstance) character));
		}
	}

	protected Collection<L2Character> getCharacterList()
	{
		return _characterList.values();
	}

	protected int getHPDamagePerSecond()
	{
		return _damageHPPerSec;
	}

	protected void stopTask()
	{
		if (_task != null)
		{
			_task.cancel(false);
			_task = null;
		}
	}

	protected Castle getCastle()
	{
		if (_castleId > 0 && _castle == null)
			_castle = CastleManager.getInstance().getCastleById(_castleId);

		return _castle;
	}

	class ApplyDamage implements Runnable
	{
		private final L2DamageZone _dmgZone;
		private final Castle _castleZone;

		ApplyDamage(L2DamageZone zone)
		{
			_dmgZone = zone;
			_castleZone = zone.getCastle();
		}

		@Override
		public void run()
		{
			boolean siege = false;

			if (_castleZone != null)
			{
				// castle zones active only during siege
				siege = _castleZone.getSiege().getIsInProgress();
				if (!siege)
				{
					_dmgZone.stopTask();
					return;
				}
			}

			for (L2Character temp : _dmgZone.getCharacterList())
			{
				if (temp != null && !temp.isDead())
				{
					if (siege)
					{
						// during siege defenders not affected
						final L2PcInstance player = temp.getActingPlayer();
						if (player != null && player.isInSiege() && player.getSiegeState() == 2)
							continue;
					}

					double multiplier = 1 + (temp.calcStat(Stats.DAMAGE_ZONE_VULN, 0, null, null) / 100);
					if (getHPDamagePerSecond() != 0)
						temp.reduceCurrentHp(_dmgZone.getHPDamagePerSecond() * multiplier, null, null);
				}
			}
		}
	}

	@Override
	public void onDieInside(L2Character character)
	{
	}

	@Override
	public void onReviveInside(L2Character character)
	{
	}
}