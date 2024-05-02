package net.rossonet.bot.rocketchat;

import java.io.IOException;

import com.github.baloise.rocketchatrestclient.RocketChatClient;

import net.rossonet.bot.BotChannel;

public class RocketChatChannel implements BotChannel {

	private final RocketChatClient rocketChatClient;

	public RocketChatChannel(final String serverUrl, final String user, final String password) {
		rocketChatClient = new RocketChatClient(serverUrl, user, password);

	}

	public String getServerVersion() throws IOException {
		return rocketChatClient.getServerInformation().getVersion();
	}

}
