/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2006, Beneficent
Technology, Inc. (Benetech).

Martus is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later
version with the additions and exceptions described in the
accompanying Martus license file entitled "license.txt".

It is distributed WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, including warranties of fitness of purpose or
merchantability.  See the accompanying Martus License and
GPL license for more details on the required license terms
for this software.

You should have received a copy of the GNU General Public
License along with this program; if not, write to the Free
Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA 02111-1307, USA.

*/
package org.martus.client.swingui.fields;

import org.martus.client.swingui.MartusLocalization;
import org.martus.client.swingui.UiMainWindow;
import org.martus.client.swingui.dialogs.UiDialogLauncher;
import org.martus.common.fieldspec.DropDownFieldSpec;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.fieldspec.GridFieldSpec;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.util.MultiCalendar;

public class UiEditableFieldCreator extends UiFieldCreator
{
	public UiEditableFieldCreator(UiMainWindow mainWindowToUse)
	{
		super(mainWindowToUse);
	}

	public UiField createUnknownField()
	{
		return new UiUnknownViewer(getLocalization());
	}
	
	public UiField createNormalField()
	{
		return new UiNormalTextEditor(getLocalization(), mainWindow.getEditingTextFieldColumns());
	}

	public UiField createMultilineField()
	{
		return new UiMultilineTextEditor(getLocalization(), mainWindow.getEditingTextFieldColumns());
	}

	public UiField createMessageField(FieldSpec spec)
	{
		return new UiMessageField(spec, mainWindow.getEditingTextFieldColumns());
	}

	public UiField createChoiceField(DropDownFieldSpec spec)
	{
		UiChoiceEditor dropDownField = new UiChoiceEditor(spec);
		return dropDownField;
	}
	
	public UiField createDateField(FieldSpec spec)
	{
		MultiCalendar maxDate = null;
		if(StandardFieldSpecs.isStandardFieldTag(spec.getTag()))
			maxDate = new MultiCalendar();

		return new UiDateEditor(getLocalization(), maxDate);		
	}
	
	public UiField createFlexiDateField(FieldSpec spec)
	{
		return new UiFlexiDateEditor(getLocalization(), spec);	
	}

	public UiField createBoolField()
	{
		return new UiBoolEditor();
	}

	public UiField createGridField(GridFieldSpec fieldSpec)
	{
		MartusLocalization localization = mainWindow.getLocalization();
		fieldSpec.setColumnZeroLabel(localization.getFieldLabel("ColumnGridRowNumber"));
		UiDialogLauncher dlgLauncher = new UiDialogLauncher(mainWindow.getCurrentActiveFrame(), localization);
		UiGridEditor gridEditor = new UiGridEditor(mainWindow, fieldSpec, dlgLauncher, editableGridFields, mainWindow.getEditingTextFieldColumns());
		editableGridFields.put(fieldSpec.getTag(), gridEditor);
		return gridEditor;
	}
}
