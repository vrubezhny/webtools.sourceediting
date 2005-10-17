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
package org.eclipse.jst.jsp.core.internal.text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jst.jsp.core.internal.contentmodel.tld.provisional.JSP12TLDNames;
import org.eclipse.jst.jsp.core.internal.encoding.JSPDocumentHeadContentDetector;
import org.eclipse.jst.jsp.core.internal.parser.JSPSourceParser;
import org.eclipse.jst.jsp.core.internal.provisional.JSP11Namespace;
import org.eclipse.jst.jsp.core.internal.provisional.JSP12Namespace;
import org.eclipse.jst.jsp.core.internal.provisional.text.IJSPPartitionTypes;
import org.eclipse.jst.jsp.core.internal.regions.DOMJSPRegionContexts;
import org.eclipse.wst.html.core.internal.text.StructuredTextPartitionerForHTML;
import org.eclipse.wst.sse.core.internal.ltk.parser.StructuredDocumentRegionHandler;
import org.eclipse.wst.sse.core.internal.ltk.parser.StructuredDocumentRegionHandlerExtension;
import org.eclipse.wst.sse.core.internal.ltk.parser.StructuredDocumentRegionParser;
import org.eclipse.wst.sse.core.internal.parser.ForeignRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredTextPartitioner;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.internal.text.rules.StructuredTextPartitioner;
import org.eclipse.wst.sse.core.internal.util.StringUtils;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.core.internal.text.rules.StructuredTextPartitionerForXML;

public class StructuredTextPartitionerForJSP extends StructuredTextPartitioner {

	private class PrefixListener implements StructuredDocumentRegionHandler, StructuredDocumentRegionHandlerExtension {
		// track the list of prefixes introduced by taglib directives
		private List fCustomActionPrefixes = null;
		private String fLastTrue = null;

		public PrefixListener() {
			super();
			fCustomActionPrefixes = new ArrayList(1);
			resetNodes();
		}

		private JSPSourceParser getTextSource() {
			return (JSPSourceParser) fStructuredDocument.getParser();
		}

		public void nodeParsed(IStructuredDocumentRegion sdRegion) {
			// Largely taken from the TLDCMDocumentManager
			// could test > 1, but since we only care if there are 8 (<%@,
			// taglib, uri, =, where, prefix, =, what) [or 4 for includes]
			if (sdRegion.getNumberOfRegions() > 4 && sdRegion.getRegions().get(1).getType() == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME) {
				ITextRegion nameRegion = sdRegion.getRegions().get(1);
				try {
					boolean tablibdetected = false;
					boolean directiveTaglibdetected;
					int startOffset = sdRegion.getStartOffset(nameRegion);
					int textLength = nameRegion.getTextLength();

					if (getTextSource() != null) {
						tablibdetected = getTextSource().regionMatches(startOffset, textLength, JSP12TLDNames.TAGLIB);
						directiveTaglibdetected = getTextSource().regionMatches(startOffset, textLength, JSP12Namespace.ElementName.DIRECTIVE_TAGLIB);
					}
					else {
						// old fashioned way
						String directiveName = getTextSource().getText(startOffset, textLength);
						tablibdetected = directiveName.equals(JSP12TLDNames.TAGLIB);
						directiveTaglibdetected = directiveName.equals(JSP12Namespace.ElementName.DIRECTIVE_TAGLIB);
					}
					if (tablibdetected || directiveTaglibdetected) {
						processTaglib(sdRegion);
					}
				}
				catch (StringIndexOutOfBoundsException sioobExc) {
					// ISSUE: why is this "normal" here?
					//do nothing
				}
			}
		}


		private void processTaglib(IStructuredDocumentRegion taglibStructuredDocumentRegion) {
			ITextRegionList regions = taglibStructuredDocumentRegion.getRegions();
			String prefixValue = null;
			boolean prefixnameDetected = false;
			try {
				for (int i = 0; i < regions.size(); i++) {
					ITextRegion region = regions.get(i);
					int startOffset = taglibStructuredDocumentRegion.getStartOffset(region);
					int textLength = region.getTextLength();
					if (region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_NAME) {
						prefixnameDetected = getTextSource().regionMatches(startOffset, textLength, JSP12TLDNames.PREFIX);
						//String regionText =
						// fTextSource.getText(startOffset, textLength);
						//prefixname =
						// regionText.equals(JSP12TLDNames.PREFIX);
					}
					else if (prefixnameDetected && region.getType() == DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE) {
						prefixValue = getTextSource().getText(startOffset, textLength);
					}
				}
			}
			catch (StringIndexOutOfBoundsException sioobExc) {
				// nothing to be done
				prefixValue = null;
			}
			if (prefixValue != null) {
				String prefixText = StringUtils.strip(prefixValue) + ":"; //$NON-NLS-1$
				if (!fCustomActionPrefixes.contains(prefixText)) {
					if(debugPrefixListener == true) {
						System.out.println("StructuredTextPartitionerForJSP.PrefixListener learning prefix: " + prefixText); //$NON-NLS-1$
					}
					fCustomActionPrefixes.add(prefixText);
				}
			}
		}

		public void resetNodes() {
			fLastTrue = null;
			fCustomActionPrefixes.clear();
			fCustomActionPrefixes.add(JSP11Namespace.JSP_TAG_PREFIX + ":"); //$NON-NLS-1$
			if(debugPrefixListener == true) {
				System.out.println("StructuredTextPartitionerForJSP.PrefixListener forgetting learned prefixes"); //$NON-NLS-1$
			}
		}


		public void setStructuredDocument(IStructuredDocument newDocument) {
			resetNodes();
			((StructuredDocumentRegionParser) fStructuredDocument.getParser()).removeStructuredDocumentRegionHandler(this);
			if(newDocument != null) {
				((StructuredDocumentRegionParser) newDocument.getParser()).addStructuredDocumentRegionHandler(this);
			}
		}

		public boolean startsWithCustomActionPrefix(String tagname) {
			if (tagname.equals(fLastTrue))
				return true;
			for (int i = 0; i < fCustomActionPrefixes.size(); i++)
				if (tagname.startsWith((String) fCustomActionPrefixes.get(i))) {
					fLastTrue = tagname;
					return true;
				}
			return false;
		}
	}

	static final boolean debugPrefixListener = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.jst.jsp.core/partitioner/prefixlistener")); //$NON-NLS-1$ //$NON-NLS-2$


	// for compatibility with v5.1.0, we'll reuse ST_JSP_DIRECTIVE for action
	// tags
	private final static boolean fEnableJSPActionPartitions = true;
	// list of valid JSP 1.2 tag and action names
	private static List fJSPActionTagNames = null;
	private static final String HTML_MIME_TYPE = "text/html"; //$NON-NLS-1$
	private static final String XHTML_MIME_TYPE = "text/xhtml"; //$NON-NLS-1$
	private static final String XML_MIME_TYPE = "text/xml"; //$NON-NLS-1$

	private final static String[] fConfiguredContentTypes = new String[]{IJSPPartitionTypes.JSP_DEFAULT, IJSPPartitionTypes.JSP_DEFAULT_EL, IJSPPartitionTypes.JSP_DIRECTIVE, IJSPPartitionTypes.JSP_CONTENT_DELIMITER, IJSPPartitionTypes.JSP_CONTENT_JAVA, IJSPPartitionTypes.JSP_CONTENT_JAVASCRIPT, IJSPPartitionTypes.JSP_COMMENT};

	/**
	 * @return
	 */
	public static String[] getConfiguredContentTypes() {
		return fConfiguredContentTypes;
	}

	private IStructuredTextPartitioner fEmbeddedPartitioner = null;

	
	/**
	 * Assume language=java by default ... client, such as
	 * PageDirectiveAdapter, must set language of document partitioner,
	 * if/when it changes.
	 */
	private String fLanguage = "java"; //$NON-NLS-1$
	private PrefixListener fPrefixParseListener;

	/**
	 * Constructor for JSPDocumentPartioner.
	 */
	public StructuredTextPartitionerForJSP() {
		super();
		if (fJSPActionTagNames == null) {
			fJSPActionTagNames = new ArrayList(); // uses .equals() for
			// contains()
			fJSPActionTagNames.add(JSP12Namespace.ElementName.DECLARATION);
			//			fJSPActionTagNames.add(JSP12Namespace.ElementName.DIRECTIVE_INCLUDE);
			//			fJSPActionTagNames.add(JSP12Namespace.ElementName.DIRECTIVE_PAGE);
			//			fJSPActionTagNames.add(JSP12Namespace.ElementName.DIRECTIVE_TAGLIB);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.EXPRESSION);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.FALLBACK);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.FORWARD);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.GETPROPERTY);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.INCLUDE);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.PARAM);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.PARAMS);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.PLUGIN);
			//			fJSPActionTagNames.add(JSP12Namespace.ElementName.ROOT);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.SCRIPTLET);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.SETPROPERTY);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.TEXT);
			fJSPActionTagNames.add(JSP12Namespace.ElementName.USEBEAN);
		}
	}

	/**
	 * @see org.eclipse.jface.text.IDocumentPartitioner#connect(org.eclipse.jface.text.IDocument)
	 */
	public void connect(IDocument document) {
		super.connect(document);
		fSupportedTypes = null;

		// be extra paranoid
		if (fEnableJSPActionPartitions && fStructuredDocument.getParser() instanceof JSPSourceParser) {
			StructuredDocumentRegionParser parser = (StructuredDocumentRegionParser) fStructuredDocument.getParser();
			parser.removeStructuredDocumentRegionHandler(fPrefixParseListener);
			fPrefixParseListener = new PrefixListener();
			parser.addStructuredDocumentRegionHandler(fPrefixParseListener);
		}
	}

	private IStructuredTextPartitioner createStructuredTextPartitioner(IStructuredDocument structuredDocument) {
		IStructuredTextPartitioner result = null;
		JSPDocumentHeadContentDetector jspHeadContentDetector = new JSPDocumentHeadContentDetector();
		jspHeadContentDetector.set(structuredDocument);
		String contentType;
		try {
			contentType = jspHeadContentDetector.getContentType();
		}
		catch (IOException e) {
			// should be impossible in this context
			throw new Error(e);
		}
		if (contentType == null) {
			contentType = "text/html"; //$NON-NLS-1$
		}
		// we currently only have two ... eventually should
		// make or tie-in to existing registry.
		if (contentType.equalsIgnoreCase(HTML_MIME_TYPE)) {
			result = new StructuredTextPartitionerForHTML();
			result.connect(structuredDocument);
		}
		else if (contentType.equalsIgnoreCase(XHTML_MIME_TYPE)) {
			result = new StructuredTextPartitionerForHTML();
			result.connect(structuredDocument);
		}
		else if (contentType.equalsIgnoreCase(XML_MIME_TYPE) || contentType.endsWith("+xml")) {
			result = new StructuredTextPartitionerForXML();
			result.connect(structuredDocument);
		}
		else {
			result = new StructuredTextPartitioner();
			result.connect(structuredDocument);
		}
		return result;

	}

	/**
	 * @see org.eclipse.jface.text.IDocumentPartitioner#disconnect()
	 */
	public void disconnect() {
		// we'll check for null document, just for bullet proofing (incase
		// disconnnect is called without corresponding connect.
		if (fStructuredDocument != null) {
			StructuredDocumentRegionParser parser = (StructuredDocumentRegionParser) fStructuredDocument.getParser();
			if (fPrefixParseListener != null)
				parser.removeStructuredDocumentRegionHandler(fPrefixParseListener);
			fPrefixParseListener = null;
		}

		if (fEmbeddedPartitioner != null) {
			fEmbeddedPartitioner.disconnect();
			// https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=4909
			/**
			 * force recreation when reconnected
			 */
			fEmbeddedPartitioner = null;
		}
		// super.disconnect should come at end, since it (may) set
		// structuredDocument to null
		super.disconnect();
	}

	public String getDefaultPartitionType() {
		return getEmbeddedPartitioner().getDefaultPartitionType();
	}

	/**
	 * Returns the embeddedPartitioner.
	 * 
	 * @return IStructuredTextPartitioner
	 */
	public IStructuredTextPartitioner getEmbeddedPartitioner() {
		if (fEmbeddedPartitioner == null) {
			fEmbeddedPartitioner = createStructuredTextPartitioner(fStructuredDocument);
			fEmbeddedPartitioner.connect(fStructuredDocument);
		}

		return fEmbeddedPartitioner;
	}

	/**
	 * Returns the language.
	 * 
	 * @return String
	 */
	public String getLanguage() {
		return fLanguage;
	}

	private List getLocalLegalContentTypes() {
		List types = new ArrayList();
		Object[] configuredTypes = getConfiguredContentTypes();
		for (int i = 0; i < configuredTypes.length; i++)
			types.add(configuredTypes[i]);
		return types;
	}

	private String getParentName(IStructuredDocumentRegion sdRegion) {
		String result = "UNKNOWN"; //$NON-NLS-1$
		while (sdRegion != null && isValidJspActionRegionType(sdRegion.getType()))
			sdRegion = sdRegion.getPrevious();

		if (sdRegion != null) {
			ITextRegionList regions = sdRegion.getRegions();
			// only find parent names from a start tag
			if (regions.size() > 1) {
				ITextRegion r = regions.get(1);
				if (regions.get(0).getType().equals(DOMRegionContext.XML_TAG_OPEN) && r.getType().equals(DOMRegionContext.XML_TAG_NAME)) {
					result = sdRegion.getText(r);
				}
			}
		}
		return result;
	}

	protected String getPartitionType(ForeignRegion region, int offset) {
		return getEmbeddedPartitioner().getPartitionType(region, offset);
	}


	public String getPartitionType(ITextRegion region, int offset) {
		String result = null;
		final String region_type = region.getType();
		if (region_type == DOMJSPRegionContexts.JSP_CONTENT) {
			result = getPartitionTypeForDocumentLanguage();
		}
		else if (region_type == DOMJSPRegionContexts.JSP_COMMENT_TEXT || region_type == DOMJSPRegionContexts.JSP_COMMENT_OPEN || region_type == DOMJSPRegionContexts.JSP_COMMENT_CLOSE)
			result = IJSPPartitionTypes.JSP_COMMENT;
		else if (region_type == DOMJSPRegionContexts.JSP_DIRECTIVE_NAME || region_type == DOMJSPRegionContexts.JSP_DIRECTIVE_OPEN || region_type == DOMJSPRegionContexts.JSP_DIRECTIVE_CLOSE)
			result = IJSPPartitionTypes.JSP_DIRECTIVE;
		else if (region_type == DOMJSPRegionContexts.JSP_CLOSE || region_type == DOMJSPRegionContexts.JSP_SCRIPTLET_OPEN || region_type == DOMJSPRegionContexts.JSP_EXPRESSION_OPEN || region_type == DOMJSPRegionContexts.JSP_DECLARATION_OPEN)
			result = IJSPPartitionTypes.JSP_CONTENT_DELIMITER;
		else if (region_type == DOMJSPRegionContexts.JSP_ROOT_TAG_NAME)
			result = IJSPPartitionTypes.JSP_DEFAULT;
		else if (region_type == DOMJSPRegionContexts.JSP_EL_OPEN || region_type == DOMJSPRegionContexts.JSP_EL_CONTENT || region_type == DOMJSPRegionContexts.JSP_EL_CLOSE || region_type == DOMJSPRegionContexts.JSP_EL_DQUOTE
					|| region_type == DOMJSPRegionContexts.JSP_EL_SQUOTE || region_type == DOMJSPRegionContexts.JSP_EL_QUOTED_CONTENT)
			result = IJSPPartitionTypes.JSP_DEFAULT_EL;
		else if (region_type == DOMRegionContext.XML_CONTENT) {
			// possibly between <jsp:scriptlet>, <jsp:expression>,
			// <jsp:declaration>
			IStructuredDocumentRegion sdRegion = this.fStructuredDocument.getRegionAtCharacterOffset(offset);
			if (isJspJavaActionName(getParentName(sdRegion)))
				result = getPartitionTypeForDocumentLanguage();
			else
				result = getDefaultPartitionType();
		}
		else {
			result = getEmbeddedPartitioner().getPartitionType(region, offset);
		}
		return result;
	}

	public String getPartitionTypeBetween(IStructuredDocumentRegion previousNode, IStructuredDocumentRegion nextNode) {
		return getEmbeddedPartitioner().getPartitionTypeBetween(previousNode, nextNode);
	}

	private String getPartitionTypeForDocumentLanguage() {
		String result;
		if (fLanguage == null || fLanguage.equalsIgnoreCase("java")) { //$NON-NLS-1$
			result = IJSPPartitionTypes.JSP_CONTENT_JAVA;
		}
		else if (fLanguage.equalsIgnoreCase("javascript")) { //$NON-NLS-1$
			result = IJSPPartitionTypes.JSP_CONTENT_JAVASCRIPT;
		}
		else {
			result = IJSPPartitionTypes.JSP_SCRIPT_PREFIX + getLanguage().toUpperCase(Locale.ENGLISH);
		}
		return result;
	}

	protected void initLegalContentTypes() {
		List combinedTypes = getLocalLegalContentTypes();
		if (getEmbeddedPartitioner() != null) {
			String[] moreTypes = getEmbeddedPartitioner().getLegalContentTypes();
			for (int i = 0; i < moreTypes.length; i++)
				combinedTypes.add(moreTypes[i]);
		}
		fSupportedTypes = new String[0];
		combinedTypes.toArray(fSupportedTypes);
	}

	/**
	 * @param sdRegion
	 * @param offset
	 * @return
	 */
	private boolean isAction(IStructuredDocumentRegion sdRegion, int offset) {
		if (!sdRegion.getType().equals(DOMRegionContext.XML_TAG_NAME))
			return false;
		// shouldn't get a tag name region type unless a tag name region
		// exists
		// at [1]
		ITextRegion tagNameRegion = sdRegion.getRegions().get(1);
		String tagName = sdRegion.getText(tagNameRegion);
		// TODO: support custom JSP actions
		// the jsp: prefix is already loaded in the prefix listener
		//		if (fJSPActionTagNames.contains(tagName))
		//			return true;
		return fPrefixParseListener.startsWithCustomActionPrefix(tagName);
	}

	protected boolean isDocumentRegionBasedPartition(IStructuredDocumentRegion sdRegion, ITextRegion containedChildRegion, int offset) {
		String documentRegionContext = sdRegion.getType();
		if (containedChildRegion != null) {
			if (documentRegionContext.equals(DOMJSPRegionContexts.JSP_DIRECTIVE_NAME) || documentRegionContext.equals(DOMJSPRegionContexts.JSP_ROOT_TAG_NAME)) {
				setInternalPartition(offset, containedChildRegion.getLength(), IJSPPartitionTypes.JSP_DIRECTIVE);
				return true;
			}
			if (fEnableJSPActionPartitions && isAction(sdRegion, offset)) {
				setInternalPartition(offset, containedChildRegion.getLength(), IJSPPartitionTypes.JSP_DIRECTIVE);
				return true;
			}
		}
		return super.isDocumentRegionBasedPartition(sdRegion, containedChildRegion, offset);
	}

	/**
	 * @param possibleJspJavaAction
	 * @return
	 */
	private boolean isJspJavaActionName(String possibleJspJavaAction) {
		return possibleJspJavaAction.equals(JSP11Namespace.ElementName.SCRIPTLET) || possibleJspJavaAction.equals(JSP11Namespace.ElementName.EXPRESSION) || possibleJspJavaAction.equals(JSP11Namespace.ElementName.DECLARATION);
	}

	private boolean isValidJspActionRegionType(String type) {
		// true for anything that can be within <jsp:scriptlet>,
		// <jsp:expression>, <jsp:declaration>
		return type == DOMRegionContext.XML_CONTENT || type == DOMRegionContext.BLOCK_TEXT || type == DOMRegionContext.XML_CDATA_OPEN || type == DOMRegionContext.XML_CDATA_TEXT || type == DOMRegionContext.XML_CDATA_CLOSE;
	}

	public IDocumentPartitioner newInstance() {
		StructuredTextPartitionerForJSP instance = new StructuredTextPartitionerForJSP();
		instance.setEmbeddedPartitioner(createStructuredTextPartitioner(fStructuredDocument));
		instance.setLanguage(fLanguage);
		return instance;
	}

	/**
	 * Sets the embeddedPartitioner.
	 * 
	 * @param embeddedPartitioner
	 *            The embeddedPartitioner to set
	 */
	public void setEmbeddedPartitioner(IStructuredTextPartitioner embeddedPartitioner) {
		// https://w3.opensource.ibm.com/bugzilla/show_bug.cgi?id=4909
		/**
		 * manage connected state of embedded partitioner
		 */
		if(fEmbeddedPartitioner != null && fStructuredDocument != null) {
			fEmbeddedPartitioner.disconnect();
		}
		
		this.fEmbeddedPartitioner = embeddedPartitioner;
		
		if(fEmbeddedPartitioner != null && fStructuredDocument != null) {
			fEmbeddedPartitioner.connect(fStructuredDocument);
		}
	}

	protected void setInternalPartition(int offset, int length, String type) {
		//TODO: need to carry this single instance idea further to be
		// complete,
		// but hopefully this will be less garbage than before (especially for
		// HTML, XML,
		// naturally!)
		internalReusedTempInstance = getEmbeddedPartitioner().createPartition(offset, length, type);

	}

	/**
	 * Sets the language.
	 * 
	 * @param language
	 *            The language to set
	 */
	public void setLanguage(String language) {
		this.fLanguage = language;
	}

}