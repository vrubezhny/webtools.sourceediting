package org.eclipse.jst.jsp.ui.tests.viewer;

import junit.framework.TestCase;

import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jst.jsp.core.internal.provisional.text.IJSPPartitionTypes;
import org.eclipse.jst.jsp.ui.internal.provisional.StructuredTextViewerConfigurationJSP;
import org.eclipse.jst.jsp.ui.tests.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.wst.html.core.internal.provisional.text.IHTMLPartitionTypes;
import org.eclipse.wst.sse.ui.internal.StructuredTextViewer;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.xml.core.internal.provisional.text.IXMLPartitions;

/**
 * @author pavery
 */
public class TestViewerConfigurationJSP extends TestCase {
    
	private StructuredTextViewerConfigurationJSP fConfig = null;
	private boolean fDisplayExists = true;
	private StructuredTextViewer fViewer = null;
	private boolean isSetup = false;
	
    public TestViewerConfigurationJSP() {
        super("TestViewerConfigurationJSP");
    }
    protected void setUp() throws Exception {
		
    	super.setUp();
		if(!this.isSetup){
			setUpViewerConfiguration();
			this.isSetup = true;
		}
    }
	
	private void setUpViewerConfiguration() {

		if (Display.getCurrent() != null) {
			
			Shell shell = null;
			Composite parent = null;
			
			if(PlatformUI.isWorkbenchRunning()) {
				shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			}
			else {	
				shell = new Shell(Display.getCurrent());
			}
			parent = new Composite(shell, SWT.NONE);
			
			// dummy viewer
			fViewer = new StructuredTextViewer(parent, null, null, false, SWT.NONE);
			fConfig = new StructuredTextViewerConfigurationJSP();
		}
		else {
			fDisplayExists = false;
			Logger.log(Logger.INFO, "Remember, viewer configuration tests are not run because workbench is not open (normal on build machine)");
		}
	}
    
	/**
	 * Not necessary
	 */
	public void testGetAnnotationHover() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IAnnotationHover hover = fConfig.getAnnotationHover(fViewer);
		assertNotNull("AnnotationHover is null", hover);
    }
	
	public void testGetAutoEditStrategies() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IAutoEditStrategy[] strategies = fConfig.getAutoEditStrategies(fViewer, IJSPPartitionTypes.JSP_DEFAULT);
		assertNotNull(strategies);
		assertTrue("there are no auto edit strategies", strategies.length > 0);
	}
	
	public void testGetConfiguredContentTypes() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		String[] configuredContentTypes = fConfig.getConfiguredContentTypes(fViewer);
		assertNotNull(configuredContentTypes);
		assertTrue("there are no configured content types", configuredContentTypes.length > 1);
	}
	
	public void testGetContentAssistant() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IContentAssistant ca = fConfig.getContentAssistant(fViewer);
		assertNotNull("there is no content assistant", ca);
	}
	
	public void testGetContentFormatter() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IContentFormatter cf = fConfig.getContentFormatter(fViewer);
		assertNotNull("there is no content formatter", cf);
	}
	
	public void testGetCorrectionAssistant() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IContentAssistant ca = fConfig.getCorrectionAssistant(fViewer);
		assertNull("there is now a correction assistant?", ca);
	}
	
	public void testGetDoubleClickStrategy() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		String[] contentTypes = fConfig.getConfiguredContentTypes(fViewer);
		for (int i = 0; i < contentTypes.length; i++) {
			ITextDoubleClickStrategy strategy = fConfig.getDoubleClickStrategy(fViewer, contentTypes[i]);
			if(strategy != null) {
				return;
			}
		}
		assertTrue("there are no configured double click strategies", false);
	}
	
	public void testGetHyperlinkDetectors() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IHyperlinkDetector[] detectors = fConfig.getHyperlinkDetectors(fViewer);
		assertNotNull(detectors);
		assertTrue("there are no hyperlink detectors", detectors.length > 1);
	}
	
    public void testGetIndentPrefixes() {
		// probably no display
		if(!fDisplayExists)
			return;
		
		String[] contentTypes = fConfig.getConfiguredContentTypes(fViewer);
		for (int i = 0; i < contentTypes.length; i++) {
			String prefixes[] = fConfig.getIndentPrefixes(fViewer, contentTypes[i]);
			if(prefixes != null) {
				return;
			}
		}
		assertTrue("there are no configured indent prefixes", false);
	}
	
    public void testGetInformationControlCreator() {
		// probably no display
		if(!fDisplayExists)
			return;
		
		IInformationControlCreator infoCreator = fConfig.getInformationControlCreator(fViewer);
		assertNotNull("InformationControlCreator is null", infoCreator);
    }
    
	public void testGetInformationPresenter() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IInformationPresenter presenter = fConfig.getInformationPresenter(fViewer);
		assertNotNull("InformationPresenter is null", presenter);
	}
	
	public void testGetLineStyleProviders() {
		// probably no display
		if(!fDisplayExists)
			return;
		
		String[] contentTypes = fConfig.getConfiguredContentTypes(fViewer);
		for (int i = 0; i < contentTypes.length; i++) {
			LineStyleProvider providers[] = fConfig.getLineStyleProviders(fViewer, contentTypes[i]);
			if(providers != null) {
				return;
			}
		}
		assertTrue("there are no configured line style providers", false);
	}
	
	public void testGetReconciler() {
		
		// probably no display
		if(!fDisplayExists)
			return;
		
		IReconciler r = fConfig.getReconciler(fViewer);
		assertNotNull("Reconciler is null", r);
	}
	
	public void testGetTextHover() {
		
		// probably no display
		if(!fDisplayExists)
			return;

		String[] hoverPartitions = new String[]{IHTMLPartitionTypes.HTML_DEFAULT,
													IHTMLPartitionTypes.SCRIPT,
													IJSPPartitionTypes.JSP_DEFAULT,
													IJSPPartitionTypes.JSP_DIRECTIVE,
													IJSPPartitionTypes.JSP_CONTENT_JAVA,
													IXMLPartitions.XML_DEFAULT};
		
		for (int i = 0; i < hoverPartitions.length; i++) {
			ITextHover hover = fConfig.getTextHover(fViewer, hoverPartitions[i], SWT.NONE);
			assertNotNull("hover was null for partition: " + hoverPartitions[i], hover);
		}
	}
}
