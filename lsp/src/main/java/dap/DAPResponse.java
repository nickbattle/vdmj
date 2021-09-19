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

package dap;

import json.JSONObject;

public class DAPResponse extends JSONObject
{
	private static final long serialVersionUID = 1L;
	
	private static Long nextSequence = 0L;
	
	private void addSequence()
	{
		synchronized (nextSequence)
		{
			put("seq", ++nextSequence);
		}
	}
	
	public DAPResponse(DAPRequest request, boolean success, String message, Object body)
	{
		put("type", "response");
		addSequence();
		put("request_seq", request.get("seq"));
		put("command", request.getCommand());

		put("success", success);
		if (message != null) put("message", message);
		if (body != null) put("body", body);
	}
	
	public DAPResponse(String event, Object body)
	{
		put("type", "event");
		addSequence();

		put("event", event);
		if (body != null) put("body", body);
	}
}
