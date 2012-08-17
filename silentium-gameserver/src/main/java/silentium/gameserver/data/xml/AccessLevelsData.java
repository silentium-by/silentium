/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.configs.PlayersConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.model.L2AccessLevel;

/**
 * @author FBIagent
 */
public class AccessLevelsData
{
	private static Logger _log = LoggerFactory.getLogger(AccessLevelsData.class.getName());

	public static final int _masterAccessLevelNum = PlayersConfig.MASTERACCESS_LEVEL;
	public static L2AccessLevel _masterAccessLevel = new L2AccessLevel(_masterAccessLevelNum, "Master Access", PlayersConfig.MASTERACCESS_NAME_COLOR, PlayersConfig.MASTERACCESS_TITLE_COLOR, null, true, true, true, true, true, true, true, true);

	public static final int _userAccessLevelNum = 0;
	public static L2AccessLevel _userAccessLevel = new L2AccessLevel(_userAccessLevelNum, "User", 0xFFFFFF, 0xFFFF77, null, false, false, false, true, false, true, true, true);

	private final TIntObjectHashMap<L2AccessLevel> _accessLevels;

	public static AccessLevelsData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected AccessLevelsData()
	{
		_accessLevels = new TIntObjectHashMap<>();

		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/access_levels.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			int accessLevel, nameColor, titleColor;
			String name, childs;
			boolean isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp = false;

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("access"))
				{
					accessLevel = Integer.valueOf(d.getAttributes().getNamedItem("level").getNodeValue());
					name = String.valueOf(d.getAttributes().getNamedItem("name").getNodeValue());

					if (accessLevel == _userAccessLevelNum)
					{
						_log.warn("AccessLevelsData: Access level " + name + " is using reserved user access level " + _userAccessLevelNum + ". Ignoring it!");
						continue;
					}
					else if (accessLevel == _masterAccessLevelNum)
					{
						_log.warn("AccessLevelsData: Access level " + name + " is using reserved master access level " + _masterAccessLevelNum + ". Ignoring it!");
						continue;
					}
					else if (accessLevel < 0)
					{
						_log.warn("AccessLevelsData: Access level " + name + " is using banned access level (below 0). Ignoring it!");
						continue;
					}

					try
					{
						nameColor = Integer.decode("0x" + String.valueOf(d.getAttributes().getNamedItem("nameColor").getNodeValue()));
					}
					catch (NumberFormatException nfe)
					{
						nameColor = Integer.decode("0xFFFFFF");
					}

					try
					{
						titleColor = Integer.decode("0x" + String.valueOf(d.getAttributes().getNamedItem("titleColor").getNodeValue()));
					}
					catch (NumberFormatException nfe)
					{
						titleColor = Integer.decode("0x77FFFF");
					}

					childs = String.valueOf(d.getAttributes().getNamedItem("childAccess").getNodeValue());
					isGm = Boolean.valueOf(d.getAttributes().getNamedItem("isGm").getNodeValue());
					allowPeaceAttack = Boolean.valueOf(d.getAttributes().getNamedItem("allowPeaceAttack").getNodeValue());
					allowFixedRes = Boolean.valueOf(d.getAttributes().getNamedItem("allowFixedRes").getNodeValue());
					allowTransaction = Boolean.valueOf(d.getAttributes().getNamedItem("allowTransaction").getNodeValue());
					allowAltG = Boolean.valueOf(d.getAttributes().getNamedItem("allowAltg").getNodeValue());
					giveDamage = Boolean.valueOf(d.getAttributes().getNamedItem("giveDamage").getNodeValue());
					takeAggro = Boolean.valueOf(d.getAttributes().getNamedItem("takeAggro").getNodeValue());
					gainExp = Boolean.valueOf(d.getAttributes().getNamedItem("gainExp").getNodeValue());

					_accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, name, nameColor, titleColor, childs.isEmpty() ? null : childs, isGm, allowPeaceAttack, allowFixedRes, allowTransaction, allowAltG, giveDamage, takeAggro, gainExp));
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("AccessLevelsData: Error loading from database: " + e.getMessage(), e);
		}

		_log.info("AccessLevelsData: Loaded " + _accessLevels.size() + " accesses.");

		// Add finally the normal user access level.
		_accessLevels.put(_userAccessLevelNum, _userAccessLevel);
	}

	/**
	 * Returns the access level by characterAccessLevel
	 * 
	 * @param accessLevelNum
	 *            as int
	 * @return AccessLevel: AccessLevel instance by char access level<br>
	 */
	public L2AccessLevel getAccessLevel(int accessLevelNum)
	{
		L2AccessLevel accessLevel = null;

		synchronized (_accessLevels)
		{
			accessLevel = _accessLevels.get(accessLevelNum);
		}
		return accessLevel;
	}

	public void addBanAccessLevel(int accessLevel)
	{
		synchronized (_accessLevels)
		{
			if (accessLevel > -1)
				return;

			_accessLevels.put(accessLevel, new L2AccessLevel(accessLevel, "Banned", -1, -1, null, false, false, false, false, false, false, false, false));
		}
	}

	private static class SingletonHolder
	{
		protected static final AccessLevelsData _instance = new AccessLevelsData();
	}
}
