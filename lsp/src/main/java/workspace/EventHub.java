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
import workspace.events.Event;
import workspace.events.LSPEvent;

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
	
	public synchronized void register(Class<? extends Event> eventClass, EventListener listener)
	{
		String key = eventClass.getName();
		List<EventListener> list = registrations.get(key);
		
		if (list == null)
		{
			list = new Vector<EventListener>();
			registrations.put(key, list);
		}
		
		list.add(listener);	// registration order
		Diag.config("Registered %s event handler for event %s", listener.getName(), key);
	}
	
	public List<EventListener> query(Event type)
	{
		return registrations.get(type.getKey());
	}
	
	public RPCMessageList publish(LSPEvent event)
	{
		List<EventListener> list = registrations.get(event.getKey());
		RPCMessageList responses = new RPCMessageList();

		if (list != null)
		{
			for (EventListener listener: list)
			{
				try
				{
					Diag.fine("Invoking %s event handler for %s", listener.getName(), event.getKey());
					RPCMessageList response = listener.handleEvent(event);
					
					if (response != null)
					{
						responses.addAll(response);
					}
				}
				catch (Exception e)
				{
					Diag.error(e);
					Diag.error("Error in s event handler for %s", listener.getName(), event.getKey());
				}
			}
		}
		else
		{
			Diag.fine("No plugins registered for %s", event.getKey());
		}
		
		return responses;
	}
	
	public DAPMessageList publish(DAPEvent event)
	{
		List<EventListener> list = registrations.get(event.getKey());
		DAPMessageList responses = new DAPMessageList();

		if (list != null)
		{
			for (EventListener listener: list)
			{
				try
				{
					Diag.fine("Invoking %s event handler for %s", listener.getName(), event.getKey());
					DAPMessageList response = listener.handleEvent(event);
					
					if (response != null)
					{
						responses.addAll(response);
					}
				}
				catch (Exception e)
				{
					Diag.error(e);
					Diag.error("Error in s event handler for %s", listener.getName(), event.getKey());
				}
			}
		}
		else
		{
			Diag.fine("No plugins registered for %s", event.getKey());
		}
		
		return responses;
	}
}
