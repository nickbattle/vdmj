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
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package com.fujitsu.vdmj.tc.types.visitors;

import com.fujitsu.vdmj.tc.types.TCBasicType;
import com.fujitsu.vdmj.tc.types.TCBooleanType;
import com.fujitsu.vdmj.tc.types.TCBracketType;
import com.fujitsu.vdmj.tc.types.TCCharacterType;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCInMapType;
import com.fujitsu.vdmj.tc.types.TCIntegerType;
import com.fujitsu.vdmj.tc.types.TCInvariantType;
import com.fujitsu.vdmj.tc.types.TCMapType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCNaturalOneType;
import com.fujitsu.vdmj.tc.types.TCNaturalType;
import com.fujitsu.vdmj.tc.types.TCNumericType;
import com.fujitsu.vdmj.tc.types.TCOperationType;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCProductType;
import com.fujitsu.vdmj.tc.types.TCQuoteType;
import com.fujitsu.vdmj.tc.types.TCRationalType;
import com.fujitsu.vdmj.tc.types.TCRealType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCSeq1Type;
import com.fujitsu.vdmj.tc.types.TCSeqType;
import com.fujitsu.vdmj.tc.types.TCSet1Type;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCTokenType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUndefinedType;
import com.fujitsu.vdmj.tc.types.TCUnionType;
import com.fujitsu.vdmj.tc.types.TCUnknownType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.tc.types.TCVoidReturnType;
import com.fujitsu.vdmj.tc.types.TCVoidType;

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

	public R caseInvariantType(TCInvariantType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseMapType(TCMapType node, S arg)
	{
		return caseType(node, arg);
	}

	public R caseNamedType(TCNamedType node, S arg)
	{
		return caseInvariantType(node, arg);
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
		return caseInvariantType(node, arg);
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
