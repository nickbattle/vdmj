/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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

package commands;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.PluginRegistry;
import com.fujitsu.vdmj.plugins.analyses.TCPlugin;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import examples.v2c.tr.TRNode;
import examples.v2c.tr.definitions.TRClassList;
import examples.v2c.tr.modules.TRModuleList;

/**
 * All command line plugins must extend AnalysisCommand and be in the class "plugins" by
 * default. The packages searched for plugins can be changed using the "vdmj.cmd.plugin_packages"
 * JVM property.
 */
public class TranslateCommand extends AnalysisCommand
{
	/**
	 * The constructor is called from the command line interpreter when the user first types
	 * "translate <args>". It is passed the whole line typed by the user, which is broken into
	 * an argv[] array by the superclass.
	 */
	public TranslateCommand(String line)
	{
		super(line);
	}
	
	/**
	 * The run method is called whenever the user types "translate <args>" in the VDMJ command
	 * line interpreter (CommandReader.java). Note that this class has access to the "argv"
	 * array, which is created during construction (above).
	 * 
	 * The example run method uses the ClassMapper to turn the type checked tree (from the TCPlugin)
	 * into a "TR" tree. This uses the mappings file defined in the TRNode root class, which all
	 * translatable classes must extend.
	 * 
	 * After converting the TC tree to a TR tree, this is then used to translate the specification
	 * into "C". The result, a String, is just returned by the method, which appears in the user console
	 * session.
	 */
	@Override
	public String run(String line)
	{
		try
		{
			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			
			switch (Settings.dialect)
			{
				case VDM_SL:
					TCModuleList tcModules = tc.getTC();
					TRModuleList trModules = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(tcModules);
					return trModules.translate();
				
				case VDM_PP:
				case VDM_RT:
					TCClassList tcClasses = tc.getTC();
					TRClassList trClasses = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(tcClasses);
					return trClasses.translate();

				default:
					return "Unknown interpreter type?";
			}
		}
		catch (Exception e)
		{
			System.out.println("Specification contains untranslatable clauses:");
			System.out.println(e.getMessage());
			return null;
		}
	}

	/**
	 * Once this plugin is loaded  the following help line is added to the general
	 * "help" output in the command line. The format is the same as the other help lines, with
	 * an indication of what arguments the command takes, and a one line description.
	 */
	public static void help()
	{
		System.out.println("translate - translate the VDM specification");
	}
}
