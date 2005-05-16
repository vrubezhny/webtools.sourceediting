/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.xml.core.internal.contentmodel.internal.modelqueryimpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.extension.ModelQueryExtension;

public class ModelQueryExtensionRegistry
{
  protected static final String EXTENSION_POINT_ID = "modelQueryExtensions"; //$NON-NLS-1$
  protected static final String TAG_NAME = "modelQueryExtension"; //$NON-NLS-1$
  private List descriptors;

  public List getApplicableExtensions(String contentTypeId, String namespace)
  {
    List list = new ArrayList();
    if (contentTypeId != null)
    {
      ensureExtensionsLoaded();
      for (Iterator i = descriptors.iterator(); i.hasNext();)
      {
        ModelQueryExtensionDescriptor descriptor = (ModelQueryExtensionDescriptor) i.next();
        if (contentTypeId.equals(descriptor.getContentTypeId()))
        {
          if (descriptor.getNamespace() == null ||  descriptor.getNamespace().equals(namespace))
          {  
            try
            {
              ModelQueryExtension extension = descriptor.createModelQueryExtension();
              list.add(extension);
            }
            catch (Exception e) {             
            }
          }  
        }
      }
    }
    return list;
  }
  
  /*
   *  TODO : consider providing a non-plugin means add/remove extensions
   *   
  public void addExtension(ModelQueryExtension extension)
  {     
  }

  public void removeExtension(ModelQueryExtensionDeprecated extension)
  {
  }*/

  /**
   * Reads all extensions.
   * <p>
   * This method can be called more than once in order to reload from a changed
   * extension registry.
   * </p>
   */
  private synchronized void reloadExtensions()
  {
    descriptors = new ArrayList();
    String bundleid = "org.eclipse.wst.xml.core"; //$NON-NLS-1$      
    IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(bundleid, EXTENSION_POINT_ID);
    for (int i = 0; i < elements.length; i++)
    {
      ModelQueryExtensionDescriptor descriptor = new ModelQueryExtensionDescriptor(elements[i]);
      descriptors.add(descriptor);
    }
  }

  /**
   * Ensures the extensions have been loaded at least once.
   */
  private void ensureExtensionsLoaded()
  {
    if (descriptors == null)
    {
      reloadExtensions();
    }
  }
}
