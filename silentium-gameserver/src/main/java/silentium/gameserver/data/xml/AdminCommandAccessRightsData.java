/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.model.L2AccessLevel;
import silentium.gameserver.model.L2AdminCommandAccessRight;

/**
 * @author FBIagent
 */
public class AdminCommandAccessRightsData
{
	private static Logger _log = LoggerFactory.getLogger(AdminCommandAccessRightsData.class.getName());

	private final Map<String, L2AdminCommandAccessRight> _adminCommandAccessRights;

	public static AdminCommandAccessRightsData getInstance()
	{
		return SingletonHolder._instance;
	}

	protected AdminCommandAccessRightsData()
	{
		_adminCommandAccessRights = new HashMap<>();
		load();
	}

	public void reload()
	{
		_adminCommandAccessRights.clear();
		load();
	}

	private void load()
	{
		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/admin_commands_rights.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();
			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("aCar"))
				{
					String adminCommand = String.valueOf(d.getAttributes().getNamedItem("name").getNodeValue());
					String accessLevels = String.valueOf(d.getAttributes().getNamedItem("accessLevel").getNodeValue());
					_adminCommandAccessRights.put(adminCommand, new L2AdminCommandAccessRight(adminCommand, accessLevels));
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("AdminCommandAccessRightsData: Error loading from database:" + e.getMessage(), e);
		}

		_log.info("AdminCommandAccessRightsData: Loaded " + _adminCommandAccessRights.size() + " commands accesses' rights.");
	}

	public boolean hasAccess(String adminCommand, L2AccessLevel accessLevel)
	{
		if (accessLevel.getLevel() == AccessLevelsData._masterAccessLevelNum)
			return true;

		L2AdminCommandAccessRight acar = _adminCommandAccessRights.get(adminCommand);
		if (acar == null)
		{
			_log.info("AdminCommandAccessRightsData: No rights defined for admin command " + adminCommand + ".");
			return false;
		}

		return acar.hasAccess(accessLevel);
	}

	private static class SingletonHolder
	{
		protected static final AdminCommandAccessRightsData _instance = new AdminCommandAccessRightsData();
	}
}
