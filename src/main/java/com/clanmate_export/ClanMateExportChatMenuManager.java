package com.clanmate_export;

import com.google.common.util.concurrent.Runnables;
import javax.inject.Inject;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.game.chatbox.ChatboxTextMenuInput;

public class ClanMateExportChatMenuManager
{
	@Inject
	private ChatboxPanelManager chatboxPanelManager;

	@Inject
	private ClanMateExportPlugin plugin;

	@Inject
	private ClanMateExportConfig config;

	public enum WhatToShow
	{
		OPEN_MEMBERS_SCREEN,
		CHECK_COLUMNS_RANKED_JOINED,
		CHECK_COLUMNS_RANKED_LAST_SEEN,
		RANK_JOINED_CAPTURED,
		SUCCESS,
		SHOW_EXPORT_OPTIONS,
		WEB_REQUEST_FAILED
	}

	public void update(WhatToShow whatToShow)
	{
		switch (whatToShow)
		{
			case OPEN_MEMBERS_SCREEN:
				chatboxPanelManager.openTextMenuInput("To export Clanmembers click 'Members' on left side.<br>(This can be disabled in the plugin settings).")
						.option("Okay", Runnables.doNothing())
						.build();
				break;

			case CHECK_COLUMNS_RANKED_JOINED:
				chatboxPanelManager.openTextMenuInput("Set the member list columns to:<br><br>1st column: Rank<br>2nd column: Joined")
						.option("Okay", Runnables.doNothing())
						.build();
				break;

			case CHECK_COLUMNS_RANKED_LAST_SEEN:
				chatboxPanelManager.openTextMenuInput("Set the member list columns to:<br><br>1st column: Rank<br>2nd column: Last seen")
						.option("Okay", Runnables.doNothing())
						.build();
				break;

			case RANK_JOINED_CAPTURED:
			case SHOW_EXPORT_OPTIONS:
				addChoices();
				break;

			case SUCCESS:
				chatboxPanelManager.openTextMenuInput("Clan member export complete.")
						.option("Okay", Runnables.doNothing())
						.build();
				break;

			case WEB_REQUEST_FAILED:
				chatboxPanelManager.openTextMenuInput("Failed to send clan member data to URL.")
						.option("Okay", Runnables.doNothing())
						.build();
				break;
		}
	}

	private void addChoices()
	{
		if (config.getCaptureLastSeen())
		{
			addTwoStepChoices();
		}
		else
		{
			addSingleStepChoices();
		}
	}

	private void addTwoStepChoices()
	{
		ChatboxTextMenuInput chatboxTextMenuInput;

		if (plugin.isRankAndJoinedCaptured())
		{
			chatboxTextMenuInput = chatboxPanelManager.openTextMenuInput(
					"Rank + Joined captured.<br>Change the 3rd column to Last seen, then export."
			);

			chatboxTextMenuInput.option("1. Capture Last Seen + Export to clipboard.", plugin::CaptureLastSeenAndExportToClipBoard);

			if (config.getSendWebRequest())
			{
				chatboxTextMenuInput.option("2. Capture Last Seen + Export to URL.", plugin::CaptureLastSeenAndSendToUrl);
				chatboxTextMenuInput.option("3. Cancel.", Runnables.doNothing());
			}
			else
			{
				chatboxTextMenuInput.option("2. Cancel.", Runnables.doNothing());
			}
		}
		else
		{
			chatboxTextMenuInput = chatboxPanelManager.openTextMenuInput("Select an export step.");

			chatboxTextMenuInput.option("1. Capture Rank + Joined.", plugin::CaptureRankAndJoined);
			chatboxTextMenuInput.option("2. Cancel.", Runnables.doNothing());
		}

		chatboxTextMenuInput.build();
	}

	private void addSingleStepChoices()
	{
		ChatboxTextMenuInput chatboxTextMenuInput = chatboxPanelManager.openTextMenuInput(
				"Capture Last Seen is disabled.<br>Exporting RSN, Rank, and Joined only."
		);

		chatboxTextMenuInput.option("1. Export Rank + Joined to clipboard.", plugin::ExportRankAndJoinedToClipBoard);

		if (config.getSendWebRequest())
		{
			chatboxTextMenuInput.option("2. Export Rank + Joined to URL.", plugin::ExportRankAndJoinedToUrl);
			chatboxTextMenuInput.option("3. Cancel.", Runnables.doNothing());
		}
		else
		{
			chatboxTextMenuInput.option("2. Cancel.", Runnables.doNothing());
		}

		chatboxTextMenuInput.build();
	}
}