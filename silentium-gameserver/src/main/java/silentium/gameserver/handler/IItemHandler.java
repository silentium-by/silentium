/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.actor.L2Playable;

/**
 * Mother class of all item.
 */
public interface IItemHandler
{
	public static Logger _log = LoggerFactory.getLogger(IItemHandler.class.getName());

	/**
	 * Launch task associated to the item.
	 * 
	 * @param playable
	 *            L2Playable designating the player
	 * @param item
	 *            L2ItemInstance designating the item to use
	 * @param forceUse
	 *            ctrl hold on item use
	 */
	public void useItem(L2Playable playable, L2ItemInstance item, boolean forceUse);
}
