/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package net.rossonet.waldot.gremlin.opcgraph.structure.io.graphson;

import org.apache.tinkerpop.gremlin.GraphProvider;
import org.apache.tinkerpop.gremlin.jsr223.JavaTranslator;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.TranslationStrategy;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.io.graphson.GraphSONVersion;

import net.rossonet.waldot.gremlin.opcgraph.OpcGraphProvider;
import net.rossonet.waldot.gremlin.opcgraph.process.computer.OpcGraphComputer;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.ProfileTest", method = "*", reason = "Tests for profile() are not supported for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.branch.BranchTest", method = "g_V_branchXlabel_eq_person__a_bX_optionXa__ageX_optionXb__langX_optionXb__nameX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.branch.ChooseTest", method = "g_V_chooseXlabel_eqXpersonX__outXknowsX__inXcreatedXX_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.DedupTest", method = "g_V_both_name_order_byXa_bX_dedup_value", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_E_filterXfalseX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_E_filterXtrueX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_V_filterXfalseX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_V_filterXlang_eq_javaX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_V_filterXname_startsWith_m_OR_name_startsWith_pX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_V_filterXtrueX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_VX1X_filterXage_gt_30X", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_VX1X_out_filterXage_gt_30X", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.FilterTest", method = "g_VX2X_filterXage_gt_30X", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.GroupTest", method = "g_V_group_byXname_substring_1X_byXconstantX1XX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.GroupTest", method = "g_V_groupXaX_byXname_substring_1X_byXconstantX1XX_capXaX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.filter.HasTest", method = "g_V_outXcreatedX_hasXname__mapXlengthX_isXgtX3XXX_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.InjectTest", method = "g_VX1X_out_name_injectXdanielX_asXaX_mapXlengthX_path", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MapTest", method = "g_VX1X_mapXnameX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MapTest", method = "g_VX1X_out_mapXnameX_mapXlengthX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MapTest", method = "g_VX1X_outE_label_mapXlengthX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MapTest", method = "g_withPath_V_asXaX_out_mapXa_nameX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MapTest", method = "g_withPath_V_asXaX_out_out_mapXa_name_it_nameX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderTest", method = "g_V_hasLabelXpersonX_order_byXvalueXageX_descX_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderTest", method = "g_V_name_order_byXa1_b1X_byXb2_a2X", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderTest", method = "g_V_order_byXname_a1_b1X_byXname_b2_a2X_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.OrderTest", method = "g_VX1X_hasXlabel_personX_mapXmapXint_ageXX_orderXlocalX_byXvalues_descX_byXkeys_ascX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.branch.RepeatTest", method = "g_V_repeatXbothX_untilXname_eq_marko_or_loops_gt_1X_groupCount_byXnameX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SackTest", method = "g_withSackXmap__map_cloneX_V_out_out_sackXmap_a_nameX_sack", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_VX1X_out_sideEffectXincr_cX_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_VX1X_out_sideEffectXX_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_VX1X_sideEffectXstore_aX_name", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_withSideEffectsXa__linkedhashmapX_withSideEffectXb__arraylist__addAllX_withSideEffectXc__arrayList__addAllX_V_groupXaX_byXlabelX_byXcountX_sideEffectXb__1_2_3X_out_out_out_sideEffectXc__bob_danielX_capXaX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_withSideEffectXa_0_sumX_V_out_sideEffectXsideEffectsXa_bulkXX_capXaX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_withSideEffectXa_0X_V_out_sideEffectXsideEffectsXa_1XX_capXaX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SideEffectTest", method = "g_withSideEffectXa__linkedhashmapX_V_out_groupCountXaX_byXlabelX_out_out_capXaX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SubgraphTest", method = "g_V_withSideEffectXsgX_outEXknowsX_subgraphXsgX_name_capXsgX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SubgraphTest", method = "g_V_withSideEffectXsgX_repeatXbothEXcreatedX_subgraphXsgX_outVX_timesX5X_name_dedup", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SubgraphTest", method = "g_withSideEffectXsgX_V_hasXname_danielXout_capXsgX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.UnfoldTest", method = "g_V_valueMap_unfold_mapXkeyX", reason = "Tests that include lambdas are not supported by the test suite for remotes")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.ProgramTest", method = "*", reason = "Reason requires investigation")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.TraversalInterruptionTest", method = "*", reason = "Reason requires investigation")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.TraversalInterruptionComputerTest", method = "*", reason = "Reason requires investigation")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.EventStrategyProcessTest", method = "*", reason = "Strategy not properly supported by Bytecode based traversals")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.strategy.decoration.ElementIdStrategyProcessTest", method = "*", reason = "Strategy not properly supported by Bytecode based traversals")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.CoreTraversalTest", method = "shouldNeverPropagateANoBulkTraverser", reason = "Reason requires investigation")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.ReadTest", method = "*", reason = "read and write tests don't translate locally well because of calling iterate() inside read()/write() add a none()")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.WriteTest", method = "*", reason = "read and write tests don't translate locally well because of calling iterate() inside read()/write() add a none()")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.StoreTest", method = "g_withSideEffectXa_setX_V_both_name_storeXaX_capXaX", reason = "This test returns BulkSet which isn't supported in GraphSON 3.0 until 3.4.0.")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.AggregateTest", method = "g_withSideEffectXa_setX_V_both_name_aggregateXlocal_aX_capXaX", reason = "This test returns BulkSet which isn't supported in GraphSON 3.0 until 3.4.0.")
public abstract class AbstractTinkerGraphGraphSONTranslatorProvider extends OpcGraphProvider {

	@GraphProvider.Descriptor(computer = OpcGraphComputer.class)
	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.computer.GraphComputerTest", method = "shouldSucceedWithProperTraverserRequirements", reason = "Reason requires investigation")
	public static class TinkerGraphGraphSONv2TranslatorComputerProvider
			extends TinkerGraphGraphSONv2TranslatorProvider {

		@Override
		public GraphTraversalSource traversal(final Graph graph) {
			return super.traversal(graph).withComputer();
		}
	}

	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.OrderabilityTest", method = "g_inject_order", reason = "GraphSONv2 does not properly round trip Maps and Sets")
	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.OrderabilityTest", method = "g_inject_order_with_unknown_type", reason = "Remoting serializers only support known Gremlin types")
	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MergeEdgeTest", method = "*", reason = "Remoting serializers only support known Gremlin types")
	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.MergeVertexTest", method = "*", reason = "Remoting serializers only support known Gremlin types")
	public static class TinkerGraphGraphSONv2TranslatorProvider extends AbstractTinkerGraphGraphSONTranslatorProvider {
		public TinkerGraphGraphSONv2TranslatorProvider() {
			super(GraphSONVersion.V2_0);
		}
	}

	@GraphProvider.Descriptor(computer = OpcGraphComputer.class)
	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.computer.GraphComputerTest", method = "shouldSucceedWithProperTraverserRequirements", reason = "Reason requires investigation")
	public static class TinkerGraphGraphSONv3TranslatorComputerProvider
			extends TinkerGraphGraphSONv3TranslatorProvider {

		@Override
		public GraphTraversalSource traversal(final Graph graph) {
			return super.traversal(graph).withComputer();
		}
	}

	@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.OrderabilityTest", method = "g_inject_order_with_unknown_type", reason = "Remoting serializers only support known Gremlin types")
	public static class TinkerGraphGraphSONv3TranslatorProvider extends AbstractTinkerGraphGraphSONTranslatorProvider {
		public TinkerGraphGraphSONv3TranslatorProvider() {
			super(GraphSONVersion.V3_0);
		}
	}

	private final GraphSONVersion version;

	AbstractTinkerGraphGraphSONTranslatorProvider(final GraphSONVersion version) {
		this.version = version;
	}

	@Override
	public GraphTraversalSource traversal(final Graph graph) {
		final GraphTraversalSource g = graph.traversal();
		return g.withStrategies(
				new TranslationStrategy(g, new GraphSONTranslator<>(JavaTranslator.of(g), version), true));
	}
}