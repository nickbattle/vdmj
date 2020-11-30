/*******************************************************************************
 *
 *	Copyright (c) 2020 Nick Battle.
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
 *
 ******************************************************************************/

package plugins;

import com.fujitsu.vdmj.commands.CommandPlugin;
import com.fujitsu.vdmj.runtime.Interpreter;

public class OsPlugin extends CommandPlugin
{
	public OsPlugin(Interpreter interpreter)
	{
		super(interpreter);
	}

	@Override
	public boolean run(String[] argv) throws Exception
	{
		String[] cmd = new String[argv.length - 1];
		System.arraycopy(argv, 1, cmd, 0, cmd.length);		
		
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.inheritIO();
		Process p = pb.start();
		p.waitFor();
		
		return true;	// Even if command failed
	}
}
