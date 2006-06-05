/*******************************************************************************
 * Copyright (c) 2006 Zend Corporation and IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Zend and IBM - Initial implementation
 *******************************************************************************/
/*
 * GetVariableValueResponse.java
 *
 * Created on 29 ����? 2001, 16:40
 */

package org.eclipse.php.debug.core.debugger.messages;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.php.debug.core.communication.CommunicationUtilities;

/**
 * @author guy
 */
public class GetVariableValueResponse extends DebugMessageResponseImpl implements IDebugResponseMessage {

	private String variableValue = null;

	/**
	 * Sets the DefaultExpression result.
	 */
	public void setVarResult(String varResult) {
		variableValue = varResult;
	}

	/**
	 * Returns the DefaultExpression result.
	 */
	public String getVarResult() {
		return variableValue;
	}

	public void deserialize(DataInputStream in) throws IOException {
		setID(in.readInt());
		setStatus(in.readInt());
		setVarResult(CommunicationUtilities.readString(in));
	}

	public int getType() {
		return 1032;
	}

	public void serialize(DataOutputStream out) throws IOException {
		out.writeShort(getType());
		out.writeInt(getID());
		out.writeInt(getStatus());
		CommunicationUtilities.writeString(out, getVarResult());
	}
}