/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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

package quickcheck.visitors;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.po.expressions.POApplyExpression;
import com.fujitsu.vdmj.po.expressions.POCasesExpression;
import com.fujitsu.vdmj.po.expressions.POCompExpression;
import com.fujitsu.vdmj.po.expressions.PODivExpression;
import com.fujitsu.vdmj.po.expressions.PODivideExpression;
import com.fujitsu.vdmj.po.expressions.POExpression;
import com.fujitsu.vdmj.po.expressions.POHeadExpression;
import com.fujitsu.vdmj.po.expressions.POIotaExpression;
import com.fujitsu.vdmj.po.expressions.POLetBeStExpression;
import com.fujitsu.vdmj.po.expressions.POMapInverseExpression;
import com.fujitsu.vdmj.po.expressions.POMapUnionExpression;
import com.fujitsu.vdmj.po.expressions.POMkTypeExpression;
import com.fujitsu.vdmj.po.expressions.PONotYetSpecifiedExpression;
import com.fujitsu.vdmj.po.expressions.POStarStarExpression;
import com.fujitsu.vdmj.po.expressions.POTailExpression;
import com.fujitsu.vdmj.po.expressions.visitors.POLeafExpressionVisitor;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCType;

/**
 * Search a PO expression and note whether the expression is total (ie. defined for
 * all possible argument values).
 */
public class TotalExpressionVisitor extends POLeafExpressionVisitor<Boolean, List<Boolean>, Object>
{
	private boolean isTotal = true;		// Set false by some cases
	
	public boolean isTotal()
	{
		return isTotal;
	}
	
	@Override
	protected List<Boolean> newCollection()
	{
		return new Vector<Boolean>();
	}
	
	@Override
	public List<Boolean> caseApplyExpression(POApplyExpression node, Object arg)
	{
		TCType rtype = node.root.getExptype();

		if (rtype.isMap(node.location))
		{
			isTotal = false;
		}
		else if (rtype.isSeq(node.location))
		{
			isTotal = false;
		}
		else if (rtype.isFunction(node.location))
		{
			TCFunctionType ftype = rtype.getFunction();
			isTotal = !ftype.partial;
		}
		
		return super.caseApplyExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseCompExpression(POCompExpression node, Object arg)
	{
		isTotal = false;
		return super.caseCompExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseMapUnionExpression(POMapUnionExpression node, Object arg)
	{
		isTotal = false;
		return super.caseMapUnionExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseDivideExpression(PODivideExpression node, Object arg)
	{
		if (node.right.getExptype().getNumeric().getWeight() > 0)
		{
			isTotal = false;
		}
		
		return super.caseDivideExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseDivExpression(PODivExpression node, Object arg)
	{
		if (node.right.getExptype().getNumeric().getWeight() > 0)
		{
			isTotal = false;
		}

		return super.caseDivExpression(node, arg);
	}

	@Override
	public List<Boolean> caseStarStarExpression(POStarStarExpression node, Object arg)
	{
		isTotal = false;
		return super.caseStarStarExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseCasesExpression(POCasesExpression node, Object arg)
	{
		isTotal = false;
		return super.caseCasesExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseIotaExpression(POIotaExpression node, Object arg)
	{
		isTotal = false;
		return super.caseIotaExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseLetBeStExpression(POLetBeStExpression node, Object arg)
	{
		isTotal = false;
		return super.caseLetBeStExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseMkTypeExpression(POMkTypeExpression node, Object arg)
	{
		if (node.recordType.invdef != null)
		{
			isTotal = false;
		}
		
		return super.caseMkTypeExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseMapInverseExpression(POMapInverseExpression node, Object arg)
	{
		isTotal = false;
		return super.caseMapInverseExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseHeadExpression(POHeadExpression node, Object arg)
	{
		isTotal = false;
		return super.caseHeadExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseTailExpression(POTailExpression node, Object arg)
	{
		isTotal = false;
		return super.caseTailExpression(node, arg);
	}
	
	@Override
	public List<Boolean> caseNotYetSpecifiedExpression(PONotYetSpecifiedExpression node, Object arg)
	{
		isTotal = false;
		return super.caseNotYetSpecifiedExpression(node, arg);
	}

	@Override
	public List<Boolean> caseExpression(POExpression node, Object arg)
	{
		return newCollection();
	}
}
