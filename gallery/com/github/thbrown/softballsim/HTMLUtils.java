package com.github.thbrown.softballsim;

import java.util.Arrays;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gitlab.GitLabExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Collection of utils for generating and processing snippits of HTML.
 */
public class HTMLUtils {

  private final Parser parser;
  private final HtmlRenderer renderer;
  private final PolicyFactory customHTMLPolicy;

  public HTMLUtils() {
    // Markdown configuration
    MutableDataSet options = new MutableDataSet();
    options.set(Parser.EXTENSIONS, Arrays.asList(
        GitLabExtension.create(),
        TablesExtension.create(),
        StrikethroughExtension.create(),
        WikiLinkExtension.create()));

    options.set(WikiLinkExtension.IMAGE_LINKS, true);

    this.parser = Parser.builder(options).build();
    this.renderer = HtmlRenderer.builder(options).build();

    // HTML sanitization configuration - policy based on Github's here:
    // https://github.com/jch/html-pipeline/blob/master/lib/html/pipeline/sanitization_filter.rb#L44-L106
    // TODO: # Top-level <li> elements are removed because they can break out of containing markup.
    // TODO: # Table child elements that are not contained by a <table> are removed.
    ImmutableSet<String> ALLOWED_CLASSES = ImmutableSet.of(
        "katex", "mermaid");
    this.customHTMLPolicy = new HtmlPolicyBuilder()
        .allowElements("h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8", "br", "b", "i", "strong", "em", "a", "pre",
            "code", "img", "tt", "div", "ins", "del", "sup", "sub", "p", "ol", "ul", "table", "thead", "tbody", "tfoot",
            "blockquote", "dl", "dt", "dd", "kbd", "q", "samp", "var", "hr", "ruby", "rt", "rp", "li", "tr", "td", "th",
            "s", "strike", "summary", "details")
        .allowUrlProtocols("http", "https")
        .allowAttributes("href").onElements("a")
        .allowAttributes("src", "longdesc").onElements("img")
        .allowAttributes("div", "longdesc").onElements("itemscope", "itemtype")
        .allowAttributes("cite").onElements("blockquote")
        .allowAttributes("cite").onElements("del")
        .allowAttributes("cite").onElements("ins")
        .allowAttributes("cite").onElements("q")
        .allowAttributes("class")
        .matching((elementName, attributeName, value) -> Joiner.on(' ')
            .join(Iterables.filter(Splitter.onPattern("\\s+").omitEmptyStrings().split(value),
                Predicates.in(ALLOWED_CLASSES))))
        .onElements("div")
        // .requireRelNofollowOnLinks()
        .toFactory();
  }

  /**
   * Sanitize HTML based on custom policy, see comments in class for details.
   */
  public String santitize(String input) {
    return this.customHTMLPolicy.sanitize(input);
  }

  /**
   * Converts markdown to HTML, sanitizes that html, and returns that result.
   */
  public String markdownify(String input) {
    Node document = parser.parse(input);
    String markdownHtml = renderer.render(document);
    return santitize(markdownHtml);
  }

}
