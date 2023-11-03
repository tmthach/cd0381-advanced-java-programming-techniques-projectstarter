package com.udacity.webcrawler.main;

import com.google.inject.Guice;
import com.udacity.webcrawler.WebCrawler;
import com.udacity.webcrawler.WebCrawlerModule;
import com.udacity.webcrawler.json.ConfigurationLoader;
import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.json.CrawlResultWriter;
import com.udacity.webcrawler.json.CrawlerConfiguration;
import com.udacity.webcrawler.profiler.Profiler;
import com.udacity.webcrawler.profiler.ProfilerModule;

import javax.inject.Inject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class WebCrawlerMain {

  private final CrawlerConfiguration config;

  private WebCrawlerMain(CrawlerConfiguration config) {
    this.config = Objects.requireNonNull(config);
  }

  @Inject
  private WebCrawler crawler;

  @Inject
  private Profiler profiler;

  private void run() throws Exception {
    Guice.createInjector(new WebCrawlerModule(config), new ProfilerModule()).injectMembers(this);

    CrawlResult result = crawler.crawl(config.getStartPages());
    CrawlResultWriter resultWriter = new CrawlResultWriter(result);
    // TODO: Write the crawl results to a JSON file (or System.out if the file name is empty)
    // check ResultPath is exised or not
    if(!config.getResultPath().isEmpty())
    {
      // get path
      Path outPath = Paths.get(config.getResultPath());
      resultWriter.write(outPath);
    }
    else {
    	// Exchange to system.out
		 Writer outWriter = new OutputStreamWriter(System.out);
	      // resultWriter write
	      resultWriter.write(outWriter);
	      // write all data
	      outWriter.flush();
    }
    
    // TODO: Write the profile data to a text file (or System.out if the file name is empty)
    // check outputPathOfProfile is exised or not
    if(!config.getProfileOutputPath().isEmpty())
    {
      // get path
      Path outPath = Paths.get(config.getProfileOutputPath());
      profiler.writeData(outPath);
    }
    else {
      // Exchange to system.out
      Writer outWriter = new OutputStreamWriter(System.out);
      // profiler write 
      profiler.writeData(outWriter);
      // write all data
      outWriter.flush();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 1) {
      System.out.println("Usage: WebCrawlerMain [starting-url]");
      return;
    }

    CrawlerConfiguration config = new ConfigurationLoader(Path.of(args[0])).load();
    
    new WebCrawlerMain(config).run();
  }
}
