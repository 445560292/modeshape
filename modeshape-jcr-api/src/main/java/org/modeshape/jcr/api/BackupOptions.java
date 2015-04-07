/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.api;

/**
 * Class which allows a customization of the backup process.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class BackupOptions {

    /**
     * By default, 100K nodes will be exported to a single backup file. 
     * So, if each node requires about 200 bytes (compressed), the resulting files will be about 19 MB in size.
     */
    public static final long DEFAULT_DOCUMENTS_PER_FILE = 100000L;
    
    /**
     * Default backup options which will be used when a backup is performed without an explicit set of options.
     */
    public static final BackupOptions DEFAULT = new BackupOptions(){};

    /**
     * Whether or not binary data should be part of the backup or not. Since ModeShape stores references from the Infinispan documents
     * toward the binary values which are being used, it might not always be desired to do a full binary backup.
     * 
     * @return {@code true} if binary data should be exported; defaults to {@code true}
     */
    public boolean includeBinaries() {
        return true;
    }

    /**
     * Return the number of documents which should be backed up in a single file. 
     *
     * @return the number documents; defaults to {@code 100000}
     */
    public long documentsPerFile() {
        return DEFAULT_DOCUMENTS_PER_FILE;    
    }

    /**
     * Return whether or not each backup file (which contains multiple documents) should be compressed or not.
     *
     * @return {@code true} if the backup files should be compressed; defaults to {@code true}
     */
    public boolean compress() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("[backup_options: ");
        builder.append("include_binaries=").append(includeBinaries());
        builder.append(", documents_per_file=").append(documentsPerFile());
        builder.append(", compress=").append(compress());
        builder.append("]");
        return builder.toString();
    }
}
