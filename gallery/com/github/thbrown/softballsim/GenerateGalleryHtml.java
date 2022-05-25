package com.github.thbrown.softballsim;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import org.junit.Test;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import com.github.thbrown.softballsim.optimizer.OptimizerDefinitionComposite;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinition;
import com.github.thbrown.softballsim.optimizer.gson.OptimizerDefinitionOption;
import com.github.thbrown.softballsim.optimizer.gson.VisibilityEnum;
import com.github.thbrown.softballsim.util.GsonAccessor;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;
import static j2html.TagCreator.*; // Use static star import

public class GenerateGalleryHtml {

  // TODO: can we just make this header using j2html?
  private static String header = "<!doctype html>"
      + "<meta content=\"text/html;charset=utf-8\" http-equiv=\"Content-Type\">\r\n"
      + "<meta content=\"utf-8\" http-equiv=\"encoding\">";

  private static HTMLUtils markdown = new HTMLUtils();

  @Test
  public void generateGalleryHtml() throws IOException {

    // Get an array of all the optimizer json files
    File[] fileList = new File("./docs/definitions").listFiles();
    List<OptimizerDefinitionComposite> definitions = new ArrayList<>(fileList.length);
    for (int i = 0; i < fileList.length; i++) {
      if (fileList[i].getName().equals("README.md")) {
        continue;
      }
      String contents = new String(Files.readAllBytes(Paths.get(fileList[i].getCanonicalPath())));
      OptimizerDefinition definition = GsonAccessor.getInstance().getCustom().fromJson(contents,
          OptimizerDefinition.class);
      if (definition.getUiVisibility() == null || definition.getUiVisibility() == VisibilityEnum.STANDERD) {
        definitions.add(new OptimizerDefinitionComposite(definition, fileList[i].getName()));
      }
    }

    // Get optimizer performance data from the file system
    List<OptimizerPerf> optimizerPerfData = new ArrayList<>();
    Scorer scorer = new Scorer();
    try {
      File myObj = new File("optimizers.tsv");
      Scanner myReader = new Scanner(myObj);
      while (myReader.hasNextLine()) {
        String data = myReader.nextLine();
        optimizerPerfData.add(new OptimizerPerf(data, scorer));
      }
      myReader.close();
    } catch (FileNotFoundException e) {
      System.out.println("An error occurred.");
      e.printStackTrace();
    }

    for (OptimizerPerf op : optimizerPerfData) {
      System.out.println(op);
    }

    // Render the remainder of the html
    ContainerTag htmlElement = html(
        head(title("Softball.app Lineup Optimizer Gallery"),
            link().withRel("stylesheet").withHref("css/main.css"),
            link().withRel("stylesheet").withHref("https://fonts.googleapis.com/css?family=Roboto"), link()
                .withRel(
                    "stylesheet")
                .withHref("https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/katex.min.css"),
            link().withRel("icon").withHref("https://softball.app/server/assets/icons/favicon.ico"),
            link().withRel("stylesheet").withHref(
                "https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/3.0.1/github-markdown.min.css"),
            script().withType("text/javascript").attr("defer")
                .withSrc("https://cdnjs.cloudflare.com/ajax/libs/mermaid/8.8.4/mermaid.min.js"),
            script().withType("text/javascript").attr("defer").attr("onload", "renderKatex()")
                .withSrc("https://cdn.jsdelivr.net/npm/katex@0.11.1/dist/katex.min.js")),
        body(main(attrs("#main.content"),
            div(attrs("#header .gallery-header-wrapper"),
                div(attrs(".gallery-header .inner"), h1("Softball.app Lineup Optimizer Gallery"))),
            div(attrs("#gallery .inner"), definitions.stream()
                .map(d -> getGalleryTile(d.getDefinition().getId(), d.getDefinition().getImageUrl(),
                    d.getDefinition().getName(), readLongDescriptionFile(d.getDefinition().getLongDescriptionFile()),
                    d.getDefinition().getOptions(),
                    d.getDefinition().getShortDescription(),
                    optimizerPerfData))
                .toArray(ContainerTag[]::new)),
            div(attrs("#optimizer-modal .modal"),
                div(attrs(".modal-content"), span(attrs(".close"), rawHtml("&times;")),
                    div(attrs("#modal-body"), div(attrs(".loader"))))))),
        script().withType("text/javascript").withSrc("js/script.js"));

    String htmlString = htmlElement.render();

    try {
      FileWriter myWriter = new FileWriter("./docs/index.html");
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

  private Tag<?> getGalleryTile(String id, String imgLink, String name, String description,
      List<OptimizerDefinitionOption> options, String shortDescription,
      List<OptimizerPerf> optimizerPerfData) {
    // This system to get the appropriate data to the modal is not great, but I
    // don't like the
    // alternatives with either:
    // Current Method: Copy the description html, name, and image from the gallery
    // tile to the modal
    // once a modal is clicked
    // Alternative 1: Store the description html, name, and image as a js object in
    // a script tag, render
    // it using js when necessary
    // Alternative 2: Save the description html on server-side (json is already
    // there) and request it
    // via ajax when the user clicks a tile.
    // Alternative 3: Render a hidden modal for each optimizer, instead of one that
    // gets it's inner html
    // set.

    // Performance numbers HTML
    List<OptimizerPerf> filtered =
        optimizerPerfData.stream().filter(v -> v.getOptimizerId() == Integer.parseInt(id)).collect(Collectors.toList());

    for (OptimizerPerf p : filtered) {
      // System.out.println(p.getOptimizer());
    }

    double avgQualityScore = filtered.stream().mapToDouble(v -> v.getQualityScore()).average().orElse(0);
    double avgSpeedScore = filtered.stream().mapToDouble(v -> v.getSpeedScore()).average().orElse(0);

    String qualityScoreHtml =
        "<div><div>Quality:</div><progress id=\"file\" max=\"100\" value=\"" + avgQualityScore * 100 + "70\"> "
            + avgQualityScore
            + " </progress></div>";
    String speedScoreHtml =
        "<div><div>Speed:</div><progress id=\"file\" max=\"100\" value=\"" + avgSpeedScore * 100 + "70\"> "
            + avgSpeedScore
            + " </progress></div>";

    // Options HTML
    String optionsHTML = "<h2>Options:</h2><div>";
    for (OptimizerDefinitionOption option : options) {
      // System.out.println(p.getOptimizer());
      if (option.getUiVisibility() != VisibilityEnum.HIDDEN) {
        optionsHTML += "<div><b>"
            + sanitizeHtml(option.getLongLabel()) + " (" + sanitizeHtml(option.getShortLabel()) + ")</b> - "
            + sanitizeHtml(option.getDescription())
            + "</div>";
      }
    }
    // TODO: filter hiddens
    if (options.size() == 0) {
      optionsHTML += "<i>There are no options for this optimizer</i>";
    }
    optionsHTML += "</div>";

    String prefixName = "optimizer-name-";
    String prefixImg = "optimizer-img-";
    String prefixDescription = "optimizer-description-";
    return div(attrs(".gallery-tile"),
        div(attrs(".img-container"), img().withSrc(imgLink).attr("width", "600").attr("height", "400")),
        div(attrs(".text-container"),
            div(attrs("#" + prefixImg + id + " .gallery-tile-img .hidden"),
                (rawHtml(sanitizeHtml(imgLink)))),
            div(attrs("#" + prefixName + id + ".gallery-tile-name"), (rawHtml(sanitizeHtml(name)))),
            div(attrs("#" + prefixDescription + id + ".gallery-tile-body"),
                rawHtml(speedScoreHtml),
                rawHtml(qualityScoreHtml),
                rawHtml(sanitizeHtml(shortDescription)),
                rawHtml(optionsHTML),
                rawHtml("<h2>Description:</h2>"),
                rawHtml(markdown.markdownify(description))),
            div(attrs(".gallery-fade")),
            div(attrs("#button- " + id + " .gallery-tile-add-button .add-button .hidden"), text("+ Add")).attr(
                "onClick",
                "selectToggleClick(\"" + id + "\",event)"))).attr("onClick",
                    "optimizerClick(\"" + id + "\",\"" + prefixName + id + "\",\"" + prefixImg + id
                        + "\",\"" + prefixDescription + id + "\")");
  }

  private String readLongDescriptionFile(String fileName) {
    try {
      return new String(Files.readAllBytes(Paths.get("./docs/descriptions/" + fileName)));
    } catch (IOException e) {
      throw new RuntimeException("Error while reading " + fileName, e);
    }
  }
}
