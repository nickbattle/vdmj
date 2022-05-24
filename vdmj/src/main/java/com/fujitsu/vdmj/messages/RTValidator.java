/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle.
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

package com.fujitsu.vdmj.messages;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fujitsu.vdmj.in.annotations.INAnnotation;
import com.fujitsu.vdmj.in.definitions.INSystemDefinition;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.runtime.Context;
import com.fujitsu.vdmj.tc.lex.TCNameToken;
import com.fujitsu.vdmj.values.CPUValue;
import com.fujitsu.vdmj.values.IntegerValue;
import com.fujitsu.vdmj.values.ObjectValue;
import com.fujitsu.vdmj.values.SeqValue;

public class RTValidator
{
	public static final String KIND = "_kind_";
	public static final String HISTORY = "_history_";
	
	private static final Pattern TYPE = Pattern.compile("(\\w+) ->");
	private static final Pattern ITEM = Pattern.compile(" (\\w+): ((\"[^\"]+\")|(\\w+))");
	private static List<INAnnotation> conjectures = null;
	
	public static int validate(File logfile) throws IOException
	{
		if (!loadConjectures())
		{
			throw new IOException("No conjectures found in specification");
		}
		
		BufferedReader br = null;
		
		try
		{
			br = new BufferedReader(new FileReader(logfile));
			String line = br.readLine();
			Context ctxt = new Context(LexLocation.ANY, "Conjecture context", null);
			ctxt.setThreadState(CPUValue.vCPU);

			ObjectValue sysObject = INSystemDefinition.getSystemObject();
			String systemName = sysObject == null ? "SYS" : sysObject.classdef.name.getName();
			
			while (line != null)
			{
				Map<String, String> record = parse(line);
				
				if (record.get(KIND).equals("InstVarChange"))
				{
					String name = record.get("instnm").replace("\"", "");
					String value = record.get("val").replace("\"", "");
					
					// InstVarChange -> instnm: "counts((A`interval + 1))" val: "1" objref: 1 id: 15 time: 255
					// InstVarChange -> instnm: "last" val: "227" objref: 1 id: 15 time: 259
					
					TCNameToken tcname = new TCNameToken(LexLocation.ANY, systemName, name);
					
					try
					{
						long v = Long.parseLong(value);
						ctxt.put(tcname, new IntegerValue(v));
					}
					catch (NumberFormatException e)
					{
						ctxt.put(tcname, new SeqValue(value));
					}
				}
				else
				{
					validate(record, ctxt);
				}
				
				line = br.readLine();
			}
			
			return writeViolations(new File(logfile.getAbsolutePath() + ".violations"));
		}
		finally
		{
			if (br != null)
			{
				try
				{
					br.close();
				}
				catch (IOException e)
				{
					// ignore
				}
			}
		}
	}

	private static Map<String, String> parse(String line) throws IOException
	{
		Map<String, String> map = new LinkedHashMap<String, String>();
		Matcher m = TYPE.matcher(line);
		String rtype = null;
		
		if (m.find())
		{
			map.put(KIND, m.group(1));
			rtype = m.group(1);
		}
		else
		{
			throw new IOException("Malformed log record: no record type");
		}
		
		m = ITEM.matcher(line);
		
		while (m.find())
		{
			map.put(m.group(1), m.group(2));
		}

		String opname = map.get("opname");	// eg. "A`op(nat, nat)"
		
		if (opname != null)
		{
			opname = opname.substring(1);
			opname = opname.replaceFirst("\\(.*$", "");		// Remove types and quotes
		}

		switch (rtype)
		{
			case "OpRequest":
				map.put(HISTORY, "#req(" + opname + ")");
				break;
				
			case "OpActivate":
				map.put(HISTORY, "#act(" + opname + ")");
				break;
				
			case "OpCompleted":
				map.put(HISTORY, "#fin(" + opname + ")");
				break;
		}
		
		return map;
	}

	private static boolean loadConjectures()
	{
		conjectures = INAnnotation.getInstances(ConjectureProcessor.class);
	
		for (INAnnotation annotation: conjectures)
		{
			ConjectureProcessor processor = (ConjectureProcessor) annotation;
			processor.processReset();
		}
		
		return !conjectures.isEmpty();
	}

	private static boolean validate(Map<String, String> record, Context ctxt)
	{
		boolean result = true;
		
		for (INAnnotation annotation: conjectures)
		{
			ConjectureProcessor processor = (ConjectureProcessor) annotation;
			result = result && processor.process(record, ctxt);
		}
		
		return result;
	}

	private static int writeViolations(File violations) throws IOException
	{
		int count = 0;
		violations.delete();
		
		for (INAnnotation annotation: conjectures)
		{
			ConjectureProcessor processor = (ConjectureProcessor) annotation;
			count += processor.processComplete(violations);
		}
		
		return count;
	}
}
