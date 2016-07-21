/*******************************************************************************
 *
 *	Copyright (C) 2008 Fujitsu Services Ltd.
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

package org.overturetool.vdmj.types;

import java.util.TreeSet;

import org.overturetool.vdmj.lex.LexLocation;
import org.overturetool.vdmj.util.Utils;


@SuppressWarnings("serial")
public class TypeSet extends TreeSet<Type>
{
	public TypeSet()
	{
		super();
	}

	public TypeSet(Type t)
	{
		add(t);
	}

	public TypeSet(Type t1, Type t2)
	{
		add(t1);
		add(t2);
	}

	@Override
	public boolean add(Type t)
	{
		if (t instanceof Seq1Type)
		{
			// If we add a Seq1Type, and there is already a SeqType in the set
			// we ignore the Seq1Type.
			
			Seq1Type s1t = (Seq1Type)t;
			SeqType st = new SeqType(s1t.location, s1t.seqof);
			
			if (contains(st))
			{
				return false;	// Was already there
			}
		}
		else if (t instanceof SeqType)
		{
			// If we add a SeqType, and there is already a Seq1Type in the set
			// we replace the Seq1Type.
			
			SeqType st = (SeqType)t;
			Seq1Type s1t = new Seq1Type(st.location, st.seqof);
			
			if (contains(s1t))
			{
				remove(s1t);	// Replace seq with seq1
			}
		}
		else if (t instanceof Set1Type)
		{
			// If we add a Set1Type, and there is already a SetType in the set
			// we ignore the Set1Type.
			
			Set1Type s1t = (Set1Type)t;
			SetType st = new SetType(s1t.location, s1t.setof);
			
			if (contains(st))
			{
				return false;	// Was already there
			}
		}
		else if (t instanceof SetType)
		{
			// If we add a SetType, and there is already a Set1Type in the set
			// we replace the Set1Type.
			
			SetType st = (SetType)t;
			SetType s1t = new Set1Type(st.location, st.setof);
			
			if (contains(s1t))
			{
				remove(s1t);	// Replace seq with set1
			}
		}
		else if (t instanceof NumericType)
		{
			for (Type x: this)
			{
				if (x instanceof NumericType)
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
		else if (t instanceof OptionalType)
		{
			OptionalType opt = (OptionalType)t;
			
			if (!opt.type.isUnknown(opt.type.location) && contains(opt.type))
			{
				remove(opt.type);	// Because T | [T] = [T]
			}
		}
		
		return super.add(t);
	}

	@Override
	public String toString()
	{
		return Utils.setToString(this, ", ");
	}

	public Type getType(LexLocation location)
	{
		// If there are any Optional(Unknowns) these are the result of
		// nil values, which set the overall type as optional. Other
		// optional types stay.

		boolean optional = false;
		assert this.size() > 0 : "Getting type of empty TypeSet";
		Type result = null;

		if (this.size() == 1)
		{
			result = iterator().next();
		}
		else
		{
			result = new UnionType(location, this);
		}

		return (optional ? new OptionalType(location, result) : result);
	}

	public TypeList getComposeTypes()
	{
		TypeList list = new TypeList();
		
		for (Type type: this)
		{
			list.addAll(type.getComposeTypes());
		}
		
		return list;
	}
}
