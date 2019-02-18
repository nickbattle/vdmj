package plugins;

import plugins.doc.DOCNode;
import plugins.doc.definitions.DOCClassList;
import plugins.doc.modules.DOCModuleList;

import com.fujitsu.vdmj.commands.CommandPlugin;
import com.fujitsu.vdmj.mapper.ClassMapper;
import com.fujitsu.vdmj.runtime.ClassInterpreter;
import com.fujitsu.vdmj.runtime.Interpreter;
import com.fujitsu.vdmj.runtime.ModuleInterpreter;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.modules.TCModuleList;

public class DocPlugin extends CommandPlugin
{
	public DocPlugin(Interpreter interpreter)
	{
		super(interpreter);
	}

	@Override
	public boolean run(String[] argv) throws Exception
	{
		if (interpreter instanceof ModuleInterpreter)
		{
			ModuleInterpreter minterpreter = (ModuleInterpreter)interpreter;
			TCModuleList tclist = minterpreter.getTC();
			DOCModuleList docModules = ClassMapper.getInstance(DOCNode.MAPPINGS).init().convert(tclist);
			System.out.println(docModules.toHTML());
			return true;
		}
		else if (interpreter instanceof ClassInterpreter)
		{
			ClassInterpreter cinterpreter = (ClassInterpreter)interpreter;
			TCClassList tclist = cinterpreter.getTC();
			DOCClassList docClasses = ClassMapper.getInstance(DOCNode.MAPPINGS).init().convert(tclist);
			System.out.println(docClasses.toHTML());
			return true;
		}
		else
		{
			return false;
		}
	}
}
