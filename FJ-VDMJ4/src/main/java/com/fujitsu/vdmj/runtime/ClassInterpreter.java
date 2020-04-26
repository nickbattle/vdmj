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

package com.fujitsu.vdmj.runtime;

import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.VDMJ;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INClassList;
import com.fujitsu.vdmj.in.definitions.INDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.RTLogger;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.definitions.POClassList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.scheduler.CTMainThread;
import com.fujitsu.vdmj.scheduler.MainThread;
import com.fujitsu.vdmj.scheduler.RunState;
import com.fujitsu.vdmj.scheduler.SchedulableThread;
import com.fujitsu.vdmj.scheduler.SystemClock;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinitionSet;
import com.fujitsu.vdmj.tc.definitions.TCLocalDefinition;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.traces.CallSequence;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.FlatCheckedEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PublicClassEnvironment;
import com.fujitsu.vdmj.values.BUSValue;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.NameValuePair;
import com.fujitsu.vdmj.values.NameValuePairList;
import com.fujitsu.vdmj.values.NameValuePairMap;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.Value;

/**
 * The VDM++ interpreter.
 */
public class ClassInterpreter extends Interpreter
{
	private final TCClassList checkedClasses;
	private final INClassList executableClasses;
	
	private INClassDefinition defaultClass;
	private NameValuePairMap createdValues;
	private TCDefinitionSet createdDefinitions;
	private POClassList pogClasses;

	public ClassInterpreter(INClassList executableClasses, TCClassList checkedClasses) throws Exception
	{
		super();
		
		this.checkedClasses = checkedClasses;
		this.executableClasses = executableClasses;
		this.createdValues = new NameValuePairMap();
		this.createdDefinitions = new TCDefinitionSet();

		if (executableClasses.isEmpty())
		{
			setDefaultName(null);
		}
		else
		{
			setDefaultName(executableClasses.get(0).name.getName());
		}
	}

	@Override
	public void setDefaultName(String cname) throws Exception
	{
		if (cname == null || cname.equals("?"))
		{
			defaultClass = new INClassDefinition();
			executableClasses.add(defaultClass);
		}
		else
		{
    		for (INClassDefinition c: executableClasses)
    		{
    			if (c.name.getName().equals(cname))
    			{
    				defaultClass = c;
    				return;
    			}
    		}

    		throw new Exception("Class " + cname + " not loaded");
		}
	}

	@Override
	public Environment getGlobalEnvironment()
	{
		Environment env = new PublicClassEnvironment(checkedClasses);
		
		if (!createdDefinitions.isEmpty())
		{
			env = new FlatCheckedEnvironment(createdDefinitions.asList(), env, NameScope.NAMESANDSTATE);
		}
		
		return env; 
	}

	@Override
	public String getDefaultName()
	{
		return defaultClass.name.getName();
	}

	@Override
	public File getDefaultFile()
	{
		return defaultClass.name.getLocation().file;
	}

	@Override
	public Set<File> getSourceFiles()
	{
		return executableClasses.getSourceFiles();
	}

	public INClassList getClasses()
	{
		return executableClasses;
	}

	@Override
	public void init()
	{
		SchedulableThread.terminateAll();

		scheduler.init();
		SystemClock.init();
		CPUValue.init(scheduler);
		BUSValue.init();
		ObjectValue.init();

		logSwapIn();
		initialContext = executableClasses.creatInitialContext();
		executableClasses.initialize((StateContext) initialContext);
		executableClasses.systemInit(scheduler, initialContext);
		INAnnotation.init(initialContext);
		logSwapOut();

		createdValues = new NameValuePairMap();
		createdDefinitions = new TCDefinitionSet();

		scheduler.reset();	// Required before a run, as well as init above
		BUSValue.start();	// Start any BUS threads first...
	}

	@Override
	public void traceInit()
	{
		SchedulableThread.terminateAll();
		scheduler.reset();

		SystemClock.init();
		initialContext = executableClasses.creatInitialContext();
		executableClasses.initialize((StateContext) initialContext);
		createdValues = new NameValuePairMap();
		createdDefinitions = new TCDefinitionSet();
	}

	@Override
	protected TCExpression parseExpression(String line, String module) throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(line, Settings.dialect, Console.charset);
		ExpressionReader reader = new ExpressionReader(ltr);
		reader.setCurrentModule(module);
		ASTExpression ast = reader.readExpression();
		LexToken end = ltr.getLast();
		
		if (!end.is(Token.EOF))
		{
			throw new ParserException(2330, "Tokens found after expression at " + end, new LexLocation(), 0);
		}

		return ClassMapper.getInstance(TCNode.MAPPINGS).convert(ast);
	}

	private Value execute(INExpression inex) throws Exception
	{
		Context mainContext = new StateContext(
			defaultClass.name.getLocation(), "global static scope");

		mainContext.putAll(initialContext);
		mainContext.putAll(createdValues);
		mainContext.setThreadState(CPUValue.vCPU);
		clearBreakpointHits();

		// scheduler.reset();
		MainThread main = new MainThread(inex, mainContext);
		main.start();
		scheduler.start(main);

		return main.getResult();	// Can throw ContextException
	}

	/**
	 * Parse the line passed, type check it and evaluate it as an expression
	 * in the initial context.
	 *
	 * @param line A VDM expression.
	 * @return The value of the expression.
	 * @throws Exception Parser, type checking or runtime errors.
	 */
	@Override
	public Value execute(String line) throws Exception
	{
		TCExpression expr = parseExpression(line, getDefaultName());
		typeCheck(expr);
		INExpression inex = ClassMapper.getInstance(INNode.MAPPINGS).convert(expr);
		return execute(inex);
	}

	/**
	 * Parse the line passed, and evaluate it as an expression in the context
	 * passed.
	 *
	 * @param line A VDM expression.
	 * @param ctxt The context in which to evaluate the expression.
	 * @return The value of the expression.
	 * @throws Exception Parser or runtime errors.
	 */
	@Override
	public Value evaluate(String line, Context ctxt) throws Exception
	{
		TCExpression expr = parseExpression(line, getDefaultName());
//		Environment globals = getGlobalEnvironment();
//		Environment env = new PrivateClassEnvironment(defaultClass, globals);

		try
		{
			typeCheck(expr);
		}
		catch (VDMErrorsException e)
		{
			// We don't care... we just needed to type check it.
		}

		ctxt.threadState.init();
		INExpression inex = ClassMapper.getInstance(INNode.MAPPINGS).convert(expr);
		return inex.eval(ctxt);
	}

	@Override
	public INClassDefinition findClass(String classname)
	{
		TCNameToken name = new TCNameToken(null, "CLASS", classname);
		return executableClasses.findClass(name);
	}

	@Override
	protected INNamedTraceDefinition findTraceDefinition(TCNameToken name)
	{
		INDefinition d = executableClasses.findName(name);

		if (d == null || !(d instanceof INNamedTraceDefinition))
		{
			return null;
		}

		return (INNamedTraceDefinition)d;
	}

	@Override
	public Value findGlobal(TCNameToken name)
	{
		// The name will not be type-qualified, so we can't use the usual
		// findName methods

		for (INClassDefinition c: executableClasses)
		{
			for (INDefinition d: c.definitions)
			{
				if (d.isFunctionOrOperation())
				{
					NameValuePairList nvpl = d.getNamedValues(initialContext);

					for (NameValuePair n: nvpl)
					{
						if (n.name.matches(name))
						{
							return n.value;
						}
					}
				}
			}

			for (INDefinition d: c.localInheritedDefinitions)
			{
				if (d.isFunctionOrOperation())
				{
					NameValuePairList nvpl = d.getNamedValues(initialContext);

					for (NameValuePair n: nvpl)
					{
						if (n.name.matches(name))
						{
							return n.value;
						}
					}
				}
			}

			for (INDefinition d: c.superInheritedDefinitions)
			{
				if (d.isFunctionOrOperation())
				{
					NameValuePairList nvpl = d.getNamedValues(initialContext);

					for (NameValuePair n: nvpl)
					{
						if (n.name.matches(name))
						{
							return n.value;
						}
					}
				}
			}
		}

		return null;
	}

	@Override
	public INStatement findStatement(File file, int lineno)
	{
		return executableClasses.findStatement(file, lineno);
	}

	@Override
	public INExpression findExpression(File file, int lineno)
	{
		return executableClasses.findExpression(file, lineno);
	}

	public void create(String var, String exp) throws Exception
	{
		TCExpression expr = parseExpression(exp, getDefaultName());
		TCType type = typeCheck(expr);
		Value v = execute(exp);

		LexLocation location = defaultClass.location;
		TCNameToken name = new TCNameToken(location, defaultClass.name.getName(), var);

		createdValues.put(name, v);
		createdDefinitions.add(new TCLocalDefinition(location, name, type));
	}

	@Override
	public ProofObligationList getProofObligations() throws Exception
	{
		if (pogClasses == null)
		{
			long now = System.currentTimeMillis();
			pogClasses = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(checkedClasses);
			VDMJ.mapperStats(now, PONode.MAPPINGS);
		}
		
		POAnnotation.init();
		ProofObligationList list = pogClasses.getProofObligations();
		POAnnotation.close();
		return list;
	}

	private void logSwapIn()
	{
		// Show the "system constructor" thread creation

		RTLogger.log(
			"ThreadCreate -> id: " + Thread.currentThread().getId() +
			" period: false " +
			" objref: nil clnm: nil " +
			" cpunm: 0");

		RTLogger.log(
			"ThreadSwapIn -> id: " + Thread.currentThread().getId() +
			" objref: nil clnm: nil " +
			" cpunm: 0" +
			" overhead: 0");
	}

	private void logSwapOut()
	{
		RTLogger.log(
			"ThreadSwapOut -> id: " + Thread.currentThread().getId() +
			" objref: nil clnm: nil " +
			" cpunm: 0" +
			" overhead: 0");

		RTLogger.log(
			"ThreadKill -> id: " + Thread.currentThread().getId() +
			" cpunm: 0");
	}

	@Override
	protected Context getTraceContext(INClassDefinition classdef) throws ValueException
	{
		ObjectValue object = null;

		// Create a new test object
		object = classdef.newInstance(null, null, initialContext);

		Context ctxt = new ObjectContext(
				classdef.name.getLocation(), classdef.name.getName() + "()",
				initialContext, object);

		ctxt.put(classdef.name.getSelfName(), object);
		return ctxt;
	}

	@Override
	protected List<Object> runOneTrace(INClassDefinition classdef, CallSequence test, boolean debug)
	{
		List<Object> list = new Vector<Object>();
		Context ctxt = null;

		try
		{
			ctxt = getTraceContext(classdef);
		}
		catch (ValueException e)
		{
			list.add(e.getMessage());
			return list;
		}

		ctxt.setThreadState(CPUValue.vCPU);
		clearBreakpointHits();

		// scheduler.reset();
		CTMainThread main = new CTMainThread(test, ctxt, debug);
		main.start();
		scheduler.start(main);
		SchedulableThread.terminateAll();

		while (main.getRunState() != RunState.COMPLETE)
		{
			try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException e)
            {
                break;
            }
		}

		return main.getList();
	}

	public TCClassList getTC()
	{
		return checkedClasses;
	}

	public INClassList getIN()
	{
		return executableClasses;
	}

	public POClassList getPO()
	{
		return pogClasses;
	}
}
