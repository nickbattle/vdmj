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

/**
 * The base type for all TCPattern visitors. All methods, by default, call
 * the abstract casePattern method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class TCPatternVisitor<R, S>
{
 	abstract public R casePattern(TCPattern node, S arg);

 	public R caseBooleanPattern(TCBooleanPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseCharacterPattern(TCCharacterPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseConcatenationPattern(TCConcatenationPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseExpressionPattern(TCExpressionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIdentifierPattern(TCIdentifierPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIgnorePattern(TCIgnorePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIntegerPattern(TCIntegerPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapPattern(TCMapPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapletPattern(TCMapletPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapUnionPattern(TCMapUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseNilPattern(TCNilPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseObjectPattern(TCObjectPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseQuotePattern(TCQuotePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRealPattern(TCRealPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRecordPattern(TCRecordPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSeqPattern(TCSeqPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSetPattern(TCSetPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseStringPattern(TCStringPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseTuplePattern(TCTuplePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseUnionPattern(TCUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}
}
