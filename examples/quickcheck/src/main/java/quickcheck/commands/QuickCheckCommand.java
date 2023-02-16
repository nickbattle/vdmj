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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.ast.expressions.ASTExpressionList;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.ast.patterns.ASTMultipleBindList;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INExpressionList;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
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
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.visitors.TypeBindFinder;

public class QuickCheckCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: quickcheck <ranges file> [<PO numbers>]";
	
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
		
		List<Integer> numbers = new Vector<Integer>();
		
		try
		{
			for (int i=2; i<argv.length; i++)
			{
				numbers.add(Integer.parseInt(argv[i]));
			}
		}
		catch (NumberFormatException e)
		{
			println(USAGE);
			return;
		}
		
		POPlugin plugin = registry.getPlugin("PO");
		ProofObligationList all = plugin.getProofObligations();
		
		if (numbers.isEmpty())
		{
			for (int n=1; n<=all.size(); n++)
			{
				numbers.add(n);		// Every PO
			}
		}
		else
		{
			for (Integer n: numbers)
			{
				if (n < 1 || n > all.size())
				{
					println("PO# must be between 1 and " + all.size());
					return;
				}
			}
		}

		Map<String, ValueList> ranges = parseRanges(argv[1]);
		
		if (ranges == null)
		{
			return;
		}
		
		try
		{
			Interpreter i = Interpreter.getInstance();
			RootContext ctxt = i.getInitialContext();

			for (Integer n: numbers)
			{
				ProofObligation po = all.get(n - 1);
				TCExpression tcexp = po.getCheckedExpression();
				INExpression inexp = ClassMapper.getInstance(INNode.MAPPINGS).convert(tcexp);
				
				for (INMultipleTypeBind mbind: inexp.apply(new TypeBindFinder(), null))
				{
					ValueList values = ranges.get(mbind.toString());
					
					if (values != null)
					{
						mbind.setBindValues(values);
					}
					else
					{
						println("PO# " + n + ": No range defined for " + mbind);
					}
				}
				
				try
				{
					printf("PO# %d, Result = %s\n", n, inexp.eval(ctxt));
				}
				catch (Exception e)
				{
					printf("PO# %d, failed: %s\n", n, e.getMessage());
				}
			}
		}
		catch (Exception e)
		{
			println(e);
			return;
		}
	}
	
	private Map<String, ValueList> parseRanges(String filename)
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
				LexToken token = ltr.getLast();
				
				if (token.isNot(Token.EQUALS))
				{
					throw new ParserException(9000,
						"Expecting <multiple bind> '=' <set expression>;", token.location, 0);
				}
				
				ltr.nextToken();
				ExpressionReader er = new ExpressionReader(ltr);
				er.setCurrentModule(module);
				astexps.add(er.readExpression());
				token = ltr.getLast();
				
				if (token.isNot(Token.SEMICOLON))
				{
					throw new ParserException(9000,
							"Expecting semi-colon after range", token.location, 0);
				}
				
				ltr.nextToken();
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
				Value value = inexps.get(i).eval(ctxt);
				
				if (value instanceof SetValue)
				{
					SetValue svalue = (SetValue)value;
					ValueList list = new ValueList();
					list.addAll(svalue.values);
					ranges.put(key, list);
				}
				else
				{
					println("Range does not evaluate to a set");
				}
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
	
	public static void help()
	{
		println("quickcheck <ranges file> [<PO#s>] - attempt to brute force discharge POs");
	}
}
