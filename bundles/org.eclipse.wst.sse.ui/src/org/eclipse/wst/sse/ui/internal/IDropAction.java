/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Jens Lukowski/Innoopract - initial renaming/restructuring
 *     
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal;



import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.ui.IEditorPart;

/**
 */
public interface IDropAction {


	/**
	 * @param data
	 *            DropTargetEvent.data will be passed.
	 * @return boolean true for supported data type
	 */
	public boolean isSupportedData(Object data);

	/**
	 * @return boolean true for the action is actually processed false for the
	 *         action is not processed.
	 */
	public boolean run(DropTargetEvent event, IEditorPart targetEditor);
}
