/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.foundation.ui.archetypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class is a replacement for <code>mvn archetype:generate</code> without dependencies to
 * maven-archetype related libraries.
 */
public class ArchetypeHelper {

    private static String archetypeDescriptorUri = "http://maven.apache.org/plugins/maven-archetype-plugin/archetype-descriptor/1.0.0";
    private static String requiredPropertyXPath = "/ad:archetype-descriptor/ad:requiredProperties/ad:requiredProperty";

    /* Value properties - initialized in constructor */

    private InputStream archetypeIn;
    private File outputDir;
    private String groupId;
    private String artifactId;
    private String version;

    /* private properties */

    private String packageName = "";
    private Boolean verbose = Boolean.FALSE;
    private Boolean createDefaultDirectories = Boolean.TRUE;

    private Map<String, String> overrideProperties = new HashMap<String, String>();

    private String zipEntryPrefix = "archetype-resources/";
    private List<String> binarySuffixes = Arrays.asList(".png", ".ico", ".gif", ".jpg", ".jpeg", ".bmp");

    protected String webInfResources = "src/main/webapp/WEB-INF/resources";

    // conscious removal of "scala" source dir
//    protected Pattern sourcePathRegexPattern = Pattern.compile("(src/(main|test)/(java|scala)/)(.*)");
    protected Pattern sourcePathRegexPattern = Pattern.compile("(src/(main|test)/(java)/)(.*)");

    public ArchetypeHelper(InputStream archetypeIn, File outputDir, String groupId, String artifactId) {
        this(archetypeIn, outputDir, groupId, artifactId, "1.0-SNAPSHOT");
    }

    public ArchetypeHelper(InputStream archetypeIn, File outputDir, String groupId, String artifactId, String version) {
        this.archetypeIn = archetypeIn;
        this.outputDir = outputDir;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    private void info(String s) {
        System.out.println(s);
    }

    private void debug(String s) {
        if (verbose) {
            System.out.println(s);
        }
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setOverrideProperties(Map<String, String> overrideProperties) {
        this.overrideProperties = overrideProperties;
    }

    /**
     * Main method which extracts given Maven Archetype in destination directory
     *
     * @return
     */
    public int execute() throws IOException {
        outputDir.mkdirs();

        if (packageName == null || packageName.length() == 0) {
            packageName = groupId + "." + artifactId;
        }

        String packageDir = packageName.replace('.', '/');

        info("Creating archetype using Maven groupId: " +
            groupId + ", artifactId: " + artifactId + ", version: " + version
            + " in directory: " + outputDir);

        Map<String, String> replaceProperties = new HashMap<>();

        try (ZipInputStream zip = new ZipInputStream(archetypeIn)) {
            boolean ok = true;
            while (ok) {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null) {
                    ok = false;
                } else {
                    if (!entry.isDirectory()) {
                        String fullName = entry.getName();
                        if (fullName != null && fullName.startsWith(zipEntryPrefix)) {
                            String name = replaceFileProperties(fullName.substring(zipEntryPrefix.length()), replaceProperties);
                            debug("Processing resource: " + name);

                            int idx = name.lastIndexOf('/');
                            Matcher matcher = sourcePathRegexPattern.matcher(name);
                            String dirName;
                            if (packageName.length() > 0 && idx > 0 && matcher.matches()) {
                                String prefix = matcher.group(1);
                                dirName = prefix + packageDir + "/" + name.substring(prefix.length());
                            } else if (packageName.length() > 0 && name.startsWith(webInfResources)) {
                                dirName = "src/main/webapp/WEB-INF/" + packageDir + "/resources" + name.substring(webInfResources.length());
                            } else {
                                dirName = name;
                            }

                            // lets replace properties...
                            File file = new File(outputDir, dirName);
                            file.getParentFile().mkdirs();
                            try (FileOutputStream out = new FileOutputStream(file)) {
                                boolean isBinary = false;
                                for (String suffix : binarySuffixes) {
                                    if (name.endsWith(suffix)) {
                                        isBinary = true;
                                        break;
                                    }
                                }
                                if (isBinary) {
                                    // binary file?  don't transform.
                                    copy(zip, out);
                                } else {
                                    // text file...
                                	try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                                		copy(zip, bos);
                                		String text = new String(bos.toByteArray(), StandardCharsets.UTF_8);
                                		out.write(transformContents(text, replaceProperties).getBytes(StandardCharsets.UTF_8));
                                	}
                                }
                            }
                        } else if (fullName != null && fullName.equals("META-INF/maven/archetype-metadata.xml")) {
                            // we assume that this resource will be first in Archetype's ZIP
                            // this way we can find out what are the required properties before we will actually use them
                            parseReplaceProperties(zip, replaceProperties);
                            replaceProperties.putAll(overrideProperties);
                        }
                    }
                    zip.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }

        info("Using replace properties: " + replaceProperties);

        // now lets replace all the properties in the pom.xml
        if (!replaceProperties.isEmpty()) {
            File pom = new File(outputDir, IMavenConstants.POM_FILE_NAME);
            FileReader reader = new FileReader(pom);
            String text = IOUtils.toString(reader);
            IOUtils.closeQuietly(reader);
            for (Map.Entry<String, String> e : replaceProperties.entrySet()) {
                text = replaceVariable(text, e.getKey(), e.getValue());
            }
            FileWriter writer = new FileWriter(pom);
            IOUtils.write(text, writer);
            IOUtils.closeQuietly(writer);
        }

        // now lets create the default directories
        if (createDefaultDirectories) {
            File srcDir = new File(outputDir, "src");
            File mainDir = new File(srcDir, "main");
            File testDir = new File(srcDir, "test");

            String srcDirName = "java";
            // Who needs Scala in 2014?
//            if (new File(mainDir, "scala").exists() || new File(textDir, "scala").exists()) {
//                srcDirName = "scala";
//            }

            for (File dir : new File[] { mainDir, testDir }) {
                for (String name : new String[] { srcDirName + "/" + packageDir, "resources" }) {
                    new File(dir, name).mkdirs();
                }
            }
        }

        return 0;
    }

    /**
     * Searches ZIP archive and returns properties found in "META-INF/maven/archetype-metadata.xml" entry
     *
     * @return
     * @throws IOException
     */
    public Map<String, String> parseProperties() throws IOException {
        Map<String, String> replaceProperties = new HashMap<>();
        try (ZipInputStream zip = new ZipInputStream(archetypeIn)) {
            boolean ok = true;
            while (ok) {
                ZipEntry entry = zip.getNextEntry();
                if (entry == null) {
                    ok = false;
                } else {
                    if (!entry.isDirectory()) {
                        String fullName = entry.getName();
                        if (fullName != null && fullName.equals("META-INF/maven/archetype-metadata.xml")) {
                            parseReplaceProperties(zip, replaceProperties);
                        }
                    }
                    zip.closeEntry();
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage(), e);
        }
        return replaceProperties;
    }

    /**
     * Extracts properties declared in "META-INF/maven/archetype-metadata.xml" file
     *
     * @param zip
     * @param replaceProperties
     * @throws IOException
     */
    protected void parseReplaceProperties(ZipInputStream zip, Map<String, String> replaceProperties) throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        copy(zip, bos);
        Map<String, String> requiredPropertiesFromOriginal = getArchetypeRequiredProperties(bos,false,false);
        replaceProperties.putAll(requiredPropertiesFromOriginal);
    }

    protected String transformContents(String fileContents, Map<String, String> replaceProperties) {
        String answer = removeInvalidHeaderCommentsAndProcessVelocityMacros(fileContents);
        answer = replaceVariable(answer, "package", packageName);
        answer = replaceVariable(answer, "packageName", packageName);
        answer = replaceAllVariable(answer, "groupId", groupId);
        answer = replaceAllVariable(answer, "artifactId", artifactId);
        answer = replaceAllVariable(answer, "version", version);
        for (Map.Entry<String, String> e : replaceProperties.entrySet()) {
            answer = replaceVariable(answer, e.getKey(), e.getValue());
        }
        return answer;
    }

    /**
     * This method should do a full Velocity macro processing...
     *
     * @param text
     * @return
     */
    protected String removeInvalidHeaderCommentsAndProcessVelocityMacros(String text) {
        String answer = "";
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            String l = line.trim();
            // a bit of Velocity here
            if (!l.startsWith("##") && !l.startsWith("#set(")) {
                if (line.contains("${D}")) {
                    line = line.replaceAll("\\$\\{D\\}", "\\$");
                }
                answer = answer.concat(line);
                answer = answer.concat("\n"); // TODO: maybe "line.separator"?
            }
        }
        return answer;
    }

    protected String replaceFileProperties(String fileName, Map<String, String> replaceProperties) {
        String answer = fileName;
        for (Map.Entry<String, String> e : replaceProperties.entrySet()) {
            answer = answer.replace("__" + e.getKey() + "__", e.getValue());
        }
        return answer;
    }

    protected String replaceVariable(String text, String name, String value) {
        if (value.contains("}")) {
            System.out.println("Ignoring dodgy value '" + value + "'");
            return text;
        } else {
//            System.out.println("Replacing '" + name + "' with '" + value + "'");
            return text.replaceAll(Pattern.quote("${" + name + "}"), value);
        }
    }

    protected String replaceAllVariable(String text, String name, String value) {
        String answer;
        answer = text.replaceAll(Pattern.quote("${" + name + "}"), value);
        answer = answer.replaceAll(Pattern.quote("$" + name), value);
        return answer;
    }

    // from org.fusesource.scalate.util.IOUtil#copy
    private static long copy(InputStream in, OutputStream out) throws IOException {
        long bytesCopied = 0;
        byte[] buffer = new byte[16384];

        int bytes = in.read(buffer);
        while (bytes >= 0) {
            out.write(buffer, 0, bytes);
            bytesCopied += bytes;
            bytes = in.read(buffer);
        }

        return bytesCopied;
    }
    
	public static Map<String, String> getArchetypeRequiredProperties(URL jarURL)
			throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
    	try (ZipInputStream zis = new ZipInputStream(jarURL.openStream())) {
        	ZipEntry entry = null;
        	while ((entry=zis.getNextEntry())!=null) {
        		try{
                    if (!entry.isDirectory() && "META-INF/maven/archetype-metadata.xml".equals(entry.getName())) {
                		ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        copy(zis, bos);
                        bos.flush();
                        return getArchetypeRequiredProperties(bos,true,true);
                    }
        		}finally{
                    zis.closeEntry();
        		}
            }
    	}
    	return Collections.EMPTY_MAP;
    }
    
	private static Map<String, String> getArchetypeRequiredProperties(ByteArrayOutputStream copyOfArchetypeJar,
			boolean excludeDepVersionProperties,boolean defaultAllProperties) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
    	Map<String,String> propertiesMap = new LinkedHashMap<>();
 
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        InputSource inputSource = new InputSource(new ByteArrayInputStream(copyOfArchetypeJar.toByteArray()));
        Document document = db.parse(inputSource);

        XPath xpath = XPathFactory.newInstance().newXPath();
        SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
        nsContext.registerMapping("ad", archetypeDescriptorUri);
        xpath.setNamespaceContext(nsContext);

        NodeList properties = (NodeList) xpath.evaluate(requiredPropertyXPath, document, XPathConstants.NODESET);

        for (int p = 0; p < properties.getLength(); p++) {
            Element requiredProperty = (Element) properties.item(p);

            String key = requiredProperty.getAttribute("key");
            //exclude version properties (externalize and specify the excluded properties?)
            if(excludeDepVersionProperties && (key.startsWith("version")||key.endsWith("version"))){
            	continue; 
            }
            NodeList children = requiredProperty.getElementsByTagNameNS(archetypeDescriptorUri, "defaultValue");
            String value = "";
            if (children.getLength() == 1 && children.item(0).hasChildNodes()) {
                value = children.item(0).getTextContent();
            } else {
                if (("name".equals(key)||defaultAllProperties) && value.isEmpty()) {
                    value = "HelloWorld";
                }
            }
            propertiesMap.put(key, value);
        }
    	return propertiesMap;
    }

}