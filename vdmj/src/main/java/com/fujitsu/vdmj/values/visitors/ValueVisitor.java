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

package com.fujitsu.vdmj.values.visitors;

import com.fujitsu.vdmj.values.BUSValue;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.CompFunctionValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.IterFunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.NumericValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.ParameterValue;
import com.fujitsu.vdmj.values.QuoteValue;
import com.fujitsu.vdmj.values.RationalValue;
import com.fujitsu.vdmj.values.RealValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.ReferenceValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TokenValue;
import com.fujitsu.vdmj.values.TransactionValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.UndefinedValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.VoidReturnValue;
import com.fujitsu.vdmj.values.VoidValue;

public abstract class ValueVisitor<R, S>
{
 	public abstract R caseValue(Value node, S arg);

 	public R caseBooleanValue(BooleanValue node, S arg)
	{
		return caseValue(node, arg);
	}

	public R caseBUSValue(BUSValue node, S arg)
	{
		return caseObjectValue(node, arg);
	}

 	public R caseCharacterValue(CharacterValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseCompFunctionValue(CompFunctionValue node, S arg)
	{
		return caseFunctionValue(node, arg);
	}

 	public R caseCPUValue(CPUValue node, S arg)
	{
		return caseObjectValue(node, arg);
	}

 	public R caseFunctionValue(FunctionValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseIntegerValue(IntegerValue node, S arg)
	{
		return caseRationalValue(node, arg);
	}

 	public R caseInvariantValue(InvariantValue node, S arg)
	{
		return caseReferenceValue(node, arg);
	}

 	public R caseIterFunctionValue(IterFunctionValue node, S arg)
	{
		return caseFunctionValue(node, arg);
	}

 	public R caseMapValue(MapValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseNaturalOneValue(NaturalOneValue node, S arg)
	{
		return caseNaturalValue(node, arg);
	}

 	public R caseNaturalValue(NaturalValue node, S arg)
	{
		return caseIntegerValue(node, arg);
	}

 	public R caseNilValue(NilValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseNumericValue(NumericValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseObjectValue(ObjectValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseOperationValue(OperationValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseParameterValue(ParameterValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseQuoteValue(QuoteValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseRationalValue(RationalValue node, S arg)
	{
		return caseRealValue(node, arg);
	}

 	public R caseRealValue(RealValue node, S arg)
	{
		return caseNumericValue(node, arg);
	}

 	public R caseRecordValue(RecordValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseReferenceValue(ReferenceValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseSeqValue(SeqValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseSetValue(SetValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseTokenValue(TokenValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseTransactionValue(TransactionValue node, S arg)
	{
		return caseUpdatableValue(node, arg);
	}

 	public R caseTupleValue(TupleValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseUndefinedValue(UndefinedValue node, S arg)
	{
		return caseValue(node, arg);
	}

 	public R caseUpdatableValue(UpdatableValue node, S arg)
	{
		return caseReferenceValue(node, arg);
	}

 	public R caseVoidReturnValue(VoidReturnValue node, S arg)
	{
		return caseVoidValue(node, arg);
	}

 	public R caseVoidValue(VoidValue node, S arg)
	{
		return caseValue(node, arg);
	}
}