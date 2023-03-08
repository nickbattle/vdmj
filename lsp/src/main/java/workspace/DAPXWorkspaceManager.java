/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package workspace;

import dap.DAPMessageList;
import dap.DAPRequest;
import workspace.events.UnknownCommandEvent;

public class DAPXWorkspaceManager
{
	private static DAPXWorkspaceManager INSTANCE = null;
	private final EventHub eventhub;
	
	protected DAPXWorkspaceManager()
	{
		this.eventhub = EventHub.getInstance();
		Diag.info("Created DAPXWorkspaceManager");
	}

	public static synchronized DAPXWorkspaceManager getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new DAPXWorkspaceManager();		
		}

		return INSTANCE;
	}

	/**
	 * This is only used by unit testing.
	 */
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE = null;
		}
	}
	
	public DAPMessageList unhandledCommand(DAPRequest request)
	{
		DAPMessageList responses = eventhub.publish(new UnknownCommandEvent(request));
		
		if (responses.isEmpty())
		{
			Diag.error("No external plugin registered for unknownMethodEvent (%s)", request.getCommand());
			return new DAPMessageList(request, false, "Unknown DAP command: " + request.getCommand(), null);
		}
		else
		{
			return responses;
		}
	}
}
