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

package workspace.plugins;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import com.fujitsu.vdmj.messages.VDMMessage;
import rpc.RPCMessageList;
import rpc.RPCRequest;
import workspace.WorkspaceManager;
import workspace.WorkspacePlugin;

public abstract class ASTPlugin extends WorkspacePlugin
{
	protected List<VDMMessage> errs = new Vector<VDMMessage>();
	protected List<VDMMessage> warns = new Vector<VDMMessage>();

	public ASTPlugin(WorkspaceManager manager)
	{
		super(manager);
	}
	
	@Override
	public String getName()
	{
		return "AST";
	}

	@Override
	public void init()
	{
	}

	public RPCMessageList fileChanged(File file) throws IOException
	{
		return messages.diagnosticResponses(parseFile(file), file);
	}
	
	public void preCheck()
	{
		errs.clear();
		warns.clear();
	}
	
	abstract public boolean checkLoadedFiles();
	
	public List<VDMMessage> getErrs()
	{
		return errs;
	}
	
	public List<VDMMessage> getWarns()
	{
		return warns;
	}
	
	abstract protected List<VDMMessage> parseFile(File file);

	abstract public RPCMessageList documentSymbols(RPCRequest request, File file);
}
