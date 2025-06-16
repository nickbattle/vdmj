/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A utility to provide diagnostic logging.
 */
public class Diag
{
	/** The Java logger. */
	private static Logger logger;
	
	/** True if we are the LSPServerDebug */
	private static boolean isDebugServer;

	/**
	 * Set a handler to save diags to a file for the LSP configuration.
	 */
	public static synchronized void init(boolean debugServer)
	{
		isDebugServer = debugServer;
		
		Formatter formatter = new Formatter()
		{
			@Override
			public String format(LogRecord rec)
			{
				Calendar now = new GregorianCalendar();
				now.setTimeInMillis(rec.getMillis());
				
				return String.format("%02d:%02d:%02d.%03d: [%s][%s] %s\n",
					now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE),
					now.get(Calendar.SECOND), now.get(Calendar.MILLISECOND),
					rec.getLevel().getName(), rec.getThreadID(), rec.getMessage());
			}
		};
				
		logger = Logger.getLogger("LSPServer");
		logger.setUseParentHandlers(false);

		String filename = System.getProperty("lsp.log.filename");
		
		if (filename != null)
		{
			try
			{
				Handler handler = new FileHandler(filename);
				handler.setFormatter(formatter);
				handler.setLevel(Level.ALL);
				logger.addHandler(handler);
			}
			catch (Exception e)
			{
				System.err.printf("Cannot create log file: %s\n", filename);
			}
		}
		else
		{
			Handler handler = new ConsoleHandler();
			handler.setFormatter(formatter);
			handler.setLevel(Level.ALL);
			logger.addHandler(handler);
		}
		
		if (isDebugServer)
		{
			logger.setLevel(Level.ALL);
			info("Enabling all logging for debug server");
		}
		else
		{
			String defLevel = (filename != null ? "info" : "off");
			setLevel(System.getProperty("lsp.log.level", defLevel));
		}
	}
	
	/**
	 * Check whether we are the debug server.
	 */
	public static synchronized boolean isDebugServer()
	{
		return isDebugServer;
	}
	
	/**
	 * Check whether a given diag Level is being logged. Note this is
	 * always true if we are the debug server.
	 */
	public static synchronized boolean isLoggable(Level level)
	{
		return logger.isLoggable(level) || isDebugServer;
	}

	/**
     * Set the current diagnostic level using a Level object.
     * 
     * @param level
     *            The new diagnostic level
     */
	public static synchronized void setLevel(Level level)
	{
		Level old = logger.getLevel();
		logger.setLevel(level);
		
		if (old != null)
		{
			info("Logging level changed from " + old.getName()+ " to " + level.getName());
		}
		else
		{
			info("Logging level set to " + level.getName());
		}
	}

	/**
     * Set the current diagnostic level using a string level name. The name can
     * be: all, finest, finer, fine, info, config, warning, severe or off.
     * An unrecognised level sets the level to SEVERE.
     */
	public static synchronized void setLevel(String level)
	{
		if (level.equalsIgnoreCase("all"))
			setLevel(Level.ALL);
		else if (level.equalsIgnoreCase("off"))
			setLevel(Level.OFF);
		else if (level.equalsIgnoreCase("finest"))
			setLevel(Level.FINEST);
		else if (level.equalsIgnoreCase("finer"))
			setLevel(Level.FINER);
		else if (level.equalsIgnoreCase("fine"))
			setLevel(Level.FINE);
		else if (level.equalsIgnoreCase("info"))
			setLevel(Level.INFO);
		else if (level.equalsIgnoreCase("config"))
			setLevel(Level.CONFIG);
		else if (level.equalsIgnoreCase("warning"))
			setLevel(Level.WARNING);
		else
			setLevel(Level.SEVERE);
	}

	/**
     * Log a message unconditionally. This bypasses the Logger and uses
     * the handler(s) directly.
     */
	public static synchronized void log(String format, Object... args)
	{
		LogRecord rec = new LogRecord(Level.ALL, String.format(format, args));
		
		for (Handler h: logger.getHandlers())
		{
			h.publish(rec);
		}
	}

	/**
     * Log a message at various levels.
     */
	public static synchronized void finest(String format, Object... args)
	{
		logger.log(Level.FINEST, String.format(format, args));
	}

	public static synchronized void finer(String format, Object... args)
	{
		logger.log(Level.FINER, String.format(format, args));
	}

	public static synchronized void fine(String format, Object... args)
	{
		logger.log(Level.FINE, String.format(format, args));
	}

	public static synchronized void info(String format, Object... args)
	{
		logger.log(Level.INFO, String.format(format, args));
	}

	public static synchronized void config(String format, Object... args)
	{
		logger.log(Level.CONFIG, String.format(format, args));
	}

	public static synchronized void warning(String format, Object... args)
	{
		logger.log(Level.WARNING, String.format(format, args));
	}

	public static synchronized void severe(String format, Object... args)
	{
		logger.log(Level.SEVERE, String.format(format, args));
	}

	public static synchronized void error(String format, Object... args)
	{
		logger.log(Level.SEVERE, String.format("ERROR: " + format, args));
	}

	private static final int STACK_DUMP = 10;	// Frames to dump for exceptions
	
	public static synchronized void error(Throwable throwable)
	{
		logger.log(Level.SEVERE, String.format("EXCEPTION: %s %s",
				throwable.getClass().getSimpleName(), throwable.getMessage()));
		
		StackTraceElement[] stack = throwable.getStackTrace();
		int count = stack.length < STACK_DUMP ? stack.length : STACK_DUMP;
		
		for (int i=0; i<count; i++)
		{
			StackTraceElement frame = stack[i];
			logger.log(Level.SEVERE, String.format("%s %s at %s line %d",
					frame.getClassName(), frame.getMethodName(), frame.getFileName(), frame.getLineNumber()));
		}
	}
}
