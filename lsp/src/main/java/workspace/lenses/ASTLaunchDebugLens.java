/*******************************************************************************
 *
 *	Copyright (c) 2021 Nick Battle.
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

package workspace.lenses;

import com.fujitsu.vdmj.Settings;
import com.fujitsu.vdmj.ast.definitions.ASTClassDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTExplicitOperationDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitFunctionDefinition;
import com.fujitsu.vdmj.ast.definitions.ASTImplicitOperationDefinition;
import com.fujitsu.vdmj.ast.expressions.ASTSubclassResponsibilityExpression;
import com.fujitsu.vdmj.ast.lex.LexNameToken;
import com.fujitsu.vdmj.ast.modules.ASTModule;
import com.fujitsu.vdmj.ast.patterns.ASTPattern;
import com.fujitsu.vdmj.ast.patterns.ASTPatternList;
import com.fujitsu.vdmj.ast.statements.ASTSubclassResponsibilityStatement;
import com.fujitsu.vdmj.ast.types.ASTFunctionType;
import com.fujitsu.vdmj.ast.types.ASTOperationType;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePair;
import com.fujitsu.vdmj.ast.types.ASTPatternListTypePairList;
import com.fujitsu.vdmj.ast.types.ASTType;
import com.fujitsu.vdmj.ast.types.ASTTypeList;
import com.fujitsu.vdmj.ast.types.ASTUnresolvedType;
import com.fujitsu.vdmj.lex.Dialect;
import com.fujitsu.vdmj.lex.Token;

import json.JSONArray;
import json.JSONObject;

public class ASTLaunchDebugLens extends AbstractLaunchDebugLens implements ASTCodeLens
{
	@Override
	public JSONArray getDefinitionLenses(ASTDefinition def, ASTModule module)
	{
		return getDefinitionLenses(def, (ASTClassDefinition)null);
	}

	@Override
	public JSONArray getDefinitionLenses(ASTDefinition def, ASTClassDefinition cls)
	{
		JSONArray results = new JSONArray();
		
		if (isClientType("vscode") && isPublic(def))
		{
			String launchName = null;
			String defaultName = null;
			String applyName = null;
			JSONArray applyTypes = null;
			JSONArray applyArgs = new JSONArray();
			boolean needsCtor = (Settings.dialect != Dialect.VDM_SL);
			
			if (def instanceof ASTExplicitFunctionDefinition)
			{
				ASTExplicitFunctionDefinition exdef = (ASTExplicitFunctionDefinition) def;
				
				if (!(exdef.body instanceof ASTSubclassResponsibilityExpression))
				{
					applyName = exdef.name.getName();
					launchName = applyName;
					defaultName = exdef.name.module;
					
					if (exdef.typeParams != null)
					{
						applyTypes = new JSONArray();
						
						for (LexNameToken ptype: exdef.typeParams)
						{
							applyTypes.add("@" + ptype.toString());
						}
					}
					
					ASTFunctionType ftype = (ASTFunctionType) exdef.type;
					ASTTypeList ptypes = ftype.parameters;
					int i = 0;
					
					for (ASTPattern p: exdef.paramPatternList.get(0))	// Curried?
					{
						applyArgs.add(new JSONObject("name", p.toString(), "type", fix(ptypes.get(i++))));
					}
				}
			}
			else if (def instanceof ASTImplicitFunctionDefinition)
			{
				ASTImplicitFunctionDefinition imdef = (ASTImplicitFunctionDefinition) def;
				
				if (imdef.body != null && !(imdef.body instanceof ASTSubclassResponsibilityExpression))
				{
					applyName = imdef.name.getName();
					launchName = applyName;
					defaultName = imdef.name.module;
					
					for (ASTPatternListTypePair param: imdef.parameterPatterns)
					{
						for (ASTPattern p: param.patterns)
						{
							applyArgs.add(new JSONObject("name", p.toString(), "type", fix(param.type)));
						}
					}
				}
			}
			else if (def instanceof ASTExplicitOperationDefinition)
			{
				ASTExplicitOperationDefinition exop = (ASTExplicitOperationDefinition) def;
				
				if (!(exop.body instanceof ASTSubclassResponsibilityStatement))
				{
					applyName = exop.name.getName();
					defaultName = exop.name.module;
					needsCtor = !exop.accessSpecifier.isStatic;
					
					if (!applyName.equals(defaultName))		// Not a constructor
					{
						launchName = applyName;
						
						ASTOperationType ftype = (ASTOperationType) exop.type;
						ASTTypeList ptypes = ftype.parameters;
						int i = 0;
						
						for (ASTPattern p: exop.parameterPatterns)
						{
							applyArgs.add(new JSONObject("name", p.toString(), "type", fix(ptypes.get(i++))));
						}
					}
				}
			}
			else if (def instanceof ASTImplicitOperationDefinition)
			{
				ASTImplicitOperationDefinition imop = (ASTImplicitOperationDefinition) def;
				
				if (imop.body != null && !(imop.body instanceof ASTSubclassResponsibilityStatement))
				{
					applyName = imop.name.getName();
					defaultName = imop.name.module;
					needsCtor = !imop.accessSpecifier.isStatic;
	
					if (!applyName.equals(defaultName))	// Not a constructor
					{
						launchName = applyName;
						
						for (ASTPatternListTypePair param: imop.parameterPatterns)
						{
							for (ASTPattern p: param.patterns)
							{
								applyArgs.add(new JSONObject("name", p.toString(), "type", fix(param.type)));
							}
						}
					}
				}
			}

			if (launchName != null)
			{
				JSONArray constructors = null;
				
				if (cls != null && needsCtor)
				{
					constructors = new JSONArray();
					
					for (ASTDefinition cdef: cls.definitions)
					{
						if (cdef instanceof ASTExplicitOperationDefinition)
						{
							ASTExplicitOperationDefinition exop = (ASTExplicitOperationDefinition)cdef;
							
							if (exop.name.getName().equals(defaultName))
							{
								ASTOperationType optype = (ASTOperationType) exop.type;
								constructors.add(getParams(exop.parameterPatterns, optype.parameters));
							}
						}
						else if (cdef instanceof ASTImplicitOperationDefinition)
						{
							ASTImplicitOperationDefinition imop = (ASTImplicitOperationDefinition)cdef;
							
							if (imop.name.getName().equals(defaultName))
							{
								constructors.add(getParams(imop.parameterPatterns));
							}
						}
					}
					
					if (constructors.isEmpty())
					{
						// create an entry for the default constructor, new C()
						constructors.add(new JSONArray());
					}
				}
			
				results.add(makeLens(def.location, "Launch", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, false, constructors, applyName, applyTypes, applyArgs)));
					
				results.add(makeLens(def.location, "Debug", CODE_LENS_COMMAND,
						launchArgs(launchName, defaultName, true, constructors, applyName, applyTypes, applyArgs)));
			}
		}
		
		return results;
	}

	private JSONArray getParams(ASTPatternList patterns, ASTTypeList types)
	{
		JSONArray params = new JSONArray();
		int i = 0;
		
		for (ASTPattern p: patterns)
		{
			params.add(new JSONObject("name", p.toString(), "type", fix(types.get(i++))));
		}
		
		return params;
	}
	
	private JSONArray getParams(ASTPatternListTypePairList ptList)
	{
		JSONArray params = new JSONArray();

		for (ASTPatternListTypePair param: ptList)
		{
			for (ASTPattern p: param.patterns)
			{
				params.add(new JSONObject("name", p.toString(), "type", fix(param.type)));
			}
		}
		
		return params;
	}
	
	private String fix(ASTType type)
	{
		if (type instanceof ASTUnresolvedType)
		{
			ASTUnresolvedType ut = (ASTUnresolvedType)type;
			return ut.typename.name;
		}
		else
		{
			return type.toString();
		}
	}

	private boolean isPublic(ASTDefinition def)
	{
		if (Settings.dialect != Dialect.VDM_SL)
		{
			return def.accessSpecifier.access == Token.PUBLIC;	// Not private or protected
		}
		else
		{
			return true;
		}
	}
}
