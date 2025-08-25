package net.rossonet.waldot.commands;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.methods.AbstractMethodInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.rossonet.waldot.api.models.WaldotNamespace;
import net.rossonet.waldot.opc.AbstractOpcCommand;

public class AboutCommand extends AbstractOpcCommand {
	private static final String LABEL_DESCRIPTION = "description";
	private static final String LABEL_LICENSE = "license";
	private static final String LABEL_LICENSE_URL = "licence URL";
	private static final String LABEL_NAME = "name";
	private static final String LABEL_REPOSITORY_URL = "repository URL";
	private static final String LICENSE = "Apache License 2.0";
	private static final String LICENSE_URL = "https://www.apache.org/licenses/LICENSE-2.0";
	private final static Logger logger = LoggerFactory.getLogger(AboutCommand.class);
	private static final String LONG_LABEL_DESCRIPTION = "description of the software";
	private static final String LONG_LABEL_LICENSE = "license of the software";
	private static final String LONG_LABEL_LICENSE_URL = "URL of the license of the software";
	private static final String LONG_LABEL_NAME = "the name of the software";
	private static final String LONG_LABEL_REPOSITORY_URL = "URL of the repository of the software";
	private static final String REPOSITORY_URL = "https://github.com/rossonet/waldot/";
	private static final String SOFTWARE_DESCRIPTION = "basic WaldOT Agent with Rule Engine";
	private static final String SOFTWARE_NAME = "WaldOT Agent";

	private static Properties loadProperties(final String propertiesFilename) {
		final Properties prop = new Properties();
		final ClassLoader loader = AboutCommand.class.getClassLoader();
		try (InputStream stream = loader.getResourceAsStream(propertiesFilename)) {
			if (stream == null) {
				throw new FileNotFoundException("git properties file not found");
			}
			prop.load(stream);
		} catch (final IOException e) {
			logger.error("loading git parameters: " + e.getMessage());
		}
		return prop;
	}

	private final List<String> aboutReplyList = new ArrayList<>();

	public AboutCommand(final WaldotNamespace waldotNamespace) {
		super(waldotNamespace.getGremlinGraph(), waldotNamespace,
				waldotNamespace.getConfiguration().getAboutCommandLabel(),
				waldotNamespace.getConfiguration().getAboutCommandDescription(),
				waldotNamespace.getConfiguration().getAboutCommandWriteMask(),
				waldotNamespace.getConfiguration().getAboutCommandUserWriteMask(),
				waldotNamespace.getConfiguration().getAboutCommandExecutable(),
				waldotNamespace.getConfiguration().getAboutCommandUserExecutable());
		super.addOutputArgument(LABEL_NAME, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_NAME));
		super.addOutputArgument(LABEL_DESCRIPTION, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_DESCRIPTION));
		super.addOutputArgument(LABEL_LICENSE, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_LICENSE));
		super.addOutputArgument(LABEL_LICENSE_URL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_LICENSE_URL));
		super.addOutputArgument(LABEL_REPOSITORY_URL, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
				LocalizedText.english(LONG_LABEL_REPOSITORY_URL));
		aboutReplyList.add(SOFTWARE_NAME);
		aboutReplyList.add(SOFTWARE_DESCRIPTION);
		aboutReplyList.add(LICENSE);
		aboutReplyList.add(LICENSE_URL);
		aboutReplyList.add(REPOSITORY_URL);
		final String propertiesFilename = "git.properties";
		final Properties gitProperties = loadProperties(propertiesFilename);
		for (final String key : gitProperties.stringPropertyNames()) {
			super.addOutputArgument(key, VariableNodeTypes.String.getNodeId(), ValueRanks.Scalar, null,
					LocalizedText.english("git property " + key));
			aboutReplyList.add(gitProperties.getProperty(key));
		}
		this.addReference(new Reference(this.getNodeId(), Identifiers.HasModellingRule,
				Identifiers.ModellingRule_Mandatory.expanded(), true));

	}

	@Override
	public Object clone() {
		return new AboutCommand(this.waldotNamespace);
	}

	@Override
	public String[] runCommand(final InvocationContext invocationContext, final String[] inputValues) {
		return aboutReplyList.toArray(new String[0]);
	}

}
