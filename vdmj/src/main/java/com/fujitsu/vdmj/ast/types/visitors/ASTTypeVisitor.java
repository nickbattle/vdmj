/*******************************************************************************
 *
 *	Copyright (c) 2019 Nick Battle.
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

package com.fujitsu.vdmj.ast.types.visitors;

import com.fujitsu.vdmj.ast.types.ASTBasicType;
import com.fujitsu.vdmj.ast.types.ASTBooleanType;
import com.fujitsu.vdmj.ast.types.ASTBracketType;
import com.fujitsu.vdmj.ast.types.ASTCharacterType;
import com.fujitsu.vdmj.ast.types.ASTClassType;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTInMapType;
import com.fujitsu.vdmj.ast.types.ASTIntegerType;
import com.fujitsu.vdmj.ast.types.ASTInvariantType;
import com.fujitsu.vdmj.ast.types.ASTMapType;
import com.fujitsu.vdmj.ast.types.ASTNamedType;
import com.fujitsu.vdmj.ast.types.ASTNaturalOneType;
import com.fujitsu.vdmj.ast.types.ASTNaturalType;
import com.fujitsu.vdmj.ast.types.ASTNumericType;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.ast.types.ASTOptionalType;
import com.fujitsu.vdmj.ast.types.ASTParameterType;
import com.fujitsu.vdmj.ast.types.ASTProductType;
import com.fujitsu.vdmj.ast.types.ASTQuoteType;
import com.fujitsu.vdmj.ast.types.ASTRationalType;
import com.fujitsu.vdmj.ast.types.ASTRealType;
import com.fujitsu.vdmj.ast.types.ASTRecordType;
import com.fujitsu.vdmj.ast.types.ASTSeq1Type;
import com.fujitsu.vdmj.ast.types.ASTSeqType;
import com.fujitsu.vdmj.ast.types.ASTSet1Type;
import com.fujitsu.vdmj.ast.types.ASTSetType;
import com.fujitsu.vdmj.ast.types.ASTTokenType;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTUndefinedType;
import com.fujitsu.vdmj.ast.types.ASTUnionType;
import com.fujitsu.vdmj.ast.types.ASTUnknownType;
import com.fujitsu.vdmj.ast.types.ASTUnresolvedType;
import com.fujitsu.vdmj.ast.types.ASTVoidReturnType;
import com.fujitsu.vdmj.ast.types.ASTVoidType;

/**
 * The base type for all ASTType visitors. All methods, by default, call
 * the abstract caseType method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTTypeVisitor<R, S>
{
	abstract public R caseType(ASTType node, S arg);

	public R caseBasicType(ASTBasicType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseBooleanType(ASTBooleanType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseBracketType(ASTBracketType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseCharacterType(ASTCharacterType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseClassType(ASTClassType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseFunctionType(ASTFunctionType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseInMapType(ASTInMapType node, S arg)
	{
		return caseMapType(node, arg);
	}

	public R caseIntegerType(ASTIntegerType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseInvariantType(ASTInvariantType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseNumericType(ASTInvariantType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseMapType(ASTMapType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseNamedType(ASTNamedType node, S arg)
	{
		return caseInvariantType(node, arg);
	}

	public R caseNaturalOneType(ASTNaturalOneType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseNaturalType(ASTNaturalType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseNumericType(ASTNumericType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseOperationType(ASTOperationType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseOptionalType(ASTOptionalType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseParameterType(ASTParameterType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseProductType(ASTProductType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseQuoteType(ASTQuoteType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseRationalType(ASTRationalType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseRealType(ASTRealType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseRecordType(ASTRecordType node, S arg)
	{
		return caseInvariantType(node, arg);
	}

	public R caseSeq1Type(ASTSeq1Type node, S arg)
	{
		return caseSeqType(node, arg);
	}

	public R caseSeqType(ASTSeqType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseSet1Type(ASTSet1Type node, S arg)
	{
		return caseSetType(node, arg);
	}

	public R caseSetType(ASTSetType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseTokenType(ASTTokenType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseUndefinedType(ASTUndefinedType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseUnionType(ASTUnionType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseUnknownType(ASTUnknownType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseUnresolvedType(ASTUnresolvedType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseVoidReturnType(ASTVoidReturnType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseVoidType(ASTVoidType node, S arg)
	{
		return caseType(node, arg);
	}
}
