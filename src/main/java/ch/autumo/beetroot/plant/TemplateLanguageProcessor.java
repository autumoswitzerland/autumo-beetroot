/**
 * 
 * Copyright (c) 2024 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package ch.autumo.beetroot.plant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.utils.common.Colors;

/**
 * Extract translatable text from templates.
 * 
 * Note this overwrites existing templates and columns.cfg
 * files in 'web/html' and template language translation
 * files in 'web/lang'. It always looks up these files in
 * 'web'-folder, unless a specific folder is defined, which
 * must point should a similar beetRoot web-folder structure.
 */
public class TemplateLanguageProcessor {

	// Default language template 
	private static final String LANG_TMPL_FILE = "/lang/tmpl_lang_default.properties";
	
	// List of special elements to handle
	private static final List<String> SPECIAL_ELEMENTS = Arrays.asList(
		    "script",  // JavaScript code
		    "style",   // CSS styles
		    "iframe",  // Inline frames
		    "textarea", // Text input area
		    "noscript", // Content to display if scripting is disabled
		    "title",    // Title of the document
		    "head",     // Metadata and links for the document
		    "html",     // Root element of the document
		    "body",     // Main content of the document
		    "svg",      // Scalable Vector Graphics content
		    "math",     // Mathematical markup
		    "object",   // Embedded content like multimedia
		    "embed",    // External content, like videos or interactive elements
		    "canvas",   // Drawing area for graphics
		    "template"  // Template content that is not rendered by default
		);
	
	private static final Set<String> SELF_CLOSING_TAGS = new HashSet<>();
    static {
        // Define self-closing tags
        SELF_CLOSING_TAGS.add("area");
        SELF_CLOSING_TAGS.add("base");
        SELF_CLOSING_TAGS.add("br");
        SELF_CLOSING_TAGS.add("col");
        SELF_CLOSING_TAGS.add("command");
        SELF_CLOSING_TAGS.add("embed");
        SELF_CLOSING_TAGS.add("hr");
        SELF_CLOSING_TAGS.add("img");
        SELF_CLOSING_TAGS.add("input");
        SELF_CLOSING_TAGS.add("keygen");
        SELF_CLOSING_TAGS.add("link");
        SELF_CLOSING_TAGS.add("meta");
        SELF_CLOSING_TAGS.add("param");
        SELF_CLOSING_TAGS.add("source");
        SELF_CLOSING_TAGS.add("track");
        SELF_CLOSING_TAGS.add("wbr");
    }
    
    private static int keyCounter = 1;
    private static String currentDirectory = "";
	
    /**
     * Process.
     * 
     * @param templateBaseDir HTML template base directory, usually 'web/', 
     * but if could be more specific if not all templates should be translated 
     * @param templateLangBaseDir language template base directory, usually 'web/', 
     * reads 'lang/tmpl_lang_default.properties' from there. 
     */
    public void process(String templateBaseDir, String templateLangBaseDir) {
    	
        final File baseDir = new File(templateBaseDir);
        if (!baseDir.exists() || !baseDir.isDirectory()) {
    		System.out.println(Colors.red("Given folder '"+templateBaseDir+"' doesn't exist or isn't a directory!"));
    		return;
        }

        final Path basePath = baseDir.toPath();
        
        if (templateLangBaseDir == null)
        	templateLangBaseDir = templateBaseDir;
        
        final String propertiesFilePath = templateLangBaseDir + LANG_TMPL_FILE;
        final Path propertiesFile = Paths.get(propertiesFilePath);
        
        try {
        	// Ensure that the parent directories exist
            Files.createDirectories(propertiesFile.getParent());
            
            // Write properties to the file using ISO_8859_1 (resource bundle) encoding
            final BufferedWriter propertiesWriter = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(propertiesFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND), StandardCharsets.ISO_8859_1));
        	
            // Collect all files
            final List<Path> allFiles = Files.walk(basePath)
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::toString)) // Sort alphanumerically
                    .collect(Collectors.toList());

            // Process .cfg files first
            for (Path file : allFiles) {
                if (file.toString().toLowerCase().endsWith("columns.cfg")) {
                    processCfgFile(file, basePath, propertiesWriter);
                    System.out.println(Colors.green("File '"+file.toAbsolutePath().toString()+"' processed."));                    
                }
            }

            // Process .html files
            for (Path file : allFiles) {
            	final String fName = file.toString().toLowerCase();
                if (fName.endsWith("add.html") ||
	                	fName.endsWith("edit.html") ||
	                	fName.endsWith("view.html") ||
	                	fName.endsWith("index.html")) {
                    keyCounter = 1; // Reset the counter for each HTML file
                    processHtmlFile(file, basePath, propertiesWriter);
                    System.out.println(Colors.green("File '"+file.toAbsolutePath().toString()+"' processed."));                    
                }
            }

            System.out.println(Colors.darkCyan("Language template file '"+propertiesFile.toAbsolutePath().toString()+"' written."));                    
            
            propertiesWriter.flush();
            propertiesWriter.close();
           
        } catch (IOException e) {
    		System.out.println(Colors.red("ERROR: Processing malfunction!"));
            e.printStackTrace();
        }
    }

    private static void processCfgFile(Path cfgFile, Path baseDir, BufferedWriter propertiesWriter) throws IOException {
    	
        currentDirectory = cfgFile.getParent().getFileName().toString();

        // Get relative path to the base directory
        final Path relativePath = baseDir.relativize(cfgFile);
        
        final List<String> existingLines = Files.readAllLines(cfgFile, StandardCharsets.UTF_8);
        final List<String> modifiedLines = new ArrayList<>();
        
        // Prepare to write header only if a new key is added
        final StringBuilder newKeysBuilder = new StringBuilder();
        
        for (String line : existingLines) {
            // Skip empty lines or comments
            if (line.trim().isEmpty() || line.startsWith("#")) {
            	modifiedLines.add(line); // Preserve comments and empty lines
                continue;
            }

            // Split the line into key and value
            String[] keyValue = line.split("=", 2);
            if (keyValue.length < 2) {
            	modifiedLines.add(line); // Keep invalid lines unchanged
                continue;
            }

            String key = keyValue[0].trim();
            String value = keyValue[1].trim();

            // Apply the rules
            if (key.equals("unique") || 
            		key.equals("transient") || 
            		key.startsWith("init.") || 
            		key.startsWith("list_json.") || 
            		value.equals("NO_SHOW")) {
            	
            	modifiedLines.add(line); // Preserve unchanged lines
                continue; // Skip this key-value pair
            }

            // Create the new key for the properties file
            String newKey = currentDirectory + "." + key;

            
            // Append the new key to the StringBuilder
            newKeysBuilder.append(newKey).append("=").append(value).append("\n");

            // Create the modified line
            String modifiedLine = key + "=" + BaseHandler.TAG_PREFIX_LANG + newKey + "}";
            modifiedLines.add(modifiedLine);
        }
        
        // Write header to propertiesWriter if there are new keys
        if (newKeysBuilder.length() > 0) {
            propertiesWriter.newLine();
            propertiesWriter.write("# Columns: " + relativePath);
            propertiesWriter.newLine();
            propertiesWriter.write(newKeysBuilder.toString());
        }        
        
        // Write modified lines back to the original cfg file, preserving structure
        Files.write(cfgFile, modifiedLines, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private static void processHtmlFile(Path htmlFile, Path baseDir, BufferedWriter propertiesWriter) throws IOException {
    	
    	currentDirectory = htmlFile.getParent().getFileName().toString();
    	
        // Get relative path to the base directory
        final Path relativePath = baseDir.relativize(htmlFile);

        // Parse the HTML file using Jsoup
        final Document extractionDoc = Jsoup.parse(htmlFile.toFile(), "UTF-8");
        
        // Process the HTML file and extract translatable text
        final Map<String, String> translations = extractTranslationsFromHtml(extractionDoc, currentDirectory, htmlFile.getFileName().toString());

        if (!translations.isEmpty()) {
            // Write the relative path as a comment in the properties file
            propertiesWriter.newLine();
            propertiesWriter.write("# Template: " + relativePath);
            propertiesWriter.newLine();
            // Write translations to the properties file in the form of key=value
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                propertiesWriter.write(entry.getKey() + "=" + entry.getValue());
                propertiesWriter.newLine();
            }
        }
        
        final Document updateDoc = Jsoup.parse(htmlFile.toFile(), "UTF-8");
        // Update the HTML file by replacing text with translation keys
        updateHtmlWithKeys(updateDoc, translations);

        
        List<Node> kids = updateDoc.body().childNodes();
        if (kids.isEmpty()) // special cases
        	kids = updateDoc.head().childNodes();
        // Overwrite the original HTML file with the modified content
        try (BufferedWriter htmlWriter = Files.newBufferedWriter(htmlFile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING)) {
        	final String t = prettyPrint(kids);
            htmlWriter.write(t);
        }
    }

    private static String prettyPrint(List<Node> nodes) {
        final StringBuilder result = new StringBuilder();
        for (Node node : nodes) {
            prettyPrint(node, result, 0); // Start indentLevel at 0 for the root children
        }
        return result.toString();
    }
    
    private static void prettyPrint(Node node, StringBuilder result, int indentLevel) {
    	
        final String indent = "    ".repeat(indentLevel); // Customize indentation (4 spaces)
        
        // Unused, we only treat edit, add, index, view templates:
        //   Check if the node is a special element
        if (node instanceof Element && SPECIAL_ELEMENTS.contains(node.nodeName())) {
            // Append the opening tag
            result.append(indent).append("<").append(node.nodeName());

            // Preserve original attributes
            Element element = (Element) node;
            element.attributes().forEach(attribute -> 
                result.append(" ").append(attribute.getKey()).append("=\"").append(attribute.getValue()).append("\"")
            );

            result.append(">\n");

            result.append(indent).append(element.ownText().trim());
            /*
            // Include text inside the special tag
            result.append(indent).append(node.childNodes().stream()
                .filter(child -> child instanceof TextNode)
                .map(Node::toString)
                .collect(Collectors.joining("\n", "", "\n")));
            */
            
            // Append the closing tag
            result.append(indent).append("</").append(node.nodeName()).append(">\n");
            return; // Return early to avoid further processing for this node
        }
        
        if (node instanceof Element) {
        	
            Element element = (Element) node;
            // Start with the opening tag and include original attributes
            result.append(indent).append("<").append(element.tagName());
            // Preserve original attributes
            element.attributes().forEach(attribute -> 
                result.append(" ").append(attribute.getKey()).append("=\"").append(attribute.getValue()).append("\"")
            );
            // Check if the tag is self-closing
            if (SELF_CLOSING_TAGS.contains(element.tagName())) {
                result.append(" />\n"); // Self-closing format
            } else {
                result.append(">\n");
                for (Node child : node.childNodes()) {
                    prettyPrint(child, result, indentLevel + 1);
                }
                result.append(indent).append("</").append(element.tagName()).append(">\n");
            }
            
        } else if (node instanceof TextNode) {
        	
        	final TextNode textNode = (TextNode) node;
            final String text = textNode.text().trim();
            if (!text.isEmpty()) {
                result.append(indent).append(encodeHtmlEntities(text)).append("\n");
            }
            
        } else if (node.nodeName().equals("#comment")) {
        	
        	result.append(indent).append(node.toString()).append("\n");
        	
        }
    }
    
    // Extracts all visible text from the HTML file and creates translation keys for them
    private static Map<String, String> extractTranslationsFromHtml(Document doc, String directoryName, String fileName) {
    	final String fnNoExt = fileName.substring(0, fileName.indexOf(".")).replace(" ", "_");
        final Map<String, String> translations = new TreeMap<>();
        // Select all elements that contain visible text, like <p>, <div>, <span>, <h1>, etc.
        final Elements elementsWithText = doc.select("*:not(script):not(style):not(meta):not(link)");
        for (Element element : elementsWithText) {
            final String text = element.ownText().trim();
            if (!ignore(text)) {
                // Generate the translation key using the directory name and counter
                final String key = directoryName + "." + fnNoExt + "." + keyCounter;
                translations.put(key, encodeHtmlEntities(text));
                // Increment the counter for each piece of text found
                keyCounter++;
            }
            // Handle the 'data-confirm-message' attribute if the element is an 'a' tag
            if (element.tagName().equals("a") && element.hasAttr("data-confirm-message")) {
                final String confirmMessage = element.attr("data-confirm-message").trim();
                if (!ignore(confirmMessage)) {
                    // Generate a key for the attribute message
                    final String keyForAttribute = directoryName + "." + fnNoExt + ".confirm_message." + keyCounter;
                    translations.put(keyForAttribute, encodeHtmlEntities(confirmMessage));
                    // Increment the counter for the attribute message
                    keyCounter++;
                }
            }            
        }
        return translations;
    }

    // Replace text in the HTML with the corresponding translation keys
    private static void updateHtmlWithKeys(Document doc, Map<String, String> translations) {
        final Elements elementsWithText = doc.select("*");
        for (Element element : elementsWithText) {
            final String text = encodeHtmlEntities(element.ownText().trim());
            if (!ignore(text)) {
	            for (Map.Entry<String, String> entry : translations.entrySet()) {
	                if (text.equals(entry.getValue())) {
	                    element.text(BaseHandler.TAG_PREFIX_LANG + entry.getKey() + "}");
	                }
	            }
            }
            // Check for 'data-confirm-message' in 'a' tags
            if (element.tagName().equals("a") && element.hasAttr("data-confirm-message")) {
                String confirmMessage = encodeHtmlEntities(element.attr("data-confirm-message").trim());
                for (Map.Entry<String, String> entry : translations.entrySet()) {
                    if (confirmMessage.equals(entry.getValue())) {
                        element.attr("data-confirm-message", BaseHandler.TAG_PREFIX_LANG + entry.getKey() + "}");
                    }
                }
            }            
        }
    }
    
    private static String encodeHtmlEntities(String text) {
        // This method encodes special characters like &, <, >, etc., back to HTML entities
        return text.replace("&",  "&amp;")
                   .replace("<",  "&lt;")
                   .replace(">",  "&gt;")
                   .replace("©",  "&copy;")
                   .replace("«",  "&laquo;")
                   .replace("»",  "&raquo;")
                   .replace("\"", "&quot;");
                   //.replace("'",  "&#39;");
    }
    
    private static boolean ignore(String text) {
    	final String s = text.trim();
    	return  s.isEmpty() || 
    			s.startsWith("{#") ||
    			s.startsWith("{$") ||
    			s.startsWith("({$") ||
    			s.length() == 1;
    }

    /**
    public static void main(String[] args) {
        // Base directory where HTML files are stored
		String baseDirStr = null;
    	if (args.length < 1) {
    		System.out.println("Note: No <root-path> specified, using 'web/' ");
    		baseDirStr = "web/";
    	} else {
    		baseDirStr = args[0].trim();    		
    	}
    	new TemplateLanguageProcessor().process(baseDirStr, baseDirStr);
    }
    */
    
}
