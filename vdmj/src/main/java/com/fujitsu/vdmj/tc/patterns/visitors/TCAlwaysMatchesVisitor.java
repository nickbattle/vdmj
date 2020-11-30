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

package com.fujitsu.vdmj.tc.patterns.visitors;

import com.fujitsu.vdmj.tc.patterns.TCConcatenationPattern;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCIgnorePattern;
import com.fujitsu.vdmj.tc.patterns.TCNamePatternPair;
import com.fujitsu.vdmj.tc.patterns.TCObjectPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.patterns.TCUnionPattern;

public class TCAlwaysMatchesVisitor extends TCPatternVisitor<Boolean, Object>
{
	@Override
	public Boolean casePattern(TCPattern node, Object arg)
	{
		return false;	// Default
	}

	@Override
	public Boolean caseConcatenationPattern(TCConcatenationPattern node, Object arg)
	{
		return node.left.apply(this, arg) && node.right.apply(this, arg);
	}
	
	@Override
	public Boolean caseIdentifierPattern(TCIdentifierPattern node, Object arg)
	{
		return true;
	}
	
	@Override
	public Boolean caseIgnorePattern(TCIgnorePattern node, Object arg)
	{
		return true;
	}
	
	@Override
	public Boolean caseObjectPattern(TCObjectPattern node, Object arg)
	{
		for (TCNamePatternPair p: node.fieldlist)
		{
			if (!p.pattern.apply(this, arg))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public Boolean caseRecordPattern(TCRecordPattern node, Object arg)
	{
		for (TCPattern p: node.plist)
		{
			if (!p.apply(this, arg))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public Boolean caseTuplePattern(TCTuplePattern node, Object arg)
	{
		for (TCPattern p: node.plist)
		{
			if (!p.apply(this, arg))
			{
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public Boolean caseUnionPattern(TCUnionPattern node, Object arg)
	{
		return node.left.apply(this, arg) && node.right.apply(this, arg);
	}
}
