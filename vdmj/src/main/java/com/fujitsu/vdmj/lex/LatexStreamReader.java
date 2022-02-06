/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Stack;

/**
 * A class to read LaTeX encoded VDM files.
 */
public class LatexStreamReader extends InputStreamReader
{
	private final static String BOM = "\uFEFF";

	private Stack<Boolean> ifstack = new Stack<Boolean>();

	public LatexStreamReader(InputStream in, String charsetName)
		throws UnsupportedEncodingException
	{
		super(in, charsetName);
	}

	@Override
	public int read(char[] array) throws IOException
	{
		BufferedReader br = new BufferedReader(this);
		String line = br.readLine();
		
		if (line != null && line.startsWith(BOM))
		{
			line = line.substring(1);
		}

		boolean supress = false;
		int pos = 0;

		while (line != null)
		{
			String trimmed = line.trim();

			if (trimmed.startsWith("%"))
 			{
 				supress = true;
 				line = "";
 			}
			else if (trimmed.startsWith("\\"))
			{
    			if (trimmed.startsWith("\\begin{vdm_al}"))
    			{
    				supress = false;
    				line = "";
    			}
    			else if (trimmed.startsWith("\\end{vdm_al}") ||
    					 trimmed.startsWith("\\section") ||
    					 trimmed.startsWith("\\subsection") ||
    					 trimmed.startsWith("\\subsubsection") ||
    					 trimmed.startsWith("\\document"))
    			{
    				supress = true;
    				line = "";
    			}
			}
			else if (trimmed.startsWith("#"))
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
			}

			if (!supress)
			{
				line.getChars(0, line.length(), array, pos);
				pos += line.length();
			}

			array[pos++] = '\n';
			line = br.readLine();
		}

		br.close();
		return pos;
	}
}
