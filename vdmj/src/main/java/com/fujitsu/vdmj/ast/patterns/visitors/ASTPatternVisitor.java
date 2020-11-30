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

package com.fujitsu.vdmj.ast.patterns.visitors;

import com.fujitsu.vdmj.ast.patterns.ASTBooleanPattern;
import com.fujitsu.vdmj.ast.patterns.ASTCharacterPattern;
import com.fujitsu.vdmj.ast.patterns.ASTConcatenationPattern;
import com.fujitsu.vdmj.ast.patterns.ASTExpressionPattern;
import com.fujitsu.vdmj.ast.patterns.ASTIdentifierPattern;
import com.fujitsu.vdmj.ast.patterns.ASTIgnorePattern;
import com.fujitsu.vdmj.ast.patterns.ASTIntegerPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapPattern;
import com.fujitsu.vdmj.ast.patterns.ASTMapUnionPattern;
import com.fujitsu.vdmj.ast.patterns.ASTNilPattern;
import com.fujitsu.vdmj.ast.patterns.ASTObjectPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTQuotePattern;
import com.fujitsu.vdmj.ast.patterns.ASTRealPattern;
import com.fujitsu.vdmj.ast.patterns.ASTRecordPattern;
import com.fujitsu.vdmj.ast.patterns.ASTSeqPattern;
import com.fujitsu.vdmj.ast.patterns.ASTSetPattern;
import com.fujitsu.vdmj.ast.patterns.ASTStringPattern;
import com.fujitsu.vdmj.ast.patterns.ASTTuplePattern;
import com.fujitsu.vdmj.ast.patterns.ASTUnionPattern;

/**
 * The base type for all TCPattern visitors. All methods, by default, call
 * the abstract casePattern method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTPatternVisitor<R, S>
{
 	abstract public R casePattern(ASTPattern node, S arg);

 	public R caseBooleanPattern(ASTBooleanPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseCharacterPattern(ASTCharacterPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseConcatenationPattern(ASTConcatenationPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseExpressionPattern(ASTExpressionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIdentifierPattern(ASTIdentifierPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIgnorePattern(ASTIgnorePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseIntegerPattern(ASTIntegerPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapPattern(ASTMapPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseMapUnionPattern(ASTMapUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseNilPattern(ASTNilPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseObjectPattern(ASTObjectPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseQuotePattern(ASTQuotePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRealPattern(ASTRealPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseRecordPattern(ASTRecordPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSeqPattern(ASTSeqPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseSetPattern(ASTSetPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseStringPattern(ASTStringPattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseTuplePattern(ASTTuplePattern node, S arg)
	{
		return casePattern(node, arg);
	}

 	public R caseUnionPattern(ASTUnionPattern node, S arg)
	{
		return casePattern(node, arg);
	}
}
