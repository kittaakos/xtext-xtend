/*******************************************************************************
 * Copyright (c) 2012 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtend.ide.validator.preferences;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.xtext.xbase.ui.validation.XbaseValidationConfigurationBlock;

/**
 * @author Dennis Huebner - Initial contribution and API
 */
public class XtendValidatorConfigurationBlock extends XbaseValidationConfigurationBlock {

	@Override
	protected void fillSettingsPage(Composite composite, int nColumns, int defaultIndent) {
		// using keys from xbase for now.
		super.fillSettingsPage(composite, nColumns, defaultIndent);
	}
	
}
