/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package com.fujitsu.vdmj.tc.types;

import com.fujitsu.vdmj.lex.LexLocation;

/**
 * A qualifier is used in TCUnionType matches to extract only those union members
 * that qualify by some test. Static methods are offered to construct the common
 * type qualifiers kinds. 
 */
abstract public class TCTypeQualifier
{
	abstract public boolean matches(TCType member);
	
	// Cache these for efficiency
	private static TCTypeQualifier setQualifier = null; 
	private static TCTypeQualifier seqQualifier = null; 
	private static TCTypeQualifier mapQualifier = null; 
	private static TCTypeQualifier numericQualifier = null; 
	private static TCTypeQualifier boolQualifier = null; 
	private static TCTypeQualifier anyQualifier = null; 
	
	public static TCTypeQualifier getSetQualifier()
	{
		if (setQualifier == null)
		{
			setQualifier = new TCTypeQualifier()
			{
				@Override
				public boolean matches(TCType member)
				{
					return member.isSet(LexLocation.ANY);
				}
			};
		}

		return setQualifier;
	}

	public static TCTypeQualifier getSeqQualifier()
	{
		if (seqQualifier == null)
		{
			seqQualifier =  new TCTypeQualifier()
			{
				@Override
				public boolean matches(TCType member)
				{
					return member.isSeq(LexLocation.ANY);
				}
			};
		}

		return seqQualifier;
	}

	public static TCTypeQualifier getMapQualifier()
	{
		if (mapQualifier == null)
		{
			mapQualifier =  new TCTypeQualifier()
			{
				@Override
				public boolean matches(TCType member)
				{
					return member.isMap(LexLocation.ANY);
				}
			};
		}

		return mapQualifier;
	}

	public static TCTypeQualifier getNumericQualifier()
	{
		if (numericQualifier == null)
		{
			numericQualifier =  new TCTypeQualifier()
			{
				@Override
				public boolean matches(TCType member)
				{
					return member.isNumeric(LexLocation.ANY);
				}
			};
		}

		return numericQualifier;
	}

	public static TCTypeQualifier getBoolQualifier()
	{
		if (boolQualifier == null)
		{
			boolQualifier =  new TCTypeQualifier()
			{
				@Override
				public boolean matches(TCType member)
				{
					return member.isType(TCBooleanType.class, LexLocation.ANY);
				}
			};
		}

		return boolQualifier;
	}

	public static TCTypeQualifier getAnyQualifier()
	{
		if (anyQualifier == null)
		{
			anyQualifier =  new TCTypeQualifier()
			{
				@Override
				public boolean matches(TCType member)
				{
					return true;	// Anything goes!
				}
			};
		}

		return anyQualifier;
	}
}
