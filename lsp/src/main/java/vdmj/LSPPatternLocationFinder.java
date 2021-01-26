/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCPatternList;
import com.fujitsu.vdmj.tc.patterns.TCPatternListList;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;

/**
 * Lookup a LexLocation within a pattern, looking for record pattern matches.
 * TODO: This ought to be a PatternVisitor within the TCLeafXXXVisitor framework,
 * but the leaf visitors do not currently process patterns or binds.
 */
public class LSPPatternLocationFinder
{
	protected Set<TCNode> newCollection()
	{
		return new HashSet<TCNode>();
	}

	public Set<TCNode> patternCheck(TCPatternListList paramPatternList, LexLocation arg)
	{
		Set<TCNode> all = newCollection();

		for (TCPatternList list: paramPatternList)
		{
			all.addAll(patternCheck(list, arg));
		}
		
		return all;
	}

	public Set<TCNode> patternCheck(TCPatternList list, LexLocation arg)
	{
		Set<TCNode> all = newCollection();

		for (TCPattern p: list)
		{
			all.addAll(patternCheck(p, arg));
		}
		
		return all;
	}
	
	public Set<TCNode> patternCheck(TCPattern p, LexLocation arg)
	{
		Set<TCNode> all = newCollection();

		if (p instanceof TCRecordPattern)
		{
			TCRecordPattern r = (TCRecordPattern)p;
			
			if (arg.within(r.typename.getLocation()))
			{
				all.add(r.typename);
			}
		}

		return all;
	}
}
