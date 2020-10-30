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

import java.util.Map;

import com.fujitsu.vdmj.tc.lex.TCNameList;

abstract public class CTPlugin extends AnalysisPlugin
{
	public CTPlugin()
	{
		super();
	}
	
	@Override
	public String getName()
	{
		return "CT";
	}

	@Override
	public void init()
	{
	}

	abstract public void preCheck();

	abstract public <T> boolean checkLoadedFiles(T inList) throws Exception;

	abstract public Map<String, TCNameList> getTraceNames();

	abstract public <T> T getCT();
}
