/**
 * 
 */
package org.eclipse.wst.jsdt.web.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ISetSelectionTarget;
import org.eclipse.wst.jsdt.core.IJavaElement;
import org.eclipse.wst.jsdt.internal.ui.actions.ActionMessages;
import org.eclipse.wst.jsdt.internal.ui.packageview.PackageExplorerPart;
import org.eclipse.wst.jsdt.internal.ui.util.ExceptionHandler;

/**
 * @author childsb
 *
 */
public class ShowInNavigatorAction extends JsElementActionProxy {
	

	public void selectionChanged(IAction action, ISelection selection) {
		setSelection(selection);
		IJavaElement elements[] = getJsElementsFromSelection(getCurrentSelection());
		
		for(int i = 0;i<elements.length;i++) {
			if(elements[i].isVirtual()) {
				
				IResource resource = getHostResource( elements[i]);
				if(resource==null || !resource.exists()) {
					action.setEnabled(false);
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	private IResource getHostResource(IJavaElement virtualElement) {
		IProject project = virtualElement.getJavaProject().getProject();
		IPath path = new Path(virtualElement.getHostPath().getPath());
		IResource host = project.getWorkspace().getRoot().findMember(path);
		return host;
		
	}
	

	public void run(IAction action) {
		IJavaElement elements[] = getJsElementsFromSelection(getCurrentSelection());
		if(elements==null || elements.length==0) return;
		
		IResource resource = null;
		
		if(elements[0].isVirtual()) {
			resource = getHostResource(elements[0]);
		}else {
			resource = elements[0].getResource();
		}
		
		if (resource == null)
			return;
		try {
			IWorkbenchPage page= targetWorkbenchPart.getSite().getPage();
			IViewPart view= page.showView(IPageLayout.ID_RES_NAV);
			if (view instanceof ISetSelectionTarget) {
				
				ISelection selection= new StructuredSelection(resource);
				((ISetSelectionTarget)view).selectReveal(selection);
			}
		} catch(PartInitException e) {
			ExceptionHandler.handle(e, targetWorkbenchPart.getSite().getShell(), "Error Opening in Script View", "Error while displaying element in Script View:\n" + e); 
		}
	}
}