/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2007, Beneficent
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
package org.martus.client.swingui.fields;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;

import org.martus.client.swingui.dialogs.UiDialogLauncher;
import org.martus.client.swingui.grids.GridTable;
import org.martus.client.swingui.grids.GridTableModel;
import org.martus.clientside.UiLocalization;
import org.martus.common.GridData;
import org.martus.common.fieldspec.GridFieldSpec;
import org.martus.swing.UiButton;
import org.martus.swing.UiScrollPane;
import org.martus.swing.Utilities;


abstract public class UiGrid extends UiField
{
	public UiGrid(GridFieldSpec fieldSpec, UiDialogLauncher dlgLauncher, boolean isEditable)
	{
		this(new GridTableModel(fieldSpec), dlgLauncher, isEditable);
	}
	
	public UiGrid(GridTableModel modelToUse, UiDialogLauncher dlgLauncher, boolean isEditable)
	{
		model = modelToUse;
		table = new GridTable(model, dlgLauncher, isEditable);
		table.setColumnSelectionAllowed(false);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setShowGrid(true);
		table.changeSelection(0, 1, false, false);
		table.setRowHeight(table.getRowHeight() + ROW_HEIGHT_PADDING);
		UiScrollPane tableScroller = new UiScrollPane(table);
		widget = new JPanel();
		widget.setLayout(new BorderLayout());

		buttonBox = Box.createHorizontalBox();
		buttonBox.setBorder(new EmptyBorder(10,0,0,0));
		setButtons(createButtons());
		
		widget.add(buttonBox, BorderLayout.SOUTH);
		widget.add(tableScroller, BorderLayout.CENTER);
	} 
	
	protected UiLocalization getLocalization()
	{
		return table.getDialogLauncher().GetLocalization();
	}
	
	protected Vector createButtons()
	{
		return new Vector();
	}
	
	protected UiButton createShowExpandedButton()
	{
		UiButton expand = new UiButton(getLocalization().getButtonLabel("ShowGridExpanded"));
		expand.addActionListener(new ExpandButtonHandler());
		return expand;
	}
	
	class ExpandButtonHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent arg0)
		{
			System.out.println("Expand button pressed");
		}
		
	}

	protected void setButtons(Vector buttons) 
	{
		buttonBox.removeAll();
		buttons.add(Box.createHorizontalGlue());
		Component[] buttonsAsArray = (Component[])buttons.toArray(new Component[0]);
		Utilities.addComponentsRespectingOrientation(buttonBox, buttonsAsArray);
	}	
	
	public JComponent getComponent()
	{
		return widget;
	}
	
	public String getText()
	{
		return model.getXmlRepresentation();
	}

	public void setText(String newText)
	{
		try
		{
			model.setFromXml(newText);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean isRowSelected()
	{
		return (table.getSelectedRow() != NO_ROW_SELECTED);
	}
	
	public void insertRow() throws ArrayIndexOutOfBoundsException
	{
		stopCellEditing();
		model.insertEmptyRow(table.getSelectedRow());
	}

	public void deleteSelectedRow() throws ArrayIndexOutOfBoundsException
	{
		stopCellEditing();
		model.deleteSelectedRow(table.getSelectedRow());
	}

	private void stopCellEditing() 
	{
		TableCellEditor cellEditor = table.getCellEditor();
		if(cellEditor != null)
			cellEditor.stopCellEditing();
	}
	
	public GridTableModel getGridTableModel()
	{
		return model;
	}

	public GridData getGridData()
	{
		return model.getGridData();
	}
	
	public GridTable getTable()
	{
		return table;
	}
	
	private static final int NO_ROW_SELECTED = -1;
	private static final int ROW_HEIGHT_PADDING = 10;
	JPanel widget;
	Box buttonBox;
	GridTable table;
	GridTableModel model;
}

