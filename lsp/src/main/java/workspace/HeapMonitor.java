/*******************************************************************************
 *
 *	Copyright (c) 2026 Nick Battle.
 *
 *	Author: Nick Battle
 *
 *	This file is part of VDMJ.
 *
 *	VDMJ is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	VDMJ is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace;

public class HeapMonitor extends Thread
{
	private static HeapMonitor INSTANCE;
	private Runtime runtime = Runtime.getRuntime();
	private long highTide = 0;

	private HeapMonitor()
	{
		setName("HeapMonitor");
		Diag.info("Created HeapMonitor");
	}

	public static synchronized HeapMonitor getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new HeapMonitor();
		}

		return INSTANCE;
	}

	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE.interrupt();
			INSTANCE = null;
		}
	}

	@Override
	public void run()
	{
		try
		{
			while (true)
			{
				long total = runtime.totalMemory();
				long free = runtime.freeMemory();
				long used = total - free;

				if (used > highTide)
				{
					highTide = used;
				}

				sleep(1000);
			}
		}
		catch (InterruptedException e)
		{
			// End gracefully
		}
	}

	public void check()
	{
		long max = runtime.maxMemory();
		double percent = (double) highTide / max * 100.0;
		Diag.info("Heap percent usage: %f", percent);

		if (percent > 90)
		{
			Diag.warning("Heap too full!");
			// LSPPlugin plugin = PluginRegistry.getInstance().getPlugin("LSP");
			// plugin.sendMessage(2L, "JVM heap over 90%. Use -Xmx to increase.");
		}

		highTide = 0;
	}
}
