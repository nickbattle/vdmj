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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types;

import java.util.TreeSet;

import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeSet;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.util.Utils;


@SuppressWarnings("serial")
public class TCTypeSet extends TreeSet<TCType> implements Mappable
{
	// This ought to be a MappedSet
	public TCTypeSet(ASTTypeSet from) throws Exception
	{
		ClassMapper mapper = ClassMapper.getInstance(TCNode.MAPPINGS);
		
		for (ASTType type: from)
		{
			add((TCType)mapper.convert(type));
		}
	}
	
	public TCTypeSet()
	{
		super();
	}

	public TCTypeSet(TCType t)
	{
		add(t);
	}

	public TCTypeSet(TCType t1, TCType t2)
	{
		add(t1);
		add(t2);
	}

	public boolean has(Object other)
	{
		if (other instanceof TCType)
		{
			for (TCType t: this)
			{
				if (t.getClass().equals(other.getClass()) && t.equals(other))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public boolean add(TCType t)
	{
		if (t instanceof TCSeq1Type)
		{
			// If we add a TCSeq1Type, and there is already a TCSeqType in the set
			// we ignore the TCSeq1Type.
			
			TCSeq1Type s1t = (TCSeq1Type)t;
			TCSeqType st = new TCSeqType(s1t.location, s1t.seqof);
			
			if (contains(st))
			{
				return false;	// Was already there
			}
		}
		else if (t instanceof TCSeqType)
		{
			// If we add a TCSeqType, and there is already a TCSeq1Type in the set
			// we replace the TCSeq1Type.
			
			TCSeqType st = (TCSeqType)t;
			TCSeq1Type s1t = new TCSeq1Type(st.location, st.seqof);
			
			if (contains(s1t))
			{
				remove(s1t);	// Replace seq with seq1
			}
		}
		else if (t instanceof TCSet1Type)
		{
			// If we add a TCSet1Type, and there is already a TCSetType in the set
			// we ignore the TCSet1Type.
			
			TCSet1Type s1t = (TCSet1Type)t;
			TCSetType st = new TCSetType(s1t.location, s1t.setof);
			
			if (contains(st))
			{
				return false;	// Was already there
			}
		}
		else if (t instanceof TCSetType)
		{
			// If we add a TCSetType, and there is already a TCSet1Type in the set
			// we replace the TCSet1Type.
			
			TCSetType st = (TCSetType)t;
			TCSetType s1t = new TCSet1Type(st.location, st.setof);
			
			if (contains(s1t))
			{
				remove(s1t);	// Replace seq with set1
			}
		}
		else if (t instanceof TCNumericType)
		{
			for (TCType x: this)
			{
				if (x instanceof TCNumericType)
				{
					if (x.getNumeric().getWeight() < t.getNumeric().getWeight())
					{
						remove(x);
						break;
					}
					else
					{
						return false;	// Was already there
					}
				}
			}
		}
		else if (t instanceof TCOptionalType)
		{
			TCOptionalType opt = (TCOptionalType)t;
			
			if (!opt.type.isUnknown(opt.type.location) && contains(opt.type))
			{
				remove(opt.type);	// Because T | [T] = [T]
			}
		}
		else if (t instanceof TCTokenType)
		{
			TCTokenType tt = (TCTokenType)t;

			for (TCType x: this)
			{
				if (x instanceof TCTokenType)
				{
					TCTokenType xt = (TCTokenType)x;
					xt.argtypes.addAll(tt.argtypes);
					return false;	// Was already there
				}
			}
		}
		
		return super.add(t);
	}

	@Override
	public String toString()
	{
		return Utils.setToString(this, ", ");
	}

	public TCType getType(LexLocation location)
	{
		// If there are any Optional(Unknowns) these are the result of
		// nil values, which set the overall type as optional. Other
		// optional types stay.

		boolean optional = false;
		assert this.size() > 0 : "Getting type of empty TypeSet";
		TCType result = null;

		if (this.size() == 1)
		{
			result = iterator().next();
		}
		else
		{
			result = new TCUnionType(location, this);
		}

		return (optional ? new TCOptionalType(location, result) : result);
	}

	public TCTypeList getComposeTypes()
	{
		TCTypeList list = new TCTypeList();
		
		for (TCType type: this)
		{
			list.addAll(type.getComposeTypes());
		}
		
		return list;
	}
}
