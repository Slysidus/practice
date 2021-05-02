package com.oxymore.practice.configuration;

import com.oxymore.practice.configuration.match.ArenaConfiguration;
import com.oxymore.practice.configuration.match.EloSearchConfiguration;
import com.oxymore.practice.configuration.match.GeneralArenasConfiguration;
import com.oxymore.practice.configuration.match.MatchMode;
import com.oxymore.practice.configuration.ui.ViewConfiguration;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
public class PracticeConfiguration {
    public final String mongoDBConnectionUrl;

    public final int endTeleportTime;
    public final boolean hideConnectMessages;
    public final boolean pingCommandEnabled;

    public final Map<String, ViewConfiguration> views;

    public final EloSearchConfiguration eloSearch;
    public final GeneralArenasConfiguration arena;
    public final List<ArenaConfiguration> arenas;
    public final List<MatchMode> matchModes;
}
