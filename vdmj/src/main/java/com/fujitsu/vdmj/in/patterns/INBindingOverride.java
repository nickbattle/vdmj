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

package com.fujitsu.vdmj.in.patterns;

import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.values.ValueList;

public class INBindingOverride
{
	private final String key;
	private final TCType type;
	private ValueList bindValues = null;

	public INBindingOverride(String key, TCType type)
	{
		this.key = key;
		this.type = type;
	}
	
	public String toString()
	{
		return key;
	}
	
	public boolean hasOverride()
	{
		return (bindValues != null);
	}
	
	public void setBindValues(ValueList values)
	{
		if (values == null)
		{
			bindValues = null;
		}
		else
		{
			bindValues = new ValueList();
			bindValues.addAll(values);
		}
	}
	
	public ValueList getBindValues()
	{
		return this.bindValues;
	}
	
	public TCType getType()
	{
		return type;
	}
}
