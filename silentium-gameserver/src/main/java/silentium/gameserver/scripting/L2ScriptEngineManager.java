/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.scripting;

import com.google.common.base.Predicates;
import javolution.util.FastMap;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withName;

/**
 * Caches script engines and provides funcionality for executing and managing scripts.<BR>
 *
 * @author KenM
 */
public final class L2ScriptEngineManager
{
	private static final Logger _log = LoggerFactory.getLogger(L2ScriptEngineManager.class.getName());

	public static L2ScriptEngineManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private final Map<String, ScriptEngine> _nameEngines = new FastMap<>();
	private final Map<String, ScriptEngine> _extEngines = new FastMap<>();
	private final List<ScriptManager<?>> _scriptManagers = new LinkedList<>();

	private File _currentLoadingScript;

	// Configs
	// TODO move to config file
	/**
	 * Informs(logs) the scripts being loaded.<BR>
	 * Apply only when executing script from files.<BR>
	 */
	private final boolean VERBOSE_LOADING = false;

	/**
	 * Clean an previous error log(if such exists) for the script being loaded before trying to load.<BR>
	 * Apply only when executing script from files.<BR>
	 */
	private final boolean PURGE_ERROR_LOG = true;

	protected L2ScriptEngineManager()
	{
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		List<ScriptEngineFactory> factories = scriptEngineManager.getEngineFactories();

		for (ScriptEngineFactory factory : factories)
		{
			try
			{
				ScriptEngine engine = factory.getScriptEngine();
				for (String name : factory.getNames())
				{
					ScriptEngine existentEngine = _nameEngines.get(name);

					if (existentEngine != null)
					{
						double engineVer = Double.parseDouble(factory.getEngineVersion());
						double existentEngVer = Double.parseDouble(existentEngine.getFactory().getEngineVersion());

						if (engineVer <= existentEngVer)
						{
							continue;
						}
					}

					_nameEngines.put(name, engine);
				}

				for (String ext : factory.getExtensions())
				{
					if (!ext.equals("java") || factory.getLanguageName().equals("java"))
					{
						_extEngines.put(ext, engine);
					}
				}
			}
			catch (Exception e)
			{
				_log.warn("Failed initializing factory. ");
				e.printStackTrace();
			}
		}
	}

	private ScriptEngine getEngineByName(String name)
	{
		return _nameEngines.get(name);
	}

	private ScriptEngine getEngineByExtension(String ext)
	{
		return _extEngines.get(ext);
	}

	public void initializeScripts()
	{
		final Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage("silentium.scripts"))
				.filterInputsBy(new FilterBuilder().exclude("silentium.scripts.handlers"))
				.setScanners(new SubTypesScanner(false))
				.useParallelExecutor());
		// TODO сделать общий интерфейс для всех скриптов.
		final Set<Class<?>> classes = reflections.getSubTypesOf(Object.class);

		final Set<Method> mainMethods = getAllMethods(classes, Predicates.and(withName("main")));

		final String[] stringArray = { "tatanka rules." };

		// TODO нормально обрабатывать ошибки.
		for (final Method main : mainMethods)
		{
			try {
				main.invoke(null, (Object) stringArray);
			} catch (IllegalAccessException | InvocationTargetException e) {
				_log.warn("Script can't be initialized.");
			}
		}
	}

	public void registerScriptManager(ScriptManager<?> manager)
	{
		_scriptManagers.add(manager);
	}

	public void removeScriptManager(ScriptManager<?> manager)
	{
		_scriptManagers.remove(manager);
	}

	public List<ScriptManager<?>> getScriptManagers()
	{
		return _scriptManagers;

	}

	/**
	 * @param currentLoadingScript
	 *            The currentLoadingScript to set.
	 */
	protected void setCurrentLoadingScript(File currentLoadingScript)
	{
		_currentLoadingScript = currentLoadingScript;
	}

	/**
	 * @return Returns the currentLoadingScript.
	 */
	protected File getCurrentLoadingScript()
	{
		return _currentLoadingScript;
	}

	private static class SingletonHolder
	{
		protected static final L2ScriptEngineManager _instance = new L2ScriptEngineManager();
	}
}
