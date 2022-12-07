/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.util;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.values.CompFunctionValue;
import com.fujitsu.vdmj.values.FunctionValue;
import com.fujitsu.vdmj.values.InvariantValue;
import com.fujitsu.vdmj.values.IterFunctionValue;
import com.fujitsu.vdmj.values.MapValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.OperationValue;
import com.fujitsu.vdmj.values.RecordValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.UpdatableValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.visitors.LeafValueVisitor;

/**
 * A diagnostic class for dumping the deep Value structure of values.
 */
public class ValuePrinter extends LeafValueVisitor<String, List<String>, Integer>
{
	public static void print(Value v)
	{
		ValuePrinter visitor = new ValuePrinter();
		
		for (String line: v.apply(visitor, 0))
		{
			System.out.println(line);
		}
	}
	
	private final static int INDENT = 2;
	
	private List<String> indent(List<String> into, String line, int spaces)
	{
		StringBuilder sb = new StringBuilder();
		char[] chars = new char[spaces];
		Arrays.fill(chars, ' ');
		sb.append(chars);
		sb.append(line);
		into.add(sb.toString());
		return into;
	}
	
	@Override
	public List<String> caseValue(Value node, Integer arg)
	{
		return indent(newCollection(), node.toString(), arg);
	}
	
	@Override
	public List<String> caseCompFunctionValue(CompFunctionValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "comp", arg);
		all.addAll(super.caseCompFunctionValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	public List<String> caseFunctionValue(FunctionValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "function " + node.name + node.toString(), arg);
		all.addAll(super.caseFunctionValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	public List<String> caseInvariantValue(InvariantValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "inv value ", arg);
		all.addAll(super.caseInvariantValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	public List<String> caseIterFunctionValue(IterFunctionValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "comp", arg);
		all.addAll(super.caseIterFunctionValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	public List<String> caseMapValue(MapValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "map {", arg);
		all.addAll(super.caseMapValue(node, arg + INDENT));
		return indent(all, "}", arg);
	}
	
	@Override
	public List<String> caseObjectValue(ObjectValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "object " + node.type, arg);
		all.addAll(super.caseObjectValue(node, arg + INDENT));
		return all;
	}

	@Override
	public List<String> caseOperationValue(OperationValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "operation " + node.name + node.toString(), arg);
		all.addAll(super.caseOperationValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	public List<String> caseRecordValue(RecordValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "record " + node.type, arg);
		all.addAll(super.caseRecordValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	public List<String> caseSeqValue(SeqValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "seq [", arg);
		all.addAll(super.caseSeqValue(node, arg + INDENT));
		return indent(all, "]", arg);
	}
	
	@Override
	public List<String> caseSetValue(SetValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "set {", arg);
		all.addAll(super.caseSetValue(node, arg + INDENT));
		return indent(all, "}", arg);
	}
	
	@Override
	public List<String> caseTupleValue(TupleValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "mk_(", arg);
		all.addAll(super.caseTupleValue(node, arg + INDENT));
		return indent(all, ")", arg);
	}
	
	@Override
	public List<String> caseUpdatableValue(UpdatableValue node, Integer arg)
	{
		List<String> all = indent(newCollection(), "updatable", arg);
		all.addAll(super.caseUpdatableValue(node, arg + INDENT));
		return all;
	}
	
	@Override
	protected List<String> newCollection()
	{
		return new Vector<String>();
	}
}
