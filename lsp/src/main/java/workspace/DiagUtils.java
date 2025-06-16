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

package workspace;

import java.util.List;
import java.util.logging.Level;

import com.fujitsu.vdmj.messages.VDMMessage;

import json.JSONObject;

public class DiagUtils
{
	public static void dump(List<VDMMessage> messages)
	{
		if (Diag.isLoggable(Level.FINE))
		{
			for (VDMMessage m: messages)
			{
				Diag.fine("MSG: %s", m.toString());
			}
		}
	}
	
	public static void dumpEdit(JSONObject range, StringBuilder buffer)
	{
		if (Diag.isLoggable(Level.FINE))
		{
			JSONObject position = range.get("start");
			long line = position.get("line");
			long count = 0;
			int start = 0;
			
			while (count < line)
			{
				if (buffer.charAt(start++) == '\n')
				{
					count++;
				}
			}
			
			int end = start;
			while (end < buffer.length() && buffer.charAt(end) != '\n') end++;
			Diag.fine("EDITED %d: [%s]", line+1, buffer.substring(start, end));
		}
	}
}
