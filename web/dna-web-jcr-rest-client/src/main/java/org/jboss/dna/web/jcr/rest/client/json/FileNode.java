/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.web.jcr.rest.client.json;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import net.jcip.annotations.Immutable;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.dna.common.util.Base64;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.web.jcr.rest.client.IJcrConstants;
import org.jboss.dna.web.jcr.rest.client.Utils;
import org.jboss.dna.web.jcr.rest.client.domain.Workspace;

/**
 * The <code>FileNode</code> class is responsible for knowing how to create a URL for a file, create a JSON representation of a
 * file, and to create the appropriate JCR nodes for a file.
 */
@Immutable
public final class FileNode extends JsonNode {

    // ===========================================================================================================================
    // Fields
    // ===========================================================================================================================

    /**
     * The file on the local file system.
     */
    private final File file;

    /**
     * The folder in the workspace where the file is or will be published or unpublished.
     */
    private final String path;

    /**
     * The workspace where the file is or will be published or unpublished.
     */
    private final Workspace workspace;

    // ===========================================================================================================================
    // Constructors
    // ===========================================================================================================================

    /**
     * @param workspace the workspace being used (never <code>null</code>)
     * @param path the path in the workspace (never <code>null</code>)
     * @param file the file on the local file system (never <code>null</code>)
     * @throws Exception if there is a problem constructing the file node
     */
    public FileNode( Workspace workspace,
                     String path,
                     File file ) throws Exception {
        super(file.getName());

        CheckArg.isNotNull(workspace, "workspace");
        CheckArg.isNotNull(path, "path");

        this.file = file;
        this.path = path;
        this.workspace = workspace;

        // add properties
        JSONObject properties = new JSONObject();
        put(IJsonConstants.PROPERTIES_KEY, properties);
        properties.put(IJcrConstants.PRIMARY_TYPE_PROPERTY, IJcrConstants.FILE_NODE_TYPE);

        // add children
        JSONObject children = new JSONObject();
        put(IJsonConstants.CHILDREN_KEY, children);

        // add content child
        JSONObject kid = new JSONObject();
        children.put(IJcrConstants.CONTENT_PROPERTY, kid);

        // add child properties
        properties = new JSONObject();
        kid.put(IJsonConstants.PROPERTIES_KEY, properties);
        properties.put(IJcrConstants.PRIMARY_TYPE_PROPERTY, IJcrConstants.RESOURCE_NODE_TYPE);

        // add required jcr:lastModified property
        Calendar lastModified = Calendar.getInstance();
        lastModified.setTimeInMillis(file.lastModified());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        properties.put(IJcrConstants.LAST_MODIFIED, formatter.format(lastModified.getTime()));

        // add required jcr:mimeType property (just use a default value)
        properties.put(IJcrConstants.MIME_TYPE, Utils.getMimeType(file));
    }

    // ===========================================================================================================================
    // Methods
    // ===========================================================================================================================

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.json.JsonNode#getContent()
     */
    @Override
    public byte[] getContent() throws Exception {
        // add required jcr:data property (do this lazily only when the content is requested)
        JSONObject children = (JSONObject)get(IJsonConstants.CHILDREN_KEY);
        JSONObject kid = (JSONObject)children.get(IJcrConstants.CONTENT_PROPERTY);
        JSONObject props = (JSONObject)kid.get(IJsonConstants.PROPERTIES_KEY);
        props.put(IJcrConstants.DATA_PROPERTY, readFile());

        return super.getContent();
    }

    /**
     * Note: Currently used for testing only.
     * 
     * @param jsonResponse the JSON response obtained from performing a GET using the file content URL
     * @return the encoded file contents
     * @throws Exception if there is a problem obtaining the contents
     * @see #getFileContentsUrl()
     */
    String getFileContents( String jsonResponse ) throws Exception {
        CheckArg.isNotNull(jsonResponse, "jsonResponse");
        JSONObject contentNode = new JSONObject(jsonResponse);
        JSONObject props = (JSONObject)contentNode.get(IJsonConstants.PROPERTIES_KEY);
        String encodedContents = props.getString(IJcrConstants.DATA_PROPERTY);
        return encodedContents;
    }

    /**
     * Note: Currently used for testing only.
     * 
     * @return the URL that can be used to obtain the encoded file contents from a workspace
     * @throws Exception if there is a problem constructing the URL
     */
    URL getFileContentsUrl() throws Exception {
        StringBuilder url = new StringBuilder(getUrl().toString());
        url.append('/').append(IJcrConstants.CONTENT_PROPERTY);
        return new URL(url.toString());
    }

    /**
     * @return the path where the file is or will be published or unpublished
     */
    public String getPath() {
        return this.path;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.web.jcr.rest.client.json.JsonNode#getUrl()
     */
    @Override
    public URL getUrl() throws Exception {
        FolderNode folderNode = new FolderNode(this.workspace, getPath());
        StringBuilder url = new StringBuilder(folderNode.getUrl().toString());

        // add file to path and encode the name
        url.append('/').append(JsonUtils.encode(this.file.getName()));
        return new URL(url.toString());
    }

    /**
     * @return the base 64 encoded file content
     * @throws Exception if there is a problem reading the file
     */
    String readFile() throws Exception {
        return Base64.encode(new FileInputStream(this.file.getAbsoluteFile()));
    }

}
