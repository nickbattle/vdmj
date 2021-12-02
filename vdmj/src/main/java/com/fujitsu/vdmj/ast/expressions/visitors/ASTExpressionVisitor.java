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

package com.fujitsu.vdmj.ast.expressions.visitors;

import com.fujitsu.vdmj.ast.annotations.ASTAnnotatedExpression;
import com.fujitsu.vdmj.ast.expressions.*;

/**
 * The base type for all ASTExpression visitors. All methods, by default, call
 * the abstract caseExpression method, via the various intermediate default
 * methods for their parent types.
 */
public abstract class ASTExpressionVisitor<R, S>
{
	abstract public R caseExpression(ASTExpression node, S arg);

	public R caseAnnotatedExpression(ASTAnnotatedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}
	
 	public R caseAbsoluteExpression(ASTAbsoluteExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseAndExpression(ASTAndExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseApplyExpression(ASTApplyExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBinaryExpression(ASTBinaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseBooleanBinaryExpression(ASTBooleanBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseBooleanLiteralExpression(ASTBooleanLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCardinalityExpression(ASTCardinalityExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseCasesExpression(ASTCasesExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCharLiteralExpression(ASTCharLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseCompExpression(ASTCompExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDefExpression(ASTDefExpression node, S arg)
	{
		return caseLetDefExpression(node, arg);
	}

 	public R caseDistConcatExpression(ASTDistConcatExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistIntersectExpression(ASTDistIntersectExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistMergeExpression(ASTDistMergeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDistUnionExpression(ASTDistUnionExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseDivExpression(ASTDivExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDivideExpression(ASTDivideExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseDomainResByExpression(ASTDomainResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseDomainResToExpression(ASTDomainResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseElementsExpression(ASTElementsExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseElseIfExpression(ASTElseIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseEqualsExpression(ASTEqualsExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseEquivalentExpression(ASTEquivalentExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseExists1Expression(ASTExists1Expression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseExistsExpression(ASTExistsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldExpression(ASTFieldExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFieldNumberExpression(ASTFieldNumberExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFloorExpression(ASTFloorExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseForAllExpression(ASTForAllExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseFuncInstantiationExpression(ASTFuncInstantiationExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseGreaterEqualExpression(ASTGreaterEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseGreaterExpression(ASTGreaterExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseHeadExpression(ASTHeadExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseHistoryExpression(ASTHistoryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIfExpression(ASTIfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseImpliesExpression(ASTImpliesExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R caseIndicesExpression(ASTIndicesExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseInSetExpression(ASTInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseIntegerLiteralExpression(ASTIntegerLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIotaExpression(ASTIotaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsExpression(ASTIsExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfBaseClassExpression(ASTIsOfBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseIsOfClassExpression(ASTIsOfClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLambdaExpression(ASTLambdaExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLenExpression(ASTLenExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseLessEqualExpression(ASTLessEqualExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLessExpression(ASTLessExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseLetBeStExpression(ASTLetBeStExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseLetDefExpression(ASTLetDefExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapCompExpression(ASTMapCompExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapDomainExpression(ASTMapDomainExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapEnumExpression(ASTMapEnumExpression node, S arg)
	{
		return caseMapExpression(node, arg);
	}

 	public R caseMapExpression(ASTMapExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMapInverseExpression(ASTMapInverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapRangeExpression(ASTMapRangeExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseMapUnionExpression(ASTMapUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseMkBasicExpression(ASTMkBasicExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseMkTypeExpression(ASTMkTypeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseModExpression(ASTModExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseMuExpression(ASTMuExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNarrowExpression(ASTNarrowExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNewExpression(ASTNewExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNilExpression(ASTNilExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNotEqualExpression(ASTNotEqualExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotExpression(ASTNotExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseNotInSetExpression(ASTNotInSetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseNotYetSpecifiedExpression(ASTNotYetSpecifiedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseNumericBinaryExpression(ASTNumericBinaryExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseOrExpression(ASTOrExpression node, S arg)
	{
		return caseBooleanBinaryExpression(node, arg);
	}

 	public R casePlusExpression(ASTPlusExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R casePlusPlusExpression(ASTPlusPlusExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R casePostOpExpression(ASTPostOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePowerSetExpression(ASTPowerSetExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R casePreExpression(ASTPreExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R casePreOpExpression(ASTPreOpExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseProperSubsetExpression(ASTProperSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseQuoteLiteralExpression(ASTQuoteLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRangeResByExpression(ASTRangeResByExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRangeResToExpression(ASTRangeResToExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseRealLiteralExpression(ASTRealLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseRemExpression(ASTRemExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseReverseExpression(ASTReverseExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseSameBaseClassExpression(ASTSameBaseClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSameClassExpression(ASTSameClassExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSelfExpression(ASTSelfExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSeqCompExpression(ASTSeqCompExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqConcatExpression(ASTSeqConcatExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSeqEnumExpression(ASTSeqEnumExpression node, S arg)
	{
		return caseSeqExpression(node, arg);
	}

 	public R caseSeqExpression(ASTSeqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetCompExpression(ASTSetCompExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetDifferenceExpression(ASTSetDifferenceExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetEnumExpression(ASTSetEnumExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetExpression(ASTSetExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSetIntersectExpression(ASTSetIntersectExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSetRangeExpression(ASTSetRangeExpression node, S arg)
	{
		return caseSetExpression(node, arg);
	}

 	public R caseSetUnionExpression(ASTSetUnionExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStarStarExpression(ASTStarStarExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseStateInitExpression(ASTStateInitExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseStringLiteralExpression(ASTStringLiteralExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubclassResponsibilityExpression(ASTSubclassResponsibilityExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubseqExpression(ASTSubseqExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseSubsetExpression(ASTSubsetExpression node, S arg)
	{
		return caseBinaryExpression(node, arg);
	}

 	public R caseSubtractExpression(ASTSubtractExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTailExpression(ASTTailExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseThreadIdExpression(ASTThreadIdExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimeExpression(ASTTimeExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseTimesExpression(ASTTimesExpression node, S arg)
	{
		return caseNumericBinaryExpression(node, arg);
	}

 	public R caseTupleExpression(ASTTupleExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryExpression(ASTUnaryExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseUnaryMinusExpression(ASTUnaryMinusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUnaryPlusExpression(ASTUnaryPlusExpression node, S arg)
	{
		return caseUnaryExpression(node, arg);
	}

 	public R caseUndefinedExpression(ASTUndefinedExpression node, S arg)
	{
		return caseExpression(node, arg);
	}

 	public R caseVariableExpression(ASTVariableExpression node, S arg)
	{
		return caseExpression(node, arg);
	}
}
