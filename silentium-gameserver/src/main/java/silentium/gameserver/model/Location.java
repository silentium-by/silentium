/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.model;

import silentium.commons.utils.Point3D;
import silentium.gameserver.model.actor.L2Character;

public class Location extends Point3D
{
	public volatile int _heading;
	private static final long serialVersionUID = -8892572567626311527L;

	public Location(int x, int y, int z)
	{
		super(x, y, z);
		_heading = 0;
	}

	public Location(int x, int y, int z, int heading)
	{
		super(x, y, z);
		_heading = heading;
	}

	public Location(L2Object obj)
	{
		this(obj.getX(), obj.getY(), obj.getZ());
	}

	public Location(L2Character obj)
	{
		this((L2Object) obj);
		_heading = obj.getHeading();
	}

	public int getHeading()
	{
		return _heading;
	}
}