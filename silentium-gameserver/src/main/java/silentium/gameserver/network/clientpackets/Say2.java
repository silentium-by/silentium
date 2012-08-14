/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program
 * is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details. You should have
 * received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.network.clientpackets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silentium.gameserver.configs.ChatFilterConfig;
import silentium.gameserver.configs.CustomConfig;
import silentium.gameserver.configs.MainConfig;
import silentium.gameserver.handler.ChatHandler;
import silentium.gameserver.handler.IChatHandler;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ActionFailed;
import silentium.gameserver.utils.IllegalPlayerAction;
import silentium.gameserver.utils.LoggingUtils;
import silentium.gameserver.utils.Util;

import java.util.regex.Pattern;

public final class Say2 extends L2GameClientPacket {
	private static Logger chatLogger = LoggerFactory.getLogger("chat");

	public static final int ALL = 0;
	public static final int SHOUT = 1; // !
	public static final int TELL = 2;
	public static final int PARTY = 3; // #
	public static final int CLAN = 4; // @
	public static final int GM = 5;
	public static final int PETITION_PLAYER = 6;
	public static final int PETITION_GM = 7;
	public static final int TRADE = 8; // +
	public static final int ALLIANCE = 9; // $
	public static final int ANNOUNCEMENT = 10;
	public static final int BOAT = 11;
	public static final int L2FRIEND = 12;
	public static final int MSNCHAT = 13;
	public static final int PARTYMATCH_ROOM = 14;
	public static final int PARTYROOM_COMMANDER = 15; // (Yellow)
	public static final int PARTYROOM_ALL = 16; // (Red)
	public static final int HERO_VOICE = 17;

	private final static String[] CHAT_NAMES = { "ALL", "SHOUT", "TELL", "PARTY", "CLAN", "GM", "PETITION_PLAYER", "PETITION_GM", "TRADE", "ALLIANCE", "ANNOUNCEMENT", // 10
			"BOAT", "WILLCRASHCLIENT:)", "FAKEALL?", "PARTYMATCH_ROOM", "PARTYROOM_COMMANDER", "PARTYROOM_ALL", "HERO_VOICE" };

	private String _text;
	private int _type;
	private String _target;

	@Override
	protected void readImpl() {
		_text = readS();
		_type = readD();
		_target = (_type == TELL) ? readS() : null;
	}

	@Override
	protected void runImpl() {
		log.debug("Say2: Msg Type = '" + _type + "' Text = '" + _text + "'.");

		L2PcInstance activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (_type < 0 || _type >= CHAT_NAMES.length) {
			log.warn("Say2: Invalid type: " + _type + " Player : " + activeChar.getName() + " text: " + String.valueOf(_text));
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			activeChar.logout();
			return;
		}

		if (_text.isEmpty()) {
			log.warn(activeChar.getName() + ": sending empty text. Possible packet hack.");
			activeChar.sendPacket(ActionFailed.STATIC_PACKET);
			activeChar.logout();
			return;
		}

		if (_text.length() >= 100)
			return;

		if (!activeChar.isGM() && _type == ANNOUNCEMENT) {
			Util.handleIllegalPlayerAction(activeChar, activeChar.getName() + " tried to announce without GM statut.", IllegalPlayerAction.PUNISH_BROADCAST);
			log.warn(activeChar.getName() + " tried to use announcements without GM statut.");
			return;
		}

		if (activeChar.isChatBanned() || (activeChar.isInJail() && !activeChar.isGM())) {
			activeChar.sendPacket(SystemMessageId.CHATTING_PROHIBITED);
			return;
		}

		if (_type == PETITION_PLAYER && activeChar.isGM())
			_type = PETITION_GM;

		if (!activeChar.isGM() && CustomConfig.USE_SAY_FILTER)
			checkText();

		_text = _text.replaceAll("\\\\n", "");

		IChatHandler handler = ChatHandler.getInstance().getChatHandler(_type);
		if (handler != null) {
			handler.handleChat(_type, activeChar, _target, _text);

			if (MainConfig.LOG_CHAT)
				LoggingUtils.logChat(chatLogger, _type == TELL ? activeChar.getName() : null, _target, _text,
						CHAT_NAMES[_type]);
		} else
			log.warn(activeChar.getName() + " tried to use unregistred chathandler type: " + _type + ".");
	}

	private void checkText() {
		String filteredText = _text;
		for (String pattern : ChatFilterConfig.FILTER_LIST) {
			if (matches(filteredText, ".*" + pattern + ".*", Pattern.CASE_INSENSITIVE))
				filteredText = CustomConfig.CHAT_FILTER_CHARS;
		}
		_text = filteredText;
	}

	private boolean matches(String str, String regex, int flags) {
		return Pattern.compile(regex, flags).matcher(str).matches();
	}

	@Override
	protected boolean triggersOnActionRequest() {
		return false;
	}
}
