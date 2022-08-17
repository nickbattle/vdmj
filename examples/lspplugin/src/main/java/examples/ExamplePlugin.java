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

package examples;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.lex.Dialect;

import rpc.RPCMessageList;
import workspace.Diag;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.Event;
import workspace.plugins.AnalysisPlugin;

abstract public class ExamplePlugin extends AnalysisPlugin implements EventListener
{
	public static ExamplePlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new ExamplePluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new ExamplePluginPR();
				
			default:
				Diag.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}

	public ExamplePlugin()
	{
		// Not used, because of the factory method above.
	}
	
	@Override
	public String getName()
	{
		return "ExamplePlugin";
	}

	@Override
	public void init()
	{
		EventHub eventhub = EventHub.getInstance();
		eventhub.register(this, "initialize", this);
		eventhub.register(this, "initialized", this);
		eventhub.register(this, "textDocument/didChange", this);
	}
	
	@Override
	abstract public RPCMessageList handleEvent(Event event) throws Exception;
}