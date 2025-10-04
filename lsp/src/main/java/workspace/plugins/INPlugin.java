/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace.plugins;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.values.Value;

import rpc.RPCMessageList;
import vdmj.commands.AnalysisCommand;
import vdmj.commands.DefaultCommand;
import vdmj.commands.HelpCommand;
import vdmj.commands.InitCommand;
import vdmj.commands.PluginsCommand;
import vdmj.commands.PrintCommand;
import vdmj.commands.QuitCommand;
import vdmj.commands.RestartCommand;
import vdmj.commands.ScriptCommand;
import vdmj.commands.SetCommand;
import vdmj.commands.VersionCommand;
import workspace.Diag;
import workspace.EventListener;
import workspace.PluginRegistry;
import workspace.events.CheckCompleteEvent;
import workspace.events.CheckPrepareEvent;
import workspace.events.LSPEvent;

abstract public class INPlugin extends AnalysisPlugin implements EventListener
{
	public static INPlugin factory(Dialect dialect)
	{
		switch (dialect)
		{
			case VDM_SL:
				return new INPluginSL();
				
			case VDM_PP:
			case VDM_RT:
				return new INPluginPR();
				
			default:
				Diag.error("Unsupported dialect " + dialect);
				throw new IllegalArgumentException("Unsupported dialect: " + dialect);
		}
	}

	protected INPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "IN";
	}
	
	@Override
	public int getPriority()
	{
		return IN_PRIORITY;
	}

	@Override
	public void init()
	{
		eventhub.register(CheckPrepareEvent.class, this);
		eventhub.register(CheckCompleteEvent.class, this);
	}

	@Override
	public RPCMessageList handleEvent(LSPEvent event) throws Exception
	{
		if (event instanceof CheckPrepareEvent)
		{
			preCheck((CheckPrepareEvent) event);
		}
		else if (event instanceof CheckCompleteEvent)
		{
			TCPlugin tc = registry.getPlugin("TC");
			checkLoadedFiles(tc.getTC());
		}
		else
		{
			Diag.error("Unhandled %s event %s", getName(), event);
		}

		return null;
	}

	abstract protected void preCheck(CheckPrepareEvent event);
	
	@Override
	public AnalysisCommand getCommand(String line)
	{
		String[] parts = line.split("\\s+");
		
		switch (parts[0])
		{
			case "default":		return new DefaultCommand(line);
			case "print":
			case "p":			return new PrintCommand(line);
			case "set":			return new SetCommand(line);
			case "init":		return new InitCommand(line);
			case "plugins":		return new PluginsCommand(line);
			case "script":		return new ScriptCommand(line);
			case "help":
			case "?":			return new HelpCommand(line);
			case "version":		return new VersionCommand(line);
			case "reload":
			case "restart":		return new RestartCommand(line);
			case "quit":
			case "q":			return new QuitCommand(line);

			default:
				return null;
		}
	}
	
	@Override
	public HelpList getCommandHelp()
	{
		return new HelpList
		(
			DefaultCommand.HELP,
			PrintCommand.HELP,
			SetCommand.HELP,
			InitCommand.HELP,
			PluginsCommand.HELP,
			ScriptCommand.HELP,
			QuitCommand.HELP,
			HelpCommand.HELP,
			VersionCommand.HELP
			// Restart and reload hidden, because it's just advice
		);
	}

	/**
	 * Evaluate simple expressions.
	 */
	public Value evaluate(String expression) throws Exception
	{
		Interpreter interpreter = Interpreter.getInstance();
		String module = interpreter.getDefaultName();
		Environment env = interpreter.getGlobalEnvironment();
		
		ASTPlugin ast = PluginRegistry.getInstance().getPlugin("AST");
		ASTExpression parsed = ast.parseExpression(expression, module);
		
		TCPlugin tc = PluginRegistry.getInstance().getPlugin("TC");
		TCExpression checked = tc.checkExpression(parsed, env);
		
		INExpression exec = ClassMapper.getInstance(INNode.MAPPINGS).convertLocal(checked);
		
		return exec.eval(interpreter.getInitialContext());
	}
	
	/**
	 * Event handling above. Supporting methods below. 
	 */
	
	abstract public <T extends Mappable> T getIN();
	
	abstract public <T extends Mappable> boolean checkLoadedFiles(T tcList) throws Exception;
	
	abstract public <T extends Mappable> Interpreter getInterpreter() throws Exception;

	abstract public INDefinitionList findDefinition(TCNameToken lnt);
}
