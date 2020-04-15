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
 *
 ******************************************************************************/

package vdmj;

import java.util.HashSet;
import java.util.Set;

import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.types.TCLeafTypeVisitor;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;

public class LSPTypeLocationFinder extends TCLeafTypeVisitor<TCNode, Set<TCNode>, LexLocation>
{
	@Override
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	@Override
	public Set<TCNode> caseUnresolvedType(TCUnresolvedType node, LexLocation arg)
	{
		Set<TCNode> result = newCollection();

		if (arg.within(node.location))
		{
			result.add(node);
		}

		return result;
	}

	@Override
	public Set<TCNode> caseNamedType(TCNamedType node, LexLocation arg)
	{
		return node.type.apply(this, arg);
	}
	
	@Override
	public Set<TCNode> caseRecordType(TCRecordType node, LexLocation arg)
	{
		Set<TCNode> result = newCollection();

		if (arg.within(node.name.getLocation()))
		{
			result.add(node);
		}

		return result;
	}

	@Override
	public Set<TCNode> caseType(TCType node, LexLocation arg)
	{
		return newCollection();
	}
}
