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

import static com.fujitsu.vdmj.plugins.PluginConsole.errorln;
import static com.fujitsu.vdmj.plugins.PluginConsole.printf;
import static com.fujitsu.vdmj.plugins.PluginConsole.println;
import static com.fujitsu.vdmj.plugins.PluginConsole.verbose;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.fujitsu.vdmj.ast.lex.LexBooleanToken;
import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INBooleanLiteralExpression;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.expressions.INForAllExpression;
import com.fujitsu.vdmj.in.patterns.INBindingSetter;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.messages.Console;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.runtime.ContextException;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.util.GetResource;
import com.fujitsu.vdmj.values.BooleanValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueSet;

import quickcheck.qcplugins.QCPlugin;
import quickcheck.visitors.TypeBindFinder;

public class QuickCheck
{
	private int errorCount = 0;
	
	public boolean hasErrors()
	{
		return errorCount > 0;
	}
	
	public List<QCPlugin> getPlugins(List<String> argv)
	{
		List<QCPlugin> list = new Vector<QCPlugin>();
		errorCount = 0;
		
		try
		{
			List<String> plugins = GetResource.readResource("qc.plugins");
			
			for (String plugin: plugins)
			{
				try
				{
					Class<?> clazz = Class.forName(plugin);
					//ctor = clazz.getConstructor(String[].class);
					Constructor<?> ctor = clazz.getDeclaredConstructor(List.class);
					QCPlugin instance = (QCPlugin) ctor.newInstance((Object)argv);
					list.add(instance);
				}
				catch (ClassNotFoundException e)
				{
					errorln("Cannot load plugin: " + plugin);
					errorCount++;
				}
				catch (NoSuchMethodException e)
				{
					errorln("Plugin " + plugin + " must implement ctor(List<String> argv)");
					errorCount++;
				}
				catch (Throwable th)
				{
					errorln("Plugin " + plugin + ": " + th.toString());
					errorCount++;
				}
			}

			return errorCount == 0 ? list : null;
		}
		catch (Throwable e)
		{
			errorln("Cannot load plugins: " + e);
			return null;
		}
	}
	
	public ProofObligationList getPOs(ProofObligationList all, List<Integer> poList)
	{
		errorCount = 0;
		
		if (poList.isEmpty())
		{
			ProofObligationList list = new ProofObligationList();
			String def = Interpreter.getInstance().getDefaultName();
			
			for (ProofObligation po: all)
			{
				if (po.location.module.equals(def))
				{
					list.add(po);
				}
			}
			
			return list;	// No PO#s specified, so use default class/module's POs
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
					println("PO# " + n + " unknown. Must be between 1 and " + all.size());
					errorCount++;
				}
			}
			
			return errorCount > 0 ? null : list;
		}
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
	
	private List<INBindingSetter> getBindList(INExpression inexp)
	{
		return inexp.apply(new TypeBindFinder(), null);
	}
	
	public Map<String, ValueSet> getValues(List<QCPlugin> plugins, ProofObligation po)
	{
		Map<String, ValueSet> ranges = new HashMap<String, ValueSet>();
		
		for (QCPlugin plugin: plugins)
		{
			Map<String, ValueSet> values = plugin.getValues(po);
			
			for (String bind: values.keySet())
			{
				if (ranges.containsKey(bind))
				{
					ranges.get(bind).addAll(values.get(bind));
				}
				else
				{
					ranges.put(bind, values.get(bind));
				}
			}
		}
		
		return ranges;
	}
	
	public void checkObligation(ProofObligation po, Map<String, ValueSet> ranges)
	{
		try
		{
			errorCount = 0;
			RootContext ctxt = Interpreter.getInstance().getInitialContext();
			List<INBindingSetter> bindings = null;

			if (!po.isCheckable)
			{
				printf("PO #%d, UNCHECKED\n", po.number);
				return;
			}

			INExpression poexp = getPOExpression(po);
			bindings = getBindList(poexp);
			
			for (INBindingSetter mbind: bindings)
			{
				ValueSet values = ranges.get(mbind.toString());
				
				if (values != null)
				{
					verbose("PO #%d, setting %s, %d values", po.number, mbind.toString(), values.size());
					mbind.setBindValues(values);	// Unset in finally clause
				}
				else
				{
					println("PO #" + po.number + ": No range defined for " + mbind);
					errorCount++;
				}
			}
			
			try
			{
				long before = System.currentTimeMillis();
				verbose("PO #%d, starting...", po.number);
				Value result = poexp.eval(ctxt);
				long after = System.currentTimeMillis();
				
				if (result instanceof BooleanValue)
				{
					if (result.boolValue(ctxt))
					{
						printf("PO #%d, PASSED %s\n", po.number, duration(before, after));
					}
					else
					{
						printf("PO #%d, FAILED %s: ", po.number, duration(before, after));
						printFailPath(INForAllExpression.failPath, bindings);
						println("----");
						println(po);
						errorCount++;
					}
				}
				else
				{
					printf("PO #%d, Error: PO evaluation returns %s?\n", po.number, result.kind());
					println("----");
					printBindings(bindings);
					println("----");
					println(po);
					errorCount++;
				}
			}
			catch (ContextException e)
			{
				printf("PO #%d, Exception: %s\n", po.number, e.getMessage());
				
				if (e.ctxt.outer != null)
				{
					e.ctxt.printStackFrames(Console.out);
				}
				else
				{
					println("In context of " + e.ctxt.title + " " + e.ctxt.location);
				}
				
				println("----");
				printBindings(bindings);
				println("----");
				println(po);
				errorCount++;
			}
			catch (Exception e)
			{
				printf("PO #%d, Exception: %s\n", po.number, e.getMessage());
				println("----");
				printBindings(bindings);
				println("----");
				println(po);
				errorCount++;
			}
			finally
			{
				for (INBindingSetter mbind: bindings)
				{
					mbind.setBindValues(null);
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
	
	private void printBindings(List<INBindingSetter> bindings)
	{
		for (INBindingSetter bind: bindings)
		{
			printf("%s = %s\n", bind, bind.getBindValues());
		}
	}
	
	private void printFailPath(Context path, List<INBindingSetter> bindings)
	{
		if (path == null || path.isEmpty())
		{
			printf("No counterexample\n");
			printBindings(bindings);
			return;
		}
		
		printf("Counterexample: ");
		String sep = "";
		Context ctxt = path;
		
		while (true)
		{
			if (ctxt.outer != null)
			{
				if (ctxt instanceof RootContext)
				{
					sep = "; from " + ctxt.title + " where ";
				}
				
				for (TCNameToken name: ctxt.keySet())
				{
					printf("%s%s = %s", sep, name, ctxt.get(name));
					sep = ", ";
				}
				
				ctxt = ctxt.outer;
			}
			else
			{
				break;
			}
		}
		
		println("");
	}

	private String duration(long before, long after)
	{
		double duration = (double)(after - before)/1000;
		return "in " + duration + "s";
	}
}
