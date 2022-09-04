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
    			if (trimmed.startsWith("#ifdef"))
    			{
    				String label = trimmed.substring(6).trim();
    				ifstack.push(supress);

    				if (!supress && System.getProperty(label) == null)
    				{
    					supress = true;
    				}

    				line = "";
    			}
    			else if (trimmed.startsWith("#ifndef"))
    			{
    				String label = trimmed.substring(7).trim();
    				ifstack.push(supress);

    				if (!supress && System.getProperty(label) != null)
    				{
    					supress = true;
    				}

    				line = "";
    			}
    			else if (trimmed.startsWith("#else") && !ifstack.isEmpty())
    			{
    				if (!ifstack.peek())
    				{
    					supress = !supress;
    					line = "";
    				}
    			}
    			else if (trimmed.startsWith("#endif") && !ifstack.isEmpty())
    			{
    				supress = ifstack.pop();
    				line = "";
    			}
    			else if (trimmed.startsWith("#define"))
    			{
    				String[] parts = trimmed.split("\\s+");
    				
    				if (parts[0].equals("#define") && parts.length == 3 && !supress)
    				{
    					String name = parts[1];
    					String value = parts[2];
    					System.setProperty(name, value);
    					Properties.init();
    				}
    				
    				line = "";
    			}
    			else if (trimmed.startsWith("#undef"))
    			{
    				String[] parts = trimmed.split("\\s+");
    				
       				if (parts[0].equals("#undef") && parts.length == 2 && !supress)
    				{
    					String name = parts[1];
    					System.clearProperty(name);
    					Properties.init();
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
