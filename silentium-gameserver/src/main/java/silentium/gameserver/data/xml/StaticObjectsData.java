/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.data.xml;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.data.xml.parsers.XMLDocumentFactory;
import silentium.gameserver.idfactory.IdFactory;
import silentium.gameserver.model.actor.instance.L2StaticObjectInstance;

public class StaticObjectsData
{
	private static Logger _log = LoggerFactory.getLogger(StaticObjectsData.class.getName());

	public static void load()
	{
		NamedNodeMap node = null;
		try
		{
			File f = new File(MainConfig.DATAPACK_ROOT + "/data/xml/static_objects.xml");
			Document doc = XMLDocumentFactory.getInstance().loadDocument(f);

			Node n = doc.getFirstChild();

			L2StaticObjectInstance obj;

			for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
			{
				if (d.getNodeName().equalsIgnoreCase("staticobject"))
				{
					node = d.getAttributes();

					obj = new L2StaticObjectInstance(IdFactory.getInstance().getNextId());
					obj.setType(Integer.valueOf(node.getNamedItem("type").getNodeValue()));
					obj.setStaticObjectId(Integer.valueOf(node.getNamedItem("id").getNodeValue()));
					obj.setXYZ(Integer.valueOf(node.getNamedItem("x").getNodeValue()), Integer.valueOf(node.getNamedItem("y").getNodeValue()), Integer.valueOf(node.getNamedItem("z").getNodeValue()));
					obj.setMap(node.getNamedItem("texture").getNodeValue(), Integer.valueOf(node.getNamedItem("map_x").getNodeValue()), Integer.valueOf(node.getNamedItem("map_y").getNodeValue()));
					obj.spawnMe();
				}
			}
		}
		catch (Exception e)
		{
			_log.warn("StaticObject: Error while creating StaticObjectsData table: " + e);
		}
		_log.info("StaticObject: Loaded " + ((node == null) ? "0" : node.getLength()) + " templates.");
	}
}
