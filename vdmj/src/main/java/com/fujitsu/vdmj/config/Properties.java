/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.config;

import java.io.InputStream;

/**
 * The Properties class is used to hold global configuration values. The
 * values are read from the vdmj.properties file with overrides from
 * System properties.
 */
public class Properties
{
	/** The tab stop for source files. */
	public static int parser_tabstop = 4;
	
	/** Nesting of block comments: 0-3 = support, warning, error, ignore. */
	public static int parser_comment_nesting = 0;
	
	/** The search path for annotation classes to load. */
	public static String annotations_path = "com.fujitsu.vdmj.ast.annotations;annotations.ast";
	
	/** Enable annotation debugging. */
	public static boolean annotations_debug = false;
	
	/** An alternative search path for the ClassMapper. */
	public static String mapping_search_path = null;
	
	/** Skip the check for mutually-recursive function calls. */
	public static boolean tc_skip_recursive_check = false;

	/** Skip the check for definition dependency cycles. */
	public static boolean tc_skip_cyclic_check = false;
	
	/** The maximum TC errors reported before "Too many errors". */
	public static int tc_max_errors = 100;
	
	
	/** The default timeslice (statements executed) for a FCFS policy */
	public static int scheduler_fcfs_timeslice = 10;

	/** The vCPU/vBUS timeslice */
	public static int scheduler_virtual_timeslice = 10000;

	/** The timeslice variation (+/- jitter ticks) */
	public static int scheduler_jitter = 0;

	
	/** The maximum number of expansions for "+" and "*" trace patterns. */
	public static int traces_max_repeats = 5;
	
	/** Serialize the system state between trace tests. */
	public static boolean traces_save_state = false;
	
	/** The size below which trace function args are expanded. */
	public static int traces_max_arg_length = 50;

	
	/** The default duration for RT statements. */
	public static int rt_duration_default = 2;

	/** Enable transactional variable updates. */
	public static boolean rt_duration_transactions = false;

	/** Enable InstVarChange RT log entries. */
	public static boolean rt_log_instvarchanges = false;

	/** Maximum period thread overlaps allowed per object */
	public static int rt_max_periodic_overlaps = 20;

	/** Enable extra RT log diagnostics for guards etc. */
	public static boolean rt_diags_guards = false;

	/** Enable extra RT log diagnostics for timesteps. */
	public static boolean rt_diags_timestep = false;

	
	/** The packages for command plugins to load from. */
	public static String cmd_plugins_path = "plugins";
	
	/** The class name for the DebugLink */
	public static String debug_link_class = null;
	
	/**
	 * When the class is initialized, which uses the vdmj.properties file, and any System
	 * properties, to set the static fields above.
	 */
	public static void init()
	{
		try
		{
			java.util.Properties vdmj = new java.util.Properties();
			InputStream s = Properties.class.getResourceAsStream("/vdmj.properties");
			
			if (s != null)
			{
				vdmj.load(s);
				s.close();
			}
			
			parser_tabstop = get(vdmj, "vdmj.parser.tabstop", parser_tabstop);
			parser_comment_nesting = get(vdmj, "vdmj.parser.comment_nesting", parser_comment_nesting);
			mapping_search_path = get(vdmj, "vdmj.mapping.search_path", mapping_search_path);
			annotations_path = get(vdmj, "vdmj.annotations.path", annotations_path);
			annotations_debug = get(vdmj, "vdmj.annotations.debug", annotations_debug);
			tc_skip_recursive_check = get(vdmj, "vdmj.tc.skip_recursive_check", tc_skip_recursive_check);
			tc_skip_cyclic_check = get(vdmj, "vdmj.tc.skip_cyclic_check", tc_skip_cyclic_check);
			tc_max_errors = get(vdmj, "vdmj.tc.max_errors", tc_max_errors);
			
			scheduler_fcfs_timeslice = get(vdmj, "vdmj.scheduler.fcfs_timeslice", scheduler_fcfs_timeslice);
			scheduler_virtual_timeslice = get(vdmj, "vdmj.scheduler.virtual_timeslice", scheduler_virtual_timeslice);
			scheduler_jitter = get(vdmj, "vdmj.scheduler_jitter", scheduler_jitter);
			
			traces_max_repeats = get(vdmj, "vdmj.traces.max_repeats", traces_max_repeats);
			traces_save_state = get(vdmj, "vdmj.traces.save_state", traces_save_state);
			traces_max_arg_length = get(vdmj, "vdmj.traces.max_arg_length", traces_max_arg_length);
			
			rt_duration_default = get(vdmj, "vdmj.rt.duration_default", rt_duration_default);
			rt_duration_transactions = get(vdmj, "vdmj.rt.duration_transactions", rt_duration_transactions);
			rt_log_instvarchanges = get(vdmj, "vdmj.rt.log_instvarchanges", rt_log_instvarchanges);
			rt_max_periodic_overlaps = get(vdmj, "vdmj.rt.max_periodic_overlaps", rt_max_periodic_overlaps);
			rt_diags_guards = get(vdmj, "vdmj.rt.diags_guards", rt_diags_guards);
			rt_diags_timestep = get(vdmj, "vdmj.rt.diags_timestep", rt_diags_timestep);
			
			cmd_plugins_path = get(vdmj, "vdmj.cmd.plugins_path", cmd_plugins_path);
			debug_link_class = get(vdmj, "vdmj.debug.link_class", null);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private static int get(java.util.Properties local, String key, int def)
	{
		Integer value = Integer.getInteger(key);
		
		if (value == null)
		{
			if (local.containsKey(key))
			{
				try
				{
					String p = local.getProperty(key);
					value = Integer.parseInt(p);
				}
				catch (NumberFormatException e)
				{
					System.err.println(e.getMessage());
					value = def;
				}
			}
			else
			{
				value = def;
			}
		}
		
		return value;
	}
	
	private static boolean get(java.util.Properties local, String key, boolean def)
	{
		String svalue = System.getProperty(key);
		boolean value = def;
		
		if (svalue == null)
		{
			if (local.containsKey(key))
			{
				value = Boolean.parseBoolean(local.getProperty(key));
			}
		}
		else
		{
			value = Boolean.parseBoolean(svalue);
		}
		
		return value;
	}
	
	@SuppressWarnings("unused")
	private static String get(java.util.Properties local, String key, String def)
	{
		String value = System.getProperty(key);
		
		if (value == null)
		{
			if (local.containsKey(key))
			{
				value = local.getProperty(key);
			}
		}
		
		return value;
	}
}
