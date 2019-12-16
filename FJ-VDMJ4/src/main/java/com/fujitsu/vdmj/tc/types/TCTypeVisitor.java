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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types;

/**
 * The base type for all TCType visitors. All methods, by default, call
 * the abstract caseType method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class TCTypeVisitor<R, S>
{
	abstract public R caseType(TCType node, S arg);

	public R caseBasicType(TCBasicType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseBooleanType(TCBooleanType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseBracketType(TCBracketType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseCharacterType(TCCharacterType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseClassType(TCClassType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseFunctionType(TCFunctionType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseInMapType(TCInMapType node, S arg)
	{
		return caseMapType(node, arg);
	}

	public R caseIntegerType(TCIntegerType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseNumericType(TCInvariantType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseMapType(TCMapType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseNamedType(TCNamedType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseNaturalOneType(TCNaturalOneType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseNaturalType(TCNaturalType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseNumericType(TCNumericType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseOperationType(TCOperationType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseOptionalType(TCOptionalType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseParameterType(TCParameterType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseProductType(TCProductType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseQuoteType(TCQuoteType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseRationalType(TCRationalType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseRealType(TCRealType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseRecordType(TCRecordType node, S arg)
	{
		return caseNumericType(node, arg);
	}

	public R caseSeq1Type(TCSeq1Type node, S arg)
	{
		return caseSeqType(node, arg);
	}

	public R caseSeqType(TCSeqType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseSet1Type(TCSet1Type node, S arg)
	{
		return caseSetType(node, arg);
	}

	public R caseSetType(TCSetType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseTokenType(TCTokenType node, S arg)
	{
		return caseBasicType(node, arg);
	}

	public R caseUndefinedType(TCUndefinedType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseUnionType(TCUnionType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseUnknownType(TCUnknownType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseUnresolvedType(TCUnresolvedType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseVoidReturnType(TCVoidReturnType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseVoidType(TCVoidType node, S arg)
	{
		return caseType(node, arg);
	}
}
