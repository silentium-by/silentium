/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.geo.pathfinding;

import java.util.List;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.geo.pathfinding.cellnodes.CellPathFinding;
import silentium.gameserver.geo.pathfinding.geonodes.GeoPathFinding;
import silentium.gameserver.model.L2World;

/**
 * @author -Nemesiss-
 */
public abstract class PathFinding
{
	public static PathFinding getInstance()
	{
		if (!MainConfig.GEODATA_CELLFINDING)
			return GeoPathFinding.getInstance(); // Higher Memory Usage, Smaller Cpu Usage

		return CellPathFinding.getInstance(); // Cell pathfinding, calculated directly from geodata files
	}

	public abstract boolean pathNodesExist(short regionoffset);

	public abstract List<AbstractNodeLoc> findPath(int x, int y, int z, int tx, int ty, int tz, boolean playable);

	/**
	 * Convert geodata position to pathnode position
	 *
	 * @param geo_pos
	 * @return pathnode position
	 */
	public short getNodePos(int geo_pos)
	{
		return (short) (geo_pos >> 3); // OK?
	}

	/**
	 * Convert node position to pathnode block position
	 *
	 * @param node_pos
	 * @return pathnode block position (0...255)
	 */
	public short getNodeBlock(int node_pos)
	{
		return (short) (node_pos % 256);
	}

	public byte getRegionX(int node_pos)
	{
		return (byte) ((node_pos >> 8) + L2World.WORLD_X_MIN);
	}

	public byte getRegionY(int node_pos)
	{
		return (byte) ((node_pos >> 8) + L2World.WORLD_Y_MIN);
	}

	public short getRegionOffset(byte rx, byte ry)
	{
		return (short) ((rx << 5) + ry);
	}

	/**
	 * Convert pathnode x to World x position
	 *
	 * @param node_x
	 * @return
	 */
	public int calculateWorldX(short node_x)
	{
		return L2World.MAP_MIN_X + node_x * 128 + 48;
	}

	/**
	 * Convert pathnode y to World y position
	 *
	 * @param node_y
	 * @return
	 */
	public int calculateWorldY(short node_y)
	{
		return L2World.MAP_MIN_Y + node_y * 128 + 48;
	}

	public String[] getStat()
	{
		return null;
	}
}