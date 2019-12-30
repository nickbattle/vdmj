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

import com.fujitsu.vdmj.po.POMappedMap;
import com.fujitsu.vdmj.po.definitions.PODefinitionListList;
import com.fujitsu.vdmj.tc.TCRecursiveMap;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionListList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;

public class PORecursiveMap extends POMappedMap<TCNameToken, TCDefinitionListList, TCNameToken, PODefinitionListList>
{
	private static final long serialVersionUID = 1L;
	
	public PORecursiveMap(TCRecursiveMap from) throws Exception
	{
		super(from);
	}
}
