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

package examples.csvreader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Date;

import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.ExternalFormatReader;
import com.fujitsu.vdmj.lex.LexTokenReader;
import com.fujitsu.vdmj.messages.LocatedException;
import com.fujitsu.vdmj.syntax.ExpressionReader;

public class CSVReader implements ExternalFormatReader
{
	private static final String INDENT = "    ";

	@Override
	public char[] getText(File file, Charset charset) throws IOException
	{
		StringBuilder vdm = headers(file);
		vdm.append(INDENT);
		vdm.append("[");
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset));
		String sep = "";
		
		for (String line = reader.readLine(); line != null; line = reader.readLine())
		{
			vdm.append(sep);
			vdm.append("\n");
			vdm.append(INDENT);
			vdm.append(INDENT);
			vdm.append(csvLine(line));
			sep = ",";
		}
		
		vdm.append("\n");
		vdm.append(INDENT);
		vdm.append("];\n");
		reader.close();
		
		return vdm.toString().toCharArray();
	}
	
	private StringBuilder headers(File file)
	{
		StringBuilder vdm = new StringBuilder();

		vdm.append("--\n");
		vdm.append("-- Generated from ");
		vdm.append(file.getPath());
		vdm.append(" at ");
		vdm.append(new Date().toString());
		vdm.append("\n--\n");
		vdm.append("types\n");
		vdm.append(INDENT);
		vdm.append("CSVCell = CellType\n");
		vdm.append(INDENT);
		vdm.append("inv cell == cellInvariant(cell);   -- implement this!\n\n");
		vdm.append(INDENT);
		vdm.append("CSVRow  = seq1 of CSVCell\n");
		vdm.append(INDENT);
		vdm.append("inv row == rowInvariant(row);      -- implement this!\n\n");
		vdm.append(INDENT);
		vdm.append("CSVFile = seq of CSVRow\n");
		vdm.append(INDENT);
		vdm.append("inv file == fileInvariant(file);   -- implement this!\n\n");

		vdm.append("values\n");
		vdm.append(INDENT);
		vdm.append(System.getProperty("csvreader.name", nameFromFile(file)));
		vdm.append(" : CSVFile =\n");
		
		return vdm;
	}
	
	private String nameFromFile(File file)
	{
		String name = file.getName();
		return name.replaceAll("\\.", "_");
	}

	private String csvLine(String line)
	{
		try
		{
			if (line.trim().isEmpty())
			{
				return "[ ]";	// Blank line - an error
			}
			
			LexTokenReader ltr = new LexTokenReader(line, Dialect.VDM_SL);
			ltr.nextToken();
			ExpressionReader reader = new ExpressionReader(ltr);
			return "[ " + reader.readExpressionList() + " ]";
		}
		catch (LocatedException e)
		{
			return "-- " + e.getMessage() + " at pos " + e.location.startPos + ": " + line;
		}
	}

	public static void main(String[] args) throws IOException
	{
		CSVReader csv = new CSVReader();
		System.out.println(new String(csv.getText(new File(args[0]), Charset.defaultCharset())));
	}
}
