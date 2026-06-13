package com.clanmate_export;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("clanmateexport")
public interface ClanMateExportConfig extends Config
{
	@ConfigItem(
			keyName = "dataExportFormat",
			name = "Data export format",
			description = "The format to export clan member data in",
			position = 0
	)
	default ClanMateExportDataFormat getDataExportFormat()
	{
		return ClanMateExportDataFormat.CSV;
	}

	@ConfigItem(
			keyName = "exportToClipboard",
			name = "Export to clipboard",
			description = "Copies exported clan member data to your clipboard",
			position = 1
	)
	default boolean exportToClipBoard()
	{
		return true;
	}

	@ConfigItem(
			keyName = "exportUserNamesOnly",
			name = "Export usernames only",
			description = "Only export clan member usernames",
			position = 2
	)
	default boolean getExportUserNamesOnly()
	{
		return false;
	}

	@ConfigItem(
			keyName = "captureLastSeen",
			name = "Capture Last Seen",
			description = "Use the two-step export flow to capture Last Seen. Disable this to export only RSN, Rank, and Joined.",
			position = 3
	)
	default boolean getCaptureLastSeen()
	{
		return true;
	}

	@ConfigItem(
			keyName = "showHelperText",
			name = "Show helper text",
			description = "Shows helper text when opening the clan settings screen",
			position = 4
	)
	default boolean getShowHelperText()
	{
		return true;
	}

	@ConfigItem(
			keyName = "sendWebRequest",
			name = "Send web request",
			description = "Allows exporting clan member data to a configured URL",
			position = 5
	)
	default boolean getSendWebRequest()
	{
		return false;
	}

	@ConfigItem(
			keyName = "dataUrl",
			name = "Data URL",
			description = "URL to send exported clan member data to",
			position = 6
	)
	default String getDataUrl()
	{
		return "";
	}
}