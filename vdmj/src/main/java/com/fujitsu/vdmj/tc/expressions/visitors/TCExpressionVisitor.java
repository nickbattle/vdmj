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

package com.fujitsu.vdmj.tc.expressions.visitors;

import com.fujitsu.vdmj.tc.annotations.TCAnnotatedExpression;
import com.fujitsu.vdmj.tc.expressions.*;

/**
 * The base type for all TCExpression visitors. All methods, by default, call
 * the abstract caseExpression method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class TCExpressionVisitor<R, S>
{
	abstract public R caseExpression(TCExpression node, S arg);

 	public R caseAbsoluteExpression(TCAbsoluteExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseAndExpression(TCAndExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseAnnotatedExpression(TCAnnotatedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseApplyExpression(TCApplyExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBinaryExpression(TCBinaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBooleanBinaryExpression(TCBooleanBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseBooleanLiteralExpression(TCBooleanLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCardinalityExpression(TCCardinalityExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseCasesExpression(TCCasesExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCharLiteralExpression(TCCharLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCompExpression(TCCompExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDefExpression(TCDefExpression node, S arg)
	{
		return caseLetDefExpression(node, arg);
	}

 	public R caseDistConcatExpression(TCDistConcatExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistIntersectExpression(TCDistIntersectExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistMergeExpression(TCDistMergeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistUnionExpression(TCDistUnionExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDivExpression(TCDivExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDivideExpression(TCDivideExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDomainResByExpression(TCDomainResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDomainResToExpression(TCDomainResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseElementsExpression(TCElementsExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseElseIfExpression(TCElseIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseEqualsExpression(TCEqualsExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseEquivalentExpression(TCEquivalentExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseExists1Expression(TCExists1Expression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseExistsExpression(TCExistsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldExpression(TCFieldExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldNumberExpression(TCFieldNumberExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFloorExpression(TCFloorExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseForAllExpression(TCForAllExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFuncInstantiationExpression(TCFuncInstantiationExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseGreaterEqualExpression(TCGreaterEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseGreaterExpression(TCGreaterExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseHeadExpression(TCHeadExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseHistoryExpression(TCHistoryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIfExpression(TCIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseImpliesExpression(TCImpliesExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseIndicesExpression(TCIndicesExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseInSetExpression(TCInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseIntegerLiteralExpression(TCIntegerLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIotaExpression(TCIotaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsExpression(TCIsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfBaseClassExpression(TCIsOfBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfClassExpression(TCIsOfClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLambdaExpression(TCLambdaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLenExpression(TCLenExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseLessEqualExpression(TCLessEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLessExpression(TCLessExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLetBeStExpression(TCLetBeStExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLetDefExpression(TCLetDefExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapCompExpression(TCMapCompExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapDomainExpression(TCMapDomainExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapEnumExpression(TCMapEnumExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapExpression(TCMapExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapInverseExpression(TCMapInverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapRangeExpression(TCMapRangeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapUnionExpression(TCMapUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseMkBasicExpression(TCMkBasicExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMkTypeExpression(TCMkTypeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseModExpression(TCModExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseMuExpression(TCMuExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNarrowExpression(TCNarrowExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNewExpression(TCNewExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNilExpression(TCNilExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNotEqualExpression(TCNotEqualExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotExpression(TCNotExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseNotInSetExpression(TCNotInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotYetSpecifiedExpression(TCNotYetSpecifiedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNumericBinaryExpression(TCNumericBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseOrExpression(TCOrExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R casePlusExpression(TCPlusExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R casePlusPlusExpression(TCPlusPlusExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R casePostOpExpression(TCPostOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePowerSetExpression(TCPowerSetExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R casePreExpression(TCPreExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePreOpExpression(TCPreOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseProperSubsetExpression(TCProperSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseQuoteLiteralExpression(TCQuoteLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRangeResByExpression(TCRangeResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRangeResToExpression(TCRangeResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRealLiteralExpression(TCRealLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRemExpression(TCRemExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseReverseExpression(TCReverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseSameBaseClassExpression(TCSameBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSameClassExpression(TCSameClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSelfExpression(TCSelfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSeqCompExpression(TCSeqCompExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqConcatExpression(TCSeqConcatExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSeqEnumExpression(TCSeqEnumExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqExpression(TCSeqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetCompExpression(TCSetCompExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetDifferenceExpression(TCSetDifferenceExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetEnumExpression(TCSetEnumExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetExpression(TCSetExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetIntersectExpression(TCSetIntersectExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetRangeExpression(TCSetRangeExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetUnionExpression(TCSetUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStarStarExpression(TCStarStarExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStateInitExpression(TCStateInitExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseStringLiteralExpression(TCStringLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubclassResponsibilityExpression(TCSubclassResponsibilityExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubseqExpression(TCSubseqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubsetExpression(TCSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSubtractExpression(TCSubtractExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTailExpression(TCTailExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseThreadIdExpression(TCThreadIdExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimeExpression(TCTimeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimesExpression(TCTimesExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTupleExpression(TCTupleExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryExpression(TCUnaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryMinusExpression(TCUnaryMinusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUnaryPlusExpression(TCUnaryPlusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUndefinedExpression(TCUndefinedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseVariableExpression(TCVariableExpression node, S arg)
	{
		return caseExpression(node, arg);
	}
}
