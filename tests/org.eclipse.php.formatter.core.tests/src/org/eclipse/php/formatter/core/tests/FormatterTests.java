/*******************************************************************************
 * Copyright (c) 2013, 2014, 2018 Zend Techologies Ltd.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Zend Technologies Ltd. - initial API and implementation
 *     Dawid Pakuła - convert to JUnit4
 *******************************************************************************/
package org.eclipse.php.formatter.core.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.text.IDocument;
import org.eclipse.php.core.PHPVersion;
import org.eclipse.php.core.tests.PDTTUtils;
import org.eclipse.php.core.tests.PdttFile;
import org.eclipse.php.core.tests.TestSuiteWatcher;
import org.eclipse.php.core.tests.TestUtils;
import org.eclipse.php.core.tests.TestUtils.ColliderType;
import org.eclipse.php.core.tests.runner.AbstractPDTTRunner.Context;
import org.eclipse.php.core.tests.runner.PDTTList;
import org.eclipse.php.core.tests.runner.PDTTList.AfterList;
import org.eclipse.php.core.tests.runner.PDTTList.BeforeList;
import org.eclipse.php.core.tests.runner.PDTTList.Parameters;
import org.eclipse.php.formatter.core.profiles.PHPDefaultFormatterPreferences;
import org.eclipse.php.formatter.ui.preferences.ProfileManager;
import org.eclipse.php.formatter.ui.preferences.ProfileManager.CustomProfile;
import org.eclipse.php.formatter.ui.preferences.ProfileManager.Profile;
import org.eclipse.php.formatter.ui.preferences.ProfileStore;
import org.eclipse.php.internal.formatter.core.Logger;
import org.eclipse.php.ui.format.PHPFormatProcessorProxy;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;

@RunWith(PDTTList.class)
public class FormatterTests {

	@ClassRule
	public static TestWatcher watcher = new TestSuiteWatcher();

	@Parameters(recursive = true)
	public static final Map<PHPVersion, String[]> TESTS = new LinkedHashMap<>();

	static {
		TESTS.put(PHPVersion.PHP5,
				new String[] { "/workspace/formatter/php5", "/workspace/formatter_indent_empty_lines/php5" });
		TESTS.put(PHPVersion.PHP5_3, new String[] { "/workspace/formatter/php53" });
		TESTS.put(PHPVersion.PHP5_4, new String[] { "/workspace/formatter/php54" });
		TESTS.put(PHPVersion.PHP5_5,
				new String[] { "/workspace/formatter/php55", "/workspace/formatter_indent_empty_lines/php55" });
		TESTS.put(PHPVersion.PHP5_6, new String[] { "/workspace/formatter/php56" });
		TESTS.put(PHPVersion.PHP7_0,
				new String[] { "/workspace/formatter/php7", "/workspace/formatter_indent_empty_lines/php7" });
		TESTS.put(PHPVersion.PHP7_1, new String[] { "/workspace/formatter/php71" });
		TESTS.put(PHPVersion.PHP7_2, new String[] { "/workspace/formatter/php72" });
		TESTS.put(PHPVersion.PHP7_3, new String[] { "/workspace/formatter/php73" });
		TESTS.put(PHPVersion.PHP7_4, new String[] { "/workspace/formatter/php74" });
		TESTS.put(PHPVersion.PHP8_0, new String[] { "/workspace/formatter/php80" });
		TESTS.put(PHPVersion.PHP8_1, new String[] { "/workspace/formatter/php81" });
		TESTS.put(PHPVersion.PHP8_2, new String[] { "/workspace/formatter/php82" });
	};

	protected static int suiteCounter = 0;

	@Context
	public static Bundle getContext() {
		return Activator.getDefault().getBundle();
	}

	protected Map<String, IFile> files = new LinkedHashMap<>();
	protected Map<String, PdttFile> pdttFiles = new LinkedHashMap<>();
	protected final String[] fileNames;
	protected IProject project;
	protected int count;
	protected final PHPVersion phpVersion;
	protected final IScopeContext scopeContext;
	protected final ProfileManager profileManager;
	protected final String xmlFile;

	public FormatterTests(PHPVersion version, String[] fileNames) throws Exception {
		this.phpVersion = version;
		this.fileNames = fileNames;
		Bundle bundle = getContext();
		if (fileNames.length > 0) {
			IPath path = new Path(fileNames[0]);
			path = path.removeLastSegments(1);
			String[] formatterConfigurationFile = PDTTUtils.getFiles(path.toString(), bundle, ".xml");

			if (formatterConfigurationFile.length > 0) {
				xmlFile = formatterConfigurationFile[0];
			} else {
				xmlFile = null;
			}
		} else {
			xmlFile = null;
		}
		scopeContext = InstanceScope.INSTANCE;
		profileManager = new ProfileManager(new ArrayList<Profile>(), scopeContext);
	}

	@BeforeList
	public void setUpSuite() throws Exception {
		TestUtils.disableColliders(ColliderType.ALL);
		project = TestUtils.createProject("FormatterTests_" + suiteCounter++);
		TestUtils.setProjectPHPVersion(project, phpVersion, true, true);
		// Create files to format
		for (String fileName : fileNames) {
			PdttFile pdttFile = new PdttFile(getContext(), fileName);
			IFile file = createFile(pdttFile.getFile().trim());
			files.put(fileName, file);
			pdttFiles.put(fileName, pdttFile);
		}
		// Wait for indexer...
		profileManager.clearAllSettings(scopeContext);
		profileManager.commitChanges(scopeContext);
		if (xmlFile != null) {
			// apply configuration to the formatter configuration
			// manager
			String abcolutXmlFilePath = null;
			URL url = FileLocator.find(getContext(), new Path(xmlFile), null);
			URL resolved = FileLocator.resolve(url);
			IPath path = Path.fromOSString(resolved.getFile());
			abcolutXmlFilePath = path.toString();
			final File file = new File(abcolutXmlFilePath);
			assertTrue("Formatter Configuration Not Found " + file.toString(), file.exists());
			List<Profile> profiles = null;
			try {
				profiles = ProfileStore.readProfilesFromFile(file);
			} catch (CoreException e) {
				Logger.logException("Error while reading profile configuration xml file", e);
			}
			// should be only one profile in file
			if (profiles != null && profiles.size() > 0) {
				// update formatter configuration profile
				CustomProfile profile = (CustomProfile) profiles.iterator().next();
				profileManager.addProfile(profile);
				profileManager.setSelected(profile);
				profileManager.commitChanges(scopeContext);
			}
		}
	}

	@AfterList
	public void tearDownSuite() throws Exception {
		setDefaultFormatter(scopeContext, profileManager);
		TestUtils.deleteProject(project);
		TestUtils.enableColliders(ColliderType.ALL);
	}

	@Test
	public void formatter(String fileName) throws Exception {
		IFile file = files.get(fileName);
		IDocument document = StructuredModelManager.getModelManager().getModelForRead(file).getStructuredDocument();
		PHPFormatProcessorProxy formatter = new PHPFormatProcessorProxy();
		formatter.formatDocument(document, 0, document.getLength());
		// Compare contents
		PDTTUtils.assertContents(pdttFiles.get(fileName).getExpected(), document.get());
	}

	private static void setDefaultFormatter(IScopeContext scopeContext, ProfileManager profileManager) {
		profileManager.clearAllSettings(scopeContext);
		if (profileManager.getSelected().getID() != PHPDefaultFormatterPreferences.ID) {
			profileManager.setSelected(profileManager.getProfile(PHPDefaultFormatterPreferences.ID));
		}
		profileManager.commitChanges(scopeContext);
	}

	protected IFile createFile(String data) throws Exception {
		return TestUtils.createFile(project, "test" + (++count) + ".php", data);
	}

}
