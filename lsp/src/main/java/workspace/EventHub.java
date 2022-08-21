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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import dap.DAPMessageList;
import rpc.RPCMessageList;
import workspace.events.DAPEvent;
import workspace.events.LSPEvent;
import workspace.plugins.AnalysisPlugin;

/**
 * A singleton to control the publication of events from the WorkspaceManagers to
 * plugins that are subscribed to those events.
 */
public class EventHub
{
	private static EventHub INSTANCE = null;
	
	private final Map<String, List<EventListener>> registrations;
	
	private EventHub()
	{
		this.registrations = new HashMap<String, List<EventListener>>();
	}
	
	public synchronized static EventHub getInstance()
	{
		if (INSTANCE == null)
		{
			INSTANCE = new EventHub();
		}
		
		return INSTANCE;
	}
	
	public static void reset()
	{
		if (INSTANCE != null)
		{
			INSTANCE.registrations.clear();
			INSTANCE = null;
		}
	}
	
	public synchronized void register(AnalysisPlugin plugin, String event, EventListener listener)
	{
		List<EventListener> list = registrations.get(event);
		
		if (list == null)
		{
			list = new Vector<EventListener>();
		}
		
		list.add(listener);	// registration order
		registrations.put(event, list);
		Diag.config("Registered %s event handler for event %s", plugin.getName(), event);
	}
	
	public List<EventListener> query(String type)
	{
		return registrations.get(type);
	}
	
	public RPCMessageList publish(LSPEvent event)
	{
		List<EventListener> list = registrations.get(event.type);
		RPCMessageList responses = new RPCMessageList();

		if (list != null)
		{
			for (EventListener listener: list)
			{
				try
				{
					Diag.fine("Invoking %s event handler for %s", listener.getName(), event.type);
					RPCMessageList response = listener.handleEvent(event);
					
					if (response == null)
					{
						throw new Exception("Handler returned null rather than empty response");
					}
					
					responses.addAll(response);
				}
				catch (Exception e)
				{
					Diag.error(e);
					Diag.error("Error in s event handler for %s", listener.getName(), event.type);
				}
	
				Diag.fine("Completed %s event handler for %s", listener.getName(), event.type);
			}
		}
		else
		{
			Diag.fine("No plugins registered for %s", event.type);
		}
		
		return responses;
	}
	
	public DAPMessageList publish(DAPEvent event)
	{
		List<EventListener> list = registrations.get(event.type);
		DAPMessageList responses = new DAPMessageList();

		if (list != null)
		{
			for (EventListener listener: list)
			{
				try
				{
					Diag.fine("Invoking %s event handler for %s", listener.getName(), event.type);
					DAPMessageList response = listener.handleEvent(event);
					
					if (response == null)
					{
						throw new Exception("Handler returned null rather than empty response");
					}
					
					responses.addAll(response);
				}
				catch (Exception e)
				{
					Diag.error(e);
					Diag.error("Error in s event handler for %s", listener.getName(), event.type);
				}
	
				Diag.fine("Completed %s event handler for %s", listener.getName(), event.type);
			}
		}
		else
		{
			Diag.fine("No plugins registered for %s", event.type);
		}
		
		return responses;
	}
}
