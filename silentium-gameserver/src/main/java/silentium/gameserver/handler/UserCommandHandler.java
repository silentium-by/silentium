/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.handler;

import gnu.trove.map.hash.TIntObjectHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCommandHandler
{
	private static Logger _log = LoggerFactory.getLogger(UserCommandHandler.class.getName());

	private final TIntObjectHashMap<IUserCommandHandler> _datatable;

	public static UserCommandHandler getInstance()
	{
		return SingletonHolder._instance;
	}

	protected UserCommandHandler()
	{
		_datatable = new TIntObjectHashMap<>();
	}

	public void registerUserCommandHandler(IUserCommandHandler handler)
	{
		int[] ids = handler.getUserCommandList();
		for (int id : ids)
		{
			_log.debug("Adding handler for user command " + id);

			_datatable.put(new Integer(id), handler);
		}
	}

	public IUserCommandHandler getUserCommandHandler(int userCommand)
	{
		_log.debug("getting handler for user command: " + userCommand);

		return _datatable.get(new Integer(userCommand));
	}

	/**
	 * @return
	 */
	public int size()
	{
		return _datatable.size();
	}

	private static class SingletonHolder
	{
		protected static final UserCommandHandler _instance = new UserCommandHandler();
	}
}
