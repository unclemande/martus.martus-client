/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2004, Beneficent
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

import java.io.IOException;
import java.util.Vector;

import javax.swing.table.DefaultTableModel;
import javax.xml.parsers.ParserConfigurationException;

import org.martus.common.GridData;
import org.martus.common.GridFieldSpec;
import org.xml.sax.SAXException;


public class GridTableModel extends DefaultTableModel
{
	
	public int getColumnCount() 
	{
		int realDataColumnCount = gridData.getColumnCount();
		return realDataColumnCount + EXTRA_COLUMN;
	}

	public boolean isCellEditable(int row, int column)
	{
		if(column == 0)
			return false;
		return super.isCellEditable(row, column);
	}
	public GridTableModel(GridFieldSpec fieldSpec)
	{
		Vector columnLabels = new Vector();
		columnLabels.add(fieldSpec.getColumnZeroLabel());
		columnLabels.addAll(fieldSpec.getAllColumnLabels());
		setColumnIdentifiers(columnLabels);
		gridData = new GridData(fieldSpec.getColumnCount());
	}
	
	public void addEmptyRow()
	{
		gridData.addEmptyRow();
		int newRowIndex = getRowCount()-1;
		this.fireTableRowsInserted(newRowIndex, newRowIndex);
	}
	
	public int getRowCount()
	{
		if(gridData == null)
			return 0;
		return gridData.getRowCount();
	}

	public Object getValueAt(int row, int column)
	{
		if(column == 0)
			return new Integer(row+1).toString();
		return gridData.getValueAt(row, column - EXTRA_COLUMN );
	}

	public void setValueAt(Object aValue, int row, int column)
	{
		if(column == 0)
			return;
		gridData.setValueAt((String)aValue, row, column - EXTRA_COLUMN);
		fireTableCellUpdated(row,column);
	}
	
	public String getXmlRepresentation()
	{
		return gridData.getXmlRepresentation();
	}
	
	public void setFromXml(String xmlText) throws IOException, ParserConfigurationException, SAXException
	{
		gridData.setFromXml(xmlText);
		fireTableDataChanged();
	}
	private int EXTRA_COLUMN = 1;
	private GridData gridData;
}
