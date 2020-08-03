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

import com.fujitsu.vdmj.tc.patterns.TCBooleanPattern;
import com.fujitsu.vdmj.tc.patterns.TCCharacterPattern;
import com.fujitsu.vdmj.tc.patterns.TCConcatenationPattern;
import com.fujitsu.vdmj.tc.patterns.TCExpressionPattern;
import com.fujitsu.vdmj.tc.patterns.TCIdentifierPattern;
import com.fujitsu.vdmj.tc.patterns.TCIgnorePattern;
import com.fujitsu.vdmj.tc.patterns.TCIntegerPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapUnionPattern;
import com.fujitsu.vdmj.tc.patterns.TCMapletPattern;
import com.fujitsu.vdmj.tc.patterns.TCNilPattern;
import com.fujitsu.vdmj.tc.patterns.TCObjectPattern;
import com.fujitsu.vdmj.tc.patterns.TCPattern;
import com.fujitsu.vdmj.tc.patterns.TCQuotePattern;
import com.fujitsu.vdmj.tc.patterns.TCRealPattern;
import com.fujitsu.vdmj.tc.patterns.TCRecordPattern;
import com.fujitsu.vdmj.tc.patterns.TCSeqPattern;
import com.fujitsu.vdmj.tc.patterns.TCSetPattern;
import com.fujitsu.vdmj.tc.patterns.TCStringPattern;
import com.fujitsu.vdmj.tc.patterns.TCTuplePattern;
import com.fujitsu.vdmj.tc.patterns.TCUnionPattern;
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

public class TCPossibleTypeVisitor extends TCPatternVisitor<TCType, Object>
{
	@Override
	public TCType casePattern(TCPattern node, Object arg)
	{
		throw new RuntimeException("Missing TCPossibleTypeVisitor method");
	}
	
	@Override
	public TCType caseBooleanPattern(TCBooleanPattern node, Object arg)
	{
		return new TCBooleanType(node.location);
	}

	@Override
	public TCType caseCharacterPattern(TCCharacterPattern node, Object arg)
	{
		return new TCCharacterType(node.location);
	}
	
	@Override
	public TCType caseConcatenationPattern(TCConcatenationPattern node, Object arg)
	{
		TCTypeSet all = new TCTypeSet();
		all.add(node.left.apply(this, arg));
		all.add(node.right.apply(this, arg));
		
		TCType type = all.getType(node.location);
		return type.isUnknown(node.location) ? 
				new TCSeqType(node.location, new TCUnknownType(node.location)) : type;
	}
	
	@Override
	public TCType caseExpressionPattern(TCExpressionPattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}

	@Override
	public TCType caseIdentifierPattern(TCIdentifierPattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIgnorePattern(TCIgnorePattern node, Object arg)
	{
		return new TCUnknownType(node.location);
	}
	
	@Override
	public TCType caseIntegerPattern(TCIntegerPattern node, Object arg)
	{
		return TCNumericType.typeOf(node.value.value, node.location);
	}
	
	@Override
	public TCType caseMapPattern(TCMapPattern node, Object arg)
	{
 		TCTypeSet types = new TCTypeSet();
 		
 		for (TCMapletPattern maplet: node.maplets)
 		{
 			types.add(new TCMapType(maplet.from.location,
 					maplet.from.apply(this, arg), maplet.to.apply(this, arg)));
 		}
 		
		return types.isEmpty() ? new TCMapType(node.location) : types.getType(node.location);
	}
	
	@Override
	public TCType caseMapUnionPattern(TCMapUnionPattern node, Object arg)
	{
		TCTypeSet all = new TCTypeSet();
		all.add(node.left.apply(this, arg));
		all.add(node.right.apply(this, arg));

		TCType s = all.getType(node.location);
		return s.isUnknown(node.location) ?
			new TCMapType(node.location, new TCUnknownType(node.location), new TCUnknownType(node.location)) : s;
	}
	
	@Override
	public TCType caseNilPattern(TCNilPattern node, Object arg)
	{
		return new TCOptionalType(node.location, new TCUnknownType(node.location));
	}
	
	@Override
	public TCType caseObjectPattern(TCObjectPattern node, Object arg)
	{
		return node.type;
	}
	
	@Override
	public TCType caseQuotePattern(TCQuotePattern node, Object arg)
	{
		return new TCQuoteType(node.location, node.value.value);
	}
	
	@Override
	public TCType caseRealPattern(TCRealPattern node, Object arg)
	{
		return new TCRealType(node.location);
	}
	
	@Override
	public TCType caseRecordPattern(TCRecordPattern node, Object arg)
	{
		return node.type;
	}
	
	@Override
	public TCType caseSeqPattern(TCSeqPattern node, Object arg)
	{
		return new TCSeqType(node.location, node.plist.getPossibleType(node.location));
	}
	
	@Override
	public TCType caseSetPattern(TCSetPattern node, Object arg)
	{
		return new TCSetType(node.location, node.plist.getPossibleType(node.location));
	}
	
	@Override
	public TCType caseStringPattern(TCStringPattern node, Object arg)
	{
		return new TCSeqType(node.location, new TCCharacterType(node.location));
	}
	
	@Override
	public TCType caseTuplePattern(TCTuplePattern node, Object arg)
	{
		TCTypeList all = new TCTypeList();
		
		for (TCPattern p: node.plist)
		{
			all.add(p.apply(this, arg));
		}
		
		return all.getType(node.location);
	}
	
	@Override
	public TCType caseUnionPattern(TCUnionPattern node, Object arg)
	{
		TCTypeSet all = new TCTypeSet();
		all.add(node.left.apply(this, arg));
		all.add(node.right.apply(this, arg));

		TCType s = all.getType(node.location);
		return s.isUnknown(node.location) ?
			new TCSetType(node.location, new TCUnknownType(node.location)) : s;
	}
}
