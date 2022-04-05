/*******************************************************************************
 *
 *	Copyright (c) 2022 Nick Battle
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

/**
 * A class to read text encoded VDM files, and look for a given marker.
 */
abstract public class TextStreamReader implements ExternalFormatReader
{
	protected char[] getText(File file, String encoding, String marker) throws IOException
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
		StringBuilder text =  new StringBuilder();
		boolean capturing = false;
		String line = br.readLine();

		while (line != null)
		{
			if (line.trim().contains(marker))
			{
				capturing = !capturing;
			}
			else
			{
				if (capturing)
				{
					text.append(line);
					text.append('\n');
				}
			}

			line = br.readLine();
		}

		br.close();
		
		return text.toString().toCharArray();
	}
}
