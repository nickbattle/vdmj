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

package commands;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import dap.DAPMessageList;
import dap.DAPRequest;
import examples.v2c.tr.TRNode;
import examples.v2c.tr.definitions.TRClassList;
import examples.v2c.tr.modules.TRModuleList;
import json.JSONObject;
import vdmj.commands.AnalysisCommand;
import workspace.PluginRegistry;
import workspace.plugins.TCPlugin;

/**
 * All LSP console commands must extend AnalysisCommand.
 */
public class TranslateCommandLSP extends AnalysisCommand
{
	private final static String CMD = "translate";
	private final static String USAGE = "Usage: " + CMD;
	public  final static String HELP = CMD + " - translate the VDM specification";

	/**
	 * The constructor is called from the plugin's getCommand when the user types
	 * "translate". It is passed the whole line typed by the user, which is broken into
	 * an argv[] array by the superclass.
	 */
	public TranslateCommandLSP(String line)
	{
		super(line);
		
		if (!argv[0].equals("translate"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}
	
	/**
	 * The run method is called whenever the user types "translate" in the VSCode console.
	 * Note that this class has access to the "argv" array, which is created during construction (above).
	 * 
	 * The example run method uses the ClassMapper to turn the type checked tree (from the TCPlugin)
	 * into a "TR" tree. This uses the mappings file defined in the TRNode root class, which all
	 * translatable classes must extend.
	 * 
	 * After converting the TC tree to a TR tree, this is used to translate the specification
	 * into "C". The result, a String, is returned by the method, which appears in the user console
	 * session.
	 */
	@Override
	public DAPMessageList run(DAPRequest request)
	{
		try
		{
			TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
			String result = null;
			
			switch (Settings.dialect)
			{
				case VDM_SL:
					TCModuleList mlist = tc.getTC();
					TRModuleList trModules = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(mlist);
					result = trModules.translate();
					break;
					
				case VDM_PP:
				case VDM_RT:
					TCClassList clist = tc.getTC();
					TRClassList trClasses = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(clist);
					result = trClasses.translate();
					break;
					
				default:
					return new DAPMessageList(request, false, "Unknown dialect?", null);
			}

			return new DAPMessageList(request, new JSONObject("result", result));
		}
		catch (Exception e)
		{
			return new DAPMessageList(request, false,
				"Specification contains untranslatable clauses:\n" +e.getMessage(), null);
		}
	}

	@Override
	public boolean notWhenRunning()
	{
		return true;
	}
}
