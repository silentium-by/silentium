package silentium.gameserver.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import silentium.gameserver.model.L2ItemInstance;
import silentium.gameserver.model.L2Object;
import silentium.gameserver.model.actor.instance.L2PcInstance;

import static silentium.gameserver.utils.IllegalPlayerAction.*;

/**
 * Класс, помогающий логгировать определенные события. Это можно было бы перенести в layouts,
 * но тогда пришлось бы отправлять аргументы для логгирования через массив, а это будет слишком жирно,
 * только если не сделать отдельный сервер для логгирования.
 *
 * @author Tatanka
 */
public class LoggingUtils {
	private static final String[] excludeItemType = { "Arrow", "Shot", "Herb" };

	// строится тут, а не в layout, потому что пришлось бы отправлять аргументы массивом, а это слишком жирно будет,
	// только если не сделать отдельный сервер для чата или логгирования.
	public static void logChat(Logger chatLogger, String senderName, String receiverName, String message,
	                           String chatName) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append('[');
		stringBuilder.append(chatName);
		stringBuilder.append("] ");

		stringBuilder.append('\'');
		stringBuilder.append(senderName);
		stringBuilder.append('\'');

		if (receiverName != null) {
			stringBuilder.append(" to ");

			stringBuilder.append('\'');
			stringBuilder.append(receiverName);
			stringBuilder.append('\'');
		}

		stringBuilder.append(", message: \"");
		stringBuilder.append(message);
		stringBuilder.append('"');

		stringBuilder.append('.');

		chatLogger.info(stringBuilder.toString());
	}

	public static void logItem(Logger itemLogger, String processPrefix, String process, L2ItemInstance item,
	                           String ownerName, L2Object reference) {
		final StringBuilder stringBuilder = new StringBuilder();

		// Don't log arrows, shots and herbs.
		if (ArrayUtils.contains(excludeItemType, item.getItemType().toString()))
			return;

		stringBuilder.append(processPrefix);
		stringBuilder.append(process);

		stringBuilder.append(", owner '");
		stringBuilder.append(ownerName);
		stringBuilder.append('\'');

		stringBuilder.append(", item object id '");
		stringBuilder.append(item.getObjectId());
		stringBuilder.append('\'');

		stringBuilder.append(", item name '");
		stringBuilder.append(item.getItem().getName());
		stringBuilder.append('\'');

		stringBuilder.append(", item count '");
		stringBuilder.append(item.getCount());
		stringBuilder.append('\'');

		if (item.getEnchantLevel() > 0){
			stringBuilder.append(", item enchant level '");
			stringBuilder.append('+');
			stringBuilder.append(item.getEnchantLevel());
			stringBuilder.append('\'');
		}

		stringBuilder.append(", reference '");
		stringBuilder.append(reference.toString());

		stringBuilder.append('.');

		itemLogger.info(stringBuilder.toString());
	}

	public static void logAudit(Logger auditLogger, String message, L2PcInstance actor, int punishment) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append(message);

		stringBuilder.append(" by ");

		stringBuilder.append(actor.getName());

		stringBuilder.append(" and punishment was ");
		switch (punishment) {
			case PUNISH_BROADCAST:
				stringBuilder.append("broadcast");
				break;
			case PUNISH_KICK:
				stringBuilder.append("kick");
				break;
			case PUNISH_KICKBAN:
				stringBuilder.append("ban");
				break;
			case PUNISH_JAIL:
				stringBuilder.append("jail");
				break;
		}
		stringBuilder.append('.');

		auditLogger.info(stringBuilder.toString());
	}
}
