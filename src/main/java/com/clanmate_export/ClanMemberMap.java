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
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.clanmate_export;

/**
 * A simple mapping of RSN -> Rank, joined date, and last seen date.
 */
public class ClanMemberMap
{
    /**
     * The RuneScape player's name.
     */
    private String rsn;

    /**
     * The RuneScape player's rank.
     */
    private String rank;

    /**
     * Date the RuneScape player joined the clan.
     */
    private String joinedDate;

    /**
     * Date the RuneScape player was last seen.
     */
    private String lastSeen;

    /**
     * Initialize a clan member without last seen data.
     *
     * @param rsn        the player name
     * @param rank       the player rank
     * @param joinedDate date player joined the clan
     */
    public ClanMemberMap(String rsn, String rank, String joinedDate)
    {
        this.rsn = rsn;
        this.rank = rank;
        this.joinedDate = joinedDate;
        this.lastSeen = "";
    }

    /**
     * Initialize a clan member with all export fields.
     *
     * @param rsn        the player name
     * @param rank       the player rank
     * @param joinedDate date player joined the clan
     * @param lastSeen   date player was last seen
     */
    public ClanMemberMap(String rsn, String rank, String joinedDate, String lastSeen)
    {
        this.rsn = rsn;
        this.rank = rank;
        this.joinedDate = joinedDate;
        this.lastSeen = lastSeen;
    }

    /**
     * @return the RuneScape player's name
     */
    public String getRSN()
    {
        return this.rsn;
    }

    /**
     * @return the RuneScape player's rank
     */
    public String getRank()
    {
        return this.rank;
    }

    /**
     * @return the RuneScape player's joined date
     */
    public String getJoinedDate()
    {
        return this.joinedDate;
    }

    /**
     * @return the RuneScape player's last seen date
     */
    public String getLastSeen()
    {
        return this.lastSeen;
    }

    /**
     * Updates the RuneScape player's last seen date.
     *
     * @param lastSeen date player was last seen
     */
    public void setLastSeen(String lastSeen)
    {
        this.lastSeen = lastSeen;
    }
}