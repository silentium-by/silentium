/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.commons.utils.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.google.common.base.Joiner;
import org.joda.time.DateTime;

/*
 * @author Tatanka
 */
public class ConsoleLogLayout extends LayoutBase<ILoggingEvent> {
	private static final Joiner joiner = Joiner.on(" ").skipNulls();
	private static final String CRLF = "\r\n";

	@Override
	public String doLayout(final ILoggingEvent event) {
		return joiner.join(new DateTime(event.getTimeStamp()).toString("HH:mm:ss"), event.getLevel().toString(),
				event.getThreadName(), "in", event.getLoggerName(), "-", event.getFormattedMessage(), CRLF);
	}
}