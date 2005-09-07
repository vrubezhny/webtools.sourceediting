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
package org.eclipse.wst.xml.ui.tests;


import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.wst.xml.ui.tests.viewer.TestViewerConfigurationXML;


public class XMLUITestSuite extends TestSuite {
	public static Test suite() {
		return new XMLUITestSuite();
	}

	public XMLUITestSuite() {
		super("XML UI Test Suite");
		addTest(new TestSuite(VerifyEditorPlugin.class));
		addTest(new TestSuite(XMLUIPreferencesTest.class));
		addTest(new TestSuite(TestViewerConfigurationXML.class));
		addTest(new TestSuite(TestEditorConfigurationXML.class));
	}
}