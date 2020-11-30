/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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
 *
 ******************************************************************************/
package com.fujitsu.vdmj.po;

import com.fujitsu.vdmj.po.definitions.PODefinitionList;
import com.fujitsu.vdmj.po.definitions.PODefinitionListList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

/**
 * A class to hold static data shared by VDM-SL and VDM++/RT.
 */
public class PORecursiveLoops extends POMappedMap<TCNameToken, TCDefinitionList, TCNameToken, PODefinitionList>
{
	private static final long serialVersionUID = 1L;
	private static PORecursiveLoops INSTANCE = null;
	private PORecursiveMap recursiveLoops = null;
	
	public PORecursiveLoops(PORecursiveMap recursiveLoops)
	{
		this.recursiveLoops = recursiveLoops;
		INSTANCE = this;
	}

	public static PORecursiveLoops getInstance()
	{
		return INSTANCE;
	}

	public PODefinitionListList get(TCNameToken name)
	{
		return recursiveLoops.get(name);
	}
}
