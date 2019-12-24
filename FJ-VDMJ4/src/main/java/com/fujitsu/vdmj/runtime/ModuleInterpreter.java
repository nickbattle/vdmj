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

import com.fujitsu.vdmj.VDMJ;
import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.ast.lex.LexToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INClassDefinition;
import com.fujitsu.vdmj.in.definitions.INNamedTraceDefinition;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.modules.INModule;
import com.fujitsu.vdmj.in.modules.INModuleList;
import com.fujitsu.vdmj.in.statements.INStatement;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.lex.Token;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.messages.VDMErrorsException;
import com.fujitsu.vdmj.po.PONode;
import com.fujitsu.vdmj.po.annotations.POAnnotation;
import com.fujitsu.vdmj.po.modules.POModuleList;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.scheduler.CTMainThread;
import com.fujitsu.vdmj.scheduler.MainThread;
import com.fujitsu.vdmj.syntax.ExpressionReader;
import com.fujitsu.vdmj.syntax.ParserException;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.TCRecursiveLoops;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCIdentifierToken;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.traces.CallSequence;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.Value;

/**
 * The VDM-SL module interpreter.
 */
public class ModuleInterpreter extends Interpreter
{
	/** A list of executable module definitions in the specification. */
	private final INModuleList executableModules;
	/** A list of executable module definitions in the specification. */
	private final TCModuleList checkedModules;

	/** The module starting execution. */
	public INModule defaultModule;
	/** The default module's environment */
	private Environment defaultEnvironment;
	/** The PO analysis tree */
	private POModuleList pogModules;

	/**
	 * Create an Interpreter from the list of executableModules passed.
	 *
	 * @param executableModules
	 * @throws Exception
	 */
	public ModuleInterpreter(INModuleList executableModules, TCModuleList checkedModules) throws Exception
	{
		super();
		
		this.executableModules = executableModules;
		this.checkedModules = checkedModules;

		if (executableModules.isEmpty())
		{
			setDefaultName(null);
		}
		else
		{
			setDefaultName(executableModules.get(0).name.getName());
		}
	}

	/**
	 * Set the default module to the name given.
	 *
	 * @param mname The name of the new default module.
	 * @throws Exception The module name is not known.
	 */
	@Override
	public void setDefaultName(String mname) throws Exception
	{
		if (mname == null)
		{
			defaultModule = new INModule();
			executableModules.add(defaultModule);
			checkedModules.add(new TCModule());
			defaultEnvironment = new ModuleEnvironment(checkedModules.get(0));
		}
		else
		{
			for (INModule m: executableModules)
			{
				if (m.name.getName().equals(mname))
				{
					defaultModule = m;
					break;
				}
			}

			for (TCModule m: checkedModules)
			{
				if (m.name.getName().equals(mname))
				{
					defaultEnvironment = new ModuleEnvironment(m);
					return;
				}
			}

			throw new Exception("Module " + mname + " not loaded");
		}
	}
	
	/**
	 * Note that this is changed by setDefaultName above.
	 */
	@Override
	public Environment getGlobalEnvironment()
	{
		return defaultEnvironment;
	}

	/**
	 * @return The current default module name.
	 */
	@Override
	public String getDefaultName()
	{
		return defaultModule.name.getName();
	}

	/**
	 * @return The current default module's file name.
	 */
	@Override
	public File getDefaultFile()
	{
		return defaultModule.name.getLocation().file;
	}

	@Override
	public Set<File> getSourceFiles()
	{
		return executableModules.getSourceFiles();
	}

	/**
	 * @return The state context for the current default module.
	 */
	public Context getStateContext()
	{
		return defaultModule.getStateContext();
	}

	/**
	 * @return The list of loaded executableModules.
	 */
	public INModuleList getModules()
	{
		return executableModules;
	}

	@Override
	public void init()
	{
		scheduler.init();
		CPUValue.init(scheduler);
		initialContext = executableModules.creatInitialContext();
		executableModules.initialize(initialContext);
		INAnnotation.init(initialContext);
	}

	@Override
	public void traceInit()
	{
		scheduler.reset();
		initialContext = executableModules.creatInitialContext();
		executableModules.initialize(initialContext);
	}

	@Override
	protected TCExpression parseExpression(String line, String module) throws Exception
	{
		LexTokenReader ltr = new LexTokenReader(line, Dialect.VDM_SL, Console.charset);
		ExpressionReader reader = new ExpressionReader(ltr);
		reader.setCurrentModule(getDefaultName());
		ASTExpression ast = reader.readExpression();
		LexToken end = ltr.getLast();
		
		if (!end.is(Token.EOF))
		{
			throw new ParserException(2330, "Tokens found after expression at " + end, new LexLocation(), 0);
		}
		
		return ClassMapper.getInstance(TCNode.MAPPINGS).convert(ast);
	}

	/**
	 * Parse the line passed, type check it and evaluate it as an expression
	 * in the initial module context (with default module's state).
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

		Context mainContext = new StateContext(defaultModule.name.getLocation(),
				"module scope",	null, defaultModule.getStateContext());

		mainContext.putAll(initialContext);
		mainContext.setThreadState(null);
		clearBreakpointHits();

		// scheduler.reset();
		INExpression inex = ClassMapper.getInstance(INNode.MAPPINGS).convert(expr);
		MainThread main = new MainThread(inex, mainContext);
		main.start();
		scheduler.start(main);

		return main.getResult();	// Can throw ContextException
	}

	/**
	 * Parse the line passed, and evaluate it as an expression in the context
	 * passed. This is called from the debugger.
	 *
	 * @param line A VDM expression.
	 * @param ctxt The context in which to evaluate the expression.
	 * @return The value of the expression.
	 * @throws Exception Parser or runtime errors.
	 */
	@Override
	public Value evaluate(String line, Context ctxt) throws Exception
	{
		TCExpression tc = null;
		
		try
		{
			tc = parseExpression(line, getDefaultName());
			typeCheck(tc);
		}
		catch (VDMErrorsException e)
		{
			// We don't care... we just needed to type check it.
		}

		ctxt.threadState.init();
		INExpression inex = ClassMapper.getInstance(INNode.MAPPINGS).convert(tc);
		return inex.eval(ctxt);
	}

	@Override
	public INModule findModule(String module)
	{
		TCIdentifierToken name = new TCIdentifierToken(null, module, false);
		return executableModules.findModule(name);
	}

	@Override
	protected INNamedTraceDefinition findTraceDefinition(TCNameToken name)
	{
		return executableModules.findTraceDefinition(name);
	}

	/**
	 * Find a ASTStatement in the given file that starts on the given line.
	 * If there are none, return null.
	 *
	 * @param file The file name to search.
	 * @param lineno The line number in the file.
	 * @return A ASTStatement starting on the line, or null.
	 */
	@Override
	public INStatement findStatement(File file, int lineno)
	{
		return executableModules.findStatement(file, lineno);
	}

	/**
	 * Find an ASTExpression in the given file that starts on the given line.
	 * If there are none, return null.
	 *
	 * @param file The file name to search.
	 * @param lineno The line number in the file.
	 * @return An ASTExpression starting on the line, or null.
	 */
	@Override
	public INExpression findExpression(File file, int lineno)
	{
		return executableModules.findExpression(file, lineno);
	}

	@Override
	public ProofObligationList getProofObligations() throws Exception
	{
		if (pogModules == null)
		{
			long now = System.currentTimeMillis();
			pogModules = ClassMapper.getInstance(PONode.MAPPINGS).init().convert(checkedModules);
			ClassMapper.getInstance(PONode.MAPPINGS).convert(TCRecursiveLoops.getInstance());
			VDMJ.mapperStats(now, PONode.MAPPINGS);
		}
		
		POAnnotation.init();
		return pogModules.getProofObligations();
	}

	@Override
	protected Context getTraceContext(INClassDefinition classdef) throws ValueException
	{
		Context mainContext = new StateContext(defaultModule.name.getLocation(),
				"module scope",	null, defaultModule.getStateContext());

		mainContext.putAll(initialContext);
		mainContext.setThreadState(CPUValue.vCPU);
		
		return mainContext;
	}

	@Override
	protected List<Object> runOneTrace(INClassDefinition classdef, CallSequence test, boolean debug)
	{
		clearBreakpointHits();

		// scheduler.reset();
		CTMainThread main = new CTMainThread(test, initialContext, debug);
		main.start();
		scheduler.start(main);

		return main.getList();
	}

	public TCModuleList getTC()
	{
		return checkedModules;
	}

	public INModuleList getIN()
	{
		return executableModules;
	}

	public POModuleList getPO()
	{
		return pogModules;
	}
}
