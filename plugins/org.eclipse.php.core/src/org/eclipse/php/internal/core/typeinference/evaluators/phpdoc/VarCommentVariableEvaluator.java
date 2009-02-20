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
package org.eclipse.php.internal.core.typeinference.evaluators.phpdoc;

import org.eclipse.dltk.ast.expressions.Expression;
import org.eclipse.dltk.ast.references.VariableReference;
import org.eclipse.dltk.ti.GoalState;
import org.eclipse.dltk.ti.goals.GoalEvaluator;
import org.eclipse.dltk.ti.goals.IGoal;
import org.eclipse.dltk.ti.types.IEvaluatedType;
import org.eclipse.php.internal.core.compiler.ast.nodes.VarComment;
import org.eclipse.php.internal.core.typeinference.PHPTypeInferenceUtils;
import org.eclipse.php.internal.core.typeinference.goals.phpdoc.VarCommentVariableGoal;

/**
 * This evaluator determines variable type from the attached special var comment.
 */
public class VarCommentVariableEvaluator extends GoalEvaluator {

	private IEvaluatedType result;

	public VarCommentVariableEvaluator(IGoal goal) {
		super(goal);
	}

	public IGoal[] init() {
		VarCommentVariableGoal typedGoal = (VarCommentVariableGoal) goal;

		VarComment varComment = typedGoal.getVarComment();
		if (varComment != null) {

			Expression originalVarNode = typedGoal.getVarNode();
			if (originalVarNode instanceof VariableReference) {

				String variableName = ((VariableReference) originalVarNode).getName();

				VariableReference variableReference = varComment.getVariableReference();
				if (variableReference.getName().equals(variableName)) {
					result = PHPTypeInferenceUtils.createEvaluatedType(varComment.getTypeReference());
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
