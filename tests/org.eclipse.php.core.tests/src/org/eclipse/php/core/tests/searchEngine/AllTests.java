/*******************************************************************************
 * Copyright (c) 2009, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Zend Technologies
 *     Dawid Pakuła - convert to JUnit4
 *******************************************************************************/
package org.eclipse.php.core.tests.searchEngine;

import org.eclipse.php.core.tests.TestSuiteWatcher;
import org.junit.ClassRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ PHP5LanguageModelTest.class, PHP53LanguageModelTest.class, SearchFieldTests.class })
public class AllTests {

	@ClassRule
	public static TestWatcher watcher = new TestSuiteWatcher();

}
