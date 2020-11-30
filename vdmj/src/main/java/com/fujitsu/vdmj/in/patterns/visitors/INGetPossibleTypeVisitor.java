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

package com.fujitsu.vdmj.in.patterns.visitors;

import com.fujitsu.vdmj.in.patterns.INBooleanPattern;
import com.fujitsu.vdmj.in.patterns.INCharacterPattern;
import com.fujitsu.vdmj.in.patterns.INConcatenationPattern;
import com.fujitsu.vdmj.in.patterns.INExpressionPattern;
import com.fujitsu.vdmj.in.patterns.INIdentifierPattern;
import com.fujitsu.vdmj.in.patterns.INIgnorePattern;
import com.fujitsu.vdmj.in.patterns.INIntegerPattern;
import com.fujitsu.vdmj.in.patterns.INMapPattern;
import com.fujitsu.vdmj.in.patterns.INMapUnionPattern;
import com.fujitsu.vdmj.in.patterns.INMapletPattern;
import com.fujitsu.vdmj.in.patterns.INNilPattern;
import com.fujitsu.vdmj.in.patterns.INObjectPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INPatternList;
import com.fujitsu.vdmj.in.patterns.INQuotePattern;
import com.fujitsu.vdmj.in.patterns.INRealPattern;
import com.fujitsu.vdmj.in.patterns.INRecordPattern;
import com.fujitsu.vdmj.in.patterns.INSeqPattern;
import com.fujitsu.vdmj.in.patterns.INSetPattern;
import com.fujitsu.vdmj.in.patterns.INStringPattern;
import com.fujitsu.vdmj.in.patterns.INTuplePattern;
import com.fujitsu.vdmj.in.patterns.INUnionPattern;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.tc.types.TCUnknownType;

public class INGetPossibleTypeVisitor extends INPatternVisitor<TCType, Object>
{
	private TCType apply(INPatternList list, LexLocation location)
	{
		switch (list.size())
		{
			case 0:
				return new TCUnknownType(location);

			case 1:
				return list.get(0).apply(this, null);

			default:
        		TCTypeSet set = new TCTypeSet();

        		for (INPattern p: list)
        		{
        			set.add(p.apply(this, null));
        		}

        		return set.getType(location);		// NB. a union of types
		}
	}
	
	@Override
	public TCType casePattern(INPattern node, Object arg)
	{
		throw new RuntimeException("Missing INGetPossibleTypeVisitor method!");
	}

	@Override
	public TCType caseBooleanPattern(INBooleanPattern node, Object arg)
	{
		return new TCBooleanType(node.location);
	}

	@Override
	public TCType caseCharacterPattern(INCharacterPattern node, Object arg)
	{
		return new TCCharacterType(node.location);
	}
	
	@Override
	public TCType caseConcatenationPattern(INConcatenationPattern node, Object arg)
	{
		INPatternList plist = new INPatternList();
		plist.add(node.left);
		plist.add(node.right);
		
		TCType type = apply(plist, node.location);
		
		return type.isUnknown(node.location) ? 
			new TCSeqType(node.location, new TCUnknownType(node.location)) : type;
	}
	
	@Override
	public TCType caseExpressionPattern(INExpressionPattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIdentifierPattern(INIdentifierPattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIgnorePattern(INIgnorePattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIntegerPattern(INIntegerPattern node, Object arg)
	{
		return TCNumericType.typeOf(node.value.value, node.location);
	}
	
	@Override
	public TCType caseMapPattern(INMapPattern node, Object arg)
	{
		TCTypeSet types = new TCTypeSet();
		
		for (INMapletPattern p: node.maplets)
		{
			TCType m = new TCMapType(p.location, p.from.apply(this, arg), p.to.apply(this, arg));
			types.add(m);
		}
		
		return types.isEmpty() ? new TCMapType(node.location) : types.getType(node.location);
	}
	
	@Override
	public TCType caseMapUnionPattern(INMapUnionPattern node, Object arg)
	{
		TCTypeSet list = new TCTypeSet();

		list.add(node.left.apply(this, arg));
		list.add(node.right.apply(this, arg));

		TCType s = list.getType(node.location);

		return s.isUnknown(node.location) ?
			new TCMapType(node.location, new TCUnknownType(node.location), new TCUnknownType(node.location)) : s;
	}
	
	@Override
	public TCType caseNilPattern(INNilPattern node, Object arg)
	{
		return new TCOptionalType(node.location, new TCUnknownType(node.location));
	}
	
	@Override
	public TCType caseObjectPattern(INObjectPattern node, Object arg)
	{
		return node.type;
	}
	
	@Override
	public TCType caseQuotePattern(INQuotePattern node, Object arg)
	{
		return new TCQuoteType(node.location, node.value.value);
	}
	
	@Override
	public TCType caseRealPattern(INRealPattern node, Object arg)
	{
		return new TCRealType(node.location);
	}
	
	@Override
	public TCType caseRecordPattern(INRecordPattern node, Object arg)
	{
		return node.type;
	}
	
	@Override
	public TCType caseSeqPattern(INSeqPattern node, Object arg)
	{
		return new TCSeqType(node.location, apply(node.plist, node.location));
	}
	
	@Override
	public TCType caseSetPattern(INSetPattern node, Object arg)
	{
		return new TCSetType(node.location, apply(node.plist, node.location));
	}
	
	@Override
	public TCType caseStringPattern(INStringPattern node, Object arg)
	{
		return new TCSeqType(node.location, new TCCharacterType(node.location));
	}
	
	@Override
	public TCType caseTuplePattern(INTuplePattern node, Object arg)
	{
		TCTypeList list = new TCTypeList();

		for (INPattern p: node.plist)
		{
			list.add(p.apply(this, arg));
		}

		return list.getType(node.location);
	}
	
	@Override
	public TCType caseUnionPattern(INUnionPattern node, Object arg)
	{
		TCTypeSet list = new TCTypeSet();

		list.add(node.left.apply(this, arg));
		list.add(node.right.apply(this, arg));

		TCType s = list.getType(node.location);

		return s.isUnknown(node.location) ?
			new TCSetType(node.location, new TCUnknownType(node.location)) : s;
	}
}
