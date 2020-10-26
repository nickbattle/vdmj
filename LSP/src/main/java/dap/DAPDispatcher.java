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
 *
 ******************************************************************************/

package dap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DAPDispatcher
{
	private Map<String, DAPHandler> handlers = new HashMap<String, DAPHandler>();
	
	public void register(DAPHandler handler, String... methods)
	{
		for (String method: methods)
		{
			handlers.put(method, handler);
		}
	}

	public DAPMessageList dispatch(DAPRequest request)
	{
		try
		{
			DAPHandler handler = handlers.get(request.get("command"));
			
			if (handler == null)
			{
				return new DAPMessageList(request, false, "Command not found", null);
			}
			else
			{
				return handler.run(request);
			}
		}
		catch (IOException e)
		{
			return new DAPMessageList(request, e);
		}
	}
}
