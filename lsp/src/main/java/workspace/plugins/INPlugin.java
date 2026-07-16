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

import java.io.File;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import com.fujitsu.vdmj.ast.expressions.ASTExpression;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.definitions.INDefinitionList;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.mapper.Mappable;
import com.fujitsu.vdmj.plugins.HelpList;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.typechecker.Environment;
import com.fujitsu.vdmj.util.DependencyOrder;
import com.fujitsu.vdmj.values.Value;

import lsp.lspx.OrderHandler;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import vdmj.commands.AnalysisCommand;
import vdmj.commands.DefaultCommand;
import vdmj.commands.HelpCommand;
import vdmj.commands.InitCommand;
import vdmj.commands.OrderCommand;
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

		lspDispatcher.register(new OrderHandler(), "slsp/ordering");
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
			case "order":		return new OrderCommand(line);
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
			OrderCommand.HELP,
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

	abstract public INDefinitionList findDefinition(String name);

	abstract public RPCMessageList getOrder(RPCRequest request);


	/**
	 * Extend the VDMJ DependencyOrder so that we can add extract filenames.
	 */
	protected static class Order extends DependencyOrder
	{
		List<String> filenames = new Vector<String>();

		public Order()
		{
			// Nothing
		}

		protected List<String> getOrder()
		{
			return filenames;
		}

		@Override
		public void moduleOrder(TCModuleList moduleList)
		{
			super.moduleOrder(moduleList);
			processGraph();
		}

		@Override
		public void classOrder(TCClassList classList)
		{
			super.classOrder(classList);
			processGraph();
		}
		
	    private void processGraph()
	    {
			/**
			 * First remove any cycles. For some reason it's not enough to search from
			 * the startpoints, so we just search from everywhere. It's reasonably
			 * quick.
			 */
			for (String start: nameToFile.keySet())
			{
				removeCycles(start, new Stack<String>());
			}
	
			/*
			 * The startpoints are where there are no incoming links to a node. So
			 * the usedBy entry is blank (removed cycles) or null.
			 */
			List<String> startpoints = getStartpoints();
			List<String> ordering = topologicalSort(startpoints);

			LSPPlugin lsp = PluginRegistry.getInstance().getPlugin("LSP");
			Path root = lsp.getRoot().toPath();

			for (String name: ordering)
			{
				for (String module: nameToFile.keySet())
				{
					if (module.equals(name))
					{
						File file = nameToFile.get(module);
						String relative = root.relativize(file.toPath()).toString();

						if (!filenames.contains(relative))	// files with >= two modules
						{
							filenames.add(relative);
						}
						break;
					}
				}
			}
	    }
	
		private int removeCycles(String start, Stack<String> stack)
		{
	    	int count = 0;
	    	Set<String> nextSet = new HashSet<String>(uses.get(start));
	    	
	    	if (!nextSet.isEmpty())
	    	{
		    	stack.push(start);
		    	
		    	for (String next: nextSet)
		    	{
		    		if (stack.contains(next))
		    		{
		    			delete(start, next);
		    			count = count + 1;
		    		}
		    		else
		    		{
		    			count += removeCycles(next, stack);
		    		}
		    	}
		    	
		    	stack.pop();
	    	}
	    	
	    	return count;
		}
	}
}
