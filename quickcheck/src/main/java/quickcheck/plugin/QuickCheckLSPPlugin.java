/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.plugin;

import java.util.Vector;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.util.Utils;

import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import quickcheck.QuickCheck;
import quickcheck.commands.QCConsole;
import quickcheck.commands.QCRunLSPCommand;
import quickcheck.commands.QuickCheckExecutor;
import quickcheck.commands.QuickCheckLSPCommand;
import vdmj.commands.AnalysisCommand;
import workspace.events.DAPEvent;
import workspace.events.UnknownCommandEvent;
import workspace.plugins.AnalysisPlugin;

public class QuickCheckLSPPlugin extends AnalysisPlugin
{
	public static AnalysisPlugin factory(Dialect dialect)
	{
		return new QuickCheckLSPPlugin();
	}
	
	@Override
	public String getName()
	{
		return "QC";
	}

	@Override
	public void init()
	{
		// Get everything from PO?
	}
	
	@Override
	public DAPMessageList handleEvent(DAPEvent event) throws Exception
	{
		if (event instanceof UnknownCommandEvent)
		{
			UnknownCommandEvent ume = (UnknownCommandEvent)event;
			
			switch (ume.request.getCommand())
			{
				case "slsp/POG/quickcheck":
					return quickCheck(ume.request);
			}
		}
		
		return null;
	}
	
	private DAPMessageList quickCheck(DAPRequest request)
	{
		QuickCheck qc = new QuickCheck();

		qc.loadStrategies(new Vector<String>());	// Use defaults
		
		if (qc.hasErrors())
		{
			return new DAPMessageList(request, false, "Failed to load QC strategies", null);
		}
		
		QCConsole.setQuiet(true);
		QCConsole.setVerbose(false);

		Vector<Integer> poList = new Vector<Integer>();
		Vector<String> poNames = new Vector<String>();
		poNames.add(".*");	// Include everything
		
		QuickCheckExecutor executor = new QuickCheckExecutor(request, qc, 1L, poList, poNames);
		executor.start();
		
		return null;
	}

	@Override
	public void setServerCapabilities(JSONObject capabilities)
	{
		JSONObject provider = capabilities.getPath("experimental.proofObligationProvider");
		
		if (provider != null)
		{
			provider.put("quickCheckProvider", true);
		}
	}
	
	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] argv = Utils.toArgv(line);
		
		switch (argv[0])
		{
			case "quickcheck":
			case "qc":
				return new QuickCheckLSPCommand(line);
				
			case "qcrun":
			case "qr":
				return new QCRunLSPCommand(line);
		}
		
		return null;
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList(
			QuickCheckLSPCommand.SHORT + " - lightweight PO verification",
			QCRunLSPCommand.HELP);
	}
}
