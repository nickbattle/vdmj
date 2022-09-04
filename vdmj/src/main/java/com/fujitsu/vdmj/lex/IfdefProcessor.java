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

package com.fujitsu.vdmj.lex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Stack;

import com.fujitsu.vdmj.config.Properties;

/**
 * A class to process #ifdef #else #endif directives.
 */
public class IfdefProcessor
{
	private Stack<Boolean> ifstack = new Stack<Boolean>();
	
	public char[] getText(char[] source) throws IOException
	{
		String text = new String(source);
		BufferedReader br = new BufferedReader(new StringReader(text));
		String line = br.readLine();
		StringBuilder output = new StringBuilder();
		
		boolean supress = false;

		while (line != null)
		{
			String trimmed = line.trim();

			if (trimmed.startsWith("#"))
			{
   				String[] parts = trimmed.split("\\s+");

    			if (parts[0].equals("#ifdef") && parts.length == 2)
    			{
    				String label = parts[1];
    				ifstack.push(supress);

    				if (!supress && System.getProperty(label) == null)
    				{
    					supress = true;
    				}

    				line = "";
    			}
    			else if (parts[0].equals("#ifndef") && parts.length == 2)
    			{
    				String label = parts[1];
    				ifstack.push(supress);

    				if (!supress && System.getProperty(label) != null)
    				{
    					supress = true;
    				}

    				line = "";
    			}
    			else if (parts[0].equals("#else") && parts.length == 1 && !ifstack.isEmpty())
    			{
    				if (!ifstack.peek())
    				{
    					supress = !supress;
    					line = "";
    				}
    			}
    			else if (parts[0].equals("#endif") && parts.length == 1 && !ifstack.isEmpty())
    			{
    				supress = ifstack.pop();
    				line = "";
    			}
    			else if (parts[0].equals("#define") && parts.length >= 3)
    			{
    				if (!supress)
    				{
    					String name = parts[1];
    					int pos = trimmed.indexOf(name) + name.length();	// eg. #define X "abc def"
    					String value = trimmed.substring(pos).trim();
    					System.setProperty(name, value);
    					if (name.startsWith("vdmj.")) Properties.init();
    				}
    				
    				line = "";
    			}
    			else if (parts[0].equals("#undef") && parts.length == 2)
    			{
       				if (!supress)
    				{
    					String name = parts[1];
    					System.clearProperty(name);
    					if (name.startsWith("vdmj.")) Properties.init();
    				}
       				
       				line = "";
    			}
			}

			if (!supress)
			{
				output.append(line);
			}

			output.append('\n');	// Maintain line numbers
			line = br.readLine();
		}

		br.close();
		return output.toString().toCharArray();
	}
}
