/*******************************************************************************
 *
 *	Copyright (c) 2016 Aarhus University.
 *
 *	Author: Nick Battle and Kenneth Lausdahl
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.VDMFunction;
import com.fujitsu.vdmj.runtime.VDMOperation;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.NilValue;
import com.fujitsu.vdmj.values.SeqValue;
import com.fujitsu.vdmj.values.TupleValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import util.CsvParser;
import util.CsvResult;
import util.CsvValueBuilder;

/**
 * Basic CSV file support for VDM. This class was imported from Overture.
 * 
 * @author kela
 */
public class CSV implements Serializable
{
	private static final long serialVersionUID = 1L;
	private static String lastError = "";

	@VDMOperation
	public static Value ferror()
	{
		return new SeqValue(lastError);
	}

	/**
	 * Writes a seq of ? in a CSV format to the file specified
	 * 
	 * @param fval
	 *            the filename
	 * @param tval
	 *            the sequence to write
	 * @param dval
	 *            append to or start a new file
	 * @return
	 */
	@VDMFunction
	public static Value fwriteval(Value fval, Value tval, Value dval)
	{
		File file = getFile(fval);
		String fdir = dval.toString(); // <start>|<append>
		StringBuffer text = new StringBuffer();
		if (tval instanceof SeqValue)
		{
			for (Value val : ((SeqValue) tval).values)
			{
				text.append(val.toString());
				text.append(",");
			}

		}

		if (text.length() > 0 && text.charAt(text.length() - 1) == ',')
		{
			text.deleteCharAt(text.length() - 1);
		}
		text.append('\n');

		try
		{
			FileOutputStream fos = new FileOutputStream(file, fdir.equals("<append>"));

			fos.write(text.toString().getBytes(Console.charset));
			fos.close();
		}
		catch (IOException e)
		{
			lastError = e.getMessage();
			return new BooleanValue(false);
		}

		return new BooleanValue(true);
	}

	/**
	 * Read a CSV live as a seq of ? in VDM
	 * 
	 * @param fval
	 *            name of the file to read from
	 * @param indexVal
	 *            the line index
	 * @return true + seq of ? or false and nil
	 */
	@VDMFunction
	public static Value freadval(Value fval, Value indexVal)
	{
		ValueList result = new ValueList();

		try
		{
			File file = getFile(fval);
			long index = indexVal.intValue(null).longValue();
			SeqValue lineCells = new SeqValue();

			boolean success = false;
			try
			{
				CsvParser parser = new CsvParser(new CsvValueBuilder()
				{
					@Override
					public Value createValue(String value)
							throws Exception
					{
						return CSV.createValue("CSV", "freadval", value);
					}
				});
				
				CsvResult res = parser.parseValues(getLine(file, index));
				
				if(!res.dataOk())
				{
					lastError = res.getErrorMsg();
					success = false;
				}
				else
				{
					lineCells.values.addAll(res.getValues());
					success = true;
				}
			}
			catch (Exception e)
			{
				success = false;
				lastError = e.getMessage();
				// OK
			}

			result.add(new BooleanValue(success));
			result.add(lineCells);
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

	/**
	 * Gets the line count of the CSV file
	 * 
	 * @param fval
	 *            name of the file
	 * @return int value with count
	 */
	@VDMFunction
	public static Value flinecount(Value fval)
	{
		ValueList result = new ValueList();

		try
		{
			File file = getFile(fval);
			long count = getLineCount(file);

			result.add(new BooleanValue(true));
			result.add(new NaturalValue(count));
		} catch (Exception e)
		{
			lastError = e.toString();
			result = new ValueList();
			result.add(new BooleanValue(false));
			result.add(new NilValue());
		}

		return new TupleValue(result);
	}

	private static int getLineCount(File file) throws IOException
	{
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		int lines = 0;
		try
		{
			while (bufRdr.readLine() != null)
			{
				lines++;
			}
		} finally
		{
			bufRdr.close();
		}
		return lines;
	}

	private static String getLine(File file, long index) throws IOException
	{
		BufferedReader bufRdr = new BufferedReader(new FileReader(file));
		String line = null;
		int lineIndex = 0;

		if (index < 1)
		{
			bufRdr.close();
			throw new IOException("CSV line index before first entry");
		}

		try
		{
			while ((line = bufRdr.readLine()) != null)
			{
				lineIndex++;
				if (lineIndex == index)
				{
					break;
				}

			}
		}
		finally
		{
			bufRdr.close();
		}

		if (line == null)
		{
			throw new IOException("CSV no data read. Empty line.");
		}

		return line;
	}

	private static Value createValue(String module, String method, String value)
			throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(value, Dialect.VDM_PP);
		ExpressionReader reader = new ExpressionReader(ltr);
		reader.setCurrentModule(module);
		ASTExpression exp = reader.readExpression();
		TCExpression tcexp = ClassMapper.getInstance(TCNode.MAPPINGS).convertLocal(exp);
		Interpreter ip = Interpreter.getInstance();
		ip.typeCheck(tcexp);
		INExpression inexp = ClassMapper.getInstance(INNode.MAPPINGS).convertLocal(tcexp);
		Context ctxt = new Context(LexLocation.ANY, method, null);
		ctxt.setThreadState(CPUValue.vCPU);
		
		return inexp.eval(ctxt);
	}

	/**
	 * Gets the absolute path the file based on the filename parsed and the working dir
	 * of the IDE or the execution dir of VDMJ
	 */
	private static File getFile(Value fval)
	{
		String path = IO.stringOf(fval).replace('/', File.separatorChar);
		return new File(path).getAbsoluteFile();
	}
}
