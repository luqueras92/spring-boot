/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.springframework.boot.loader.data.RandomAccessData;
import org.springframework.boot.loader.data.RandomAccessData.ResourceAccess;
import org.springframework.boot.loader.data.RandomAccessDataFile;
import org.springframework.boot.loader.util.AsciiBytes;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
 * offers the following additional functionality.
 * <ul>
 * <li>A nested {@link JarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} based
 * on any directory entry.</li>
 * <li>A nested {@link JarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} for
 * embedded JAR files (as long as their entry is not compressed).</li>
 * <li>Entry data can be accessed as {@link RandomAccessData}.</li>
 * </ul>
 *
 * @author Phillip Webb
 */
public class JarFile extends java.util.jar.JarFile implements Iterable<JarEntryData> {

	private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");

	private static final AsciiBytes MANIFEST_MF = new AsciiBytes("META-INF/MANIFEST.MF");

	private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");

	private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";

	private static final String HANDLERS_PACKAGE = "org.springframework.boot.loader";

	private final RandomAccessDataFile rootFile;

	private final String pathFromRoot;

	private final RandomAccessData data;

	private JarFileEntries entries;

	private boolean signed;

	private JarEntryData manifestEntry;

	private SoftReference<Manifest> manifest;

	private URL url;

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @throws IOException if the file cannot be read
	 */
	public JarFile(File file) throws IOException {
		this(new RandomAccessDataFile(file));
	}

	/**
	 * Create a new {@link JarFile} backed by the specified file.
	 * @param file the root jar file
	 * @throws IOException if the file cannot be read
	 */
	JarFile(RandomAccessDataFile file) throws IOException {
		this(file, "", file);
	}

	/**
	 * Private constructor used to create a new {@link JarFile} either directly or from a
	 * nested entry.
	 * @param rootFile the root jar file
	 * @param pathFromRoot the name of this file
	 * @param data the underlying data
	 * @throws IOException if the file cannot be read
	 */
	private JarFile(RandomAccessDataFile rootFile, String pathFromRoot,
			RandomAccessData data) throws IOException {
		super(rootFile.getFile());
		CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(data);
		this.rootFile = rootFile;
		this.pathFromRoot = pathFromRoot;
		this.data = getArchiveData(endRecord, data);
		this.entries = new JarFileEntries(this, endRecord) {

			@Override
			protected void processEntry(JarEntryData entry) {
				JarFile.this.processEntry(entry);
			};

		};
	}

	private JarFile(RandomAccessDataFile rootFile, String pathFromRoot,
			RandomAccessData data, JarFileEntries entries, JarEntryFilter filter)
					throws IOException {
		super(rootFile.getFile());
		this.rootFile = rootFile;
		this.pathFromRoot = pathFromRoot;
		this.data = data;
		this.entries = new JarFileEntries(this, entries, filter);
	}

	private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord,
			RandomAccessData data) {
		long offset = endRecord.getStartOfArchive(data);
		if (offset == 0) {
			return data;
		}
		return data.getSubsection(offset, data.getSize() - offset);
	}

	private void processEntry(JarEntryData entry) {
		AsciiBytes name = entry.getName();
		if (name.startsWith(META_INF)) {
			processMetaInfEntry(name, entry);
		}
	}

	private void processMetaInfEntry(AsciiBytes name, JarEntryData entry) {
		if (name.equals(MANIFEST_MF)) {
			this.manifestEntry = entry;
		}
		if (name.endsWith(SIGNATURE_FILE_EXTENSION)) {
			this.signed = true;
		}
	}

	protected final RandomAccessDataFile getRootJarFile() {
		return this.rootFile;
	}

	RandomAccessData getData() {
		return this.data;
	}

	@Override
	public Manifest getManifest() throws IOException {
		if (this.manifestEntry == null) {
			return null;
		}
		Manifest manifest = (this.manifest == null ? null : this.manifest.get());
		if (manifest == null) {
			InputStream inputStream = this.entries.getInputStream(this.manifestEntry);
			try {
				manifest = new Manifest(inputStream);
			}
			finally {
				inputStream.close();
			}
			this.manifest = new SoftReference<Manifest>(manifest);
		}
		return manifest;
	}

	@Override
	public Enumeration<java.util.jar.JarEntry> entries() {
		final Iterator<JarEntryData> iterator = iterator();
		return new Enumeration<java.util.jar.JarEntry>() {

			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public java.util.jar.JarEntry nextElement() {
				return iterator.next().asJarEntry();
			}
		};
	}

	@Override
	public Iterator<JarEntryData> iterator() {
		return this.entries.iterator();
	}

	@Override
	public JarEntry getJarEntry(String name) {
		return (JarEntry) getEntry(name);
	}

	@Override
	public ZipEntry getEntry(String name) {
		JarEntryData jarEntryData = getJarEntryData(name);
		return (jarEntryData == null ? null : jarEntryData.asJarEntry());
	}

	public boolean containsEntry(String name) {
		return getJarEntryData(name) != null;
	}

	private JarEntryData getJarEntryData(String name) {
		if (name == null) {
			return null;
		}
		return getJarEntryData(new AsciiBytes(name));
	}

	JarEntryData getJarEntryData(AsciiBytes name) {
		return this.entries.getJarEntryData(name);
	}

	boolean isSigned() {
		return this.signed;
	}

	void setupEntryCertificates() {
		// Fallback to JarInputStream to obtain certificates, not fast but hopefully not
		// happening that often.
		try {
			JarInputStream inputStream = new JarInputStream(
					getData().getInputStream(ResourceAccess.ONCE));
			try {
				java.util.jar.JarEntry entry = inputStream.getNextJarEntry();
				while (entry != null) {
					inputStream.closeEntry();
					JarEntry jarEntry = getJarEntry(entry.getName());
					if (jarEntry != null) {
						jarEntry.setupCertificates(entry);
					}
					entry = inputStream.getNextJarEntry();
				}
			}
			finally {
				inputStream.close();
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	InputStream getInputStream(AsciiBytes name) throws IOException {
		return this.entries.getInputStream(name);
	}

	@Override
	public synchronized InputStream getInputStream(ZipEntry ze) throws IOException {
		return this.entries.getInputStream(getContainedEntry(ze).getSource());
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param entry the zip entry
	 * @return a {@link JarFile} for the entry
	 * @throws IOException if the nested jar file cannot be read
	 */
	public synchronized JarFile getNestedJarFile(final ZipEntry entry)
			throws IOException {
		return getNestedJarFile(getContainedEntry(entry).getSource());
	}

	/**
	 * Return a nested {@link JarFile} loaded from the specified entry.
	 * @param sourceEntry the zip entry
	 * @return a {@link JarFile} for the entry
	 * @throws IOException if the nested jar file cannot be read
	 */
	public synchronized JarFile getNestedJarFile(JarEntryData sourceEntry)
			throws IOException {
		try {
			return createJarFileFromEntry(sourceEntry);
		}
		catch (IOException ex) {
			throw new IOException(
					"Unable to open nested jar file '" + sourceEntry.getName() + "'", ex);
		}
	}

	private JarFile createJarFileFromEntry(JarEntryData sourceEntry) throws IOException {
		if (sourceEntry.isDirectory()) {
			return createJarFileFromDirectoryEntry(sourceEntry);
		}
		return createJarFileFromFileEntry(sourceEntry);
	}

	private JarFile createJarFileFromDirectoryEntry(JarEntryData sourceEntry)
			throws IOException {
		final AsciiBytes sourceName = sourceEntry.getName();
		JarEntryFilter filter = new JarEntryFilter() {
			@Override
			public AsciiBytes apply(AsciiBytes name, JarEntryData entryData) {
				if (name.startsWith(sourceName) && !name.equals(sourceName)) {
					return name.substring(sourceName.length());
				}
				return null;
			}
		};
		return new JarFile(this.rootFile,
				this.pathFromRoot + "!/"
						+ sourceEntry.getName().substring(0, sourceName.length() - 1),
				this.data, this.entries, filter);
	}

	private JarFile createJarFileFromFileEntry(JarEntryData sourceEntry)
			throws IOException {
		if (sourceEntry.getMethod() != ZipEntry.STORED) {
			throw new IllegalStateException("Unable to open nested entry '"
					+ sourceEntry.getName() + "'. It has been compressed and nested "
					+ "jar files must be stored without compression. Please check the "
					+ "mechanism used to create your executable jar file");
		}
		return new JarFile(this.rootFile,
				this.pathFromRoot + "!/" + sourceEntry.getName(), sourceEntry.getData());
	}

	private JarEntry getContainedEntry(ZipEntry zipEntry) throws IOException {
		if (zipEntry instanceof JarEntry
				&& ((JarEntry) zipEntry).getSourceJarFile() == this) {
			return (JarEntry) zipEntry;
		}
		throw new IllegalArgumentException("ZipEntry must be contained in this file");
	}

	@Override
	public int size() {
		return (int) this.data.getSize();
	}

	@Override
	public void close() throws IOException {
		this.rootFile.close();
	}

	/**
	 * Return a URL that can be used to access this JAR file. NOTE: the specified URL
	 * cannot be serialized and or cloned.
	 * @return the URL
	 * @throws MalformedURLException if the URL is malformed
	 */
	public URL getUrl() throws MalformedURLException {
		if (this.url == null) {
			Handler handler = new Handler(this);
			String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
			file = file.replace("file:////", "file://"); // Fix UNC paths
			this.url = new URL("jar", "", -1, file, handler);
		}
		return this.url;
	}

	@Override
	public String toString() {
		return getName();
	}

	@Override
	public String getName() {
		String path = this.pathFromRoot;
		return this.rootFile.getFile() + path;
	}

	/**
	 * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
	 * {@link URLStreamHandler} will be located to deal with jar URLs.
	 */
	public static void registerUrlProtocolHandler() {
		String handlers = System.getProperty(PROTOCOL_HANDLER);
		System.setProperty(PROTOCOL_HANDLER, ("".equals(handlers) ? HANDLERS_PACKAGE
				: handlers + "|" + HANDLERS_PACKAGE));
		resetCachedUrlHandlers();
	}

	/**
	 * Reset any cached handlers just in case a jar protocol has already been used. We
	 * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
	 * should have no effect other than clearing the handlers cache.
	 */
	private static void resetCachedUrlHandlers() {
		try {
			URL.setURLStreamHandlerFactory(null);
		}
		catch (Error ex) {
			// Ignore
		}
	}

}
