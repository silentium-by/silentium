/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.gameserver.board.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import silentium.commons.database.DatabaseFactory;
import silentium.gameserver.model.BlockList;
import silentium.gameserver.model.L2World;
import silentium.gameserver.model.actor.instance.L2PcInstance;
import silentium.gameserver.network.SystemMessageId;
import silentium.gameserver.network.serverpackets.ExMailArrived;
import silentium.gameserver.network.serverpackets.SystemMessage;
import silentium.gameserver.tables.CharNameTable;

/**
 * @author JIV, Johan, Vital
 */
public class MailBBSManager extends BaseBBSManager
{
	private static Logger _log = LoggerFactory.getLogger(MailBBSManager.class.getName());

	private final Map<Integer, FastList<Mail>> _mails = new FastMap<>();

	private int _lastid = 0;

	private static final String SELECT_CHAR_MAILS = "SELECT * FROM character_mail WHERE charId = ? ORDER BY letterId ASC";
	private static final String INSERT_NEW_MAIL = "INSERT INTO character_mail (charId, letterId, senderId, location, recipientNames, subject, message, sentDate, unread) VALUES (?,?,?,?,?,?,?,?,?)";
	private static final String DELETE_MAIL = "DELETE FROM character_mail WHERE letterId = ?";
	private static final String MARK_MAIL_READ = "UPDATE character_mail SET unread = ? WHERE letterId = ?";
	private static final String SET_LETTER_LOC = "UPDATE character_mail SET location = ? WHERE letterId = ?";
	private static final String SELECT_LAST_ID = "SELECT letterId FROM character_mail ORDER BY letterId DESC LIMIT 1";

	public class Mail
	{
		int charId;
		int letterId;
		int senderId;
		String location;
		String recipientNames;
		String subject;
		String message;
		Timestamp sentDate;
		String sentDateString;
		boolean unread;
	}

	public static MailBBSManager getInstance()
	{
		return SingletonHolder._instance;
	}

	protected MailBBSManager()
	{
		initId();
	}

	private void initId()
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(SELECT_LAST_ID);
			ResultSet result = statement.executeQuery();
			while (result.next())
			{
				if (result.getInt(1) > _lastid)
					_lastid = result.getInt(1);
			}
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": data error on MailBBS (initId): " + e);
			e.printStackTrace();
		}
	}

	private synchronized int getNewMailId()
	{
		return ++_lastid;
	}

	private FastList<Mail> getPlayerMails(int objId)
	{
		FastList<Mail> _letters = _mails.get(objId);
		if (_letters == null)
		{
			_letters = new FastList<>();
			try (Connection con = DatabaseFactory.getConnection())
			{
				PreparedStatement statement = con.prepareStatement(SELECT_CHAR_MAILS);
				statement.setInt(1, objId);
				ResultSet result = statement.executeQuery();
				while (result.next())
				{
					Mail letter = new Mail();
					letter.charId = result.getInt("charId");
					letter.letterId = result.getInt("letterId");
					letter.senderId = result.getInt("senderId");
					letter.location = result.getString("location");
					letter.recipientNames = result.getString("recipientNames");
					letter.subject = result.getString("subject");
					letter.message = result.getString("message");
					letter.sentDate = result.getTimestamp("sentDate");
					letter.sentDateString = result.getString("sentDate");
					letter.unread = result.getInt("unread") != 0;
					_letters.addFirst(letter);
				}
				result.close();
				statement.close();
			}
			catch (Exception e)
			{
				_log.warn("couldnt load mail for ID:" + objId + " " + e.getMessage());
			}
			_mails.put(objId, _letters);
		}
		return _letters;
	}

	private Mail getLetter(L2PcInstance activeChar, int letterId)
	{
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
			if (letter.letterId == letterId)
				return letter;

		return null;
	}

	@Override
	public void parseCmd(String command, L2PcInstance activeChar)
	{
		if (command.equals("_maillist_0_1_0_"))
			showInbox(activeChar, 1);
		else if (command.startsWith("_maillist_0_1_0_ "))
			showInbox(activeChar, Integer.parseInt(command.substring(17)));
		else if (command.equals("_maillist_0_1_0_sentbox"))
			showSentbox(activeChar, 1);
		else if (command.startsWith("_maillist_0_1_0_sentbox "))
			showSentbox(activeChar, Integer.parseInt(command.substring(24)));
		else if (command.equals("_maillist_0_1_0_archive"))
			showMailArchive(activeChar, 1);
		else if (command.startsWith("_maillist_0_1_0_archive "))
			showMailArchive(activeChar, Integer.parseInt(command.substring(24)));
		else if (command.equals("_maillist_0_1_0_temp_archive"))
			showTempMailArchive(activeChar, 1);
		else if (command.startsWith("_maillist_0_1_0_temp_archive "))
			showTempMailArchive(activeChar, Integer.parseInt(command.substring(29)));
		else if (command.equals("_maillist_0_1_0_write"))
			showWriteView(activeChar);
		else if (command.startsWith("_maillist_0_1_0_view "))
		{
			Mail letter = getLetter(activeChar, Integer.parseInt(command.substring(21)));
			showLetterView(activeChar, letter);
			if (letter.unread)
				setLetterToRead(activeChar, letter.letterId);
		}
		else if (command.startsWith("_maillist_0_1_0_reply "))
		{
			Mail letter = getLetter(activeChar, Integer.parseInt(command.substring(22)));
			showWriteView(activeChar, getCharName(letter.senderId), letter);
		}
		else if (command.startsWith("_maillist_0_1_0_delete "))
		{
			Mail letter = getLetter(activeChar, Integer.parseInt(command.substring(23)));
			if (letter != null)
				deleteLetter(activeChar, letter.letterId);
			showLastForum(activeChar);
		}
		else if (command.startsWith("_maillist_0_1_0_store "))
		{
			Mail letter = getLetter(activeChar, Integer.parseInt(command.substring(22)));
			if (letter != null)
				setLetterLocation(activeChar, letter.letterId, "archive");
			showMailArchive(activeChar, Integer.parseInt(command.substring(22)));
		}
		else if (command.startsWith("_maillist_0_1_0_viewlist"))
			showLastForum(activeChar);
		else
			activeChar.sendMessage(command + " not implemented yet.");
	}

	private static String abbreviate(String s, int maxWidth)
	{
		return s.length() > maxWidth ? s.substring(0, maxWidth) : s;
	}

	public int checkUnreadMail(L2PcInstance activeChar)
	{
		int count = 0;
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
			if (letter.unread)
				count++;
		return count;
	}

	private void showInbox(L2PcInstance activeChar, int page)
	{
		int countMails = getCountLetters(activeChar, "inbox");
		int maxpage = getMaxPageId(countMails);
		if (page > maxpage)
			page = maxpage;
		if (page < 1)
			page = 1;
		activeChar.setMailPosition(page);
		int index = 0, minIndex = 0, maxIndex = 0;
		maxIndex = (page == 1 ? page * 9 : (page * 10) - 1);
		minIndex = maxIndex - 9;

		final StringBuilder html = new StringBuilder();
		html.append("<html><body><center>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;&$917;");
		html.append("</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td height=5></td></tr>");
		html.append("</table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 width=755 height=25 bgcolor=A7A19A>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td>");
		html.append("<td fixwidth=600>");
		html.append("<a action=\"bypass _maillist_0_1_0_\">[&$917;]</a>&nbsp;(" + getCountLetters(activeChar, "inbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_sentbox\">[&$918;]</a>&nbsp;(" + getCountLetters(activeChar, "sentbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_archive\">[&$919;]</a>&nbsp;(" + getCountLetters(activeChar, "archive") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_temp_archive\">[&$920;]</a>&nbsp;(" + getCountLetters(activeChar, "temparchive") + ")</td>");
		html.append("<td fixWIDTH=5></td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("</table>");
		html.append("<br>");
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=A7A19A width=755><tr>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("<td FIXWIDTH=150 align=center>&$911;</td>");
		html.append("<td FIXWIDTH=440>&$413;</td>");
		html.append("<td FIXWIDTH=150 align=center>&$910;</td>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("</tr></table>");
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.location.equals("inbox"))
			{
				if (index < minIndex)
				{
					index++;
					continue;
				}
				if (index > maxIndex)
					break;
				String tempName = getCharName(letter.senderId);
				html.append("<table border=0 cellspacing=0 cellpadding=5 width=755><tr>");
				html.append("<td FIXWIDTH=5 align=center></td>");
				html.append("<td FIXWIDTH=150 align=center>" + abbreviate(tempName, 6) + "</td>");
				html.append("<td FIXWIDTH=440 ><a action=\"bypass _maillist_0_1_0_view " + letter.letterId + "\">");
				if (letter.unread)
					html.append("<font color=\"LEVEL\">");
				html.append(abbreviate(letter.subject, 51));
				if (letter.unread)
					html.append("</font>");
				html.append("</a>");
				html.append("</td><td FIXWIDTH=150 align=center>" + letter.sentDateString.substring(0, letter.sentDateString.length() - 5) + "</td>");
				html.append("<td FIXWIDTH=5 align=center></td></tr></table>");
				html.append("<img src=\"L2UI.Squaregray\" width=\"755\" height=\"1\">");
				index++;
			}
		}
		html.append("<br>");
		html.append("<table width=755 cellspace=0 cellpadding=0><tr><td width=50></td>");
		html.append("<td width=510 align=center>");
		html.append("<table cellspacing=2 cellpadding=0 border=0><tr>");
		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_ " + (page == 1 ? page : page - 1) + "\" back=\"L2UI_CT1.Button_DF_Left_Down\" fore=\"L2UI_CT1.Button_DF_Left\" width=15 height=15>");
		html.append("</td></tr></table></td>");

		int i = 0;
		if (maxpage > 21)
		{
			if (page <= 11)
			{
				for (i = 1; i <= (10 + page); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_ " + i + "\"> " + i + " </a></td>");
				}
			}
			else if (page > 11 && (maxpage - page) > 10)
			{
				for (i = (page - 10); i <= (page - 1); i++)
				{
					if (i == page)
						continue;

					html.append("<td><a action=\"bypass _maillist_0_1_0_ " + i + "\"> " + i + " </a></td>");
				}
				for (i = page; i <= (page + 10); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_ " + i + "\"> " + i + " </a></td>");
				}
			}
			else if ((maxpage - page) <= 10)
			{
				for (i = (page - 10); i <= maxpage; i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_ " + i + "\"> " + i + " </a></td>");
				}
			}
		}
		else
		{
			for (i = 1; i <= maxpage; i++)
			{
				if (i == page)
					html.append("<td> " + i + " </td>");
				else
					html.append("<td><a action=\"bypass _maillist_0_1_0_ " + i + "\"> " + i + " </a></td>");
			}
		}

		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_ " + (page == maxpage ? page : page + 1) + "\" back=\"L2UI_CT1.Button_DF_Right_Down\" fore=\"L2UI_CT1.Button_DF_Right\" width=15 height=15 >");
		html.append("</td></tr></table></td></tr></table></td>");
		// html.append("<td align=right><button value=\"&$421;\" action=\"bypass _maillist_0_1_0_write\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("</tr><tr><td width=5 height=10></td></tr>");
		html.append("<tr>");
		html.append("<td></td>");
		html.append("<td align=center><table border=0><tr><td>");
		html.append("<combobox width=65 var=SearchTarget list=\"Writer;Title\">");
		html.append("</td><td><edit var=\"keyword\" width=130 height=15 length=\"16\"></td>");
		html.append("<td><button value=\"&$420;\" action=\"bypass _maillist_0_1_0_search $combo $keyword\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td></tr></table></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</center></body></html>");
		separateAndSend(html.toString(), activeChar);
	}

	private static void showLetterView(L2PcInstance activeChar, Mail letter)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html><body><center>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;");
		if (letter.location.equals("inbox"))
			html.append("<a action=\"bypass _maillist_0_1_0_\">&$917;</a>");
		else if (letter.location.equals("sentbox"))
			html.append("<a action=\"bypass _maillist_0_1_0_sentbox\">&$918;</a>");
		else if (letter.location.equals("archive"))
			html.append("<a action=\"bypass _maillist_0_1_0_archive\">&$919;</a>");
		else if (letter.location.equals("temparchive"))
			html.append("<a action=\"bypass _maillist_0_1_0_temp_archive\">&$920;</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;" + abbreviate(letter.subject.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;"), 51));
		html.append("</td></tr></table>");
		html.append("<br>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 bgcolor=A7A19A>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td><td WIDTH=85 valign=top>Sender:</td>");
		html.append("<td WIDTH=415 valign=top>" + getCharName(letter.senderId) + "</td>");
		html.append("<td width=10></td>");
		html.append("<td width=90 valign=top>Send Time:</td>");
		html.append("<td WIDTH=150 valign=top>" + letter.sentDateString.substring(0, letter.sentDateString.length() - 5) + "</td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td><td>Recipient:</td>");
		html.append("<td>" + letter.recipientNames + "</td>");
		html.append("<td></td>");
		html.append("<td>Delete date:</td>");
		html.append("<td>Unknown</td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td><td valign=top>Title:</td>");
		html.append("<td fixwidth=415 valign=top>" + letter.subject.replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;") + "</td>");
		html.append("<td></td>");
		html.append("<td></td>");
		html.append("<td></td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("</table><br>");
		html.append("<table border=0 cellspacing=0 cellpadding=0><tr>");
		html.append("<td width=5></td>");
		html.append("<td width=755 align=left>" + letter.message.replaceAll("\r\n", "<br>").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\"", "&quot;") + "</td>");
		html.append("<td width=5></td>");
		html.append("</tr></table><br>");
		html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">");
		html.append("<img src=\"L2UI.squaregray\" width=\"755\" height=\"1\">");
		html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\">");
		html.append("<table border=0 cellspacing=0 cellpadding=0 FIXWIDTH=755><tr>");
		html.append("<td width=70><button value=\"&$422;\" action=\"bypass _maillist_0_1_0_viewlist\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td width=340></td>");
		if (letter.location.equals("archive"))
			html.append("<td width=70 align=right></td>");
		html.append("<td width=70 align=right><button value=\"&$912;\" action=\"bypass _maillist_0_1_0_reply " + letter.letterId + "\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\">&nbsp;</td>");
		html.append("<td width=70 align=right><button value=\"&$913;\" action=\"bypass _maillist_0_1_0_deliver\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\">&nbsp;</td>");
		html.append("<td width=70 align=right><button value=\"&$425;\" action=\"bypass _maillist_0_1_0_delete " + letter.letterId + "\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\">&nbsp;</td>");
		if (!letter.location.equals("archive"))
			html.append("<td width=70 align=right><button value=\"&$914;\" action=\"bypass _maillist_0_1_0_store " + letter.letterId + "\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\">&nbsp;</td>");
		html.append("<td width=70 align=right><button value=\"&$915;\" action=\"bypass _maillist_0_1_0_write\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\" ></td>");
		html.append("</tr></table>");
		html.append("</center></body></html>");
		separateAndSend(html.toString(), activeChar);
	}

	private void showSentbox(L2PcInstance activeChar, int page)
	{
		int countMails = getCountLetters(activeChar, "sentbox");
		int maxpage = getMaxPageId(countMails);
		if (page > maxpage)
			page = maxpage;
		if (page < 1)
			page = 1;
		activeChar.setMailPosition(1 * 1000 + page);
		int index = 0, minIndex = 0, maxIndex = 0;
		maxIndex = (page == 1 ? page * 9 : (page * 10) - 1);
		minIndex = maxIndex - 9;

		final StringBuilder html = new StringBuilder();
		html.append("<html><body><center>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;&$918;");
		html.append("</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td height=5></td></tr>");
		html.append("</table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 width=755 height=25 bgcolor=A7A19A>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td>");
		html.append("<td fixwidth=600>");
		html.append("<a action=\"bypass _maillist_0_1_0_\">[&$917;]</a>&nbsp;(" + getCountLetters(activeChar, "inbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_sentbox\">[&$918;]</a>&nbsp;(" + getCountLetters(activeChar, "sentbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_archive\">[&$919;]</a>&nbsp;(" + getCountLetters(activeChar, "archive") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_temp_archive\">[&$920;]</a>&nbsp;(" + getCountLetters(activeChar, "temparchive") + ")</td>");
		html.append("<td fixWIDTH=5></td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("</table>");
		html.append("<br>");
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=A7A19A width=755><tr>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("<td FIXWIDTH=150 align=center>&$911;</td>");
		html.append("<td FIXWIDTH=440>&$413;</td>");
		html.append("<td FIXWIDTH=150 align=center>&$910;</td>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("</tr></table>");
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.location.equals("sentbox"))
			{
				if (index < minIndex)
				{
					index++;
					continue;
				}
				if (index > maxIndex)
					break;
				String tempName = getCharName(letter.senderId);
				html.append("<table border=0 cellspacing=0 cellpadding=5 width=755><tr>");
				html.append("<td FIXWIDTH=5 align=center></td>");
				html.append("<td FIXWIDTH=150 align=center>" + abbreviate(tempName, 6) + "</td>");
				html.append("<td FIXWIDTH=440 ><a action=\"bypass _maillist_0_1_0_view " + letter.letterId + "\">");
				if (letter.unread)
					html.append("<font color=\"LEVEL\">");
				html.append(abbreviate(letter.subject, 51));
				if (letter.unread)
					html.append("</font>");
				html.append("</a>");
				html.append("</td><td FIXWIDTH=150 align=center>" + letter.sentDateString.substring(0, letter.sentDateString.length() - 5) + "</td>");
				html.append("<td FIXWIDTH=5 align=center></td></tr></table>");
				html.append("<img src=\"L2UI.Squaregray\" width=\"755\" height=\"1\">");
				index++;
			}
		}
		html.append("<br>");
		html.append("<table width=755 cellspace=0 cellpadding=0><tr><td width=50></td>");
		html.append("<td width=510 align=center>");
		html.append("<table cellspacing=2 cellpadding=0 border=0><tr>");
		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_sentbox " + (page == 1 ? page : page - 1) + "\" back=\"L2UI_CT1.Button_DF_Left_Down\" fore=\"L2UI_CT1.Button_DF_Left\" width=15 height=15>");
		html.append("</td></tr></table></td>");

		int i = 0;
		if (maxpage > 21)
		{
			if (page <= 11)
			{
				for (i = 1; i <= (10 + page); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_sentbox " + i + "\"> " + i + " </a></td>");
				}
			}
			else if (page > 11 && (maxpage - page) > 10)
			{
				for (i = (page - 10); i <= (page - 1); i++)
				{
					if (i == page)
						continue;

					html.append("<td><a action=\"bypass _maillist_0_1_0_sentbox " + i + "\"> " + i + " </a></td>");
				}
				for (i = page; i <= (page + 10); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_sentbox " + i + "\"> " + i + " </a></td>");
				}
			}
			else if ((maxpage - page) <= 10)
			{
				for (i = (page - 10); i <= maxpage; i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_sentbox " + i + "\"> " + i + " </a></td>");
				}
			}
		}
		else
		{
			for (i = 1; i <= maxpage; i++)
			{
				if (i == page)
					html.append("<td> " + i + " </td>");
				else
					html.append("<td><a action=\"bypass _maillist_0_1_0_sentbox " + i + "\"> " + i + " </a></td>");
			}
		}

		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_sentbox " + (page == maxpage ? page : page + 1) + "\" back=\"L2UI_CT1.Button_DF_Right_Down\" fore=\"L2UI_CT1.Button_DF_Right\" width=15 height=15 >");
		html.append("</td></tr></table></td></tr></table></td>");
		// html.append("<td align=right><button value=\"&$421;\" action=\"bypass _maillist_0_1_0_write\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("</tr><tr><td width=5 height=10></td></tr>");
		html.append("<tr>");
		html.append("<td></td>");
		html.append("<td align=center><table border=0><tr><td>");
		html.append("<combobox width=65 var=SearchTarget list=\"Writer;Title\">");
		html.append("</td><td><edit var=\"keyword\" width=130 height=15 length=\"16\"></td>");
		html.append("<td><button value=\"&$420;\" action=\"bypass _maillist_0_1_0_search $combo $keyword\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td></tr></table></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</center></body></html>");
		separateAndSend(html.toString(), activeChar);
	}

	private void showMailArchive(L2PcInstance activeChar, int page)
	{
		int countMails = getCountLetters(activeChar, "archive");
		int maxpage = getMaxPageId(countMails);
		if (page > maxpage)
			page = maxpage;
		if (page < 1)
			page = 1;
		activeChar.setMailPosition(2 * 1000 + page);
		int index = 0, minIndex = 0, maxIndex = 0;
		maxIndex = (page == 1 ? page * 14 : (page * 15) - 1);
		minIndex = maxIndex - 14;

		final StringBuilder html = new StringBuilder();
		html.append("<html><body><center>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;&$919;");
		html.append("</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td height=5></td></tr>");
		html.append("</table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 width=755 height=25 bgcolor=A7A19A>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td>");
		html.append("<td fixwidth=600>");
		html.append("<a action=\"bypass _maillist_0_1_0_\">[&$917;]</a>&nbsp;(" + getCountLetters(activeChar, "inbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_sentbox\">[&$918;]</a>&nbsp;(" + getCountLetters(activeChar, "sentbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_archive\">[&$919;]</a>&nbsp;(" + getCountLetters(activeChar, "archive") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_temp_archive\">[&$920;]</a>&nbsp;(" + getCountLetters(activeChar, "temparchive") + ")</td>");
		html.append("<td fixWIDTH=5></td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("</table>");
		html.append("<br>");
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=A7A19A width=755><tr>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("<td FIXWIDTH=150 align=center>&$911;</td>");
		html.append("<td FIXWIDTH=440>&$413;</td>");
		html.append("<td FIXWIDTH=150 align=center>&$910;</td>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("</tr></table>");
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.location.equals("archive"))
			{
				if (index < minIndex)
				{
					index++;
					continue;
				}
				if (index > maxIndex)
					break;
				String tempName = getCharName(letter.senderId);
				html.append("<table border=0 cellspacing=0 cellpadding=5 width=755><tr>");
				html.append("<td FIXWIDTH=5 align=center></td>");
				html.append("<td FIXWIDTH=150 align=center>" + abbreviate(tempName, 6) + "</td>");
				html.append("<td FIXWIDTH=440 ><a action=\"bypass _maillist_0_1_0_view " + letter.letterId + "\">");
				if (letter.unread)
					html.append("<font color=\"LEVEL\">");
				html.append(abbreviate(letter.subject, 51));
				if (letter.unread)
					html.append("</font>");
				html.append("</a>");
				html.append("</td><td FIXWIDTH=150 align=center>" + letter.sentDateString.substring(0, letter.sentDateString.length() - 5) + "</td>");
				html.append("<td FIXWIDTH=5 align=center></td></tr></table>");
				html.append("<img src=\"L2UI.Squaregray\" width=\"755\" height=\"1\">");
				index++;
			}
		}
		html.append("<br>");
		html.append("<table width=755 cellspace=0 cellpadding=0><tr><td width=50></td>");
		html.append("<td width=510 align=center>");
		html.append("<table cellspacing=2 cellpadding=0 border=0><tr>");
		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_archive " + (page == 1 ? page : page - 1) + "\" back=\"L2UI_CT1.Button_DF_Left_Down\" fore=\"L2UI_CT1.Button_DF_Left\" width=15 height=15>");
		html.append("</td></tr></table></td>");

		int i = 0;
		if (maxpage > 21)
		{
			if (page <= 11)
			{
				for (i = 1; i <= (10 + page); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_archive " + i + "\"> " + i + " </a></td>");
				}
			}
			else if (page > 11 && (maxpage - page) > 10)
			{
				for (i = (page - 10); i <= (page - 1); i++)
				{
					if (i == page)
						continue;

					html.append("<td><a action=\"bypass _maillist_0_1_0_archive " + i + "\"> " + i + " </a></td>");
				}
				for (i = page; i <= (page + 10); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_archive " + i + "\"> " + i + " </a></td>");
				}
			}
			else if ((maxpage - page) <= 10)
			{
				for (i = (page - 10); i <= maxpage; i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_archive " + i + "\"> " + i + " </a></td>");
				}
			}
		}
		else
		{
			for (i = 1; i <= maxpage; i++)
			{
				if (i == page)
					html.append("<td> " + i + " </td>");
				else
					html.append("<td><a action=\"bypass _maillist_0_1_0_archive " + i + "\"> " + i + " </a></td>");
			}
		}

		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_archive " + (page == maxpage ? page : page + 1) + "\" back=\"L2UI_CT1.Button_DF_Right_Down\" fore=\"L2UI_CT1.Button_DF_Right\" width=15 height=15 >");
		html.append("</td></tr></table></td></tr></table></td>");
		// html.append("<td align=right><button value=\"&$421;\" action=\"bypass _maillist_0_1_0_write\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("</tr><tr><td width=5 height=10></td></tr>");
		html.append("<tr>");
		html.append("<td></td>");
		html.append("<td align=center><table border=0><tr><td>");
		html.append("<combobox width=65 var=SearchTarget list=\"Writer;Title\">");
		html.append("</td><td><edit var=\"keyword\" width=130 height=15 length=\"16\"></td>");
		html.append("<td><button value=\"&$420;\" action=\"bypass _maillist_0_1_0_search $combo $keyword\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td></tr></table></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</center></body></html>");
		separateAndSend(html.toString(), activeChar);
	}

	private void showTempMailArchive(L2PcInstance activeChar, int page)
	{
		int countMails = getCountLetters(activeChar, "temparchive");
		int maxpage = getMaxPageId(countMails);
		if (page > maxpage)
			page = maxpage;
		if (page < 1)
			page = 1;
		activeChar.setMailPosition(3 * 1000 + page);
		int index = 0, minIndex = 0, maxIndex = 0;
		maxIndex = (page == 1 ? page * 14 : (page * 15) - 1);
		minIndex = maxIndex - 14;

		final StringBuilder html = new StringBuilder();
		html.append("<html><body><center>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;&$920;");
		html.append("</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td height=5></td></tr>");
		html.append("</table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0 width=755 height=25 bgcolor=A7A19A>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td fixWIDTH=5></td>");
		html.append("<td fixwidth=600>");
		html.append("<a action=\"bypass _maillist_0_1_0_\">[&$917;]</a>&nbsp;(" + getCountLetters(activeChar, "inbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_sentbox\">[&$918;]</a>&nbsp;(" + getCountLetters(activeChar, "sentbox") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_archive\">[&$919;]</a>&nbsp;(" + getCountLetters(activeChar, "archive") + ")&nbsp;");
		html.append("<a action=\"bypass _maillist_0_1_0_temp_archive\">[&$920;]</a>&nbsp;(" + getCountLetters(activeChar, "temparchive") + ")</td>");
		html.append("<td fixWIDTH=5></td>");
		html.append("</tr>");
		html.append("<tr><td height=10></td></tr>");
		html.append("</table>");
		html.append("<br>");
		html.append("<table border=0 cellspacing=0 cellpadding=2 bgcolor=A7A19A width=755><tr>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("<td FIXWIDTH=150 align=center>&$911;</td>");
		html.append("<td FIXWIDTH=440>&$413;</td>");
		html.append("<td FIXWIDTH=150 align=center>&$910;</td>");
		html.append("<td FIXWIDTH=5 align=center></td>");
		html.append("</tr></table>");
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.location.equals("temparchive"))
			{
				if (index < minIndex)
				{
					index++;
					continue;
				}
				if (index > maxIndex)
					break;
				String tempName = getCharName(letter.senderId);
				html.append("<table border=0 cellspacing=0 cellpadding=5 width=755><tr>");
				html.append("<td FIXWIDTH=5 align=center></td>");
				html.append("<td FIXWIDTH=150 align=center>" + abbreviate(tempName, 6) + "</td>");
				html.append("<td FIXWIDTH=440 ><a action=\"bypass _maillist_0_1_0_view " + letter.letterId + "\">");
				if (letter.unread)
					html.append("<font color=\"LEVEL\">");
				html.append(abbreviate(letter.subject, 51));
				if (letter.unread)
					html.append("</font>");
				html.append("</a>");
				html.append("</td><td FIXWIDTH=150 align=center>" + letter.sentDateString.substring(0, letter.sentDateString.length() - 5) + "</td>");
				html.append("<td FIXWIDTH=5 align=center></td></tr></table>");
				html.append("<img src=\"L2UI.Squaregray\" width=\"755\" height=\"1\">");
				index++;
			}
		}
		html.append("<br>");
		html.append("<table width=755 cellspace=0 cellpadding=0><tr><td width=50></td>");
		html.append("<td width=510 align=center>");
		html.append("<table cellspacing=2 cellpadding=0 border=0><tr>");
		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_temp_archive " + (page == 1 ? page : page - 1) + "\" back=\"L2UI_CT1.Button_DF_Left_Down\" fore=\"L2UI_CT1.Button_DF_Left\" width=15 height=15>");
		html.append("</td></tr></table></td>");

		int i = 0;
		if (maxpage > 21)
		{
			if (page <= 11)
			{
				for (i = 1; i <= (10 + page); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_temp_archive " + i + "\"> " + i + " </a></td>");
				}
			}
			else if (page > 11 && (maxpage - page) > 10)
			{
				for (i = (page - 10); i <= (page - 1); i++)
				{
					if (i == page)
						continue;

					html.append("<td><a action=\"bypass _maillist_0_1_0_temp_archive " + i + "\"> " + i + " </a></td>");
				}
				for (i = page; i <= (page + 10); i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_temp_archive " + i + "\"> " + i + " </a></td>");
				}
			}
			else if ((maxpage - page) <= 10)
			{
				for (i = (page - 10); i <= maxpage; i++)
				{
					if (i == page)
						html.append("<td> " + i + " </td>");
					else
						html.append("<td><a action=\"bypass _maillist_0_1_0_temp_archive " + i + "\"> " + i + " </a></td>");
				}
			}
		}
		else
		{
			for (i = 1; i <= maxpage; i++)
			{
				if (i == page)
					html.append("<td> " + i + " </td>");
				else
					html.append("<td><a action=\"bypass _maillist_0_1_0_temp_archive " + i + "\"> " + i + " </a></td>");
			}
		}

		html.append("<td><table><tr><td></td></tr><tr><td>");
		html.append("<button action=\"bypass _maillist_0_1_0_temp_archive " + (page == maxpage ? page : page + 1) + "\" back=\"L2UI_CT1.Button_DF_Right_Down\" fore=\"L2UI_CT1.Button_DF_Right\" width=15 height=15 >");
		html.append("</td></tr></table></td></tr></table></td>");
		// html.append("<td align=right><button value=\"&$421;\" action=\"bypass _maillist_0_1_0_write\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("</tr><tr><td width=5 height=10></td></tr>");
		html.append("<tr>");
		html.append("<td></td>");
		html.append("<td align=center><table border=0><tr><td>");
		html.append("<combobox width=65 var=SearchTarget list=\"Writer;Title\">");
		html.append("</td><td><edit var=\"keyword\" width=130 height=15 length=\"16\"></td>");
		html.append("<td><button value=\"&$420;\" action=\"bypass _maillist_0_1_0_search $combo $keyword\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td></tr></table></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("</center></body></html>");
		separateAndSend(html.toString(), activeChar);
	}

	private static void showWriteView(L2PcInstance activeChar)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;&$421;");
		html.append("</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0><tr><td width=755><img src=\"L2UI.SquareGray\" width=\"755\" height=\"1\"></td></tr></table>");
		html.append("<table fixwidth=755 border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td width=5 height=20></td></tr>");
		html.append("<tr>");
		html.append("<td width=5></td>");
		html.append("<td FIXWIDTH=80 height=29>Recipient:</td>");
		html.append("<td FIXWIDTH=520><edit var=\"Recipients\" width=670 height=13 length=\"128\"></td>");
		html.append("<td width=5></td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td ></td>");
		html.append("<td height=29>&$413;:</td>");
		html.append("<td><edit var=\"Title\" width=670 height=13 length=\"128\"></td>");
		html.append("<td></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<table fixwidth=755 border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr>");
		html.append("<td width=5></td>");
		html.append("<td FIXWIDTH=80 height=29 valign=top>&$427;:</td>");
		html.append("<td FIXWIDTH=510><MultiEdit var=\"Message\" width=670 height=313></td>");
		html.append("<td width=5></td>");
		html.append("</tr>");
		html.append("<tr><td width=5 height=10></td></tr>");
		html.append("</table>");
		html.append("<table fixwidth=755 border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td width=5></td>");
		html.append("<td align=center FIXWIDTH=80 height=29>&nbsp;</td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$1078;\" action=\"Write Mail Send _ Recipients Title Message\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$141;\" action=\"bypass _maillist_0_1_0_viewlist\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$658;\" action=\"\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXWIDTH=340>&nbsp;</td><td width=5></td>");
		html.append("</tr></table>");
		html.append("</body></html>");
		separateAndSend(html.toString(), activeChar);
	}

	private static void showWriteView(L2PcInstance activeChar, String parcipientName, Mail letter)
	{
		final StringBuilder html = new StringBuilder();
		html.append("<html><body>");
		html.append("<br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td FIXWIDTH=15>&nbsp;</td>");
		html.append("<td width=755 height=30 align=left>");
		html.append("<a action=\"bypass _bbshome\">Home</a>");
		html.append("&nbsp;&nbsp;&gt;&nbsp;<a action=\"bypass _maillist_0_1_0_viewlist\">&$905;</a>&nbsp;&nbsp;&gt;&nbsp;&$421;");
		html.append("</td></tr></table>");
		html.append("<table border=0 cellspacing=0 cellpadding=0><tr><td width=755><img src=\"L2UI.SquareGray\" width=\"755\" height=\"1\"></td></tr></table>");
		html.append("<table fixwidth=755 border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td width=5 height=20></td></tr>");
		html.append("<tr>");
		html.append("<td width=5></td>");
		html.append("<td FIXWIDTH=80 height=29>Recipient:</td>");
		html.append("<td FIXWIDTH=520><combobox var=\"Recipients\" width=670 height=13 list=\"" + parcipientName + "\"></td>");
		html.append("<td width=5></td>");
		html.append("</tr>");
		html.append("<tr>");
		html.append("<td ></td>");
		html.append("<td height=29>&$413;:</td>");
		html.append("<td><edit var=\"Title\" width=670 height=13 length=\"128\"></td>");
		html.append("<td></td>");
		html.append("</tr>");
		html.append("</table>");
		html.append("<table fixwidth=755 border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr>");
		html.append("<td width=5></td>");
		html.append("<td FIXWIDTH=80 height=29 valign=top>&$427;:</td>");
		html.append("<td FIXWIDTH=520><MultiEdit var=\"Message\" width=670 height=313></td>");
		html.append("<td width=5></td>");
		html.append("</tr>");
		html.append("<tr><td width=5 height=10></td></tr>");
		html.append("</table>");
		html.append("<table fixwidth=755 border=0 cellspacing=0 cellpadding=0>");
		html.append("<tr><td height=10></td></tr>");
		html.append("<tr>");
		html.append("<td width=5></td>");
		html.append("<td align=center FIXWIDTH=80 height=29>&nbsp;</td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$1078;\" action=\"Write Mail Send _ Recipients Title Message\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$141;\" action=\"bypass _maillist_0_1_0_viewlist\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$658;\" action=\"\" back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXwidth=70><button value=\"&$425;\" action=\"bypass _maillist_0_1_0_delete " + letter.letterId + "\"  back=\"l2ui_ct1.button.button_df_small_down\" width=70 height=25 fore=\"l2ui_ct1.button.button_df_small\"></td>");
		html.append("<td align=center FIXWIDTH=340>&nbsp;</td><td width=5></td>");
		html.append("</tr></table>");
		html.append("</body></html>");
		send1001(html.toString(), activeChar);
		send1002(activeChar, " ", "Re: " + letter.subject, "0");
	}

	private void sendLetter(String recipients, String subject, String message, L2PcInstance activeChar)
	{
		int countTodaysLetters = 0;
		Timestamp ts = new Timestamp(Calendar.getInstance().getTimeInMillis() - 86400000L);
		long date = Calendar.getInstance().getTimeInMillis();

		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
			if (letter.sentDate.after(ts) && letter.location.equals("sentbox"))
				countTodaysLetters++;

		if (countTodaysLetters >= 10 && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.NO_MORE_MESSAGES_TODAY);
			return;
		}

		if (subject == null || subject.isEmpty())
			subject = "(no subject)";

		try (Connection con = DatabaseFactory.getConnection())
		{
			Set<String> recipts = new HashSet<>(5);
			String[] recipAr = recipients.split(";");
			for (String r : recipAr)
				recipts.add(r.trim());

			message = message.replaceAll("\n", "<br1>");

			boolean sent = false;
			int countRecips = 0;

			Timestamp time = new Timestamp(date);
			PreparedStatement statement = null;

			for (String recipient : recipts)
			{
				int recipId = CharNameTable.getInstance().getIdByName(recipient);
				if (recipId <= 0)
					activeChar.sendMessage("Could not find " + recipient + ", Therefore will not get mail.");
				else if (isGM(recipId) && !activeChar.isGM())
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.CANNOT_MAIL_GM_S1).addString(recipient));
				else if (isBlocked(activeChar, recipId) && !activeChar.isGM())
					activeChar.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_BLOCKED_YOU_CANNOT_MAIL).addString(recipient));
				else if (isRecipInboxFull(recipId) && !activeChar.isGM())
				{
					activeChar.sendPacket(SystemMessageId.MESSAGE_NOT_SENT);
					L2PcInstance PCrecipient = L2World.getInstance().getPlayer(recipient);
					if (PCrecipient != null)
						PCrecipient.sendPacket(SystemMessageId.MAILBOX_FULL);
				}
				else if (countRecips < 5 && !activeChar.isGM() || activeChar.isGM())
				{
					int id = getNewMailId();
					if (statement == null)
					{
						statement = con.prepareStatement(INSERT_NEW_MAIL);
						statement.setInt(3, activeChar.getObjectId());
						statement.setString(4, "inbox");
						statement.setString(5, recipients);
						statement.setString(6, abbreviate(subject, 128));
						statement.setString(7, message);
						statement.setTimestamp(8, time);
						statement.setInt(9, 1);
					}
					statement.setInt(1, recipId);
					statement.setInt(2, id);
					statement.execute();
					sent = true;

					Mail letter = new Mail();
					letter.charId = recipId;
					letter.letterId = id;
					letter.senderId = activeChar.getObjectId();
					letter.location = "inbox";
					letter.recipientNames = recipients;
					letter.subject = abbreviate(subject, 128);
					letter.message = message;
					letter.sentDate = time;
					letter.sentDateString = String.valueOf(time);
					letter.unread = true;
					getPlayerMails(recipId).addFirst(letter);

					countRecips++;

					L2PcInstance PCrecipient = L2World.getInstance().getPlayer(recipient);
					if (PCrecipient != null)
					{
						PCrecipient.sendPacket(SystemMessageId.NEW_MAIL);
						PCrecipient.sendPacket(ExMailArrived.STATIC_PACKET);
					}
				}
			}

			// Create a copy into activeChar's sent box
			if (statement != null)
			{
				int id = getNewMailId();

				statement.setInt(1, activeChar.getObjectId());
				statement.setInt(2, id);
				statement.setString(4, "sentbox");
				statement.setInt(9, 0);
				statement.execute();
				statement.close();

				Mail letter = new Mail();
				letter.charId = activeChar.getObjectId();
				letter.letterId = id;
				letter.senderId = activeChar.getObjectId();
				letter.location = "sentbox";
				letter.recipientNames = recipients;
				letter.subject = abbreviate(subject, 128);
				letter.message = message;
				letter.sentDate = time;
				letter.sentDateString = String.valueOf(time);
				letter.unread = false;
				getPlayerMails(activeChar.getObjectId()).addFirst(letter);
			}

			if (countRecips > 5 && !activeChar.isGM())
				activeChar.sendPacket(SystemMessageId.ONLY_FIVE_RECIPIENTS);

			if (sent)
				activeChar.sendPacket(SystemMessageId.SENT_MAIL);
		}
		catch (Exception e)
		{
			_log.warn("couldnt send letter for " + activeChar.getName() + " " + e.getMessage());
		}
	}

	private int getCountLetters(L2PcInstance activeChar, String location)
	{
		return getCountLetters(activeChar.getObjectId(), location);
	}

	private int getCountLetters(int objId, String location)
	{
		int count = 0;
		for (Mail letter : getPlayerMails(objId))
		{
			if (letter.location.equals(location))
				count++;
		}
		return count;
	}

	private static boolean isBlocked(L2PcInstance activeChar, int recipId)
	{
		for (L2PcInstance player : L2World.getInstance().getAllPlayers().values())
		{
			if (player.getObjectId() == recipId)
			{
				if (BlockList.isInBlockList(player, activeChar))
					return true;

				return false;
			}
		}
		return false;
	}

	private void deleteLetter(L2PcInstance activeChar, int letterId)
	{
		for (Mail letter : getPlayerMails(activeChar.getObjectId()))
		{
			if (letter.letterId == letterId)
			{
				getPlayerMails(activeChar.getObjectId()).remove(letter);
				break;
			}
		}

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(DELETE_MAIL);
			statement.setInt(1, letterId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("couldnt delete letter " + letterId + " " + e);
		}
	}

	private void setLetterToRead(L2PcInstance activeChar, int letterId)
	{
		getLetter(activeChar, letterId).unread = false;

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(MARK_MAIL_READ);
			statement.setInt(1, 0);
			statement.setInt(2, letterId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("couldnt set unread to false for " + letterId + " " + e);
		}
	}

	private void setLetterLocation(L2PcInstance activeChar, int letterId, String location)
	{
		getLetter(activeChar, letterId).location = location;

		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement(SET_LETTER_LOC);
			statement.setString(1, location);
			statement.setInt(2, letterId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn("couldnt set location to false for " + letterId + " " + e);
		}
	}

	private static String getCharName(int charId)
	{
		String name = CharNameTable.getInstance().getNameById(charId);
		return name == null ? "Unknown" : name;
	}

	private static boolean isGM(int charId)
	{
		boolean isGM = false;
		try (Connection con = DatabaseFactory.getConnection())
		{
			PreparedStatement statement = con.prepareStatement("SELECT accesslevel FROM characters WHERE obj_Id = ?");
			statement.setInt(1, charId);
			ResultSet result = statement.executeQuery();
			result.next();
			isGM = result.getInt(1) > 0;
			result.close();
			statement.close();
		}
		catch (Exception e)
		{
			_log.warn(e.getMessage());
		}
		return isGM;
	}

	private boolean isRecipInboxFull(int charId)
	{
		return getCountLetters(charId, "inbox") >= 100;
	}

	@Override
	public void parseWrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		if (ar1.equals("Send"))
		{
			sendLetter(ar3, ar4, ar5, activeChar);
			showSentbox(activeChar, 1);
		}
	}

	private void showLastForum(L2PcInstance activeChar)
	{
		int page = activeChar.getMailPosition() % 1000;
		int type = activeChar.getMailPosition() / 1000;

		switch (type)
		{
			case 0:
				showInbox(activeChar, page);
				break;

			case 1:
				showSentbox(activeChar, page);
				break;

			case 2:
				showMailArchive(activeChar, page);
				break;

			case 3:
				showTempMailArchive(activeChar, page);
				break;
		}
	}

	private static int getMaxPageId(int letterCount)
	{
		if (letterCount < 1)
			return 1;

		if (letterCount % 10 == 0)
			return letterCount / 10;

		return (letterCount / 10) + 1;
	}

	private static class SingletonHolder
	{
		protected static final MailBBSManager _instance = new MailBBSManager();
	}
}
