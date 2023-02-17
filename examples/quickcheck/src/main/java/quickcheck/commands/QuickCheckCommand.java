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

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.syntax.BindReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.util.Selector;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.visitors.TypeBindFinder;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: quickcheck [>]<ranges file> [<PO numbers>]";
	
	public QuickCheckCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("quickcheck") && !argv[0].equals("qc"))
		{
			throw new IllegalArgumentException(USAGE);
		}
	}

	@Override
	public void run()
	{
		if (argv.length < 2)
		{
			println(USAGE);
			return;
		}
		
		POPlugin plugin = registry.getPlugin("PO");
		ProofObligationList obligations = plugin.getProofObligations();
		obligations.renumber();
		ProofObligationList chosen = getPOs(argv, obligations);

		if (chosen != null)
		{
			if (argv[1].startsWith(">"))
			{
				createRanges(argv[1].substring(1), chosen);
			}
			else
			{
				Map<String, ValueList> ranges = generateRanges(argv[1]);
				
				if (ranges != null)
				{
					runRanges(chosen, ranges);
				}
			}
		}
	}
	
	private ProofObligationList getPOs(String[] argv, ProofObligationList all)
	{
		if (argv.length == 2)
		{
			return all;		// No PO#s specified
		}
		else
		{
			try
			{
				ProofObligationList list = new ProofObligationList();
				
				for (int i=2; i<argv.length; i++)
				{
					int n = Integer.parseInt(argv[i]);
					
					if (n > 0 && n <= all.size())
					{
						list.add(all.get(n-1));
					}
					else
					{
						println("PO# " + argv[i] + " unknown. Must be between 1 and " + all.size());
					}
				}
				
				return list;
			}
			catch (NumberFormatException e)
			{
				println(e.getMessage());
				println(USAGE);
				return null;
			}
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
	
	private Map<String, ValueList> generateRanges(String filename)
	{
		try
		{
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
			
			TCMultipleBindList tcbinds = ClassMapper.getInstance(TCNode.MAPPINGS).convert(astbinds);
			TCExpressionList tcexps = ClassMapper.getInstance(TCNode.MAPPINGS).convert(astexps);
			Environment env = interpreter.getGlobalEnvironment();
			int errors = 0;
			
			for (int i=0; i<tcbinds.size(); i++)
			{
				TCMultipleBind mb = tcbinds.get(i);
				TCType mbtype = mb.typeCheck(env, NameScope.NAMESANDSTATE);
				TCSetType mbset = new TCSetType(mb.location, mbtype);
				
				TCExpression exp = tcexps.get(i);
				TCType exptype = exp.typeCheck(env, null, NameScope.NAMESANDSTATE, null);
				
				if (!TypeComparator.compatible(mbset, exptype))
				{
					println("Range bind and expression do not match at " + exp.location);
					println("Bind type: " + mbtype);
					println("Expression type: " + exptype + ", expecting " + mbset);
					errors++;
				}
			}
			
			if (errors > 0)
			{
				return null;
			}
			
			INMultipleBindList inbinds = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcbinds);
			INExpressionList inexps = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexps);
			RootContext ctxt = interpreter.getInitialContext();
			Map<String, ValueList> ranges = new HashMap<String, ValueList>();
			
			for (int i=0; i<inbinds.size(); i++)
			{
				ctxt.threadState.init();
				String key = inbinds.get(i).toString();
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
					println("Range did not evaluate to a set " + exp.location);
					errors++;
				}
			}
			
			if (errors > 0)
			{
				return null;
			}

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
			println(e.getMessage());
		}
		catch (Exception e)
		{
			println(e);
		}
		
		return null;
	}
	
	private INExpression getPOExpression(ProofObligation po) throws Exception
	{
		TCExpression tcexp = po.getCheckedExpression();
		return ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);
	}
	
	private List<INBindingSetter> getBindList(INExpression inexp) throws Exception
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
	
	private void createRanges(String filename, ProofObligationList all)
	{
		try
		{
			File file = new File(filename);
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			Set<String> done = new HashSet<String>();

			for (ProofObligation po: all)
			{
				for (INBindingSetter mbind: getBindList(getPOExpression(po)))
				{
					if (!done.contains(mbind.toString()))
					{
						writer.println(mbind + " = { /* To be supplied */ };");
						done.add(mbind.toString());
					}
				}
			}

			writer.close();
			println("Wrote " + done.size() + " ranges to " + filename);
		}
		catch (Exception e)
		{
			println("Error: " + e.getMessage());
		}
	}
	
	private void runRanges(ProofObligationList chosen, Map<String, ValueList> ranges)
	{
		try
		{
			RootContext ctxt = Interpreter.getInstance().getInitialContext();

			for (ProofObligation po: chosen)
			{
				INExpression poexp = getPOExpression(po);
				List<INBindingSetter> bindings = getBindList(poexp);
				
				for (INBindingSetter mbind: bindings)
				{
					ValueList values = ranges.get(mbind.toString());
					
					if (values != null)
					{
						mbind.setBindValues(values);
					}
					else
					{
						println("PO# " + po.number + ": No range defined for " + mbind);
					}
				}
				
				try
				{
					Value result = poexp.eval(ctxt);
					
					if (result instanceof BooleanValue)
					{
						if (result.boolValue(ctxt))
						{
							printf("PO# %d, Result = PASSED\n", po.number);
						}
						else
						{
							printf("PO# %d, Result = FAILED: ", po.number);
							findCounterexample(bindings, poexp, ctxt);
						}
					}
					else
					{
						printf("PO# %d, Failed: PO evaluation returns %s?\n", po.number, result.kind());
					}
					
				}
				catch (Exception e)
				{
					printf("PO# %d, Failed: %s\n", po.number, e.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			println(e);
			return;
		}
	}
	
	private void findCounterexample(List<INBindingSetter> bindings, INExpression inexp, RootContext ctxt)
	{
		int[] limits = new int[bindings.size()];
		ValueList[] possibles = new ValueList[bindings.size()];
		
		for (int i=0; i<bindings.size(); i++)
		{
			INBindingSetter bind = bindings.get(i);
			possibles[i] = new ValueList(bind.getBindValues());	// Copy!
			limits[i++] = bind.getBindValues().size();
		}
		
		for (int[] attempt: new Selector(limits))
		{
			for (int i=0; i<bindings.size(); i++)
			{
				INBindingSetter bind = bindings.get(i);
				bind.getBindValues().clear();
				bind.getBindValues().add(possibles[i].get(attempt[i]));
			}
			
			Exception exception = null;
			boolean passed = false;
			
			try
			{
				passed = inexp.eval(ctxt).boolValue(ctxt);
			}
			catch (Exception e)
			{
				exception = e;
				passed = false;
			}
			
			if (!passed)
			{
				printf("Counterexample: ");
				
				if (exception != null)
				{
					println(exception.getMessage());
				}
				
				for (int i=0; i<bindings.size(); i++)
				{
					INBindingSetter bind = bindings.get(i);
					println(bind + " = " + bind.getBindValues().get(0));
				}
				
				break;	// One is good enough?
			}
		}
	}
	
	public static void help()
	{
		println("quickcheck [>]<ranges file> [<PO#s>] - lightweight PO verification");
	}
}
