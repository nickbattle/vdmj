/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

// This must be in the default package to work with VDMJ's native delegation.

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.VDMFunction;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.ValueSet;

public class VDMUtil
{
	@VDMFunction
	public static Value set2seq(Value arg) throws ValueException
	{
		ValueSet set = arg.setValue(null);
		ValueList list = new ValueList();
		list.addAll(set);
		return new SeqValue(list);
	}

	@VDMFunction
	public static Value val2seq_of_char(Value arg)
	{
		return new SeqValue(arg.toString());
	}

	@VDMFunction
	public static Value seq_of_char2val_(Value arg)
	{
		ValueList result = new ValueList();

		try
		{
			SeqValue seq = (SeqValue) arg;
			StringBuilder expression = new StringBuilder();
			
			for (Value v: seq.values)
			{
				CharacterValue ch = (CharacterValue) v;
				expression.append(ch.unicode);
			}
			
			LexTokenReader ltr = new LexTokenReader(expression.toString(), Dialect.VDM_PP);
			ExpressionReader reader = new ExpressionReader(ltr);
			reader.setCurrentModule("VDMUtil");
			ASTExpression exp = reader.readExpression();
			TCExpression tcexp = ClassMapper.getInstance(TCNode.MAPPINGS).convert(exp);
			Interpreter ip = Interpreter.getInstance();
			ip.typeCheck(tcexp);
			INExpression inexp = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);

			result.add(new BooleanValue(true));
			Context ctxt = new Context(null, "seq_of_char2val", null);
			ctxt.setThreadState(null);
			result.add(inexp.eval(ctxt));
		}
		catch (Exception e)
		{
			result = new ValueList();
			result.add(new BooleanValue(false));
			result.add(new NilValue());
		}

		return new TupleValue(result);
	}
	
	@VDMFunction
	public static Value classname(Value arg)
	{
		Value a = arg.deref();
		
		if (a instanceof ObjectValue)
		{
			ObjectValue obj = (ObjectValue)a;
			return new SeqValue(obj.type.name.getName());
		}
		else
		{
			return new NilValue();
		}
	}

	public Value get_file_pos(Context ctxt)
	{
		try
		{
			ValueList tuple = new ValueList();
			Context outer = ctxt.getRoot().outer;
			RootContext root = outer.getRoot();

			tuple.add(new SeqValue(ctxt.location.file.getPath()));
			tuple.add(new NaturalOneValue(ctxt.location.startLine));
			tuple.add(new NaturalOneValue(ctxt.location.startPos));
			tuple.add(new SeqValue(ctxt.location.module));

			int bra = root.title.indexOf('(');

			if (bra > 0)
			{
    			tuple.add(new SeqValue(root.title.substring(0, bra)));
			}
			else
			{
				tuple.add(new SeqValue(""));
			}

			return new TupleValue(tuple);
		}
		catch (Exception e)
		{
			throw new ContextException(4076, e.getMessage(), ctxt.location, ctxt);
		}
	}
}
