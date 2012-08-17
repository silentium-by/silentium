/*
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option) any later version. This program is distributed in the hope that
 * it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */
package silentium.commons.utils;

import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ATracer
 */
public class DeadLockDetector extends Thread
{
	private static Logger log = LoggerFactory.getLogger(DeadLockDetector.class.getName());

	private final long checkInterval;
	private final boolean restartWhenDeadLock;

	private static final String INDENT = "    ";
	private StringBuilder sb;

	public DeadLockDetector(final long checkInterval)
	{
		this.checkInterval = checkInterval * 1000L;
		restartWhenDeadLock = false;
	}

	public DeadLockDetector(final long checkInterval, final boolean restartWhenDeadLock)
	{
		this.checkInterval = checkInterval * 1000L;
		this.restartWhenDeadLock = restartWhenDeadLock;
	}

	@Override
	public void run()
	{
		boolean noDeadLocks = true;

		while (noDeadLocks)
			try
			{
				final ThreadMXBean bean = ManagementFactory.getThreadMXBean();
				final long[] threadIds = bean.findDeadlockedThreads();

				if (threadIds != null)
				{
					log.error("Deadlock detected !");
					sb = new StringBuilder();
					noDeadLocks = false;

					final ThreadInfo[] infos = bean.getThreadInfo(threadIds);
					sb.append("\nTHREAD LOCK INFO: \n");
					for (final ThreadInfo threadInfo : infos)
					{
						printThreadInfo(threadInfo);
						final LockInfo[] lockInfos = threadInfo.getLockedSynchronizers();
						final MonitorInfo[] monitorInfos = threadInfo.getLockedMonitors();

						printLockInfo(lockInfos);
						printMonitorInfo(threadInfo, monitorInfos);
					}

					sb.append("\nTHREAD DUMPS: \n");
					for (final ThreadInfo ti : bean.dumpAllThreads(true, true))
						printThreadInfo(ti);

					log.error(sb.toString());

					if (restartWhenDeadLock)
						System.exit(ExitCode.CODE_RESTART);
				}

				Thread.sleep(checkInterval);
			}
			catch (Exception ex)
			{
				log.error(ex.getLocalizedMessage(), ex);
			}
	}

	private void printThreadInfo(final ThreadInfo threadInfo)
	{
		printThread(threadInfo);

		sb.append(INDENT).append(threadInfo.toString()).append('\n');

		final StackTraceElement[] stacktrace = threadInfo.getStackTrace();
		final MonitorInfo[] monitors = threadInfo.getLockedMonitors();

		for (int i = 0; i < stacktrace.length; i++)
		{
			final StackTraceElement ste = stacktrace[i];
			sb.append(INDENT + "at ").append(ste.toString()).append('\n');
			for (final MonitorInfo mi : monitors)
			{
				if (mi.getLockedStackDepth() == i)
				{
					sb.append(INDENT + "  - locked ").append(mi).append('\n');
				}
			}
		}
	}

	private void printThread(final ThreadInfo ti)
	{
		sb.append("\nPrintThread\n");
		sb.append('"').append(ti.getThreadName()).append('"').append(" Id=").append(ti.getThreadId()).append(" in ").append(ti.getThreadState()).append('\n');
		if (ti.getLockName() != null)
		{
			sb.append(" on lock=").append(ti.getLockName()).append('\n');
		}
		if (ti.isSuspended())
		{
			sb.append(" (suspended)" + '\n');
		}
		if (ti.isInNative())
		{
			sb.append(" (running in native)" + '\n');
		}
		if (ti.getLockOwnerName() != null)
		{
			sb.append(INDENT + " owned by ").append(ti.getLockOwnerName()).append(" Id=").append(ti.getLockOwnerId()).append('\n');
		}
	}

	private void printMonitorInfo(final ThreadInfo threadInfo, final MonitorInfo... monitorInfos)
	{
		sb.append(INDENT + "Locked monitors: count = ").append(monitorInfos.length).append('\n');

		for (final MonitorInfo monitorInfo : monitorInfos)
		{
			sb.append(INDENT + "  - ").append(monitorInfo).append(" locked at ").append('\n');
			sb.append(INDENT + "      ").append(monitorInfo.getLockedStackDepth()).append(' ').append(monitorInfo.getLockedStackFrame()).append('\n');
		}
	}

	private void printLockInfo(final LockInfo... lockInfos)
	{
		sb.append(INDENT + "Locked synchronizers: count = ").append(lockInfos.length).append('\n');

		for (final LockInfo lockInfo : lockInfos)
			sb.append(INDENT + "  - ").append(lockInfo).append('\n');
	}
}