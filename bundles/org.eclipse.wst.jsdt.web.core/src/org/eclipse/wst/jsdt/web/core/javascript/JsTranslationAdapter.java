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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.jsdt.core.IJavaProject;
import org.eclipse.wst.jsdt.core.JavaCore;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapter;
import org.eclipse.wst.sse.core.internal.provisional.INodeNotifier;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

/**
 * An adapter for getting a JSPTranslation of the document.
 * 
 * @author pavery
 */
public class JsTranslationAdapter implements INodeAdapter, IResourceChangeListener {

	private static final boolean DEBUG = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.wst.jsdt.web.core/debug/jsptranslation")); //$NON-NLS-1$  //$NON-NLS-2$
	private IStructuredDocument fHtmlDocument = null;
	private IJsTranslation fJSPTranslation = null;
	private NullProgressMonitor fTranslationMonitor = null;
	private String baseLocation;
	private boolean listenForChanges=false;
	private static final String PRIORITY_ATTRIB = "priority";
	private static final String CLASS_ATTRIB = "class";
	private IJsTranslation fTranslationElement;
	
	public JsTranslationAdapter(IDOMModel xmlModel) {
		fHtmlDocument = xmlModel.getStructuredDocument();
		baseLocation = xmlModel.getBaseLocation();
		initializeJavaPlugins();
		
		
	}
	public void shouldListenForChanges(boolean listenForProjectChanges) {
		if(listenForProjectChanges) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		}
	}

	public IJavaProject getJavaProject() {
		IJavaProject javaProject = null;
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IPath filePath = new Path(baseLocation);
		IProject project = null;
		if (filePath.segmentCount() > 0) {
			project = root.getProject(filePath.segment(0));
		}
		if (project != null) {
			javaProject = JavaCore.create(project);
		}
		
		return javaProject;
	}
	
	/**
	 * Returns the JSPTranslation for this adapter.
	 * 
	 * @return a JSPTranslationExtension
	 */
	public IJsTranslation getJSPTranslation(boolean listenForChanges) {
		if (fJSPTranslation == null || (!this.listenForChanges && listenForChanges)) {
			if(fJSPTranslation!=null) fJSPTranslation.release();
			if(fTranslationElement==null) {
				/* load the translation factory from the extension point */
				try {
					IExtensionRegistry registry = Platform.getExtensionRegistry();
				    IExtensionPoint extensionPoint =  registry.getExtensionPoint("org.eclipse.wst.jsdt.web.core.javascriptPreProcessor");
				    IConfigurationElement points[] = extensionPoint.getConfigurationElements();
				 //   int[] priorities = new int[points.length];
				   
				    int highestPriorityValue = -1;
				    int highestPriorityIndex = -1;
				    
				    for(int i = 0;i < points.length;i++){
				    	String priority = points[i].getAttribute(PRIORITY_ATTRIB);
				    	int value = Integer.parseInt(priority);
				    	if(value>highestPriorityValue) {
				    		highestPriorityIndex = i;
				    		highestPriorityValue = value;
				    	}
				       
				    }
				    fTranslationElement = (IJsTranslation)points[highestPriorityIndex].createExecutableExtension("class");
				}catch(Exception e) {
					System.out.println(e);
				}
			}
			//fJSPTranslation = new JsTranslation(fHtmlDocument, getJavaProject(),listenForChanges);
			fJSPTranslation = fTranslationElement.getInstance(fHtmlDocument, getJavaProject(), listenForChanges);
			this.listenForChanges=listenForChanges;
		}
		shouldListenForChanges(listenForChanges);
		return fJSPTranslation;
	}
	
	
	private void initializeJavaPlugins() {
		JavaCore.getPlugin();
	}
	
	public boolean isAdapterForType(Object type) {
		return type.equals(IJsTranslation.class);
	}
	
	public void notifyChanged(INodeNotifier notifier, int eventType, Object changedFeature, Object oldValue, Object newValue, int pos) {}
	
	public void release() {
		if (fTranslationMonitor != null) {
			fTranslationMonitor.setCanceled(true);
		}
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (fJSPTranslation != null) {
			if (JsTranslationAdapter.DEBUG) {
				System.out.println("JSPTranslationAdapter releasing:" + fJSPTranslation); //$NON-NLS-1$
			}
			fJSPTranslation.release();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IResourceChangeListener#resourceChanged(org.eclipse.core.resources.IResourceChangeEvent)
	 */
	public void resourceChanged(IResourceChangeEvent event) {
		IProject changedProject = (event==null || event.getResource()==null)?null:event.getResource().getProject();
		if(changedProject!=null && getJavaProject().getProject().equals(changedProject) && fJSPTranslation!=null){
			fJSPTranslation.classpathChange();
		}	
	}
}