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

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.ConceptDescription;
import org.eclipse.digitaltwin.aas4j.v3.model.Environment;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.annotations.IRI;
import org.eclipse.digitaltwin.aas4j.v3.model.builder.EnvironmentBuilder;

/**
 * Default implementation of package
 * org.eclipse.digitaltwin.aas4j.v3.model.Environment
 * 
 * Container for the sets of different identifiables.
 */

@IRI("aas:Environment")
public class OpcNodeEnvironment implements Environment {

	/**
	 * This builder class can be used to construct a OpcNodeEnvironment bean.
	 */
	public static class Builder extends EnvironmentBuilder<OpcNodeEnvironment, Builder> {

		@Override
		protected Builder getSelf() {
			return this;
		}

		@Override
		protected OpcNodeEnvironment newBuildingInstance() {
			return new OpcNodeEnvironment();
		}
	}

	@IRI("https://admin-shell.io/aas/3/0/Environment/assetAdministrationShells")
	protected List<AssetAdministrationShell> assetAdministrationShells = new ArrayList<>();

	@IRI("https://admin-shell.io/aas/3/0/Environment/conceptDescriptions")
	protected List<ConceptDescription> conceptDescriptions = new ArrayList<>();

	@IRI("https://admin-shell.io/aas/3/0/Environment/submodels")
	protected List<Submodel> submodels = new ArrayList<>();

	public OpcNodeEnvironment() {
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
			final OpcNodeEnvironment other = (OpcNodeEnvironment) obj;
			return Objects.equals(this.assetAdministrationShells, other.assetAdministrationShells)
					&& Objects.equals(this.submodels, other.submodels)
					&& Objects.equals(this.conceptDescriptions, other.conceptDescriptions);
		}
	}

	@Override
	public List<AssetAdministrationShell> getAssetAdministrationShells() {
		return assetAdministrationShells;
	}

	@Override
	public List<ConceptDescription> getConceptDescriptions() {
		return conceptDescriptions;
	}

	@Override
	public List<Submodel> getSubmodels() {
		return submodels;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.assetAdministrationShells, this.submodels, this.conceptDescriptions);
	}

	@Override
	public void setAssetAdministrationShells(List<AssetAdministrationShell> assetAdministrationShells) {
		this.assetAdministrationShells = assetAdministrationShells;
	}

	@Override
	public void setConceptDescriptions(List<ConceptDescription> conceptDescriptions) {
		this.conceptDescriptions = conceptDescriptions;
	}

	@Override
	public void setSubmodels(List<Submodel> submodels) {
		this.submodels = submodels;
	}

	@Override
	public String toString() {
		return String.format("OpcNodeEnvironment (" + "assetAdministrationShells=%s," + "submodels=%s,"
				+ "conceptDescriptions=%s," + ")", this.assetAdministrationShells, this.submodels,
				this.conceptDescriptions);
	}
}
