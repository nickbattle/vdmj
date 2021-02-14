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
 *	SPDX-License-Identifier: GPL-3.0-or-later
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
import com.fujitsu.vdmj.in.patterns.INNilPattern;
import com.fujitsu.vdmj.in.patterns.INObjectPattern;
import com.fujitsu.vdmj.in.patterns.INPattern;
import com.fujitsu.vdmj.in.patterns.INQuotePattern;
import com.fujitsu.vdmj.in.patterns.INRealPattern;
import com.fujitsu.vdmj.in.patterns.INRecordPattern;
import com.fujitsu.vdmj.in.patterns.INSeqPattern;
import com.fujitsu.vdmj.in.patterns.INSetPattern;
import com.fujitsu.vdmj.in.patterns.INStringPattern;
import com.fujitsu.vdmj.in.patterns.INTuplePattern;
import com.fujitsu.vdmj.in.patterns.INUnionPattern;

/**
 * The base type for all INPattern visitors. All methods, by default, call
 * the abstract casePattern method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class INPatternVisitor<R, S>
{
 	abstract public R casePattern(INPattern node, S arg);

 	public R caseBooleanPattern(INBooleanPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseCharacterPattern(INCharacterPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseConcatenationPattern(INConcatenationPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseExpressionPattern(INExpressionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIdentifierPattern(INIdentifierPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIgnorePattern(INIgnorePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIntegerPattern(INIntegerPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapPattern(INMapPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapUnionPattern(INMapUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseNilPattern(INNilPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseObjectPattern(INObjectPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseQuotePattern(INQuotePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRealPattern(INRealPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRecordPattern(INRecordPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSeqPattern(INSeqPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSetPattern(INSetPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseStringPattern(INStringPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseTuplePattern(INTuplePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseUnionPattern(INUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}
}
