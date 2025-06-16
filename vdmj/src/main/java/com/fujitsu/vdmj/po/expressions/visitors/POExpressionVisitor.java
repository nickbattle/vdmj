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

package com.fujitsu.vdmj.po.expressions.visitors;

import com.fujitsu.vdmj.po.annotations.POAnnotatedExpression;
import com.fujitsu.vdmj.po.expressions.*;

/**
 * The base type for all POExpression visitors. All methods, by default, call
 * the abstract caseExpression method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class POExpressionVisitor<R, S>
{
	abstract public R caseExpression(POExpression node, S arg);

 	public R caseAbsoluteExpression(POAbsoluteExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseAndExpression(POAndExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseAnnotatedExpression(POAnnotatedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseApplyExpression(POApplyExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBinaryExpression(POBinaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBooleanBinaryExpression(POBooleanBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseBooleanLiteralExpression(POBooleanLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCardinalityExpression(POCardinalityExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseCasesExpression(POCasesExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCharLiteralExpression(POCharLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCompExpression(POCompExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDefExpression(PODefExpression node, S arg)
	{
		return caseLetDefExpression(node, arg);
	}

 	public R caseDistConcatExpression(PODistConcatExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistIntersectExpression(PODistIntersectExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistMergeExpression(PODistMergeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistUnionExpression(PODistUnionExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDivExpression(PODivExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDivideExpression(PODivideExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDomainResByExpression(PODomainResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDomainResToExpression(PODomainResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseElementsExpression(POElementsExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseElseIfExpression(POElseIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseEqualsExpression(POEqualsExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseEquivalentExpression(POEquivalentExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseExists1Expression(POExists1Expression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseExistsExpression(POExistsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldExpression(POFieldExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldNumberExpression(POFieldNumberExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFloorExpression(POFloorExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseForAllExpression(POForAllExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFuncInstantiationExpression(POFuncInstantiationExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseGreaterEqualExpression(POGreaterEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseGreaterExpression(POGreaterExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseHeadExpression(POHeadExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseHistoryExpression(POHistoryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIfExpression(POIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseImpliesExpression(POImpliesExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseIndicesExpression(POIndicesExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseInSetExpression(POInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseIntegerLiteralExpression(POIntegerLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIotaExpression(POIotaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsExpression(POIsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfBaseClassExpression(POIsOfBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfClassExpression(POIsOfClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLambdaExpression(POLambdaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLenExpression(POLenExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseLessEqualExpression(POLessEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLessExpression(POLessExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLetBeStExpression(POLetBeStExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLetDefExpression(POLetDefExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapCompExpression(POMapCompExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapDomainExpression(POMapDomainExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapEnumExpression(POMapEnumExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapExpression(POMapExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapInverseExpression(POMapInverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapRangeExpression(POMapRangeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapUnionExpression(POMapUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseMkBasicExpression(POMkBasicExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMkTypeExpression(POMkTypeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseModExpression(POModExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseMuExpression(POMuExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNarrowExpression(PONarrowExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNewExpression(PONewExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNilExpression(PONilExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNotEqualExpression(PONotEqualExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotExpression(PONotExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseNotInSetExpression(PONotInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotYetSpecifiedExpression(PONotYetSpecifiedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNumericBinaryExpression(PONumericBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseOrExpression(POOrExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R casePlusExpression(POPlusExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R casePlusPlusExpression(POPlusPlusExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R casePostOpExpression(POPostOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePowerSetExpression(POPowerSetExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R casePreExpression(POPreExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePreOpExpression(POPreOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseProperSubsetExpression(POProperSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseQuoteLiteralExpression(POQuoteLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRangeResByExpression(PORangeResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRangeResToExpression(PORangeResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRealLiteralExpression(PORealLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRemExpression(PORemExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseReverseExpression(POReverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseSameBaseClassExpression(POSameBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSameClassExpression(POSameClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSelfExpression(POSelfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSeqCompExpression(POSeqCompExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqConcatExpression(POSeqConcatExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSeqEnumExpression(POSeqEnumExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqExpression(POSeqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetCompExpression(POSetCompExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetDifferenceExpression(POSetDifferenceExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetEnumExpression(POSetEnumExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetExpression(POSetExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetIntersectExpression(POSetIntersectExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetRangeExpression(POSetRangeExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetUnionExpression(POSetUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStarStarExpression(POStarStarExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStateInitExpression(POStateInitExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseStringLiteralExpression(POStringLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubclassResponsibilityExpression(POSubclassResponsibilityExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubseqExpression(POSubseqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubsetExpression(POSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSubtractExpression(POSubtractExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTailExpression(POTailExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseThreadIdExpression(POThreadIdExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimeExpression(POTimeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimesExpression(POTimesExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTupleExpression(POTupleExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryExpression(POUnaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryMinusExpression(POUnaryMinusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUnaryPlusExpression(POUnaryPlusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUndefinedExpression(POUndefinedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseVariableExpression(POVariableExpression node, S arg)
	{
		return caseExpression(node, arg);
	}
}