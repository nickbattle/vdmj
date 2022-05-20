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

public class RTValidator
{
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
			int errors = 0;
			
			while (line != null)
			{
				Map<String, String> record = parse(line);
				
				if (!validate(record))
				{
					errors++;
				}
				
				line = br.readLine();
			}
			
			if (errors > 0)
			{
				writeViolations(new File(logfile.getAbsolutePath() + ".violations"));
			}
			
			return errors;
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

	private static Map<String, String> parse(String line)
	{
		Map<String, String> map = new LinkedHashMap<String, String>();
		Matcher m = TYPE.matcher(line);
		
		while (m.find())
		{
			map.put("kind", m.group(1));
		}
		
		m = ITEM.matcher(line);
		
		while (m.find())
		{
			map.put(m.group(1), m.group(2));
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

	private static boolean validate(Map<String, String> record)
	{
		boolean result = true;
		
		for (INAnnotation annotation: conjectures)
		{
			ConjectureProcessor processor = (ConjectureProcessor) annotation;
			result = result && processor.process(record);
		}
		
		return result;
	}

	private static void writeViolations(File violations) throws IOException
	{
		for (INAnnotation annotation: conjectures)
		{
			ConjectureProcessor processor = (ConjectureProcessor) annotation;
			processor.processComplete(violations);
		}
	}
}
