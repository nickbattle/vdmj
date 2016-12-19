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
 *
 ******************************************************************************/

// This must be in the default package to work with VDMJ's native delegation.

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;

import com.fujitsu.vdmj.VDMJ;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ValueException;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CharacterValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;
import com.fujitsu.vdmj.values.VoidValue;

/**
 * This class contains the code for native IO operations.
 */

public class IO implements Serializable
{
    private static final long serialVersionUID = 1L;
	private static String lastError = "";

	public static Value writeval(Value tval)
	{
		String text = tval.toString();

		Console.out.print(text);
		Console.out.flush();

		return new BooleanValue(true);
	}

	public static Value fwriteval(Value fval, Value tval, Value dval)
	{
		String filename = stringOf(fval);
		String text = tval.toString();// stringOf(tval);
		String fdir = dval.toString();	// <start>|<append>

		try
		{
			FileOutputStream fos =
				new FileOutputStream(filename, fdir.equals("<append>"));

			fos.write(text.getBytes(Console.charset));
			fos.close();
		}
		catch (IOException e)
		{
			lastError = e.getMessage();
			return new BooleanValue(false);
		}

		return new BooleanValue(true);
	}

	// Note that this method is not callable via the native interface, since it
	// need access to the Context to call any type invariants involved while
	// reading the data.
	
	public static Value freadval(Value fval, Context ctxt)
	{
		ValueList result = new ValueList();

		try
		{
			File file = new File(stringOf(fval).replace('/', File.separatorChar));

			if (!file.isAbsolute())
			{
				file = new File(new File(".").getParentFile(), file.getAbsolutePath());
			}

			LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_PP, VDMJ.filecharset);
			ExpressionReader reader = new ExpressionReader(ltr);
			reader.setCurrentModule("IO");
			ASTExpression exp = reader.readExpression();
			TCExpression tcexp = ClassMapper.getInstance(TCNode.MAPPINGS).convert(exp);
			Interpreter ip = Interpreter.getInstance();
			ip.typeCheck(tcexp);
			INExpression inexp = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);
			
			result.add(new BooleanValue(true));
			result.add(inexp.eval(ctxt));
		}
		catch (Exception e)
		{
			lastError = e.toString();
			result = new ValueList();
			result.add(new BooleanValue(false));
			result.add(new NilValue());
		}

		return new TupleValue(result);
	}

	public static Value fecho(Value fval, Value tval, Value dval)
	{
		String text = stringOf(tval);
		String filename = fval.toString().replace("\"", "");

		if (filename.equals("[]"))
		{
			Console.out.print(text);
			Console.out.flush();
		}
		else
		{
			String fdir = dval.toString();	// <start>|<append>

			try
			{
				FileOutputStream fos =
					new FileOutputStream(filename, fdir.equals("<append>"));

				fos.write(text.getBytes(Console.charset));
				fos.close();
			}
			catch (IOException e)
			{
				lastError = e.getMessage();
				return new BooleanValue(false);
			}
		}

		return new BooleanValue(true);
	}

	public static Value ferror()
	{
		return new SeqValue(lastError);
	}

	// We need this because the toString of the Value converts special
	// characters back to their quoted form.

	private static String stringOf(Value val)
	{
		StringBuilder s = new StringBuilder();
		val = val.deref();

		if (val instanceof SeqValue)
		{
			SeqValue sv = (SeqValue)val;

			for (Value v: sv.values)
			{
				v = v.deref();

				if (v instanceof CharacterValue)
				{
					CharacterValue cv = (CharacterValue)v;
					s.append(cv.unicode);
				}
				else
				{
					s.append("?");
				}
			}

			return s.toString();
		}
		else
		{
			return val.toString();
		}
	}

	public static Value print(Value v)
	{
		Console.out.printf("%s", stringOf(v));
		Console.out.flush();
		return new VoidValue();
	}

	public static Value println(Value v)
	{
		Console.out.printf("%s\n", stringOf(v));
		Console.out.flush();
		return new VoidValue();
	}

	public static Value printf(Value fv, Value vs)
		throws ValueException
	{
		String format = stringOf(fv);
		ValueList values = vs.seqValue(null);
		Console.out.printf(format, values.toArray());
		Console.out.flush();
		return new VoidValue();
	}
}
