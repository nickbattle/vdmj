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

import com.fujitsu.vdmj.runtime.Interpreter;

import workspace.LSPWorkspaceManager;

abstract public class INPlugin extends AnalysisPlugin
{
	protected final LSPWorkspaceManager lspManager;
	
	public INPlugin(LSPWorkspaceManager manager)
	{
		super();
		this.lspManager = manager;
	}
	
	@Override
	public String getName()
	{
		return "IN";
	}

	@Override
	public void init()
	{
	}

	public void preCheck()
	{
	}
	
	abstract public <T> T getIN();
	
	abstract public <T> boolean checkLoadedFiles(T tcList) throws Exception;
	
	abstract public <T> Interpreter getInterpreter(T tcList) throws Exception;
}
