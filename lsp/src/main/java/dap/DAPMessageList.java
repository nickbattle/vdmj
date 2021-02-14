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

import java.util.Vector;

import dap.DAPRequest;
import dap.DAPResponse;
import json.JSONObject;

public class DAPMessageList extends Vector<JSONObject>
{
	private static final long serialVersionUID = 1L;

	public DAPMessageList()
	{
		super();
	}

	// Empty successful response to request
	public DAPMessageList(DAPRequest request)
	{
		add(new DAPResponse(request, true, null, null));
	}

	// Non-empty successful response to request
	public DAPMessageList(DAPRequest request, JSONObject result)
	{
		add(new DAPResponse(request, true, null, result));
	}

	// Specific response
	public DAPMessageList(DAPResponse result)
	{
		add(result);
	}
	
	// Exception error response to request
	public DAPMessageList(DAPRequest request, Exception exception)
	{
		add(new DAPResponse(request, false, exception.getMessage(), null));
	}

	// Specific response to request
	public DAPMessageList(DAPRequest request, boolean success, String message, Object result)
	{
		add(new DAPResponse(request, success, message, result));
	}
}
