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

package examples.parser;

import java.io.File;
import java.io.IOException;

import com.fujitsu.vdmj.lex.ExternalFormatReader;

/**
 * Invoke an external reader to generate VDM source. Note that the file passed
 * does not necessarily exist, but the filename is used to generate VDM-SL.
 */
public class ExternalStreamReader implements ExternalFormatReader
{
	@Override
	public char[] getText(File file, String charset) throws IOException
	{
		try
		{
			StringBuilder sb = new StringBuilder();
			sb.append("values\n");
			sb.append("FILE = \"" + file.getAbsolutePath() + "\";\n");
			sb.append("CHARSET = \"" + charset + "\";\n");
			return sb.toString().toCharArray();
		}
		catch (Throwable th)
		{
			System.err.println(th);
			return null;
		}
	}
}
