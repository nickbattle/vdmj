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

package dap.handlers;

import java.io.File;
import java.io.IOException;

import dap.DAPHandler;
import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import lsp.Utils;
import vdmj.DAPDebugReader;
import workspace.DAPWorkspaceManager;

public class SetBreakpointsHandler extends DAPHandler
{
	public SetBreakpointsHandler()
	{
		super();
	}
	
	@Override
	public DAPMessageList run(DAPRequest request) throws IOException
	{
		switch (request.getCommand())
		{
			case "setBreakpoints":
				return setBreakpoints(request);

			case "setExceptionBreakpoints":
				return setExceptionBreakpoints(request);
		
			default:
				return new DAPMessageList(request, false, "Unexpected breakpoint request", null);
		}

	}
	
	private DAPMessageList setBreakpoints(DAPRequest request) throws IOException
	{
		try
		{
			DAPWorkspaceManager manager = DAPWorkspaceManager.getInstance();
			DAPDebugReader debugReader = manager.getDebugReader();
			
			if (debugReader != null && debugReader.isListening())
			{
				debugReader.handle(request);
				return null;
			}
			else
			{
				JSONObject arguments = request.get("arguments");
				JSONObject source = arguments.get("source");
				File file = Utils.pathToFile(source.get("path"));
				JSONArray breakpoints = arguments.get("breakpoints");
				
				return DAPWorkspaceManager.getInstance().setBreakpoints(request, file, breakpoints);
			}
		}
		catch (Exception e)
		{
			return new DAPMessageList(request, e);
		}
	}
	
	private DAPMessageList setExceptionBreakpoints(DAPRequest request) throws IOException
	{
		try
		{
			DAPWorkspaceManager manager = DAPWorkspaceManager.getInstance();
			DAPDebugReader debugReader = manager.getDebugReader();
			
			if (debugReader != null && debugReader.isListening())
			{
				debugReader.handle(request);
				return null;
			}
			else
			{
				JSONObject arguments = request.get("arguments");
				JSONArray filterOptions = arguments.get("filterOptions");
				return DAPWorkspaceManager.getInstance().setExceptionBreakpoints(request, filterOptions);
			}
		}
		catch (Exception e)
		{
			return new DAPMessageList(request, e);
		}
	}
}
