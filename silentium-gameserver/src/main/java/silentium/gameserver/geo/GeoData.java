/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.geo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.configs.MainConfig;
import silentium.commons.utils.Point3D;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.L2Spawn;
import silentium.gameserver.model.Location;
import silentium.gameserver.model.actor.instance.L2PcInstance;

/**
 * @author -Nemesiss-
 */
public class GeoData
{
	private static Logger _log = LoggerFactory.getLogger(GeoData.class.getName());

	protected GeoData()
	{
	}

	protected GeoData(final boolean disabled)
	{
		if (disabled)
			_log.info("GeoData: the system is disabled.");
	}

	public static GeoData getInstance()
	{
		return SingletonHolder._instance;
	}

	// Public Methods
	/**
	 * @param x
	 * @param y
	 * @return Geo Block Type
	 */
	public short getType(int x, int y)
	{
		return 0;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return Nearles Z
	 */
	public short getHeight(int x, int y, int z)
	{
		return (short) z;
	}

	/**
	 * @param x
	 * @param y
	 * @param zmin
	 * @param zmax
	 * @param spawn
	 * @return
	 */
	public short getSpawnHeight(int x, int y, int zmin, int zmax, L2Spawn spawn)
	{
		return (short) zmin;
	}

	/**
	 * @param x
	 * @param y
	 * @return
	 */
	public String geoPosition(int x, int y)
	{
		return "";
	}

	/**
	 * @param cha
	 * @param target
	 * @return True if cha can see target (LOS)
	 */
	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		// If geo is off do simple check :]
		// Don't allow casting on players on different dungeon lvls etc
		return (Math.abs(target.getZ() - cha.getZ()) < 1000);
	}

	public boolean canSeeTarget(L2Object cha, Point3D worldPosition)
	{
		// If geo is off do simple check :]
		// Don't allow casting on players on different dungeon lvls etc
		return Math.abs(worldPosition.getZ() - cha.getZ()) < 1000;
	}

	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		// If geo is off do simple check :]
		// Don't allow casting on players on different dungeon lvls etc
		return (Math.abs(z - tz) < 1000);
	}

	/**
	 * @param gm
	 * @param target
	 * @return True if cha can see target (LOS) and send usful info to PC
	 */
	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		return true;
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return Geo NSWE (0-15)
	 */
	public short getNSWE(int x, int y, int z)
	{
		return 15;
	}

	public short getHeightAndNSWE(int x, int y, int z)
	{
		return (short) ((z << 1) | 15);
	}

	/**
	 * @param x
	 * @param y
	 * @param z
	 * @param tx
	 * @param ty
	 * @param tz
	 * @return Last Location (x,y,z) where player can walk - just befor wall
	 */
	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz)
	{
		return new Location(tx, ty, tz);
	}

	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return true;
	}

	/**
	 * @param gm
	 * @param comment
	 */
	public void addGeoDataBug(L2PcInstance gm, String comment)
	{
		// Do Nothing
	}

	public static void unloadGeodata(byte rx, byte ry)
	{

	}

	public static boolean loadGeodataFile(byte rx, byte ry)
	{
		return false;
	}

	public boolean hasGeo(int x, int y)
	{
		return false;
	}

	private static class SingletonHolder
	{
		protected static final GeoData _instance = MainConfig.GEODATA > 0 ? GeoEngine.getInstance() : new GeoData(true);
	}
}
