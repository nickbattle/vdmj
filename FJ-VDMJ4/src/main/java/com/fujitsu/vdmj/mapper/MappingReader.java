/*******************************************************************************
 *
 *	Copyright (c) 2016 Fujitsu Services Ltd.
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

package com.fujitsu.vdmj.mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;

import com.fujitsu.vdmj.mapper.Mapping.Type;

public class MappingReader
{
	private final String filename;
	private final BufferedReader reader;
	private char nextCh;
	private String nextStr;
	private int charpos = 0;
	private int linecount = 1;
	
	private static final char EOF = (char)-1;
	
	public MappingReader(String filename, InputStream is) throws IOException
	{
		this.filename = filename;
		this.reader = new BufferedReader(new InputStreamReader(is));
		
		rdCh();
		rdToken();
	}
	
	private void error(String message) throws IOException
	{
		System.err.println(filename + ": " + message + " at " + linecount + ":" + charpos);
		readToEOL();
		rdToken();
		throw new IOException(message);
	}

	private void readToEOL() throws IOException
	{
		while (nextCh != '\n' && nextCh != EOF)
		{
			rdCh();
		}
	}

	private char rdCh() throws IOException
	{
		char c = (char)reader.read();

		if (c == '\n')
		{
			linecount++;
			charpos = 0;
		}
		else
		{
			charpos++;
		}

		nextCh = c;
		return nextCh;
	}

	private void checkFor(String token, String message) throws IOException
	{
		if (nextStr.equals(token))
		{
			rdToken();
		}
		else
		{
			error(message);
		}
	}
	
	private void rdToken() throws IOException
	{
		while (Character.isWhitespace(nextCh))
		{
			rdCh();
		}

		switch (nextCh)
		{
			case EOF:
				nextStr = null;
				break;
				
			case '#':
				while (nextCh != '\n' && nextCh != EOF)
				{
					rdCh();
				}
				
				rdToken();
				return;

			case '(':
				nextStr =  "(";
				break;
				
			case ')':
				nextStr =  ")";
				break;
				
			case '{':
				nextStr =  "{";
				break;
				
			case '}':
				nextStr =  "}";
				break;
				
			case '.':
				nextStr =  ".";
				break;
				
			case ',':
				nextStr =  ",";
				break;
				
			case ';':
				nextStr =  ";";
				break;
				
			default:
				if (Character.isJavaIdentifierStart(nextCh))
				{
					StringBuilder sb = new StringBuilder();
					
					while (Character.isJavaIdentifierPart(nextCh))
					{
						sb.append(nextCh);
						rdCh();
					}
					
					nextStr = sb.toString();
				}
				else
				{
					System.err.println("Unexpected character " + nextCh + " at " + linecount + ":" + charpos);
				}
				return;
		}
		
		rdCh();
	}
	
	public Mapping readCommand() throws IOException
	{
		try
		{
    		if (nextStr == null)
    		{
    			return Mapping.EOF;
    		}
    		else if (nextStr.equals("map"))
    		{
    			return readMap();
    		}
    		else if (nextStr.equalsIgnoreCase("unmapped"))
    		{
    			return readUnmapped();
    		}
    		else if (nextStr.equals("package"))
    		{
    			return readPackage();
    		}
    		else if (nextStr.equals("init"))
    		{
    			return readInit();
    		}
    		else
    		{
    			error("Expecting map, unmap or package");
    			return Mapping.ERROR;
    		}
		}
		catch (IOException e)
		{
			// Syntax error or file IO.
			return Mapping.ERROR;
		}
	}

	private Mapping readInit() throws IOException
	{
		int lineNo = linecount;
		
		rdToken();
		String method = readDotName();
		checkFor("(", "Expecting '('");
		checkFor(")", "Expecting ')'");
		checkFor(";", "Expecting closing semi-colon");
		
		return new Mapping(lineNo, Type.INIT, method, null, null, null, null);
	}

	private Mapping readPackage() throws IOException
	{
		int lineNo = linecount;
		
		rdToken();
		String source = readDotName();
		checkFor("to", "Expecting 'to'");
		
		String dest = readDotName();
		checkFor(";", "Expecting closing semi-colon");
		
		return new Mapping(lineNo, Type.PACKAGE, source, null, dest, null, null);
	}
	
	private Mapping readMap() throws IOException
	{
		int lineNo = linecount;
		
		rdToken();
		String srcClass = nextStr;
		
		List<String> varnames = new Vector<String>();
		rdToken();
		checkFor("{", "Expecting '{'");
		varnames = readList("}");
		checkFor("}", "Expecting '}'");
	
		checkFor("to", "Expecting 'to'");
		String destClass = nextStr;
		
		List<String> paramnames = new Vector<String>();
		rdToken();
		checkFor("(", "Expecting '('");
		paramnames = readList(")");
		checkFor(")", "Expecting ')'");
		
		List<String> setnames = new Vector<String>();
		if (nextStr.equals("set"))
		{
			rdToken();
			setnames = readList(";");
		}
		
		checkFor(";", "Expecting closing semi-colon");
		return new Mapping(lineNo, Mapping.Type.MAP, srcClass, varnames, destClass, paramnames, setnames);
	}

	private Mapping readUnmapped() throws IOException
	{
		int lineNo = linecount;
		
		rdToken();
		String source = readDotName();
		checkFor(";", "Expecting closing semi-colon");
		
		return new Mapping(lineNo, Type.UNMAPPED, source, null, null, null, null);
	}

	private List<String> readList(String term) throws IOException
	{
		List<String> names = new Vector<String>();
		
		while (true)
		{
			if (nextStr.equals(term))
			{
				break;
			}
			else
			{
				names.add(nextStr);
				rdToken();
				
				if (nextStr.equals(","))
				{
					rdToken();
					
					if (nextStr.equals(term))
					{
						error("Expecting name after comma");
					}
				}
				else if (!nextStr.equals(term))
				{
					error("Expecting comma separator");
				}
			}
		}

		return names;
	}

	private String readDotName() throws IOException
	{
		StringBuilder sb = new StringBuilder();
		sb.append(nextStr);
		rdToken();
		
		while (nextStr.equals("."))
		{
			sb.append(nextStr);
			rdToken();
			sb.append(nextStr);
			rdToken();
		}
		
		return sb.toString();
	}

	public void close() throws IOException
	{
		reader.close();
	}
}
