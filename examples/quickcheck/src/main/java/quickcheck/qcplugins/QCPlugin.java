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

package quickcheck.qcplugins;

import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.visitors.TypeBindFinder;

abstract public class QCPlugin
{
	abstract public String getName();
	abstract public boolean hasErrors();
	abstract public boolean init(ProofObligationList chosen);
	abstract public Map<String, ValueSet> getValues(ProofObligation po);
	
	protected INExpression getPOExpression(ProofObligation po) throws Exception
	{
		if (po.isCheckable)
		{
			TCExpression tcexp = po.getCheckedExpression();
			return ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);
		}
		else
		{
			// Not checkable, so just use "true"
			return new INBooleanLiteralExpression(new LexBooleanToken(true, po.location));
		}
	}
	
	protected List<INBindingSetter> getBindList(INExpression inexp)
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
}
