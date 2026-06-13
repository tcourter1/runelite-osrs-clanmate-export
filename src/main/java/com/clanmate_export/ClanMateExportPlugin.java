/*
 * Copyright (c) 2021, Bailey Townsend <baileytownsend2323@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT INCLUDING NEGLIGENCE OR OTHERWISE ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.clanmate_export;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.util.Text;
import net.runelite.http.api.RuneLiteAPI;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static net.runelite.http.api.RuneLiteAPI.JSON;

@Slf4j
@PluginDescriptor(
		name = "Clanmate Export"
)
public class ClanMateExportPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClanMateExportConfig config;

	@Inject
	private ClanMateExportChatMenuManager clanMateExportChatMenuManager;

	@Inject
	private OkHttpClient webClient;

	private static final Gson GSON = RuneLiteAPI.GSON;

	private static final int CLAN_SETTINGS_INFO_PAGE_WIDGET = 690;

	private static final int CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID = 693;
	private static final int CLAN_SETTINGS_MEMBERS_LIST_RSN_COLUMN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 10);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_FIRST_COLUMN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 11);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_SECOND_COLUMN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 13);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_FIRST_DROP_DOWN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 7);
	private static final int CLAN_SETTINGS_MEMBERS_LIST_SECOND_DROP_DOWN = WidgetInfo.PACK(CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID, 8);

	private static final String COLUMN_RANK = "Rank";
	private static final String COLUMN_JOINED = "Joined";
	private static final String COLUMN_LAST_SEEN = "Last seen";

	/**
	 * The clan members, scraped from the clan setup widget.
	 *
	 * Last Seen enabled:
	 * Step 1 fills RSN, rank, and joined date.
	 * Step 2 fills last seen by matching back to RSN.
	 *
	 * Last Seen disabled:
	 * Single-step export fills RSN, rank, and joined date only.
	 */
	private List<ClanMemberMap> clanMembers = null;

	@Provides
	ClanMateExportConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanMateExportConfig.class);
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widget)
	{
		if (widget.getGroupId() == CLAN_SETTINGS_INFO_PAGE_WIDGET && config.getShowHelperText())
		{
			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.OPEN_MEMBERS_SCREEN);
		}

		if (widget.getGroupId() == CLAN_SETTINGS_MEMBERS_PAGE_WIDGET_ID)
		{
			if (this.client.getWidget(693, 9) == null)
			{
				this.clanMembers = null;
			}
			else
			{
				clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SHOW_EXPORT_OPTIONS);
			}
		}
	}

	public boolean isRankAndJoinedCaptured()
	{
		return this.clanMembers != null && this.clanMembers.size() != 0;
	}

	/**
	 * Step 1:
	 * Capture RSN, Rank, and Joined.
	 *
	 * Expected visible columns:
	 * Username | Rank | Joined
	 */
	public void CaptureRankAndJoined()
	{
		if (!captureRankAndJoined())
		{
			return;
		}

		clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SHOW_EXPORT_OPTIONS);
	}

	/**
	 * Single-step clipboard export when Last Seen capture is disabled.
	 *
	 * Expected visible columns:
	 * Username | Rank | Joined
	 */
	public void ExportRankAndJoinedToClipBoard()
	{
		if (!captureRankAndJoined())
		{
			return;
		}

		exportCurrentMembersToClipBoard();
	}

	/**
	 * Single-step URL export when Last Seen capture is disabled.
	 *
	 * Expected visible columns:
	 * Username | Rank | Joined
	 */
	public void ExportRankAndJoinedToUrl()
	{
		if (!captureRankAndJoined())
		{
			return;
		}

		SendClanMembersToUrl();
	}

	/**
	 * Step 2:
	 * Capture Last seen, merge it into the previously captured member list,
	 * then export to clipboard.
	 *
	 * Expected visible columns:
	 * Username | Rank | Last seen
	 */
	public void CaptureLastSeenAndExportToClipBoard()
	{
		if (!config.getCaptureLastSeen())
		{
			ExportRankAndJoinedToClipBoard();
			return;
		}

		if (!captureLastSeen())
		{
			return;
		}

		exportCurrentMembersToClipBoard();
	}

	/**
	 * Step 2:
	 * Capture Last seen, merge it into the previously captured member list,
	 * then send to the configured URL.
	 *
	 * Expected visible columns:
	 * Username | Rank | Last seen
	 */
	public void CaptureLastSeenAndSendToUrl()
	{
		if (!config.getCaptureLastSeen())
		{
			ExportRankAndJoinedToUrl();
			return;
		}

		if (!captureLastSeen())
		{
			return;
		}

		SendClanMembersToUrl();
	}

	/**
	 * Backwards-compatible method name for older menu references.
	 */
	public void ClanToClipBoard()
	{
		if (config.getCaptureLastSeen())
		{
			CaptureRankAndJoined();
		}
		else
		{
			ExportRankAndJoinedToClipBoard();
		}
	}

	private boolean captureRankAndJoined()
	{
		if (!validateColumns(COLUMN_RANK, COLUMN_JOINED))
		{
			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.CHECK_COLUMNS_RANKED_JOINED);
			return false;
		}

		this.clanMembers = scrapeRankAndJoinedMembers();

		return this.clanMembers != null && this.clanMembers.size() != 0;
	}

	/**
	 * Capture the Last seen column and merge it into the members captured during step 1.
	 *
	 * @return true if last seen data was captured successfully
	 */
	private boolean captureLastSeen()
	{
		if (this.clanMembers == null || this.clanMembers.size() == 0)
		{
			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.CHECK_COLUMNS_RANKED_JOINED);
			return false;
		}

		if (!validateColumns(COLUMN_RANK, COLUMN_LAST_SEEN))
		{
			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.CHECK_COLUMNS_RANKED_LAST_SEEN);
			return false;
		}

		List<ClanMemberMap> lastSeenMembers = scrapeRankAndLastSeenMembers();

		if (lastSeenMembers == null || lastSeenMembers.size() == 0)
		{
			return false;
		}

		mergeLastSeen(lastSeenMembers);
		return true;
	}

	/**
	 * Scrapes RSN, Rank, and Joined from the current members table.
	 *
	 * @return list of clan members with RSN, rank, and joined date
	 */
	private List<ClanMemberMap> scrapeRankAndJoinedMembers()
	{
		List<ClanMemberMap> scrapedMembers = new ArrayList<>();

		Widget clanMemberNamesWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_RSN_COLUMN);
		Widget rankWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_FIRST_COLUMN);
		Widget joinedWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_SECOND_COLUMN);

		if (clanMemberNamesWidget == null || rankWidget == null || joinedWidget == null)
		{
			return scrapedMembers;
		}

		Widget[] clanMemberNamesWidgetValues = clanMemberNamesWidget.getChildren();
		Widget[] rankWidgetValues = rankWidget.getChildren();
		Widget[] joinedWidgetValues = joinedWidget.getChildren();

		if (clanMemberNamesWidgetValues == null || rankWidgetValues == null || joinedWidgetValues == null)
		{
			return scrapedMembers;
		}

		int lastSuccessfulRsnIndex = 0;
		int otherColumnsPositions = 0;

		for (int i = 0; i < clanMemberNamesWidgetValues.length; i++)
		{
			int valueOfRsnToGet;

			if (i == 0)
			{
				valueOfRsnToGet = 1;
			}
			else
			{
				valueOfRsnToGet = lastSuccessfulRsnIndex + 3;
			}

			boolean inBounds = valueOfRsnToGet >= 0 && valueOfRsnToGet < clanMemberNamesWidgetValues.length;

			if (inBounds)
			{
				int clanMemberCount = Objects.requireNonNull(this.client.getClanSettings()).getMembers().size();
				int otherColumnsIndex = otherColumnsPositions + clanMemberCount;

				if (otherColumnsIndex < rankWidgetValues.length && otherColumnsIndex < joinedWidgetValues.length)
				{
					String rsn = Text.removeTags(clanMemberNamesWidgetValues[valueOfRsnToGet].getText());
					String rank = Text.removeTags(rankWidgetValues[otherColumnsIndex].getText());
					String joinedDate = Text.removeTags(joinedWidgetValues[otherColumnsIndex].getText());

					ClanMemberMap clanMember = new ClanMemberMap(rsn, rank, joinedDate);
					scrapedMembers.add(clanMember);
				}

				lastSuccessfulRsnIndex = valueOfRsnToGet;
				otherColumnsPositions++;
			}
		}

		return scrapedMembers;
	}

	/**
	 * Scrapes RSN and Last seen from the current members table.
	 *
	 * The first swappable column is expected to still be Rank.
	 * The second swappable column is expected to be Last seen.
	 *
	 * @return list of clan members with RSN and last seen date
	 */
	private List<ClanMemberMap> scrapeRankAndLastSeenMembers()
	{
		List<ClanMemberMap> scrapedMembers = new ArrayList<>();

		Widget clanMemberNamesWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_RSN_COLUMN);
		Widget rankWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_FIRST_COLUMN);
		Widget lastSeenWidget = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_SECOND_COLUMN);

		if (clanMemberNamesWidget == null || rankWidget == null || lastSeenWidget == null)
		{
			return scrapedMembers;
		}

		Widget[] clanMemberNamesWidgetValues = clanMemberNamesWidget.getChildren();
		Widget[] rankWidgetValues = rankWidget.getChildren();
		Widget[] lastSeenWidgetValues = lastSeenWidget.getChildren();

		if (clanMemberNamesWidgetValues == null || rankWidgetValues == null || lastSeenWidgetValues == null)
		{
			return scrapedMembers;
		}

		int lastSuccessfulRsnIndex = 0;
		int otherColumnsPositions = 0;

		for (int i = 0; i < clanMemberNamesWidgetValues.length; i++)
		{
			int valueOfRsnToGet;

			if (i == 0)
			{
				valueOfRsnToGet = 1;
			}
			else
			{
				valueOfRsnToGet = lastSuccessfulRsnIndex + 3;
			}

			boolean inBounds = valueOfRsnToGet >= 0 && valueOfRsnToGet < clanMemberNamesWidgetValues.length;

			if (inBounds)
			{
				int clanMemberCount = Objects.requireNonNull(this.client.getClanSettings()).getMembers().size();
				int otherColumnsIndex = otherColumnsPositions + clanMemberCount;

				if (otherColumnsIndex < rankWidgetValues.length && otherColumnsIndex < lastSeenWidgetValues.length)
				{
					String rsn = Text.removeTags(clanMemberNamesWidgetValues[valueOfRsnToGet].getText());
					String rank = Text.removeTags(rankWidgetValues[otherColumnsIndex].getText());
					String lastSeen = Text.removeTags(lastSeenWidgetValues[otherColumnsIndex].getText());

					ClanMemberMap clanMember = new ClanMemberMap(rsn, rank, "", lastSeen);
					scrapedMembers.add(clanMember);
				}

				lastSuccessfulRsnIndex = valueOfRsnToGet;
				otherColumnsPositions++;
			}
		}

		return scrapedMembers;
	}

	/**
	 * Merge last seen values into the main clan member list by RSN.
	 *
	 * @param lastSeenMembers members scraped during step 2
	 */
	private void mergeLastSeen(List<ClanMemberMap> lastSeenMembers)
	{
		for (ClanMemberMap existingMember : this.clanMembers)
		{
			for (ClanMemberMap lastSeenMember : lastSeenMembers)
			{
				if (existingMember.getRSN().equals(lastSeenMember.getRSN()))
				{
					existingMember.setLastSeen(lastSeenMember.getLastSeen());
					break;
				}
			}
		}
	}

	/**
	 * Validate that the two swappable table columns are set to the expected values.
	 *
	 * @param expectedFirstColumn  expected text for the first swappable column
	 * @param expectedSecondColumn expected text for the second swappable column
	 * @return true if both columns match
	 */
	private boolean validateColumns(String expectedFirstColumn, String expectedSecondColumn)
	{
		Widget firstDropDown = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_FIRST_DROP_DOWN);
		Widget secondDropDown = this.client.getWidget(CLAN_SETTINGS_MEMBERS_LIST_SECOND_DROP_DOWN);

		if (firstDropDown == null || secondDropDown == null)
		{
			return false;
		}

		Widget[] firstColumnName = firstDropDown.getChildren();
		Widget[] secondColumnName = secondDropDown.getChildren();

		if (firstColumnName == null || secondColumnName == null)
		{
			return false;
		}

		if (firstColumnName.length <= 4 || secondColumnName.length <= 4)
		{
			return false;
		}

		String firstColumnText = Text.removeTags(firstColumnName[4].getText());
		String secondColumnText = Text.removeTags(secondColumnName[4].getText());

		return expectedFirstColumn.equals(firstColumnText) && expectedSecondColumn.equals(secondColumnText);
	}

	private void exportCurrentMembersToClipBoard()
	{
		if (this.config.exportToClipBoard())
		{
			String clipBoardString = "";

			switch (this.config.getDataExportFormat())
			{
				case JSON:
					clipBoardString = toJson(this.clanMembers);
					break;
				case CSV:
					clipBoardString = toCSV(this.clanMembers);
					break;
			}

			this.clanMembersToClipBoard(clipBoardString);
			clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SUCCESS);
		}
	}

	/**
	 * Creates a CSV string from clan members.
	 *
	 * @param clanMemberMaps Clan members info
	 * @return CSV with clan members info
	 */
	private String toCSV(List<ClanMemberMap> clanMemberMaps)
	{
		if (clanMemberMaps == null || clanMemberMaps.size() == 0)
		{
			return "";
		}

		StringBuilder sb = new StringBuilder();

		if (this.config.getExportUserNamesOnly())
		{
			sb.append("RSN\n");

			for (ClanMemberMap clanMember : clanMemberMaps)
			{
				sb.append(csvEscape(clanMember.getRSN())).append("\n");
			}
		}
		else
		{
			if (config.getCaptureLastSeen())
			{
				sb.append("RSN,Rank,Joined Date,Last Seen\n");
			}
			else
			{
				sb.append("RSN,Rank,Joined Date\n");
			}

			for (ClanMemberMap clanMember : clanMemberMaps)
			{
				sb.append(csvEscape(clanMember.getRSN())).append(",");
				sb.append(csvEscape(clanMember.getRank())).append(",");
				sb.append(csvEscape(clanMember.getJoinedDate()));

				if (config.getCaptureLastSeen())
				{
					sb.append(",");
					sb.append(csvEscape(clanMember.getLastSeen()));
				}

				sb.append("\n");
			}
		}

		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	/**
	 * Escapes a value for safe CSV output.
	 *
	 * @param value raw value
	 * @return escaped CSV value
	 */
	private String csvEscape(String value)
	{
		if (value == null)
		{
			return "";
		}

		if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r"))
		{
			return "\"" + value.replace("\"", "\"\"") + "\"";
		}

		return value;
	}

	private String toJson(List<ClanMemberMap> clanMemberMaps)
	{
		return GSON.toJson(clanMemberMaps);
	}

	/**
	 * Exports clanmembers to clipboard.
	 */
	private void clanMembersToClipBoard(String clipboardString)
	{
		if (this.clanMembers.size() != 0)
		{
			StringSelection stringSelection = new StringSelection(clipboardString);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(stringSelection, null);
		}
	}

	/**
	 * Exports clanmembers to remote URL.
	 */
	public void SendClanMembersToUrl()
	{
		if (this.clanMembers != null && this.clanMembers.size() != 0)
		{
			try
			{
				String clanName = Objects.requireNonNull(this.client.getClanSettings()).getName();
				ClanMateExportWebRequestModel webRequestModel = new ClanMateExportWebRequestModel(clanName, this.clanMembers);

				final Request request = new Request.Builder()
						.post(RequestBody.create(JSON, GSON.toJson(webRequestModel)))
						.url(config.getDataUrl())
						.build();

				webClient.newCall(request).enqueue(new Callback()
				{
					@Override
					public void onFailure(Call call, IOException e)
					{
						clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.WEB_REQUEST_FAILED);
					}

					@Override
					public void onResponse(Call call, Response response) throws IOException
					{
						if (response.isSuccessful())
						{
							clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.SUCCESS);
						}
						else
						{
							clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.WEB_REQUEST_FAILED);
						}
					}
				});
			}
			catch (Exception e)
			{
				clanMateExportChatMenuManager.update(ClanMateExportChatMenuManager.WhatToShow.WEB_REQUEST_FAILED);
			}
		}
	}
}