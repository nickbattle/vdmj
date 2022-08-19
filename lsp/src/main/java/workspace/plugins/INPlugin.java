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

package workspace.plugins;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

import rpc.RPCMessageList;
import workspace.Diag;
import workspace.EventListener;
import workspace.events.CheckFilesEvent;
import workspace.events.Event;

abstract public class INPlugin extends AnalysisPlugin implements EventListener
{
	public static INPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new INPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new INPluginPR();
				
			default:
				Diag.error("Unknown dialect " + dialect);
				throw new RuntimeException("Unsupported dialect: " + Settings.dialect);
		}
	}

	protected INPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "IN";
	}

	@Override
	public void init()
	{
		eventhub.register(this, "checkFilesEvent/prepare", this);
		eventhub.register(this, "checkFilesEvent/checked", this);
	}


	@Override
	public RPCMessageList handleEvent(Event event) throws Exception
	{
		if (event instanceof CheckFilesEvent)
		{
			CheckFilesEvent ev = (CheckFilesEvent)event;
			
			switch (ev.type)
			{
				case "checkFilesEvent/prepare":
					preCheck(ev);
					return new RPCMessageList();

				case "checkFilesEvent/checked":
					TCPlugin tc = registry.getPlugin("TC");
					checkLoadedFiles(tc.getTC());
					return new RPCMessageList();

				default:
					Diag.error("Unhandled %s CheckFilesEvent %s", getName(), event);
					return new RPCMessageList();
			}
		}
		else
		{
			Diag.error("Unhandled %s event %s", getName(), event);
			return new RPCMessageList();
		}
	}

	protected void preCheck(CheckFilesEvent ev)
	{
	}
	
	/**
	 * Event handling above. Supporting methods below. 
	 */
	
	abstract public <T extends Mappable> T getIN();
	
	abstract public <T extends Mappable> boolean checkLoadedFiles(T tcList) throws Exception;
	
	abstract public <T extends Mappable> Interpreter getInterpreter() throws Exception;

	abstract public INDefinitionList findDefinition(TCNameToken lnt);
}
