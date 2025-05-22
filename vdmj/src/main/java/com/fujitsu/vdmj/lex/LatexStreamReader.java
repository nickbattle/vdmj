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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;

/**
 * A class to read LaTeX encoded VDM files.
 */
public class LatexStreamReader implements ExternalFormatReader
{
	private final static String BOM = "\uFEFF";

	public char[] getText(String expression) throws IOException
	{
		return readFile(new StringReader(expression));
	}

	@Override
	public char[] getText(File file, Charset encoding) throws IOException
	{
		return readFile(new InputStreamReader(new FileInputStream(file), encoding));
	}
	
	private char[] readFile(Reader reader) throws IOException
	{
		BufferedReader br = new BufferedReader(reader);
		String line = br.readLine();
		StringBuilder array = new StringBuilder();
		
		if (line != null && line.startsWith(BOM))
		{
			line = line.substring(1);
		}

		boolean supress = false;

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

			if (!supress)
			{
				array.append(line);
			}

			array.append('\n');
			line = br.readLine();
		}

		br.close();
		return array.toString().toCharArray();
	}
}
