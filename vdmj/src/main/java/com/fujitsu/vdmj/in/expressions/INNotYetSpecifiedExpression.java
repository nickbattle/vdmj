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

package com.fujitsu.vdmj.in.expressions;

import java.lang.reflect.Method;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.expressions.visitors.INExpressionVisitor;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

public class INNotYetSpecifiedExpression extends INExpression
{
	private static final long serialVersionUID = 1L;

	public INNotYetSpecifiedExpression(LexLocation location)
	{
		super(location);
		location.executable(false);		// ie. ignore coverage for these
	}

	@Override
	public String toString()
	{
		return "not yet specified";
	}

	@Override
	public Value eval(Context ctxt)
	{
		breakpoint.check(location, ctxt);

		if (location.module.equals("VDMUtil") ||
			location.module.equals("DEFAULT"))
		{
    		if (ctxt.title.equals("get_file_pos()"))
    		{
    			// This needs location information from the context, so we
    			// can't just call down to a native method for this one.

    			return get_file_pos(ctxt);
    		}
		}

		if (location.module.equals("IO") ||
			location.module.equals("DEFAULT"))
		{
			if (ctxt.title.equals("freadval(filename)"))
			{
				// This needs type invariant information from the context, so we
				// can't just call down to a native method for this one.

				try
				{
					TCNameToken arg = new TCNameToken(location, "IO", "filename");
					Value fval = ctxt.get(arg);
					
					// We can't link with the IO class directly because it's in the default
					// package, so we reflect our way over to it.
					
					Class<?> io = Class.forName("IO");
					Method m = io.getMethod("freadval", new Class[] {Value.class, Context.class});
					return (Value)m.invoke(io.getDeclaredConstructor().newInstance(), new Object[] {fval, ctxt});
				}
				catch (Exception e)
				{
					throw new InternalException(62, "Cannot invoke native method: " + e.getMessage());
				}
			}
		}

		if (Settings.dialect == Dialect.VDM_SL)
		{
			ModuleInterpreter i = (ModuleInterpreter)Interpreter.getInstance();
			INModule module = i.findModule(location.module);

			if (module != null)
			{
				if (module.hasDelegate())
				{
					return module.invokeDelegate(ctxt, Token.FUNCTIONS);
				}
			}
		}
		else
		{
    		ObjectValue self = ctxt.getSelf();

    		if (self == null)
    		{
    			ClassInterpreter i = (ClassInterpreter)Interpreter.getInstance();
    			INClassDefinition cls = i.findClass(location.module);

    			if (cls != null)
    			{
    				if (cls.hasDelegate())
    				{
    					return cls.invokeDelegate(ctxt, Token.FUNCTIONS);
    				}
    			}
    		}
    		else
    		{
    			if (self.hasDelegate())
    			{
    				return self.invokeDelegate(ctxt, Token.FUNCTIONS);
    			}
    		}
		}

		return abort(4024, "'not yet specified' expression reached", ctxt);
	}

	private Value get_file_pos(Context ctxt)
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
		catch (ValueException e)
		{
			return abort(e);
		}
		catch (Exception e)
		{
			return abort(4076, e.getMessage(), ctxt, ctxt.location);
		}
	}

	@Override
	public <R, S> R apply(INExpressionVisitor<R, S> visitor, S arg)
	{
		return visitor.caseNotYetSpecifiedExpression(this, arg);
	}
}
