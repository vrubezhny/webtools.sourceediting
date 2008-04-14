/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     
 * Provisional API: This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 *     
 *     
 *******************************************************************************/


package org.eclipse.wst.jsdt.web.core.javascript;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.filebuffers.FileBuffers; // import
													// org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile; // import
											// org.eclipse.core.resources.IProject;
// import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.wst.jsdt.core.IBuffer;
import org.eclipse.wst.jsdt.core.ICompilationUnit;
import org.eclipse.wst.jsdt.core.IJavaElement;
import org.eclipse.wst.jsdt.core.IJavaProject;
import org.eclipse.wst.jsdt.core.IPackageFragmentRoot;
import org.eclipse.wst.jsdt.core.ISourceRange;
import org.eclipse.wst.jsdt.core.JavaModelException; // import
														// org.eclipse.wst.jsdt.core.LibrarySuperType;
import org.eclipse.wst.jsdt.core.WorkingCopyOwner;
import org.eclipse.wst.jsdt.internal.core.DocumentContextFragmentRoot;
import org.eclipse.wst.jsdt.internal.core.SourceRefElement;
import org.eclipse.wst.jsdt.web.core.internal.Logger;
import org.eclipse.wst.jsdt.web.core.internal.project.JsWebNature;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

/**
 * @author brad childs
 */
public class JsTranslation implements IJsTranslation {

	private static final boolean DEBUG;
	static {
		String value = Platform.getDebugOption("org.eclipse.wst.jsdt.web.core/debug/jsptranslation"); //$NON-NLS-1$
		DEBUG = value != null && value.equalsIgnoreCase("true"); //$NON-NLS-1$
	}

	private ICompilationUnit fCompilationUnit = null;
	private DocumentContextFragmentRoot fDocumentScope;
	private IJavaProject fJavaProject = null;
	private byte[] fLock = null;
	private IProgressMonitor fProgressMonitor = null;
	protected IStructuredDocument fHtmlDocument;
	protected String fModelBaseLocation;


	private static final String SUPER_TYPE_NAME = "Window"; //$NON-NLS-1$
	private static final String SUPER_TYPE_LIBRARY = "org.eclipse.wst.jsdt.launching.baseBrowserLibrary"; //$NON-NLS-1$

	protected IJsTranslator fTranslator;

	private String mangledName;
	protected boolean listenForChanges;

	public JsTranslation() {
		/* do nothing */
	}
	
	public IJsTranslator getTranslator() {
		if(fTranslator!=null) {
			return fTranslator;
		}
		
		fTranslator = new JsTranslator(fHtmlDocument, fModelBaseLocation, listenForChanges);
		return this.fTranslator;
	}
	

	
	protected JsTranslation(IStructuredDocument htmlDocument, IJavaProject javaProj, boolean listenForChanges) {
		fLock = new byte[0];
		fJavaProject = javaProj;
		fHtmlDocument = htmlDocument;
		setBaseLocation();
		mangledName = createMangledName();
		this.listenForChanges=listenForChanges;
	}

	public IJsTranslation getInstance(IStructuredDocument htmlDocument, IJavaProject javaProj, boolean listenForChanges) {
		return new JsTranslation(htmlDocument,javaProj, listenForChanges);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getJavaProject()
	 */
	public IJavaProject getJavaProject() {
		return fJavaProject;
	}

	private IPackageFragmentRoot getDocScope(boolean reset) {
		if (fDocumentScope == null) {
			// IProject project = getJavaProject().getProject();
			// IResource absoluteRoot =
			// ((IContainer)getJavaProject().getResource()).findMember(
			// WebRootFinder.getWebContentFolder(fJavaProject.getProject()));
			fDocumentScope = new DocumentContextFragmentRoot(fJavaProject, getFile(), WebRootFinder.getWebContentFolder(fJavaProject.getProject()), WebRootFinder.getServerContextRoot(fJavaProject.getProject()), JsWebNature.VIRTUAL_SCOPE_ENTRY);
			fDocumentScope.setIncludedFiles(getTranslator().getRawImports());
			return fDocumentScope;
		}

		if (reset)
			fDocumentScope.setIncludedFiles(getTranslator().getRawImports());
		return fDocumentScope;
	}

	private void setBaseLocation() {
		IDOMModel xmlModel = null;
		try {
			xmlModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForRead(fHtmlDocument);
			if (xmlModel == null) {
				xmlModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForRead(fHtmlDocument);
			}
			fModelBaseLocation = xmlModel.getBaseLocation();
		}
		finally {
			if (xmlModel != null)
				xmlModel.releaseFromRead();
		}
		// return xmlModel;
	}

	public IFile getFile() {
		return FileBuffers.getWorkspaceFileAtLocation(new Path(fModelBaseLocation));
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getHtmlDocument()
	 */
	public IDocument getHtmlDocument() {
		return fHtmlDocument;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getMissingTagStart()
	 */
	public int getMissingTagStart() {
		return getTranslator().getMissingEndTagRegionStart();
	}

	private String getWebRoot() {
		return WebRootFinder.getWebContentFolder(fJavaProject.getProject()).toString();
	}


	public String getDirectoryUnderRoot() {
		String webRoot = getWebRoot();
		IPath projectWebRootPath = getJavaProject().getPath().append(webRoot);
		IPath filePath = new Path(fModelBaseLocation).removeLastSegments(1);
		return filePath.removeFirstSegments(projectWebRootPath.matchingFirstSegments(filePath)).toString();
	}

	/**
	 * Originally from ReconcileStepForJava. Creates an ICompilationUnit from
	 * the contents of the JSP document.
	 * 
	 * @return an ICompilationUnit from the contents of the JSP document
	 */
	private ICompilationUnit createCompilationUnit() throws JavaModelException {
		IPackageFragmentRoot root = getDocScope(true);
		ICompilationUnit cu = root.getPackageFragment("").getCompilationUnit(getMangledName() + JsDataTypes.BASE_FILE_EXTENSION).getWorkingCopy(getWorkingCopyOwner(), getProblemRequestor(), getProgressMonitor()); //$NON-NLS-1$
		IBuffer buffer;
		try {
			buffer = cu.getBuffer();
		}
		catch (JavaModelException e) {
			e.printStackTrace();
			buffer = null;
		}
		if (buffer != null) {
			getTranslator().setBuffer(buffer);
		}
		return cu;
	}

	public String fixupMangledName(String displayString) {
		if (displayString == null) {
			return null;
		}
		return displayString.replaceAll(getMangledName() + ".js", getHtmlPageName()); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getAllElementsInJsRange(int, int)
	 */
	public IJavaElement[] getAllElementsInJsRange(int javaPositionStart, int javaPositionEnd) {
		IJavaElement[] EMTPY_RESULT_SET = new IJavaElement[0];
		IJavaElement[] result = EMTPY_RESULT_SET;
		IJavaElement[] allChildren = null;
		try {
			allChildren = getCompilationUnit().getChildren();
		}
		catch (JavaModelException e) {
		}
		Vector validChildren = new Vector();
		for (int i = 0; i < allChildren.length; i++) {
			if (allChildren[i].getElementType() != IJavaElement.PACKAGE_DECLARATION) {
				ISourceRange range = getJSSourceRangeOf(allChildren[i]);
				if (javaPositionStart <= range.getOffset() && range.getLength() + range.getOffset() <= (javaPositionEnd)) {
					validChildren.add(allChildren[i]);
				}
				else if (allChildren[i].getElementType() == IJavaElement.TYPE) {
					validChildren.add(allChildren[i]);
				}
			}
		}
		if (validChildren.size() > 0) {
			result = (IJavaElement[]) validChildren.toArray(new IJavaElement[]{});
		}
		if (result == null || result.length == 0) {
			return EMTPY_RESULT_SET;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getCompilationUnit()
	 */
	public ICompilationUnit getCompilationUnit() {
		synchronized (fLock) {
			try {
				if (fCompilationUnit == null) {
					fCompilationUnit = createCompilationUnit();
					return fCompilationUnit;
				}

			}
			catch (JavaModelException jme) {
				if (JsTranslation.DEBUG) {
					Logger.logException("error creating JSP working copy... ", jme); //$NON-NLS-1$
				}
			}

		}
		getDocScope(true);
		try {
			fCompilationUnit = fCompilationUnit.getWorkingCopy(getWorkingCopyOwner(), getProblemRequestor(), getProgressMonitor());
			// fCompilationUnit.makeConsistent(getProgressMonitor());
		}
		catch (JavaModelException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
		}
		return fCompilationUnit;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getElementsFromJsRange(int, int)
	 */
	public IJavaElement[] getElementsFromJsRange(int javaPositionStart, int javaPositionEnd) {
		IJavaElement[] EMTPY_RESULT_SET = new IJavaElement[0];
		IJavaElement[] result = EMTPY_RESULT_SET;
		try {
			ICompilationUnit cu = getCompilationUnit();
			if (cu != null) {
				synchronized (fLock) {
					int cuDocLength = cu.getBuffer().getLength();
					int javaLength = javaPositionEnd - javaPositionStart;
					if (cuDocLength > 0 && javaPositionStart >= 0 && javaLength >= 0 && javaPositionEnd <= cuDocLength) {
						result = cu.codeSelect(javaPositionStart, javaLength, getWorkingCopyOwner());
					}
				}
			}
			if (result == null || result.length == 0) {
				return EMTPY_RESULT_SET;
			}
		}
		catch (JavaModelException x) {
			Logger.logException(x);
		}
		return result;
	}

	private String getHtmlPageName() {
		IPath path = new Path(fModelBaseLocation);
		return path.lastSegment();

	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getHtmlText()
	 */
	public String getHtmlText() {
		return fHtmlDocument.get();
	}

	public String getJavaPath() {
		IPath rootPath = new Path(fModelBaseLocation).removeLastSegments(1);
		String cuPath = rootPath.append("/" + getMangledName() + JsDataTypes.BASE_FILE_EXTENSION).toString(); //$NON-NLS-1$
		return cuPath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getJsElementAtOffset(int)
	 */
	public IJavaElement getJsElementAtOffset(int jsOffset) {
		IJavaElement elements = null;
		try {
			elements = getCompilationUnit().getElementAt(jsOffset);
		}
		catch (JavaModelException e) {
			// TODO Auto-generated catch block
			if (JsTranslation.DEBUG) {
				Logger.logException("error retrieving java elemtnt from compilation unit... ", e); //$NON-NLS-1$
			}
			// }
		}
		return elements;
	}

	private ISourceRange getJSSourceRangeOf(IJavaElement element) {
		// returns the offset in html of given element
		ISourceRange range = null;
		if (element instanceof SourceRefElement) {
			try {
				range = ((SourceRefElement) element).getSourceRange();
			}
			catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return range;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getJsText()
	 */
	public String getJsText() {
		return getTranslator().getJsText();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getScriptPositions()
	 */
	public Position[] getScriptPositions() {
		return getTranslator().getHtmlLocations();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#insertInFirstScriptRegion(java.lang.String)
	 */
	public void insertInFirstScriptRegion(String text) {
		Position pos[] = getScriptPositions();
		int scriptStartOffset = 0;
		if(pos!=null && pos.length>0) {
			scriptStartOffset = pos[0].getOffset();
			
		}
		String insertText = (scriptStartOffset==0?"":"\n") + text;
		insertScript(scriptStartOffset,insertText);
		
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#insertScript(int, java.lang.String)
	 */
	public void insertScript(int offset, String text) {

		IDOMModel xmlModel = null;
		Position[] inHtml = getScriptPositions();
		boolean isInsideExistingScriptRegion = false;
		for (int i = 0; i < inHtml.length; i++) {
			if (inHtml[i].overlapsWith(offset, 1)) {
				// * inserting into a script region
				isInsideExistingScriptRegion = true;
			}
		}

		String insertText = null;

		if (isInsideExistingScriptRegion) {
			insertText = text;
		}
		else {
			insertText = offset != 0 ? "\n" : "" + "<script type=\"text/javascript\">\n" + text + "\n</script>\n";
		}
	//	translator.documentAboutToBeChanged(null);

		synchronized (fLock) {
			try {
				xmlModel = (IDOMModel) StructuredModelManager.getModelManager().getExistingModelForEdit(fHtmlDocument);
				if (xmlModel == null) {
					xmlModel = (IDOMModel) StructuredModelManager.getModelManager().getModelForEdit(fHtmlDocument);
				}
				if (xmlModel != null) {


					xmlModel.aboutToChangeModel();
					xmlModel.getDocument().getStructuredDocument().replaceText(this, offset, 0, insertText);
					xmlModel.changedModel();
					try {
						xmlModel.save();
					}

					catch (UnsupportedEncodingException e) {}
					catch (IOException e) {}
					catch (CoreException e) {}
				}
			}
			finally {
				if (xmlModel != null)
					xmlModel.releaseFromEdit();
			}
		}

	//	translator.documentChanged(null);

	}

	public String getMangledName() {
		return this.mangledName;
	}

	private String createMangledName() {
		return JsNameManglerUtil.mangle(fModelBaseLocation);
	}

	/**
	 * 
	 * @return the problem requestor for the CompilationUnit in this
	 *         JSPTranslation
	 */
	private JsProblemRequestor getProblemRequestor() {
		return CompilationUnitHelper.getInstance().getProblemRequestor();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#getProblems()
	 */
	public List getProblems() {
		List problems = getProblemRequestor().getCollectedProblems();
		getProblemRequestor().endReporting();
		return problems != null ? problems : new ArrayList();
	}

	private IProgressMonitor getProgressMonitor() {
		if (fProgressMonitor == null) {
			fProgressMonitor = new NullProgressMonitor();
		}
		return fProgressMonitor;
	}

	public WorkingCopyOwner getWorkingCopyOwner() {
		return CompilationUnitHelper.getInstance().getWorkingCopyOwner();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#ifOffsetInImportNode(int)
	 */
	public boolean ifOffsetInImportNode(int offset) {
		Position[] importRanges = getTranslator().getImportHtmlRanges();
		for (int i = 0; i < importRanges.length; i++) {
			if (importRanges[i].includes(offset)) {
				return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#reconcileCompilationUnit()
	 */
	public void reconcileCompilationUnit() {
		// if(true) return;
		ICompilationUnit cu = getCompilationUnit();
		if (fCompilationUnit == null) {
			return;
		}
		if (cu != null) {
			try {
				synchronized (fLock) {
					// if(false)
					// cu.makeConsistent(getProgressMonitor());
					cu.reconcile(ICompilationUnit.NO_AST, true, getWorkingCopyOwner(), getProgressMonitor());
				}
			}
			catch (JavaModelException e) {
				Logger.logException(e);
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#release()
	 */
	public void release() {
		if (getTranslator() != null)
			getTranslator().release();
		synchronized (fLock) {
			if (fCompilationUnit != null) {
				try {
					if (JsTranslation.DEBUG) {
						System.out.println("------------------------------------------------------------------"); //$NON-NLS-1$
						System.out.println("(-) JSPTranslation [" + this + "] discarding CompilationUnit: " + fCompilationUnit); //$NON-NLS-1$ //$NON-NLS-2$
						System.out.println("------------------------------------------------------------------"); //$NON-NLS-1$
					}
					fCompilationUnit.discardWorkingCopy();
				}
				catch (JavaModelException e) {
					// we're done w/ it anyway
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.jsdt.web.core.internal.java.IJsTranslation#setProblemCollectingActive(boolean)
	 */
	public void setProblemCollectingActive(boolean collect) {
		ICompilationUnit cu = getCompilationUnit();
		if (cu != null) {
			getProblemRequestor().setIsActive(collect);
		}
	}

	public void classpathChange() {

		if (fDocumentScope != null) {
			fDocumentScope.classpathChange();
		}
	}
}