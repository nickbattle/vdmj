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

package discharge.commands;

import static com.fujitsu.vdmj.plugins.PluginConsole.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.in.INNode;
import com.fujitsu.vdmj.in.expressions.INExpression;
import com.fujitsu.vdmj.in.patterns.INMultipleTypeBind;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.plugins.AnalysisCommand;
import com.fujitsu.vdmj.plugins.analyses.POPlugin;
import com.fujitsu.vdmj.pog.ProofObligation;
import com.fujitsu.vdmj.pog.ProofObligationList;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.RootContext;
import com.fujitsu.vdmj.tc.expressions.TCExpression;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import discharge.visitors.TypeBindFinder;

public class DischargeCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: discharge <ranges file> [<PO numbers>]";
	
	public DischargeCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("discharge"))
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

		Map<String, ValueList> ranges = null;

		try
		{
			ranges = readRanges(argv[1]);
		}
		catch (IOException e)
		{
			println("Cannot read ranges file: " + e.getMessage());
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
	
	private Map<String, ValueList> readRanges(String file) throws IOException
	{
		Map<String, ValueList> ranges = new HashMap<String, ValueList>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		Pattern rangeLine = Pattern.compile("^\\s*([^=]+?)\\s*=\\s*(.*)$");
		Interpreter i = Interpreter.getInstance();
		RootContext ctxt = i.getInitialContext();
		
		for (String line = reader.readLine(); line != null; line = reader.readLine())
		{
			Matcher m = rangeLine.matcher(line);
			
			if (m.matches())
			{
				try
				{
					Value v = i.evaluate(m.group(2), ctxt);
					SetValue sv = (SetValue)v;
					ValueList vl = new ValueList();
					vl.addAll(sv.values);
					ranges.put(m.group(1), vl);
				}
				catch (Exception e)
				{
					println("Ignoring range: " + m.group(2));
					println("Range error: " + e.getMessage());
				}
			}
		}
		
		reader.close();
		return ranges;
	}
	
	public static void help()
	{
		println("discharge <ranges file> [<PO#s>] - attempt to brute force discharge POs");
	}
}
