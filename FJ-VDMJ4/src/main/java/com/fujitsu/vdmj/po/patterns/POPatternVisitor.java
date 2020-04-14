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

package com.fujitsu.vdmj.po.patterns;

/**
 * The base type for all POPattern visitors. All methods, by default, call
 * the abstract casePattern method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class POPatternVisitor<R, S>
{
 	abstract public R casePattern(POPattern node, S arg);

 	public R caseBooleanPattern(POBooleanPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseCharacterPattern(POCharacterPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseConcatenationPattern(POConcatenationPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseExpressionPattern(POExpressionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIdentifierPattern(POIdentifierPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIgnorePattern(POIgnorePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIntegerPattern(POIntegerPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapPattern(POMapPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapUnionPattern(POMapUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseNilPattern(PONilPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseObjectPattern(POObjectPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseQuotePattern(POQuotePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRealPattern(PORealPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRecordPattern(PORecordPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSeqPattern(POSeqPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSetPattern(POSetPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseStringPattern(POStringPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseTuplePattern(POTuplePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseUnionPattern(POUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}
}
