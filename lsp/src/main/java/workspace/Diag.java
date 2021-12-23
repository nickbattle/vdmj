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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace;

import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility to provide diagnostic logging.
 */
public class Diag
{
	/** The Java logger */
	private static Logger logger;

	/** The handler - set to a FileHandler */
	private static Handler handler;

	/**
	 * Set a handler to save diags to a file for the LSP configuration.
	 */
	public static synchronized void init()
	{
		logger = Logger.getLogger("LSPServer");
		logger.setUseParentHandlers(false);
		logger.setLevel(Level.ALL);

		String filename = System.getProperty("lsp.log.filename");
		
		if (filename != null)
		{
			try
			{
				handler = new FileHandler(filename);
				handler.setFormatter(new DiagFormatter());
				handler.setLevel(Level.ALL);
				logger.addHandler(handler);
			}
			catch (Exception e)
			{
				System.err.printf("Cannot create log file: %s\n", filename);
			}
		}
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
		info("Logging level changed from " + old.getName()+ " to " + level.getName());
	}

	/**
     * Set the current diagnostic level using a string level name. The name can
     * be: all, fine, info, config, warning, severe or off. An unrecognised
     * level sets the level to SEVERE.
     * 
     * @param level
     *            The new diagnostic level
     */
	public static synchronized void setLevel(String level)
	{
		if (level.equalsIgnoreCase("all"))
			setLevel(Level.ALL);
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
     * Log a message unconditionally. This raises the message at OFF level - ie.
     * even if diagnostics are off, still raise the message.
     * 
     * @param line
     *            The text to log.
     */
	public static synchronized void log(String line)
	{
		logger.log(Level.ALL, line);
	}

	public static synchronized void log(String format, Object... args)
	{
		log(String.format(format, args));
	}

	/**
     * Log a message at FINE level.
     * 
     * @param line
     *            The text to log.
     */
	public static synchronized void fine(String line)
	{
		logger.fine(line);
	}

	public static synchronized void fine(String format, Object... args)
	{
		fine(String.format(format, args));
	}

	/**
     * Log a message at INFO level.
     * 
     * @param line
     *            The text to log.
     */
	public static synchronized void info(String line)
	{
		logger.info(line);
	}

	public static synchronized void info(String format, Object... args)
	{
		info(String.format(format, args));
	}

	/**
     * Log a message at CONFIG level.
     * 
     * @param line
     *            The text to log.
     */
	public static synchronized void config(String line)
	{
		logger.config(line);
	}

	public static synchronized void config(String format, Object... args)
	{
		config(String.format(format, args));
	}

	/**
     * Log a message at WARNING level.
     * 
     * @param line
     *            The text to log.
     */
	public static synchronized void warning(String line)
	{
		logger.warning(line);
	}

	public static synchronized void warning(String format, Object... args)
	{
		warning(String.format(format, args));
	}

	/**
     * Log a message at SEVERE level.
     * 
     * @param line
     *            The text to log.
     */
	public static synchronized void severe(String line)
	{
		logger.severe(line);
	}

	public static synchronized void severe(String format, Object... args)
	{
		severe(String.format(format, args));
	}
}
