/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.utils;

import silentium.gameserver.configs.FProtectorConfig;
import silentium.gameserver.network.L2GameClient;

/**
 * Collection of flood protectors for single player.
 *
 * @author fordfrog
 */
public final class FloodProtectors
{
	private final FloodProtectorAction _rollDice;
	private final FloodProtectorAction _heroVoice;
	private final FloodProtectorAction _subclass;
	private final FloodProtectorAction _dropItem;
	private final FloodProtectorAction _serverBypass;
	private final FloodProtectorAction _multiSell;
	private final FloodProtectorAction _manufacture;
	private final FloodProtectorAction _manor;
	private final FloodProtectorAction _sendMail;
	private final FloodProtectorAction _characterSelect;

	/**
	 * Creates new instance of FloodProtectors.
	 *
	 * @param client
	 *            client for which the collection of flood protectors is being created.
	 */
	public FloodProtectors(final L2GameClient client)
	{
		super();
		_rollDice = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_ROLL_DICE);
		_heroVoice = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_HERO_VOICE);
		_subclass = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_SUBCLASS);
		_dropItem = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_DROP_ITEM);
		_serverBypass = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_SERVER_BYPASS);
		_multiSell = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_MULTISELL);
		_manufacture = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_MANUFACTURE);
		_manor = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_MANOR);
		_sendMail = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_SENDMAIL);
		_characterSelect = new FloodProtectorAction(client, FProtectorConfig.FLOOD_PROTECTOR_CHARACTER_SELECT);
	}

	public FloodProtectorAction getRollDice()
	{
		return _rollDice;
	}

	public FloodProtectorAction getHeroVoice()
	{
		return _heroVoice;
	}

	public FloodProtectorAction getSubclass()
	{
		return _subclass;
	}

	public FloodProtectorAction getDropItem()
	{
		return _dropItem;
	}

	public FloodProtectorAction getServerBypass()
	{
		return _serverBypass;
	}

	public FloodProtectorAction getMultiSell()
	{
		return _multiSell;
	}

	public FloodProtectorAction getManufacture()
	{
		return _manufacture;
	}

	public FloodProtectorAction getManor()
	{
		return _manor;
	}

	public FloodProtectorAction getSendMail()
	{
		return _sendMail;
	}

	public FloodProtectorAction getCharacterSelect()
	{
		return _characterSelect;
	}
}