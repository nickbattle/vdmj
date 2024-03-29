/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

import com.fujitsu.vdmj.in.INMappedList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;

public class INMultipleBindList extends INMappedList<TCMultipleBind, INMultipleBind>
{
	private static final long serialVersionUID = 1L;

	public INMultipleBindList()
	{
		super();
	}

	public INMultipleBindList(TCMultipleBindList from) throws Exception
	{
		super(from);
	}

	/**
	 * Note this method takes account of non-type binds, which in their nature
	 * have all values (explicitly). This is used in forall and exists expressions
	 * to set the INBindingGlobals "maybe" flag. 
	 */
	public boolean hasAllValues()
	{
		INBindingGlobals globals = INBindingGlobals.getInstance();
		
		if (globals.hasAllValues())
		{
			return true;
		}
		
		for (INMultipleBind bind: this)
		{
			if (bind instanceof INMultipleTypeBind)
			{
				return false;
			}
		}
		
		return true;	// Note, pure set binds have all values
	}
}
