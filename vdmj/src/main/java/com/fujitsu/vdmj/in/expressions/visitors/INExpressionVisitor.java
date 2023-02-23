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

package com.fujitsu.vdmj.in.expressions.visitors;

import com.fujitsu.vdmj.in.annotations.INAnnotatedExpression;
import com.fujitsu.vdmj.in.expressions.*;

/**
 * The base type for all INExpression visitors. All methods, by default, call
 * the abstract caseExpression method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class INExpressionVisitor<R, S>
{
	abstract public R caseExpression(INExpression node, S arg);

	public R caseAnnotatedExpression(INAnnotatedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}
	
 	public R caseAbsoluteExpression(INAbsoluteExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseAndExpression(INAndExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseApplyExpression(INApplyExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBinaryExpression(INBinaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBooleanBinaryExpression(INBooleanBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseBooleanLiteralExpression(INBooleanLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCardinalityExpression(INCardinalityExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseCasesExpression(INCasesExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCharLiteralExpression(INCharLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCompExpression(INCompExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDefExpression(INDefExpression node, S arg)
	{
		return caseLetDefExpression(node, arg);
	}

 	public R caseDistConcatExpression(INDistConcatExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistIntersectExpression(INDistIntersectExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistMergeExpression(INDistMergeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistUnionExpression(INDistUnionExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDivExpression(INDivExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDivideExpression(INDivideExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDomainResByExpression(INDomainResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDomainResToExpression(INDomainResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseElementsExpression(INElementsExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseElseIfExpression(INElseIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseEqualsExpression(INEqualsExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseEquivalentExpression(INEquivalentExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseExists1Expression(INExists1Expression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseExistsExpression(INExistsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldExpression(INFieldExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldNumberExpression(INFieldNumberExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFloorExpression(INFloorExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseForAllExpression(INForAllExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFuncInstantiationExpression(INFuncInstantiationExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseGreaterEqualExpression(INGreaterEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseGreaterExpression(INGreaterExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseHeadExpression(INHeadExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseHistoryExpression(INHistoryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIfExpression(INIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseImpliesExpression(INImpliesExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseIndicesExpression(INIndicesExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseInSetExpression(INInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseIntegerLiteralExpression(INIntegerLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIotaExpression(INIotaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsExpression(INIsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfBaseClassExpression(INIsOfBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfClassExpression(INIsOfClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLambdaExpression(INLambdaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLenExpression(INLenExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseLessEqualExpression(INLessEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLessExpression(INLessExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLetBeStExpression(INLetBeStExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLetDefExpression(INLetDefExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapCompExpression(INMapCompExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapDomainExpression(INMapDomainExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapEnumExpression(INMapEnumExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapExpression(INMapExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapInverseExpression(INMapInverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapRangeExpression(INMapRangeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapUnionExpression(INMapUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseMkBasicExpression(INMkBasicExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMkTypeExpression(INMkTypeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseModExpression(INModExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseMuExpression(INMuExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNarrowExpression(INNarrowExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNewExpression(INNewExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNilExpression(INNilExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNotEqualExpression(INNotEqualExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotExpression(INNotExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseNotInSetExpression(INNotInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotYetSpecifiedExpression(INNotYetSpecifiedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNumericBinaryExpression(INNumericBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseOrExpression(INOrExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R casePlusExpression(INPlusExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R casePlusPlusExpression(INPlusPlusExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R casePostOpExpression(INPostOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePowerSetExpression(INPowerSetExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R casePreExpression(INPreExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePreOpExpression(INPreOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseProperSubsetExpression(INProperSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseQuoteLiteralExpression(INQuoteLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRangeResByExpression(INRangeResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRangeResToExpression(INRangeResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRealLiteralExpression(INRealLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRemExpression(INRemExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseReverseExpression(INReverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseSameBaseClassExpression(INSameBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSameClassExpression(INSameClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSelfExpression(INSelfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSeqCompExpression(INSeqCompExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqConcatExpression(INSeqConcatExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSeqEnumExpression(INSeqEnumExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqExpression(INSeqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetCompExpression(INSetCompExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetDifferenceExpression(INSetDifferenceExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetEnumExpression(INSetEnumExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetExpression(INSetExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetIntersectExpression(INSetIntersectExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetRangeExpression(INSetRangeExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetUnionExpression(INSetUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStarStarExpression(INStarStarExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStateInitExpression(INStateInitExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseStringLiteralExpression(INStringLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubclassResponsibilityExpression(INSubclassResponsibilityExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubseqExpression(INSubseqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubsetExpression(INSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSubtractExpression(INSubtractExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTailExpression(INTailExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseThreadIdExpression(INThreadIdExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimeExpression(INTimeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimesExpression(INTimesExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTupleExpression(INTupleExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryExpression(INUnaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryMinusExpression(INUnaryMinusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUnaryPlusExpression(INUnaryPlusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUndefinedExpression(INUndefinedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseVariableExpression(INVariableExpression node, S arg)
	{
		return caseExpression(node, arg);
	}
}
