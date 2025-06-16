/*******************************************************************************
 *
 *	Copyright (c) 2023 Nick Battle.
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
 *	along with VDMJ.  If not, see &lt;http://www.gnu.org/licenses/&gt;.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package lsp;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import workspace.GlobFinder;

public class GlobTest
{
	@Test
	public void globTest() throws IOException
	{
		File temp = Files.createTempDirectory("globTest").toFile();
		temp.deleteOnExit();
		
		File subfolder = new File(temp, "subfolder");
		subfolder.mkdir();
		subfolder.deleteOnExit();
		
		File subsubfolder = new File(subfolder, "subfolder");	// Same name
		subsubfolder.mkdir();
		subsubfolder.deleteOnExit();
		
		File topLevel = new File(temp, "topLevel.vdmsl");
		topLevel.createNewFile();
		topLevel.deleteOnExit();
		
		File subLevel = new File(subfolder, "subLevel.vdmsl");
		subLevel.createNewFile();
		subLevel.deleteOnExit();
		
		File subSubLevel = new File(subsubfolder, "subSubLevel.vdmsl");
		subSubLevel.createNewFile();
		subSubLevel.deleteOnExit();
		
		GlobFinder finder = new GlobFinder("**/*.vdmsl");	// All three
		Files.walkFileTree(Paths.get(temp.getAbsolutePath()), finder);
		assertEquals(3, finder.getMatches().size());
		System.out.println(finder.getMatches());
		
		finder = new GlobFinder("**.vdmsl");	// All three
		Files.walkFileTree(Paths.get(temp.getAbsolutePath()), finder);
		assertEquals(3, finder.getMatches().size());
		System.out.println(finder.getMatches());
		
		finder = new GlobFinder("**subfolder");		// Find both
		Files.walkFileTree(Paths.get(temp.getAbsolutePath()), finder);
		assertEquals(2, finder.getMatches().size());
		System.out.println(finder.getMatches());
		
		// Other tests tricky since we can't "cd" to temp easily in Java
	}
}
