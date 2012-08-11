package silentium.gameserver.handler;

import org.reflections.Reflections;

/**
 * @author Tatanka
 */
public class HandlerRegisterManager {
	public static void registerHandlers() {
		try {
			Reflections reflections = new Reflections("silentium.scripts.handlers.admin");
			for (Class<? extends IAdminCommandHandler> handler : reflections.getSubTypesOf(IAdminCommandHandler.class))
				AdminCommandHandler.getInstance().registerAdminCommandHandler(handler.newInstance());

			reflections = new Reflections("silentium.scripts.handlers.chat");
			for (Class<? extends IChatHandler> chatHandler : reflections.getSubTypesOf(IChatHandler.class))
				ChatHandler.getInstance().registerChatHandler(chatHandler.newInstance());

			reflections = new Reflections("silentium.scripts.handlers.item");
			for (Class<? extends IItemHandler> itemHandler : reflections.getSubTypesOf(IItemHandler.class))
				ItemHandler.getInstance().registerItemHandler(itemHandler.newInstance());

			reflections = new Reflections("silentium.scripts.handlers.skill");
			for (Class<? extends ISkillHandler> skillHandler : reflections.getSubTypesOf(ISkillHandler.class))
				SkillHandler.getInstance().registerSkillHandler(skillHandler.newInstance());

			reflections = new Reflections("silentium.scripts.handlers.user");
			for (Class<? extends IUserCommandHandler> userCommandHandler : reflections.getSubTypesOf
					(IUserCommandHandler.class))
				UserCommandHandler.getInstance().registerUserCommandHandler(userCommandHandler.newInstance());

			reflections = new Reflections("silentium.scripts.handlers.voiced");
			for (Class<? extends IVoicedCommandHandler> voicedCommandHandler : reflections.getSubTypesOf
					(IVoicedCommandHandler.class))
				VoicedCommandHandler.getInstance().registerHandler(voicedCommandHandler.newInstance());
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}
}
