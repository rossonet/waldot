/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package net.rossonet.waldot.gremlin.opcgraph;

import org.apache.tinkerpop.gremlin.features.AbstractGuiceFactory;
import org.apache.tinkerpop.gremlin.features.World;
import org.junit.runner.RunWith;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Provides;
import com.google.inject.Stage;

import io.cucumber.guice.CucumberModules;
import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;

@RunWith(Cucumber.class)
@CucumberOptions(tags = "@AllowNullPropertyValues", glue = {
		"org.apache.tinkerpop.gremlin.features" }, objectFactory = OpcGraphAllowNullFeatureTest.TinkerGraphGuiceFactory.class, features = {
				"classpath:/org/apache/tinkerpop/gremlin/test/features" }, plugin = { "progress",
						"junit:target/cucumber.xml" })
public class OpcGraphAllowNullFeatureTest {

	public static final class ServiceModule extends AbstractModule {
		@Provides
		static OpcWorld.NullWorld provideNullWorld() {
			return new OpcWorld.NullWorld(new OpcWorld.TinkerGraphWorld());
		}

		@Override
		protected void configure() {
			bind(World.class).to(OpcWorld.NullWorld.class);
		}
	}

	public static class TinkerGraphGuiceFactory extends AbstractGuiceFactory {
		public TinkerGraphGuiceFactory() {
			super(Guice.createInjector(Stage.PRODUCTION, CucumberModules.createScenarioModule(), new ServiceModule()));
		}
	}
}
