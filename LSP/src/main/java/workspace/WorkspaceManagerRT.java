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

package workspace;

import java.io.FilenameFilter;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTBUSClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTCPUClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTClassList;
import com.fujitsu.vdmj.lex.Dialect;

import workspace.plugins.ASTPluginPR;
import workspace.plugins.TCPluginPR;

public class WorkspaceManagerRT extends WorkspaceManagerPP
{
	public WorkspaceManagerRT()
	{
		Settings.dialect = Dialect.VDM_RT;
		registerPlugin(new ASTPluginPR(this));
		registerPlugin(new TCPluginPR(this));
	}
	
	@Override
	protected FilenameFilter getFilenameFilter()
	{
		return Dialect.VDM_RT.getFilter();
	}
	
	@Override
	protected String[] getFilenameFilters()
	{
		return new String[] { "**/*.vpp", "**/*.vdmrt" }; 
	}

	@Override
	protected ASTClassList extras()
	{
		try
		{
			ASTClassList ex = new ASTClassList();
			ex.add(new ASTCPUClassDefinition());
			ex.add(new ASTBUSClassDefinition());
			return ex;
		}
		catch (Exception e)
		{
			Log.error(e);
			return null;
		}
	}
}
