/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.plugins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.plugins.events.Event;

/**
 * A singleton to control the publication of events from the command reader to
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
	}
	
	public List<EventListener> query(Event type)
	{
		return registrations.get(type.getKey());
	}
	
	public <T> List<T> publish(Event event) throws Exception
	{
		List<EventListener> list = registrations.get(event.getKey());
		List<T> responses = new Vector<T>();

		if (list != null)
		{
			for (EventListener listener: list)
			{
				T response = listener.handleEvent(event);
				
				if (response != null)
				{
					responses.add(response);
				}
			}
		}
		
		return responses;
	}
}
