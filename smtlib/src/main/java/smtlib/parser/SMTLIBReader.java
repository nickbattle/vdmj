/*******************************************************************************
 *
 *	Copyright (c) 2026 Fujitsu Services Ltd.
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

package smtlib.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;

public class SMTLIBReader
{
	private final BufferedReader reader;
	private char nextCh;
	private String nextStr;
	
	private static final char EOF = (char)-1;
	
	public SMTLIBReader(BufferedReader br) throws IOException
	{
		this.reader = br;
		
		rdCh();
		rdToken();
	}
	
	public SMTLIBReader(String source) throws IOException
	{
		this.reader = new BufferedReader(new StringReader(source));
		
		rdCh();
		rdToken();
	}
	
	private char rdCh() throws IOException
	{
		nextCh = (char)reader.read();
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
			throw new IOException(message);
		}
	}
	
	private void rdToken() throws IOException
	{
		while (Character.isWhitespace(nextCh))
		{
			rdCh();
		}

		boolean doRdCh = true;

		switch (nextCh)
		{
			case EOF:
				nextStr = null;
				break;
				
			case ';':
				while (nextCh != '\n' && nextCh != EOF)
				{
					rdCh();
				}
				
				rdToken();
				return;

			case '|':
			{
				StringBuilder sb = new StringBuilder();
				rdCh();

				while (nextCh != '|' && nextCh != EOF)
				{
					sb.append(nextCh);
					rdCh();
				}
				
				nextStr = sb.toString();
				break;
			}

			case '"':
			{
				StringBuilder sb = new StringBuilder();
				boolean haveQuote = false;
				rdCh();

				while (nextCh != EOF)
				{
					if (nextCh == '"')
					{
						if (haveQuote)
						{
							sb.append(nextCh);
							haveQuote = false;
						}
						else
						{
							haveQuote = true;
						}
					}
					else if (haveQuote)
					{
						doRdCh = false;		// We already read one ahead
						break;
					}
					else
					{
						sb.append(nextCh);
					}

					rdCh();
				}
				
				nextStr = sb.toString();
				break;
			}

			case '(':
				nextStr =  "(";
				break;
				
			case ')':
				nextStr =  ")";
				break;
				
			default:
			{
				StringBuilder sb = new StringBuilder();
				
				while (nextCh != '(' &&
					nextCh != ')' &&
					nextCh != ';' &&
					nextCh != '"' &&
					nextCh != '|' &&
					nextCh != EOF &&
					!Character.isWhitespace(nextCh))
				{
					sb.append(nextCh);
					rdCh();
				}
				
				nextStr = sb.toString();
				doRdCh = false;
				break;
			}
		}
		
		if (doRdCh) rdCh();
	}

	public List<Bracket> readScript() throws IOException
	{
		List<Bracket> list = new Vector<Bracket>();

		while (nextStr != null && !nextStr.equals(")"))
		{
			list.add(readBracket());
		}

		return list;
	}
	
	public Bracket readBracket() throws IOException
	{
		checkFor("(", "Expecting '('");
		Bracket bracket = new Bracket();

		while (nextStr != null && !nextStr.equals(")"))
		{
			if (nextStr.equals("("))
			{
				bracket.add(readBracket());
			}
			else
			{
				bracket.add(nextStr);
			}

			rdToken();
		}

		return bracket;
	}

	public void close() throws IOException
	{
		reader.close();
	}

	public static void main(String[] argv) throws IOException
	{
		SMTLIBReader p = new SMTLIBReader("\"She said : \"\"Bye bye\"\" and left.\"|1 2 3|123 456");
		System.out.println(p.nextStr);
		
		while (p.nextCh != EOF)
		{
			p.rdToken();
			System.out.println(p.nextStr);
		}
	}
}
