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

package dap.handlers;

import java.io.IOException;

import dap.DAPHandler;
import dap.DAPMessageList;
import dap.DAPRequest;
import json.JSONObject;
import lsp.CancellableThread;

public class SourceHandler extends DAPHandler
{
	public SourceHandler()
	{
		super();
	}
	
	@Override
	public DAPMessageList run(DAPRequest request) throws IOException
	{
		JSONObject arguments = request.get("arguments");
		JSONObject source = arguments.get("source");
		Long reference = source.get("sourceReference");
		String result = null;
		
		switch (reference.intValue())
		{
			case 0:		// Executing command
				result = CancellableThread.currentlyRunning();
				break;
				
			default:
				result = "Unknown source reference " + reference;
		}
		
		return new DAPMessageList(request, new JSONObject("content", result));
	}
}
