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

package plugins;

import dap.DAPMessageList;
import rpc.RPCMessageList;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;
import workspace.events.UnknownCommandEvent;
import workspace.events.UnknownMethodEvent;
import workspace.plugins.AnalysisPlugin;

public class AnotherPlugin extends AnalysisPlugin implements EventListener
{
	public AnotherPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "Another";
	}

	@Override
	public void init()
	{
		EventHub.getInstance().register(UnknownMethodEvent.class, this);
		EventHub.getInstance().register(UnknownCommandEvent.class, this);
	}

	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof UnknownMethodEvent &&
			event.request.getMethod().equals("slsp/another"))
		{
			return new RPCMessageList(event.request, "Handled LSP method");
		}
		else
		{
			return null;
		}
	}

	@Override
	public DAPMessageList handleEvent(DAPEvent event) throws Exception
	{
		if (event instanceof UnknownCommandEvent &&
			event.request.getCommand().equals("sdap/another"))
		{
			return new DAPMessageList(event.request, true, "Handled DAP command", null);
		}
		else
		{
			return null;
		}
	}
}