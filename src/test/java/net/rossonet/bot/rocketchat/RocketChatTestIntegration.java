package net.rossonet.bot.rocketchat;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.github.baloise.rocketchatrestclient.model.Room;

public class RocketChatTestIntegration {

	@Test
	public void sendMessageTest() throws IOException {

		final String server = System.getenv("ROCKETCHAT_SERVER");
		final String user = System.getenv("ROCKETCHAT_USER");
		final String password = System.getenv("ROCKETCHAT_PASSWORD");
		final RocketChatChannel channel = new RocketChatChannel(server, user, password);
		System.out.println("VERSION: " + channel.getServerVersion());
		final Room room = new Room("GENERAL", false);
		System.out.println(room.getUpdatedDate());
	}

}
