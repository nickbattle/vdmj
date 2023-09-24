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

package quickcheck.strategies;

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.fujitsu.vdmj.in.patterns.INMultipleBind;
import com.fujitsu.vdmj.in.patterns.INMultipleBindList;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexException;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.InternalException;
import com.fujitsu.vdmj.messages.VDMError;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.syntax.BindReader;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionList;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.expressions.TCExpressionList;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBind;
import com.fujitsu.vdmj.tc.patterns.TCMultipleBindList;
import com.fujitsu.vdmj.tc.patterns.TCMultipleTypeBind;
import com.fujitsu.vdmj.tc.types.TCFunctionType;
import com.fujitsu.vdmj.tc.types.TCNamedType;
import com.fujitsu.vdmj.tc.types.TCParameterType;
import com.fujitsu.vdmj.tc.types.TCSetType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCTypeList;
import com.fujitsu.vdmj.tc.types.visitors.TCParameterCollector;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.TypeCheckException;
import com.fujitsu.vdmj.typechecker.TypeChecker;
import com.fujitsu.vdmj.typechecker.TypeComparator;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import quickcheck.QuickCheck;
import quickcheck.visitors.FixedRangeCreator;

public class FixedQCStrategy extends QCStrategy
{
	private int expansionLimit = 20;		// Top level binding value expansion limit
	
	private int errorCount = 0;
	private String rangesFile = "ranges.qc";
	private boolean createFile = false;

	private Map<String, ValueList> allRanges = null;
	
	public FixedQCStrategy(List<String> argv)
	{
		Iterator<String> iter = argv.iterator();
		
		while (iter.hasNext())
		{
			try
			{
				String arg = iter.next();
				
				switch (arg)
				{
					case "-fixed:file":			// Use this as ranges.qc
						iter.remove();

						if (iter.hasNext())
						{
							rangesFile = iter.next();
							iter.remove();
						}
						
						createFile = false;
						break;
						
					case "-fixed:create":		// Create ranges.qc
						iter.remove();
						
						if (iter.hasNext())
						{
							rangesFile = iter.next();
							iter.remove();
						}

						createFile = true;
						break;
						
					case "-fixed:size":		// Total top level size
						iter.remove();

						if (iter.hasNext())
						{
							expansionLimit = Integer.parseInt(iter.next());
							iter.remove();
						}
						break;
						
					default:
						if (arg.startsWith("-fixed:"))
						{
							errorln("Unknown fixed option: " + arg);
							errorln(help());
							errorCount++;
							iter.remove();
						}
				}
			}
			catch (NumberFormatException e)
			{
				errorln("Argument must be numeric");
				errorln(help());
				errorCount++;
			}
			catch (ArrayIndexOutOfBoundsException e)
			{
				errorln("Missing argument");
				errorln(help());
				errorCount++;
			}
		}
		
		verbose("fixed:size = %d\n", expansionLimit);
		verbose("fixed:file = %s\n", rangesFile);
	}
	
	@Override
	public boolean hasErrors()
	{
		return errorCount > 0;
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
	
	private Map<String, ValueList> readRangeFile(String filename)
	{
		try
		{
			verbose("Reading %s\n", filename);
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
			TCTypeList tctypes = new TCTypeList();
			Environment env = interpreter.getGlobalEnvironment();
			TypeChecker.clearErrors();
			
			for (int i=0; i<tcbinds.size(); i++)
			{
				TCMultipleBind mb = tcbinds.get(i);
				Environment penv = env;
				
				if (mb instanceof TCMultipleTypeBind)
				{
					TCMultipleTypeBind mtb = (TCMultipleTypeBind)mb;
		    		List<String> names = mtb.type.apply(new TCParameterCollector(), null);
		    		
		    		if (!names.isEmpty())
		    		{
	    				TCDefinitionList defs = new TCDefinitionList();
	    				LexLocation location = mtb.type.location;
			    		
		    			for (String name: names)
		    			{
		    				TCNameToken tcname = new TCNameToken(location, module, name.substring(1));
		    				TCParameterType ptype = new TCParameterType(tcname);
	    					TCDefinition p = new TCLocalDefinition(location, tcname, ptype);
	    					p.markUsed();
	    					defs.add(p);
		    			}
		    			
		    			penv = new FlatEnvironment(defs, env);
		    		}
				}
				
				TCType mbtype = mb.typeCheck(penv, NameScope.NAMESANDSTATE);
				TCSetType mbset = new TCSetType(mb.location, mbtype);
				tctypes.add(mbtype);
				
				TCExpression exp = tcexps.get(i);
				TCType exptype = exp.typeCheck(penv, null, NameScope.NAMESANDSTATE, null);
				
				if (TypeChecker.getErrorCount() > 0)
				{
					for (VDMError error: TypeChecker.getErrors())
					{
						println(mb + " = " + exp);
						println(error.toString());
						errorCount++;
					}
				}
				else if (exptype.isNumeric(LexLocation.ANY))
				{
					continue;	// fixed later
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
			printf("Expanding " + inbinds.size() + " ranges: ");
			
			for (int i=0; i<inbinds.size(); i++)
			{
				ctxt.threadState.init();
				String key = keyFor(inbinds.get(i));
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
				else if (value instanceof IntegerValue)
				{
					IntegerValue ivalue = (IntegerValue)value;
					int limit = ivalue.value.intValue();
					ValueList list = new ValueList();
					list.addAll(tctypes.get(i).apply(new FixedRangeCreator(ctxt), limit));
					ranges.put(key, list);
				}
				else
				{
					println("\nRange does not evaluate to a set or integer " + exp.location);
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
		catch (ContextException e)
		{
			println(e.getMessage());
		}
		catch (InternalException e)
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

	/**
	 * We can't create a key with the toString of the bind, because the type may not
	 * be explicit. 
	 */
	private String keyFor(INMultipleBind bind)
	{
		INMultipleTypeBind tb = (INMultipleTypeBind)bind;
		
		if (tb.type instanceof TCNamedType)
		{
			TCNamedType nt = (TCNamedType)tb.type;
			return tb.plist.toString() + ":" + nt.typename.getExplicit(false);
		}
		else
		{
			return tb.plist.toString() + ":" + tb.type.toString();
		}
	}

	private void createRangeFile(QuickCheck qc, String filename)
	{
		try
		{
			errorCount = 0;
			File file = new File(filename);
			PrintWriter writer = new PrintWriter(new FileWriter(file));
			Set<String> done = new HashSet<String>();

			for (ProofObligation po: qc.getChosen())
			{
				for (INBindingSetter mbind: qc.getINBindList(qc.getINExpression(po)))
				{
					if (!done.contains(mbind.toString()))
					{
						String range = null;
						TCType type = mbind.getType();
						
						if (type instanceof TCFunctionType)
						{
							range = "{ /* define lambdas! */ }";
						}
						else
						{
							range = Integer.toString(expansionLimit);
						}
						
						writer.println("-- " + po.location);
						writer.println(mbind + " = " + range + ";");
						done.add(mbind.toString());
					}
				}
			}

			writer.close();
			println("Created " + done.size() + " default ranges in '" + filename + "'");
			println("Check them! Then run 'qc -p fixed -fixed:file " + filename + "'");
		}
		catch (Exception e)
		{
			errorCount++;
			errorln("Can't create range file: " + e.getMessage());
		}
	}

	private String duration(long before, long after)
	{
		double duration = (double)(after - before)/1000;
		return "in " + duration + "s";
	}

	@Override
	public String getName()
	{
		return "fixed";
	}

	@Override
	public boolean init(QuickCheck qc)
	{
		if (createFile)
		{
			createRangeFile(qc, rangesFile);
			return false;	// Don't do checks!
		}
		else
		{
			if (new File(rangesFile).exists())
			{
				println("Using ranges file " + rangesFile);
				allRanges = readRangeFile(rangesFile);
			}
			else
			{
				println("Did not find " + rangesFile + " (see -fixed:create option)");
				allRanges = new HashMap<String, ValueList>();
			}
			
			return !hasErrors();
		}
	}

	@Override
	public Results getValues(ProofObligation po, INExpression exp, List<INBindingSetter> binds, Context ctxt)
	{
		Map<String, ValueList> values = new HashMap<String, ValueList>();
		long before = System.currentTimeMillis();
		
		try
		{
			for (INBindingSetter bind: binds)
			{
				String key = bind.toString();
				
				if (allRanges.containsKey(key))		// else a default created later
				{
					values.put(key, allRanges.get(key));
				}
			}
		}
		catch (Exception e)
		{
			// Can't happen?
			println(e);
		}
		
		return new Results(false, values, System.currentTimeMillis() - before);
	}

	@Override
	public String help()
	{
		return getName() + " [-fixed:file <file> | -fixed:create <file>][-fixed:size <size>]";
	}

	@Override
	public boolean useByDefault()
	{
		return false;	// Use if no -p given
	}
}
