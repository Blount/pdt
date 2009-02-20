/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.php.internal.core.typeinference.evaluators;

import org.eclipse.dltk.ast.references.SimpleReference;
import org.eclipse.dltk.ti.GoalState;
import org.eclipse.dltk.ti.IContext;
import org.eclipse.dltk.ti.goals.ExpressionTypeGoal;
import org.eclipse.dltk.ti.goals.GoalEvaluator;
import org.eclipse.dltk.ti.goals.IGoal;
import org.eclipse.dltk.ti.types.IEvaluatedType;
import org.eclipse.php.internal.core.compiler.ast.nodes.FormalParameter;
import org.eclipse.php.internal.core.compiler.ast.nodes.PHPDocBlock;
import org.eclipse.php.internal.core.compiler.ast.nodes.PHPDocTag;
import org.eclipse.php.internal.core.compiler.ast.nodes.PHPMethodDeclaration;
import org.eclipse.php.internal.core.typeinference.PHPTypeInferenceUtils;
import org.eclipse.php.internal.core.typeinference.context.MethodContext;

public class FormalParameterEvaluator extends GoalEvaluator {

	private IEvaluatedType result;

	public FormalParameterEvaluator(IGoal goal) {
		super(goal);
	}

	public IGoal[] init() {
		ExpressionTypeGoal typedGoal = (ExpressionTypeGoal) goal;
		FormalParameter parameter = (FormalParameter) typedGoal.getExpression();

		SimpleReference type = parameter.getParameterType();
		if (type != null) {
			result = PHPTypeInferenceUtils.createEvaluatedType(type);
		} else {
			IContext context = typedGoal.getContext();
			if (context instanceof MethodContext) {
				MethodContext methodContext = (MethodContext) context;
				PHPMethodDeclaration methodDeclaration = (PHPMethodDeclaration) methodContext.getMethodNode();
				PHPDocBlock docBlock = methodDeclaration.getPHPDoc();
				if (docBlock != null) {
					for (PHPDocTag tag : docBlock.getTags()) {
						if (tag.getTagKind() == PHPDocTag.PARAM) {
							SimpleReference[] references = tag.getReferences();
							if (references.length == 2) {
								if (references[0].getName().equals(parameter.getName())) {
									result = PHPTypeInferenceUtils.createEvaluatedType(references[1]);
								}
							}
						}
					}
				}
			}
		}
		return IGoal.NO_GOALS;
	}

	public Object produceResult() {
		return result;
	}

	public IGoal[] subGoalDone(IGoal subgoal, Object result, GoalState state) {
		return IGoal.NO_GOALS;
	}

}
