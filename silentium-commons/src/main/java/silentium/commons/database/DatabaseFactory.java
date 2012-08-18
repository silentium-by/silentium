/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.commons.database;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.commons.ServerType;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseFactory
{
	private static final Logger log = LoggerFactory.getLogger(DatabaseFactory.class);

	private static BoneCP connectionPool;

	public static void init()
	{
		final File configFile = new File("./config/database-config.xml");

		try (FileInputStream fis = new FileInputStream(configFile))
		{
			final BoneCPConfig config = new BoneCPConfig(fis, getSectionName());

			log.info("DatabaseFactory: jdbc url '{}'.", config.getJdbcUrl());
			log.info("DatabaseFactory: user name '{}'.", config.getUsername());
			log.info("DatabaseFactory: password '{}'.", config.getPassword());

			connectionPool = new BoneCP(config);

			// Test the connection
			connectionPool.getConnection().close();
		}
		catch (Exception e)
		{
			throw new Error("DatabaseFactory: Failed to init database connections: " + e.getMessage(), e);
		}
	}

	private static String getSectionName()
	{
		if (ServerType.serverMode == ServerType.MODE_GAMESERVER)
			return "gameserver";

		return "authserver";
	}

	public static void shutdown()
	{
		connectionPool.shutdown();
	}

	public static Connection getConnection()
	{
		Connection con = null;

		while (con == null)
		{
			try
			{
				con = connectionPool.getConnection();
			}
			catch (SQLException e)
			{
				log.warn("DatabaseFactory: getConnection() failed, trying again " + e.getMessage());
			}
		}

		return con;
	}
}
