/*******************************************************************************
 *
 *	Copyright (c) 2017 Fujitsu Services Ltd.
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

package org.overture.interpreter.debug;

import com.fujitsu.vdmj.dbgp.DBGPReader;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.values.CPUValue;

/**
 * Class to simulate the Overture entry point.
 */
@Deprecated
public class DBGPReaderV2 extends DBGPReader
{
	private DBGPReaderV2(String host, int port, String ideKey,
						Interpreter interpreter, String expression, CPUValue cpu)
	{
		super(host, port, ideKey, interpreter, expression, cpu);
	}

	public static void main(String[] args)
	{
		DBGPReader.main(args);
	}
}
