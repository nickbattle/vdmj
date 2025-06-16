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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.plugins;

import java.util.HashMap;
import java.util.Map;

/**
 * The root of all plugin events.
 */
abstract public class AnalysisEvent
{
	/**
	 * Properties can be attached to events to aid communication between plugins
	 * without anticipating any particular keys/values.
	 */
	protected Map<String, Object> properties = null;
	
	public String getKey()
	{
		return this.getClass().getName();
	}
	
	public Map<String, Object> getProperties()
	{
		if (properties == null)
		{
			properties = new HashMap<String, Object>();
		}
		
		return properties;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getProperty(String key)
	{
		return (T) getProperties().get(key);
	}
	
	public Object setProperty(String key, Object value)
	{
		return getProperties().put(key, value);
	}
}
