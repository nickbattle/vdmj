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

import java.io.IOException;

import dap.DAPHandler;
import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONArray;
import json.JSONObject;
import vdmj.DAPDebugReader;
import workspace.DAPWorkspaceManager;
import workspace.Log;

public class ThreadsHandler extends DAPHandler
{
	public ThreadsHandler()
	{
		super();
	}
	
	@Override
	public DAPMessageList run(DAPRequest request) throws IOException
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
			/**
			 * Why is the client asking for a threads list, when we're not stopped?
			 * If we send an empty list, the client cannot "pause" anything. So we
			 * try to send back an arbitrary dummy thread. We don't care about the
			 * thread in the pause request, so this is fine.
			 * 
			 * Was: return manager.threads(request);
			 */
			Log.printf("Received threads request while not stopped");
			JSONArray list = new JSONArray(new JSONObject("id", 0L, "name", "dummy"));
			return new DAPMessageList(request, new JSONObject("threads", list));
		}
	}
}
