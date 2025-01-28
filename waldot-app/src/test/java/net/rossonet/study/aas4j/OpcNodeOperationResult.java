/*
 * Copyright (c) 2021 Fraunhofer-Gesellschaft zur Foerderung der angewandten Forschung e. V.
 * Copyright (c) 2023, SAP SE or an SAP affiliate company
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.rossonet.study.aas4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.digitaltwin.aas4j.v3.model.ExecutionState;
import org.eclipse.digitaltwin.aas4j.v3.model.Message;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationResult;
import org.eclipse.digitaltwin.aas4j.v3.model.OperationVariable;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;
import org.eclipse.digitaltwin.aas4j.v3.model.builder.OperationResultBuilder;

/**
 * Default implementation of package
 * org.eclipse.digitaltwin.aas4j.v3.model.OperationResult
 * 
 */

@IRI("aas:OperationResult")
public class OpcNodeOperationResult implements OperationResult {

	/**
	 * This builder class can be used to construct a OpcNodeOperationResult bean.
	 */
	public static class Builder extends OperationResultBuilder<OpcNodeOperationResult, Builder> {

		@Override
		protected Builder getSelf() {
			return this;
		}

		@Override
		protected OpcNodeOperationResult newBuildingInstance() {
			return new OpcNodeOperationResult();
		}
	}

	@IRI("https://admin-shell.io/aas/3/0/BaseOperationResult/executionState")
	protected ExecutionState executionState;

	@IRI("https://admin-shell.io/aas/3/0/BaseOperationResult/success")
	protected boolean success;

	@IRI("https://admin-shell.io/aas/3/0/Result/messages")
	protected List<Message> messages = new ArrayList<>();

	@IRI("https://admin-shell.io/aas/3/0/OperationResult/inoutputArguments")
	protected List<OperationVariable> inoutputArguments = new ArrayList<>();

	@IRI("https://admin-shell.io/aas/3/0/OperationResult/outputArguments")
	protected List<OperationVariable> outputArguments = new ArrayList<>();

	public OpcNodeOperationResult() {
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null) {
			return false;
		} else if (this.getClass() != obj.getClass()) {
			return false;
		} else {
			final OpcNodeOperationResult other = (OpcNodeOperationResult) obj;
			return Objects.equals(this.inoutputArguments, other.inoutputArguments)
					&& Objects.equals(this.outputArguments, other.outputArguments)
					&& Objects.equals(this.executionState, other.executionState)
					&& Objects.equals(this.success, other.success) && Objects.equals(this.messages, other.messages);
		}
	}

	@Override
	public ExecutionState getExecutionState() {
		return executionState;
	}

	@Override
	public List<OperationVariable> getInoutputArguments() {
		return inoutputArguments;
	}

	@Override
	public List<Message> getMessages() {
		return messages;
	}

	@Override
	public List<OperationVariable> getOutputArguments() {
		return outputArguments;
	}

	@Override
	public boolean getSuccess() {
		return success;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.executionState, this.success, this.messages, this.inoutputArguments,
				this.outputArguments);
	}

	@Override
	public void setExecutionState(ExecutionState executionState) {
		this.executionState = executionState;
	}

	@Override
	public void setInoutputArguments(List<OperationVariable> inoutputArguments) {
		this.inoutputArguments = inoutputArguments;
	}

	@Override
	public void setMessages(List<Message> messages) {
		this.messages = messages;
	}

	@Override
	public void setOutputArguments(List<OperationVariable> outputArguments) {
		this.outputArguments = outputArguments;
	}

	@Override
	public void setSuccess(boolean success) {
		this.success = success;
	}

	@Override
	public String toString() {
		return String.format(
				"OpcNodeOperationResult (" + "executionState=%s," + "inoutputArguments=%s," + "messages=%s,"
						+ "outputArguments=%s," + "success=%s," + ")",
				this.executionState, this.inoutputArguments, this.messages, this.outputArguments, this.success);
	}
}
