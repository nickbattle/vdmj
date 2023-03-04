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

package quickcheck.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.types.visitors.INTypeSizeVisitor;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.syntax.BindReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeSet;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.visitors.DefaultRangeCreator;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheck
{
	private static final BigInteger FINITE_LIMIT = BigInteger.valueOf(100);
	private int errorCount = 0;
	
	public boolean hasErrors()
	{
		return errorCount > 0;
	}
	
	public ProofObligationList getPOs(ProofObligationList all, List<Integer> poList)
	{
		errorCount = 0;
		
		if (poList.isEmpty())
		{
			return all;		// No PO#s specified
		}
		else
		{
			ProofObligationList list = new ProofObligationList();
			
			for (Integer n: poList)
			{
				if (n > 0 && n <= all.size())
				{
					list.add(all.get(n-1));
				}
				else
				{
					errorln("PO# " + n + " unknown. Must be between 1 and " + all.size());
					errorCount++;
				}
			}
			
			return errorCount > 0 ? null : list;
		}
	}
	
	private void checkFor(LexTokenReader reader, Token expected, String message) throws LexException, ParserException
	{
		LexToken last = reader.getLast();
		
		if (last.isNot(expected))
		{
			throw new ParserException(9000, message, last.location, 0);
		}
		
		reader.nextToken();
	}
	
	public Map<String, ValueList> readRangeFile(String filename)
	{
		try
		{
			errorCount = 0;
			File file = new File(filename);
			LexTokenReader ltr = new LexTokenReader(file, Dialect.VDM_SL);
			Interpreter interpreter = Interpreter.getInstance();
			String module = interpreter.getDefaultName();
			
			ASTMultipleBindList astbinds = new ASTMultipleBindList();
			ASTExpressionList astexps = new ASTExpressionList();
			
			while (ltr.getLast().isNot(Token.EOF))
			{
				BindReader br = new BindReader(ltr);
				br.setCurrentModule(module);
				astbinds.add(br.readMultipleBind());
				checkFor(ltr, Token.EQUALS, "Expecting <multiple bind> '=' <set expression>;");

				ExpressionReader er = new ExpressionReader(ltr);
				er.setCurrentModule(module);
				astexps.add(er.readExpression());
				checkFor(ltr, Token.SEMICOLON, "Expecting semi-colon after previous <set expression>");
			}
			
			ltr.close();
			TCMultipleBindList tcbinds = ClassMapper.getInstance(TCNode.MAPPINGS).convert(astbinds);
			TCExpressionList tcexps = ClassMapper.getInstance(TCNode.MAPPINGS).convert(astexps);
			Environment env = interpreter.getGlobalEnvironment();
			TypeChecker.clearErrors();
			
			for (int i=0; i<tcbinds.size(); i++)
			{
				TCMultipleBind mb = tcbinds.get(i);
				TCType mbtype = mb.typeCheck(env, NameScope.NAMESANDSTATE);
				TCSetType mbset = new TCSetType(mb.location, mbtype);
				
				TCExpression exp = tcexps.get(i);
				TCType exptype = exp.typeCheck(env, null, NameScope.NAMESANDSTATE, null);
				
				if (TypeChecker.getErrorCount() > 0)
				{
					for (VDMError error: TypeChecker.getErrors())
					{
						println(error.toString());
						errorCount++;
					}
				}
				else if (!TypeComparator.compatible(mbset, exptype))
				{
					println("Range bind and expression do not match at " + exp.location);
					println("Bind type: " + mbtype);
					println("Expression type: " + exptype + ", expecting " + mbset);
					errorCount++;
				}
			}
			
			if (errorCount > 0)
			{
				return null;
			}
			
			INMultipleBindList inbinds = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcbinds);
			INExpressionList inexps = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexps);
			RootContext ctxt = interpreter.getInitialContext();
			Map<String, ValueList> ranges = new HashMap<String, ValueList>();
			long before = System.currentTimeMillis();
			println("Expanding " + inbinds.size() + " ranges:");
			
			for (int i=0; i<inbinds.size(); i++)
			{
				ctxt.threadState.init();
				String key = inbinds.get(i).toString();
				printf(".");
				INExpression exp = inexps.get(i);
				Value value = exp.eval(ctxt);
				
				if (value instanceof SetValue)
				{
					SetValue svalue = (SetValue)value;
					ValueList list = new ValueList();
					list.addAll(svalue.values);
					ranges.put(key, list);
				}
				else
				{
					println("\nRange does not evaluate to a set " + exp.location);
					errorCount++;
				}
			}
			
			if (errorCount > 0)
			{
				return null;
			}
			
			long after = System.currentTimeMillis();
			println("\nRanges expanded " + duration(before, after));

			return ranges;
		}
		catch (LexException e)
		{
			println(e.toString());
		}
		catch (ParserException e)
		{
			println(e.toString());
		}
		catch (TypeCheckException e)
		{
			println("Error: " + e.getMessage() + " " + e.location);
		}
		catch (InternalException e)
		{
			errorln(e.getMessage());
		}
		catch (ContextException e)
		{
			errorln(e.getMessage());
		}
		catch (Exception e)
		{
			errorln(e);
		}
		
		errorCount++;
		return null;
	}
	
	private INExpression getPOExpression(ProofObligation po) throws Exception
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
	
	private List<INBindingSetter> getBindList(INExpression inexp, boolean foralls) throws Exception
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
	
	public void createRangeFile(String filename, ProofObligationList chosen)
	{
		try
		{
			errorCount = 0;
			File file = new File(filename);
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			Set<String> done = new HashSet<String>();
			RootContext ctxt = Interpreter.getInstance().getInitialContext();

			for (ProofObligation po: chosen)
			{
				for (INBindingSetter mbind: getBindList(getPOExpression(po), false))
				{
					if (!done.contains(mbind.toString()))
					{
						DefaultRangeCreator rangeCreator = new DefaultRangeCreator();	// stateful
						TCType type = mbind.getType();
						String range = null;
						
						if (type.isInfinite())
						{
							range = type.apply(rangeCreator, new TCTypeSet());
						}
						else
						{
							try
							{
								BigInteger size = type.apply(new INTypeSizeVisitor(), ctxt);
								
								if (size.compareTo(FINITE_LIMIT) > 0)	// Avoid huge finite types
								{
									range = type.apply(rangeCreator, new TCTypeSet());
								}
								else
								{
									range = "{ x | x : " + type + " }";
								}
							}
							catch (Exception e)		// Probably ArithmeticException
							{
								range = type.apply(rangeCreator, new TCTypeSet());
							}
						}
						
						writer.println(mbind + " = " + range + ";");
						done.add(mbind.toString());
					}
				}
			}

			writer.close();
			println("Created " + done.size() + " default ranges in " + filename + ". Check them! Then run 'qc'");
		}
		catch (Exception e)
		{
			errorCount++;
			errorln("Can't create range file: " + e.getMessage());
		}
	}
	
	public void checkObligations(ProofObligationList chosen, Map<String, ValueList> ranges)
	{
		try
		{
			errorCount = 0;
			RootContext ctxt = Interpreter.getInstance().getInitialContext();
			List<INBindingSetter> bindings = null;

			for (ProofObligation po: chosen)
			{
				if (!po.isCheckable)
				{
					printf("PO# %d, UNCHECKED\n", po.number);
					continue;
				}

				Stack<Context> failPath = new Stack<Context>();
				INForAllExpression.setFailPath(failPath);
				INExpression poexp = getPOExpression(po);
				bindings = getBindList(poexp, false);
				
				for (INBindingSetter mbind: bindings)
				{
					ValueList values = ranges.get(mbind.toString());
					
					if (values != null)
					{
						mbind.setBindValues(values);
					}
					else
					{
						errorln("PO# " + po.number + ": No range defined for " + mbind);
						errorCount++;
					}
				}
				
				try
				{
					long before = System.currentTimeMillis();
					Value result = poexp.eval(ctxt);
					long after = System.currentTimeMillis();
					
					if (result instanceof BooleanValue)
					{
						if (result.boolValue(ctxt))
						{
							printf("PO# %d, PASSED %s\n", po.number, duration(before, after));
						}
						else
						{
							printf("PO# %d, FAILED %s: ", po.number, duration(before, after));
							printFailPath(failPath);
							println("\n" + po);
							errorCount++;
						}
					}
					else
					{
						printf("PO# %d, Error: PO evaluation returns %s?\n\n", po.number, result.kind());
						println(po);
						errorCount++;
					}
				}
				catch (Exception e)
				{
					printf("PO# %d, %s\n\n", po.number, e.getMessage());
					println(po);
					errorCount++;
				}
				finally
				{
					INForAllExpression.setFailPath(null);

					for (INBindingSetter mbind: bindings)
					{
						mbind.setBindValues(null);
					}
				}
			}
		}
		catch (Exception e)
		{
			errorCount++;
			println(e);
			return;
		}
	}
	
	private void printFailPath(Stack<Context> failPath)
	{
		if (failPath.isEmpty())
		{
			printf("No counterexample");
			return;
		}
		
		printf("Counterexample: ");
		String sep = "";
		
		for (Context path: failPath)
		{
			for (TCNameToken name: path.keySet())
			{
				printf("%s%s = %s", sep, name, path.get(name));
				sep = ", ";
			}
		}
	}

	private String duration(long before, long after)
	{
		double duration = (double)(after - before)/1000;
		return "in " + duration + "s";
	}
}
