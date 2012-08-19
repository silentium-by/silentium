/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.scripting;

import com.google.common.base.Objects;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.ai.DefaultMonsterAI;
import silentium.gameserver.handler.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Caches script engines and provides funcionality for executing and managing scripts.<BR>
 *
 * @author KenM
 */
public final class L2ScriptEngineManager {
	private static final Logger log = LoggerFactory.getLogger(L2ScriptEngineManager.class);

	private Class<? extends ScriptFile> currentLoadingScript;

	public static L2ScriptEngineManager getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public void initializeScripts() {
		final Reflections reflections = new Reflections(new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage("silentium.scripts"))
				.filterInputsBy(new FilterBuilder().exclude("silentium.scripts.handlers"))
				.setScanners(new SubTypesScanner(false))
				.useParallelExecutor());
		final Set<Class<? extends ScriptFile>> classes = reflections.getSubTypesOf(ScriptFile.class);

		DefaultMonsterAI.initialize();

		for (final Class<? extends ScriptFile> scriptClass : classes) {
			try {
				currentLoadingScript = scriptClass;

				final Method onLoadMethod = scriptClass.getMethod("onLoad");

				if (Objects.equal(onLoadMethod.getDeclaringClass(), scriptClass)) // Check for classes like Sagas
					onLoadMethod.invoke(null);
			} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				log.warn("Script {} can't be initialized: {}", scriptClass.getSimpleName(), e.getLocalizedMessage());
			}
		}

		registerHandlers();
	}

	private static void registerHandlers() {
		try {
			Reflections reflections = new Reflections("silentium.scripts.handlers.admin");
			for (final Class<? extends IAdminCommandHandler> handler : reflections.getSubTypesOf(IAdminCommandHandler.class))
				AdminCommandHandler.getInstance().registerAdminCommandHandler(handler.getConstructor().newInstance());

			reflections = new Reflections("silentium.scripts.handlers.chat");
			for (final Class<? extends IChatHandler> chatHandler : reflections.getSubTypesOf(IChatHandler.class))
				ChatHandler.getInstance().registerChatHandler(chatHandler.getConstructor().newInstance());

			reflections = new Reflections("silentium.scripts.handlers.item");
			for (final Class<? extends IItemHandler> itemHandler : reflections.getSubTypesOf(IItemHandler.class))
				ItemHandler.getInstance().registerItemHandler(itemHandler.getConstructor().newInstance());

			reflections = new Reflections("silentium.scripts.handlers.skill");
			for (final Class<? extends ISkillHandler> skillHandler : reflections.getSubTypesOf(ISkillHandler.class))
				SkillHandler.getInstance().registerSkillHandler(skillHandler.getConstructor().newInstance());

			reflections = new Reflections("silentium.scripts.handlers.user");
			for (final Class<? extends IUserCommandHandler> userCommandHandler : reflections.getSubTypesOf(IUserCommandHandler.class))
				UserCommandHandler.getInstance().registerUserCommandHandler(userCommandHandler.getConstructor().newInstance());

			reflections = new Reflections("silentium.scripts.handlers.voiced");
			for (final Class<? extends IVoicedCommandHandler> voicedCommandHandler : reflections.getSubTypesOf(IVoicedCommandHandler.class))
				VoicedCommandHandler.getInstance().registerHandler(voicedCommandHandler.getConstructor().newInstance());
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			log.warn("Handler {} can't be initialized.", e);
		}
	}

	/**
	 * @return Returns the currentLoadingScript.
	 */
	public Class<? extends ScriptFile> getCurrentLoadingScript() {
		return currentLoadingScript;
	}

	private static class SingletonHolder {
		static final L2ScriptEngineManager INSTANCE = new L2ScriptEngineManager();
	}
}
