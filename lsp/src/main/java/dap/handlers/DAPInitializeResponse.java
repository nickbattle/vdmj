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

import dap.DAPRequest;
import dap.DAPResponse;
import json.JSONArray;
import json.JSONObject;

public class DAPInitializeResponse extends DAPResponse
{
	private static final long serialVersionUID = 1L;
	
	public DAPInitializeResponse(DAPRequest request)
	{
		super(request, true, null, getServerCapabilities());
	}

	private static JSONObject getServerCapabilities()
	{
		JSONObject cap = new JSONObject();
		cap.put("supportsConfigurationDoneRequest", true);
		cap.put("supportsTerminateRequest", true);
		cap.put("supportsCancelRequest", false);

		cap.put("supportsConditionalBreakpoints", true);
		cap.put("supportsHitConditionalBreakpoints", true);
		cap.put("supportsLogPoints", true);
		
		cap.put("supportsExceptionFilterOptions", true);
		cap.put("exceptionBreakpointFilters",
				new JSONArray(
					new JSONObject(
						"filter",				"VDM Exceptions",
						"label",				"VDM Exceptions",
						"description",			"Catch VDM exit statements",
						"supportsCondition",	true,
						"conditionDescription",	"The exit value to catch")));
		
		return cap;
	}
}
