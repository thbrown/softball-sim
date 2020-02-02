package com.github.thbrown.softballsim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.util.GsonAccessor;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import static j2html.TagCreator.*; // Use static star import

public class GenerateGalleryHtml {

  // TODO: can we just make this header using j2html?
  private static String header = "<!doctype html>"
      + "<meta content=\"text/html;charset=utf-8\" http-equiv=\"Content-Type\">\r\n" +
      "<meta content=\"utf-8\" http-equiv=\"encoding\">";

  private static HTMLUtils markdown = new HTMLUtils();

  @Test
  public void generateGalleryHtml() throws IOException {

    // Get an array of all the optimizer json files
    File[] fileList = new File("./json").listFiles();
    List<OptimizerDefinitionComposite> definitions = new ArrayList<>(fileList.length);
    for (int i = 0; i < fileList.length; i++) {
      String contents = new String(Files.readAllBytes(Paths.get(fileList[i].getCanonicalPath())));
      OptimizerDefinition definition =
          GsonAccessor.getInstance().getCustom().fromJson(contents, OptimizerDefinition.class);
      definitions.add(new OptimizerDefinitionComposite(definition, fileList[i].getName()));
    }

    // Render the remainder of the html
    ContainerTag htmlElement = html(
        head(
            title("Softball.app Lineup Optimizer Gallery"),
            link().withRel("stylesheet").withHref("css/main.css"),
            link().withRel("stylesheet").withHref("https://fonts.googleapis.com/css?family=Roboto"),
            link().withRel("stylesheet").withHref("https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/katex.min.css"),

            link().withRel("stylesheet")
                .withHref("https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/3.0.1/github-markdown.min.css"),
            script().withType("text/javascript").attr("defer")
                .withSrc("https://cdnjs.cloudflare.com/ajax/libs/mermaid/8.4.4/mermaid.min.js"),
            script().withType("text/javascript").attr("defer").attr("onload", "renderKatex()")
                .withSrc("https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/katex.min.js")),
        body(
            main(attrs("#main.content"),
                div(attrs(".gallery-header-wrapper"),
                    div(attrs(".gallery-header .inner"),
                        h1("Softball.app Lineup Optimizer Gallery"))),
                div(attrs("#gallery .inner"),
                    definitions.stream()
                        .map(d -> getGalleryTile(d.getDefinition().getId(), d.getDefinition().getImageUrl(),
                            d.getDefinition().getName(), d.getDefinition().getDescription()))
                        .toArray(ContainerTag[]::new)),
                div(attrs("#optimizer-modal .modal"),
                    div(attrs(".modal-content"),
                        span(attrs(".close"),
                            rawHtml("&times;")),
                        div(attrs("#modal-body"),
                            div(attrs(".loader"))))))),
        script().withType("text/javascript").withSrc("js/script.js"));

    String htmlString = htmlElement.render();

    try {
      FileWriter myWriter = new FileWriter("index.html");
      myWriter.write(header + htmlString);
      myWriter.close();
      System.out.println("Successfully index.html.");
    } catch (IOException e) {
      System.out.println("An error occurred while writing index.html.");
      e.printStackTrace();
    }
  }

  private String sanitizeHtml(String input) {
    PolicyFactory policy = Sanitizers.FORMATTING.and(Sanitizers.FORMATTING);
    return policy.sanitize(input);
  }

  private Tag getGalleryTile(String id, String imgLink, String name, String description) {
    // This system to get the appropriate data to the modal is not great, but I don't like the
    // alternatives with either:
    // Current Method: Copy the description html, name, and image from the gallery tile to the modal
    // once a modal is clicked
    // Alternative 1: Store the description html, name, and image as a js object in a script tag, render
    // it using js when necessary
    // Alternative 2: Save the description html on server-side (json is already there) and request it
    // via ajax when the user clicks a tile.
    // Alternative 3: Render a hidden modal for each optimizer, instead of one that gets it's inner html
    // set.
    String prefixName = "optimizer-name-";
    String prefixImg = "optimizer-img-";
    String prefixDescription = "optimizer-description-";
    return div(attrs(".gallery-tile"),
        div(attrs(".img-container"),
            img().withSrc(imgLink).attr("width", "600").attr("height", "400")),
        div(attrs(".text-container"),
            div(attrs("#" + prefixImg + id + " .gallery-tile-img .hidden"),
                (rawHtml(sanitizeHtml(imgLink)))),
            div(attrs("#" + prefixName + id + ".gallery-tile-name"),
                (rawHtml(sanitizeHtml(name)))),
            div(attrs("#" + prefixDescription + id + ".gallery-tile-body"),
                rawHtml(markdown.markdownify(description))),
            div(attrs(".gallery-fade")),
            div(attrs(".gallery-tile-add-button .add-button"), rawHtml("+ Use")).attr("onClick",
                "addButtonClick(\"" + id + "\",event)"))).attr("onClick",
                    "optimizerClick(\"" + id + "\",\"" + prefixName + id + "\",\"" + prefixImg + id + "\",\""
                        + prefixDescription + id + "\")");
  }

}
