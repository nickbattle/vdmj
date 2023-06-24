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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.QuickCheck;
import quickcheck.visitors.RandomRangeCreator;

public class RandomQCPlugin extends QCPlugin
{
	public RandomQCPlugin(List<String> argv)
	{
		// Remove your "qc" plugin arguments from the list here
	}
	
	@Override
	public String getName()
	{
		return "RandomQCPlugin";
	}

	@Override
	public boolean hasErrors()
	{
		return false;
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		return true;
	}

	@Override
	public Map<String, ValueSet> getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds)
	{
		HashMap<String, ValueSet> result = new HashMap<String, ValueSet>();
		RootContext ctxt = Interpreter.getInstance().getInitialContext();
		long seed = 1234;
		
		for (INBindingSetter bind: binds)
		{
			ValueSet values = bind.getType().apply(new RandomRangeCreator(ctxt, 10, seed++), 10);
			result.put(bind.toString(), values);
		}
		
		return result;
	}
}
