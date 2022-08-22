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

package workspace.events;

import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.messages.VDMMessage;

abstract public class AbstractCheckFilesEvent extends LSPEvent
{
	private final List<VDMMessage> errs = new Vector<VDMMessage>();
	private final List<VDMMessage> warns = new Vector<VDMMessage>();
	
	public AbstractCheckFilesEvent()
	{
		super(null);
	}
	
	public List<VDMMessage> getErrs()
	{
		return errs;
	}
	
	public void addErrs(List<VDMMessage> errs)
	{
		this.errs.addAll(errs);
	}
	
	public List<VDMMessage> getWarns()
	{
		return warns;
	}
	
	public void addWarns(List<VDMMessage> warns)
	{
		this.warns.addAll(warns);
	}
	
	public List<VDMMessage> getMessages()
	{
		Vector<VDMMessage> all = new Vector<VDMMessage>();
		all.addAll(errs);
		all.addAll(warns);
		return all;
	}

	public boolean hasErrs()
	{
		return !errs.isEmpty();
	}	
}
