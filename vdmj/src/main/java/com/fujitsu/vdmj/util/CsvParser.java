/*******************************************************************************
 *
 *	Copyright (c) 2016 Aarhus University.
 *
 *	Author: Nick Battle and others
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

package com.fujitsu.vdmj.util;

import java.util.LinkedList;
import java.util.List;

import com.fujitsu.vdmj.values.Value;

public class CsvParser
{
	public static final String INVALID_CSV_MSG = "Invalid CSV data: cannot parse null";
	private CsvValueBuilder builder;
	
	public CsvParser(CsvValueBuilder builder)
	{
		this.builder = builder;
	}
	
	public CsvResult parseValues(String line)
	{
		List<Value> values = new LinkedList<>();
		
		if(line == null)
		{
			return new CsvResult(values, INVALID_CSV_MSG);
		}
		
		if(line.isEmpty())
		{
			return new CsvResult(values);
		}
		
		String lastError = null;
		int last = 0;
		for(int i = 0; i < line.length(); i++)
		{
			char c = line.charAt(i);
			
			if(c == ',' || i == line.length() - 1)
			{
				String cell;
				
				if(c == ',')
				{
					cell = line.substring(last,i);
				}
				else
				{
					cell = line.substring(last, line.length());
				}
				
				try
				{
					Value v = builder.createValue(cell);
					values.add(v);
					last = i + 1;
				}
				catch(Exception e)
				{
					// Proceed to next comma and try to parse value again
					// Happens for values such as {1,2},{3,4}
					lastError = e.getMessage();
				}
			}
		}
		
		if(last == line.length())
		{
			return new CsvResult(values);
		}
		else
		{
			return new CsvResult(values, lastError);
		}
	}
}
