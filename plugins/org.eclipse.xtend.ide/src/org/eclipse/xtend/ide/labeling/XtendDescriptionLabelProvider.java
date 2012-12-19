/*
* generated by Xtext
*/
package org.eclipse.xtend.ide.labeling;

import static org.eclipse.xtend.core.xtend.XtendPackage.Literals.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.xtend.core.resource.DescriptionFlags;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.TypesPackage;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.ui.label.DefaultDescriptionLabelProvider;

import com.google.inject.Inject;

/**
 * Provides labels for a IEObjectDescriptions and IResourceDescriptions.
 * 
 * see http://www.eclipse.org/Xtext/documentation/latest/xtext.html#labelProvider
 */
public class XtendDescriptionLabelProvider extends DefaultDescriptionLabelProvider {

	@Inject
	private XtendImages images;

	@Inject
	private DescriptionFlags descriptionFlags;

	@Override
	public Object image(IEObjectDescription element) {
		EClass eClass = element.getEClass();
		boolean isStatic = descriptionFlags.isStatic(element);
		if (eClass == XTEND_FILE)
			return images.forFile();
		else if (eClass == XTEND_IMPORT)
			return images.forImport();
		else if (eClass == XTEND_CLASS || eClass == TypesPackage.Literals.JVM_GENERIC_TYPE)
			return images.forClass(JvmVisibility.PUBLIC);
		else if (eClass == XTEND_FUNCTION)
			return images.forOperation(JvmVisibility.PUBLIC, isStatic);
		else if (eClass == XTEND_FIELD)
			return images.forField(JvmVisibility.PUBLIC, isStatic, false);
		else if (eClass == TypesPackage.Literals.JVM_OPERATION)
			return (descriptionFlags.isDispatcherOperation(element)) 
				? images.forDispatcherFunction(JvmVisibility.PUBLIC, isStatic) 
				: images.forOperation(JvmVisibility.PUBLIC, isStatic);
		else
			return super.image(element);
	}

}
