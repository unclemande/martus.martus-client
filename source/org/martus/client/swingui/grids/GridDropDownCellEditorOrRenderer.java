/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2005-2007, Beneficent
Technology, Inc. (The Benetech Initiative).

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

package org.martus.client.swingui.grids;

import java.awt.Component;
import java.util.Map;

import javax.swing.JTable;

import org.martus.client.swingui.fields.UiField;
import org.martus.client.swingui.fields.UiGrid;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.DropDownFieldSpec;
import org.martus.swing.UiComboBox;

abstract public class GridDropDownCellEditorOrRenderer extends GridCellEditorAndRenderer
{
	GridDropDownCellEditorOrRenderer(UiField field, Map otherGridFields)
	{
		super(field);
		otherGrids = otherGridFields;
	}

	abstract void setFieldSpec(DropDownFieldSpec spec);
	abstract void setChoices(ChoiceItem[] choices);
	
	public void spaceWasPressed()
	{
        UiComboBox comboBox = (UiComboBox)getComponent();
        if(!comboBox.isPopupVisible())
        	comboBox.requestFocus();
	}

	public Component getTableCellEditorComponent(JTable tableToUse, Object codeString, boolean isSelected, int row, int column)
	{
		DropDownFieldSpec spec = getFieldSpecForCell(tableToUse, row, column);
		setFieldSpec(spec);
		setChoices(getCurrentGridValuesAsChoices(spec));
		return super.getTableCellEditorComponent(tableToUse, codeString, isSelected, row, column);
	}

	public Component getTableCellRendererComponent(JTable tableToUse, Object codeString, boolean isSelected, boolean hasFocus, int row, int column)
	{
		DropDownFieldSpec spec = getFieldSpecForCell(tableToUse, row, column);
		setFieldSpec(spec);
		setChoices(getCurrentGridValuesAsChoices(spec));
		return super.getTableCellRendererComponent(tableToUse, codeString, isSelected, hasFocus, row, column);
	}

	protected DropDownFieldSpec getFieldSpecForCell(JTable tableToUse, int row, int column)
	{
		GridTable gridTable = (GridTable)tableToUse;
		return (DropDownFieldSpec)gridTable.getFieldSpecForCell(row, column);
	}

	ChoiceItem[] getCurrentGridValuesAsChoices(DropDownFieldSpec spec)
	{
		UiGrid dataSource = getGrid(spec);
		if(dataSource == null)
			return spec.getAllChoices();
		
		String gridColumnLabel = spec.getDataSourceGridColumn();
		return dataSource.buildChoicesFromColumnValues(gridColumnLabel);
	}
	
	UiGrid getGrid(DropDownFieldSpec spec)
	{
		String gridTag = spec.getDataSourceGridTag();
		if(gridTag == null)
			return null;
		
		return (UiGrid)otherGrids.get(gridTag);
	}

	Map otherGrids;
}
