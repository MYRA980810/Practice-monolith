package com.livecomerce.live.application;

import com.livecomerce.live.application.port.out.LoadLivePort.CategoryLiveCount;

import java.util.List;

/**
 * Point-in-time copy of the active-lives feed: every LIVE card (newest first, {@code
 * currentViewers} always 0 — viewer counts go stale and are filled at read time) and the
 * per-category counts, all at snapshot {@code version}.
 */
public record LiveFeedSnapshot(long version, List<LiveFeedCard> cards, List<CategoryLiveCount> counts) {}
