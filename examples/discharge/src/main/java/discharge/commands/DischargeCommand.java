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

import static com.fujitsu.vdmj.plugins.PluginConsole.println;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.NaturalOneValue;
import com.fujitsu.vdmj.values.NaturalValue;
import com.fujitsu.vdmj.values.SetValue;
import com.fujitsu.vdmj.values.Value;
import com.fujitsu.vdmj.values.ValueList;

import discharge.visitors.TypeBindFinder;

public class DischargeCommand extends AnalysisCommand
{
	private final static String USAGE = "Usage: discharge <PO#> <ranges file>";
	
	private static final int RANGE = 1000;
	private ValueList natValues;
	private ValueList nat1Values;
	private ValueList intValues;
	
	public DischargeCommand(String line)
	{
		super(line);
		
		if (!argv[0].equals("discharge"))
		{
			throw new IllegalArgumentException(USAGE);
		}
		
		setTypeRanges();
	}

	@Override
	public void run()
	{
		if (argv.length != 3)
		{
			println(USAGE);
			return;
		}
		
		int number = 0;
		
		try
		{
			number = Integer.parseInt(argv[1]);
		}
		catch (NumberFormatException e)
		{
			println(USAGE);
			return;
		}
		
		POPlugin plugin = registry.getPlugin("PO");
		ProofObligationList all = plugin.getProofObligations();

		if (number < 1 || number > all.size())
		{
			if (all.size() == 1)
			{
				println("PO# can only be 1");
			}
			else
			{
				println("PO# must be between 1 and " + all.size());
			}
			return;
		}

		Map<String, ValueList> ranges = null;

		try
		{
			ranges = readRanges(argv[2]);
		}
		catch (IOException e)
		{
			println("Cannot read ranges file: " + e.getMessage());
			return;
		}
		
		try
		{
			ProofObligation po = all.get(number - 1);
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
					println("No ranges defined for " + mbind);
					return;
				}
			}
			
			Interpreter i = Interpreter.getInstance();
			RootContext ctxt = i.getInitialContext();
			println("Result = " + inexp.eval(ctxt));
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
					println("Ignoring range: " + e.getMessage());
				}
			}
		}
		
		reader.close();
		return ranges;
	}

	private void setTypeRanges()
	{
		try
		{
			natValues = new ValueList(RANGE);
			
			for (int nat = 0; nat < RANGE; nat++)
			{
				natValues.add(new NaturalValue(nat));
			}
			
			nat1Values = new ValueList(RANGE);
			
			for (int nat = 1; nat < RANGE; nat++)
			{
				nat1Values.add(new NaturalOneValue(nat));
			}
			
			intValues = new ValueList(RANGE);
			
			for (int nat = -RANGE; nat < RANGE; nat++)
			{
				intValues.add(new IntegerValue(nat));
			}
		}
		catch (Exception e)
		{
			// Can't happen
		}
	}
	
	public static void help()
	{
		println("discharge <PO#> <ranges file> - attempt to brute force discharge PO");
	}
}
