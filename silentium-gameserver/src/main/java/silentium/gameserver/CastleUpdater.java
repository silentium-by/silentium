/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.configs.ClansConfig;
import silentium.gameserver.instancemanager.CastleManager;
import silentium.gameserver.model.L2Clan;
import silentium.gameserver.model.entity.Castle;

/**
 * Thorgrim - 2005 Class managing periodical events with castle
 */
public class CastleUpdater implements Runnable
{
	protected static Logger _log = LoggerFactory.getLogger(CastleUpdater.class.getName());
	private final L2Clan _clan;
	private int _runCount = 0;

	public CastleUpdater(L2Clan clan, int runCount)
	{
		_clan = clan;
		_runCount = runCount;
	}

	@Override
	public void run()
	{
		try
		{
			// Move current castle treasury to clan warehouse every 2 hour
			if (_clan.getWarehouse() != null && _clan.hasCastle())
			{
				Castle castle = CastleManager.getInstance().getCastleById(_clan.getCastleId());
				if (!ClansConfig.ALT_MANOR_SAVE_ALL_ACTIONS)
				{
					if (_runCount % ClansConfig.ALT_MANOR_SAVE_PERIOD_RATE == 0)
					{
						castle.saveSeedData();
						castle.saveCropData();
						_log.info("Manor System: all data for " + castle.getName() + " saved");
					}
				}
				_runCount++;
				ThreadPoolManager.getInstance().scheduleGeneral(new CastleUpdater(_clan, _runCount), 3600000);
			}
		}
		catch (Throwable e)
		{
			e.printStackTrace();
		}
	}
}
