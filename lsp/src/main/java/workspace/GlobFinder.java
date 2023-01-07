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
 *	along with VDMJ.  If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

package workspace;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Vector;

public class GlobFinder extends SimpleFileVisitor<Path>
{
	private final PathMatcher matcher;
	private final List<File> matches;

	GlobFinder(String pattern)
	{
		// Normalize first, to eliminate "./" and similar, which won't match
		pattern = Paths.get(pattern).normalize().toString();
		matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		matches = new Vector<File>();
	}
	
	public List<File> getMatches()
	{
		return matches;
	}

	private void match(Path file)
	{
		if (matcher.matches(file))
		{
			try
			{
				matches.add(file.toFile().getCanonicalFile());
			}
			catch (IOException e)
			{
				// ignore
			}
		}
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
	{
		match(file);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
	{
		match(dir);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc)
	{
		Diag.error(exc);
		return FileVisitResult.CONTINUE;
	}
	
	public static void main(String[] args) throws IOException
	{
		Diag.init(true);
		
		for (int i=0; i<args.length; i++)
		{
			GlobFinder finder = new GlobFinder(args[i]);
			Files.walkFileTree(Paths.get(""), finder);
			
			System.out.println("Pattern " + args[i] + "...");
			
			for (File file: finder.getMatches())
			{
				System.out.println(file);
			}
		}
	}
}
