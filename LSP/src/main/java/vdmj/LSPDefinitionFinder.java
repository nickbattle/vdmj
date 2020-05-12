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

package vdmj;

import java.io.File;
import java.util.Set;

import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.lex.LexLocation;
import com.fujitsu.vdmj.tc.TCNode;
import com.fujitsu.vdmj.tc.definitions.TCClassDefinition;
import com.fujitsu.vdmj.tc.definitions.TCClassList;
import com.fujitsu.vdmj.tc.definitions.TCDefinition;
import com.fujitsu.vdmj.tc.expressions.TCFieldExpression;
import com.fujitsu.vdmj.tc.expressions.TCMkTypeExpression;
import com.fujitsu.vdmj.tc.expressions.TCVariableExpression;
import com.fujitsu.vdmj.tc.modules.TCModule;
import com.fujitsu.vdmj.tc.modules.TCModuleList;
import com.fujitsu.vdmj.tc.statements.TCCallObjectStatement;
import com.fujitsu.vdmj.tc.statements.TCCallStatement;
import com.fujitsu.vdmj.tc.statements.TCIdentifierDesignator;
import com.fujitsu.vdmj.tc.types.TCClassType;
import com.fujitsu.vdmj.tc.types.TCField;
import com.fujitsu.vdmj.tc.types.TCOptionalType;
import com.fujitsu.vdmj.tc.types.TCRecordType;
import com.fujitsu.vdmj.tc.types.TCType;
import com.fujitsu.vdmj.tc.types.TCUnresolvedType;
import com.fujitsu.vdmj.typechecker.ModuleEnvironment;
import com.fujitsu.vdmj.typechecker.NameScope;
import com.fujitsu.vdmj.typechecker.PrivateClassEnvironment;
import com.fujitsu.vdmj.typechecker.PublicClassEnvironment;

import workspace.Log;

public class LSPDefinitionFinder
{
	public static class Found
	{
		public TCModule module;
		public TCClassDefinition classdef;
		public TCNode node;
		
		public Found(TCModule module, TCClassDefinition clssdef, TCNode node)
		{
			this.module = module;
			this.classdef = clssdef;
			this.node = node;
		}
	}
	
	public Found findLocation(TCModuleList modules, File file, int line, int col)
	{
		return findLocation(modules, new LexLocation(file, "?", line, col, line, col));
	}
	
	public Found findLocation(TCClassList classes, File file, int line, int col)
	{
		return findLocation(classes, new LexLocation(file, "?", line, col, line, col));
	}

	public Found findLocation(TCModuleList modules, LexLocation position)
	{
		LSPDefinitionLocationFinder finder = new LSPDefinitionLocationFinder();
		
		for (TCModule module: modules)
		{
			// Only explicit modules have a span, called (say) "M`M"
			LexNameToken sname = new LexNameToken(module.name.getName(), module.name.getLex());
			LexLocation span = LexLocation.getSpan(sname);
			
			if (span == null || position.within(span))
			{
				for (TCDefinition def: module.defs)
				{
					Set<TCNode> nodes = def.apply(finder, position);
					
					if (nodes != null && !nodes.isEmpty())	// found it!
					{
						TCNode node = nodes.iterator().next();
						return new Found(module, null, node);
					}
				}
			}
		}

		return null;
	}
	
	public Found findLocation(TCClassList classes, LexLocation position)
	{
		LSPDefinitionLocationFinder finder = new LSPDefinitionLocationFinder();
		
		for (TCClassDefinition cdef: classes)
		{
			LexLocation span = LexLocation.getSpan(cdef.name.getLex());
			
			if (span == null || position.within(span))
			{
				for (TCDefinition def: cdef.definitions)
				{
					Set<TCNode> nodes = def.apply(finder, position);
					
					if (nodes != null && !nodes.isEmpty())	// found it!
					{
						TCNode node = nodes.iterator().next();
						return new Found(null, cdef, node);
					}
				}
			}
		}

		return null;
	}
	
	public TCDefinition findDefinition(TCModuleList modules, File file, int line, int col)
	{
		return findDefinition(modules, new LexLocation(file, "?", line, col, line, col));
	}
	
	public TCDefinition findDefinition(TCModuleList modules, LexLocation position)
	{
		Found found = findLocation(modules, position);
		
		if (found != null)
		{
			TCModule module = found.module;
			ModuleEnvironment env = new ModuleEnvironment(module);
			TCNode node = found.node;
			
			if (node instanceof TCVariableExpression)
			{
				TCVariableExpression vexp = (TCVariableExpression)node;
				return vexp.getDefinition();
			}
			else if (node instanceof TCCallStatement)
			{
				TCCallStatement stmt = (TCCallStatement)node;
				return stmt.getDefinition();
			}
			else if (node instanceof TCCallObjectStatement)
			{
				TCCallObjectStatement stmt = (TCCallObjectStatement)node;
				return stmt.getDefinition();
			}
			else if (node instanceof TCUnresolvedType)
			{
				TCUnresolvedType unresolved = (TCUnresolvedType)node;
				return env.findType(unresolved.typename, module.name.getName());
			}
			else if (node instanceof TCMkTypeExpression)
			{
				TCMkTypeExpression mk = (TCMkTypeExpression)node;
				return env.findType(mk.typename, module.name.getName());
			}
			else if (node instanceof TCFieldExpression)
			{
				TCFieldExpression fexp = (TCFieldExpression)node;
				
				if (fexp.root.isRecord(fexp.location))
				{
		    		TCRecordType rec = fexp.root.getRecord();
		    		TCField field = rec.findField(fexp.field.getName());
		    		
		    		if (field != null)
		    		{
			    		TCType type = field.type;
			    		
			    		while (type instanceof TCOptionalType)
			    		{
			    			type = ((TCOptionalType)type).type;
			    		}
			    		
			    		if (type != null && type.definitions != null)
			    		{
			    			return type.definitions.get(0);
			    		}
		    		}
				}
			}
			
			Log.error("TCNode located, but unable to find definition of %s %s",
							node.getClass().getSimpleName(), position);
			return null;
		}
		else
		{
			Log.error("Unable to locate symbol %s", position);
			return null;
		}
	}
	
	public TCDefinition findDefinition(TCClassList classes, File file, int line, int col)
	{
		return findDefinition(classes, new LexLocation(file, "?", line, col, line, col));
	}
	
	public TCDefinition findDefinition(TCClassList classes, LexLocation position)
	{
		Found found = findLocation(classes, position);
		
		if (found != null)
		{
			TCClassDefinition cdef = found.classdef;
			PublicClassEnvironment globals = new PublicClassEnvironment(classes); 
			PrivateClassEnvironment env = new PrivateClassEnvironment(cdef, globals);
			TCNode node = found.node;
			
			if (node instanceof TCVariableExpression)
			{
				TCVariableExpression vexp = (TCVariableExpression)node;
				return vexp.getDefinition();
			}
			else if (node instanceof TCCallStatement)
			{
				TCCallStatement stmt = (TCCallStatement)node;
				return stmt.getDefinition();
			}
			else if (node instanceof TCCallObjectStatement)
			{
				TCCallObjectStatement stmt = (TCCallObjectStatement)node;
				return stmt.getDefinition();
			}
			else if (node instanceof TCIdentifierDesignator)
			{
				TCIdentifierDesignator id = (TCIdentifierDesignator)node;
				return env.findName(id.name, NameScope.NAMESANDSTATE);
			}
			else if (node instanceof TCUnresolvedType)
			{
				TCUnresolvedType unresolved = (TCUnresolvedType)node;
				return env.findType(unresolved.typename, cdef.name.getName());
			}
			else if (node instanceof TCMkTypeExpression)
			{
				TCMkTypeExpression mk = (TCMkTypeExpression)node;
				return env.findType(mk.typename, cdef.name.getName());
			}
			else if (node instanceof TCFieldExpression)
			{
				TCFieldExpression field = (TCFieldExpression)node;
				
				if (field.root.isRecord(field.location))
				{
		    		TCRecordType rec = field.root.getRecord();
		    		return env.findType(rec.name, cdef.name.getName());
				}
				else if (field.root.isClass(env))
				{
		    		TCClassType cls = field.root.getClassType(env);
		    		return cls.findName(field.memberName, NameScope.VARSANDNAMES);
				}
			}

			Log.error("TCNode located, but unable to find definition %s", position);
			return null;
		}
		else
		{
			Log.error("Unable to locate symbol %s", position);
			return null;
		}
	}
}
