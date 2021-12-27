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

package lsp;

import rpc.RPCRequest;
import workspace.Diag;
import json.JSONObject;
import rpc.RPCMessageList;

public class SetTraceNotificationHandler extends LSPHandler
{
	public SetTraceNotificationHandler()
	{
		super();
	}

	@Override
	public RPCMessageList request(RPCRequest request)
	{
		JSONObject params = request.get("params");
		String value = params.get("value");
		
		if (Diag.isDebugServer())
		{
			Diag.warning("Ignoring SetTrace notification: %s", value);
		}
		else
		{
			switch (value)
			{
				case "off":
					Diag.setLevel("off");
					break;
					
				case "messages":
					Diag.setLevel("info");
					break;
					
				case "verbose":
					Diag.setLevel("finest");
					break;
					
				default:
					Diag.severe("Ignoring trace notification level '%s'", value);
					break;
			}
		}
		
		return null;
	}
}
