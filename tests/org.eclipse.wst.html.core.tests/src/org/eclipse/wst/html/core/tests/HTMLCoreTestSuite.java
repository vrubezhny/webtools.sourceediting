/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.html.core.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.wst.html.core.tests.misc.HTMLCorePreferencesTest;



public class HTMLCoreTestSuite extends TestSuite {

	/**
	 * to get picked up by BVT
	 * @return
	 */
	public static Test suite() {
		return new HTMLCoreTestSuite();
	}

	public HTMLCoreTestSuite() {
		super("HTML Core TestSuite");

		addTest(ModelParserTests.suite());
		addTest(new TestSuite(HTMLCorePreferencesTest.class));
	}
}