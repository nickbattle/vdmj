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
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

import examples.v2c.tr.TRNode;
import examples.v2c.tr.definitions.TRClassList;
import examples.v2c.tr.modules.TRModuleList;

/**
 * All command line plugins must extend CommandPlugin and be in the class "plugins" by
 * default. The packages searched for plugins can be changed using the "vdmj.plugins"
 * JVM property.
 */
public class TranslatePlugin extends CommandPlugin
{
	/**
	 * The constructor is called from the command line interpreter when the user first types
	 * "translate <args>". It is passed an instance of the runtime interpreter, which is
	 * saved in a protected field by the superclass.
	 * 
	 * @param interpreter
	 */
	public TranslatePlugin(Interpreter interpreter)
	{
		super(interpreter);
	}
	
	/**
	 * The run method is called whenever the user types "translate <args>" in the VDMJ command
	 * line interpreter (CommandReader.java). Note that this class has access to the "interpreter"
	 * instance that is running, which is passed during construction (above).
	 * 
	 * The example run method uses the ClassMapper to turn the type checked tree (from the interpreter)
	 * into a "TR" tree. This uses the mappings file defined in the TRNode root class, which all
	 * translatable classes must extend.
	 * 
	 * After converting the TC tree to a TR tree, this is then used to translate the specification
	 * into "C". The result, a String, is just printed to stdout, which appears in the user console
	 * session. By returning "true", the method indicates that the command executed successfully
	 * without errors. If it returns "false", the command line interpreter will say "Unknown command".
	 */
	@Override
	public boolean run(String[] argv) throws Exception
	{
		if (interpreter instanceof ModuleInterpreter)
		{
			ModuleInterpreter minterpreter = (ModuleInterpreter)interpreter;
			TCModuleList tclist = minterpreter.getTC();
			TRModuleList trModules = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(tclist);
			System.out.println(trModules.translate());
			return true;
		}
		else if (interpreter instanceof ClassInterpreter)
		{
			ClassInterpreter cinterpreter = (ClassInterpreter)interpreter;
			TCClassList tclist = cinterpreter.getTC();
			TRClassList trClasses = ClassMapper.getInstance(TRNode.MAPPINGS).init().convert(tclist);
			System.out.println(trClasses.translate());
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Once this plugin is loaded (but not before) the following help line is added to the general
	 * "help" output in the command line. The format is the same as the other help lines, with
	 * an indication of what arguments the command takes, and a one line description.
	 */
	@Override
	public String help()
	{
		return "translate <language> [<files>] - translate the VDM specification to <language>";
	}
}
