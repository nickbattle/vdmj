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

import dap.DAPMessageList;
import rpc.RPCMessageList;
import workspace.Diag;
import workspace.EventHub;
import workspace.EventListener;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;
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
	public void init()
	{
		EventHub eventhub = EventHub.getInstance();
		eventhub.register("initialize", this);
		eventhub.register("initialized", this);
		eventhub.register("textDocument/didOpen", this);
		eventhub.register("textDocument/didChange", this);
		eventhub.register("textDocument/didClose", this);
		eventhub.register("textDocument/didSave", this);
		eventhub.register("checkFilesEvent/prepare", this);
		eventhub.register("checkFilesEvent/syntax", this);
		eventhub.register("checkFilesEvent/typecheck", this);
		eventhub.register("checkFilesEvent/checked", this);
		eventhub.register("unknownMethodEvent", this);
		eventhub.register("shutdown", this);

		eventhub.register("dap:initialize", this);
		eventhub.register("dap:launch", this);
		eventhub.register("dap:configurationDone", this);
		eventhub.register("dap:evaluate", this);
		eventhub.register("dap:disconnect", this);
		eventhub.register("dap:terminate", this);

		eventhub.register("dap:unknownCommandEvent", this);
	}

	@Override
	abstract public String getName();

	@Override
	abstract public RPCMessageList handleEvent(LSPEvent event) throws Exception;
	
	@Override
	abstract public DAPMessageList handleEvent(DAPEvent event) throws Exception;
}