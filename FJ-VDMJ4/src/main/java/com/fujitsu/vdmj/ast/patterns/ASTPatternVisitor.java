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

package com.fujitsu.vdmj.ast.patterns;

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
