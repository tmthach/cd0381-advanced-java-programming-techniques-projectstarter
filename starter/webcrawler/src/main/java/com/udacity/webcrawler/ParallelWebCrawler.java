package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
  private final Clock clock;
  private final PageParserFactory parserFactory;
  private final Duration timeout;
  private final int popularWordCount;
  private final ForkJoinPool pool;
  private List<Pattern> ignoredUrls;
  private final int maxDepth;

  @Inject
  ParallelWebCrawler(
      Clock clock,
      PageParserFactory parserFactory,
      @Timeout Duration timeout,
      @PopularWordCount int popularWordCount,
      @TargetParallelism int threadCount,
      @MaxDepth int maxDepth,
      @IgnoredUrls List<Pattern> ignoredUrls) {
    this.clock = clock;
    this.parserFactory = parserFactory;
    this.timeout = timeout;
    this.popularWordCount = popularWordCount;
    this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
    this.maxDepth = maxDepth;
    this.ignoredUrls = ignoredUrls;
    
  }

  @Override
  public CrawlResult crawl(List<String> startingUrls) {
	  Instant deadline = clock.instant().plus(timeout);
	  // map counts
	  ConcurrentHashMap<String, Integer> counts = new ConcurrentHashMap<>();
	  // set visitedUrls
	  ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
	  
	  for (String url : startingUrls) {
	      pool.invoke(new crawlInternal(url, deadline, maxDepth, counts, visitedUrls));
	   }
	  if (counts.isEmpty()) {
		  return new CrawlResult.Builder()
		      .setWordCounts(counts)
		      .setUrlsVisited(visitedUrls.size())
		      .build();
		}
		return new CrawlResult.Builder()
		    .setWordCounts(WordCounts.sort(counts, popularWordCount))
		    .setUrlsVisited(visitedUrls.size())
		    .build();
  }
  //# RecursiveAction
  private class crawlInternal extends RecursiveAction {
      private final String url;
      private final Instant deadline;
      private final int maxDepth;
      private final ConcurrentHashMap<String, Integer> counts;
//      private final HashMap<String, Integer> counts;
//      private final ConcurrentSkipListSet<String> visitedUrls;
      private final ConcurrentSkipListSet<String> visitedUrls;
	  public crawlInternal(String url, Instant deadline, int maxDepth, 
				ConcurrentHashMap<String, Integer> counts, ConcurrentSkipListSet<String> visitedUrls) {
		  this.url = url;
		  this.deadline = deadline;
		  this.maxDepth= maxDepth;
		  this.counts = counts;
		  this.visitedUrls = visitedUrls;
		}
		@Override
		protected void compute() {
		for (Pattern pattern : ignoredUrls) {
		    if (pattern.matcher(url).matches()) {
		      return;
		    }
		  }
		  if (maxDepth == 0 || clock.instant().isAfter(deadline) || visitedUrls.contains(url)) {
		    return;
		  }
		  
		  visitedUrls.add(url);
		  
		  PageParser.Result result = parserFactory.get(url).parse();
		  for (ConcurrentHashMap.Entry<String, Integer> resultChild : result.getWordCounts().entrySet()) {
			String keyResult = resultChild.getKey();
			BiFunction<? super String, ? super Integer, ? extends Integer> bitFunction = (key,value) -> (value == null) ? resultChild.getValue(): resultChild.getValue() + value;
//		    counts.compute(keyResult, (key,value) -> (value == null) ? resultChild.getValue(): resultChild.getValue() + value);
			// recursive
			counts.compute(keyResult, bitFunction);
		  }
		  List<crawlInternal> listSubtasks = new ArrayList<>();
		  // subTask add more task
		  for (String link : result.getLinks()) {
			  listSubtasks.add(new crawlInternal(link, deadline, maxDepth - 1, counts, visitedUrls));
		  }
		  invokeAll(listSubtasks);
		}
}
  @Override
  public int getMaxParallelism() {
    return Runtime.getRuntime().availableProcessors();
  }
}
