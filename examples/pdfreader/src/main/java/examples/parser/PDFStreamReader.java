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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package examples.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.fujitsu.vdmj.lex.ExternalFormatReader;

/**
 * Invoke an external pdftotext command to read a PDF file.
 */
public class PDFStreamReader implements ExternalFormatReader
{
	@Override
	public char[] getText(File file, Charset charset) throws IOException
	{
		try
		{
			Process p = Runtime.getRuntime().exec("pdftotext -layout " + file + " -");
			BufferedReader is = new BufferedReader(new InputStreamReader(p.getInputStream(), charset));
			boolean inVDM = false;
			StringBuilder sb = new StringBuilder();
			
			for (String line = is.readLine(); line != null; line = is.readLine())
			{
				if (line.trim().equals("%%VDM%%"))
				{
					inVDM = !inVDM;
				}
				else if (inVDM)
				{
					sb.append(line);
					sb.append('\n');
				}
			}
			
			is.close();
			return sb.toString().toCharArray();
		}
		catch (Throwable th)
		{
			System.err.println(th);
			return null;
		}
	}
}
