/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2013, Beneficent
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
package org.martus.client.swingui;

import java.util.Vector;

import org.martus.client.core.MartusApp;
import org.martus.common.MiniLocalization;
import org.martus.swing.UiTableModel;

abstract public class ExternalPublicKeysTableModel extends UiTableModel
{
	public ExternalPublicKeysTableModel(MartusApp app)
	{
		localization = app.getLocalization();
		fontHelper = new UiFontEncodingHelper(app.getConfigInfo().getUseZawgyi());
		entries = new Vector();
	}
	
	public void addRawEntry(SelectableHeadquartersEntry entryToAdd)
	{
		entries.add(entryToAdd);
		int rowAdded = entries.size();
		fireTableRowsInserted(rowAdded, rowAdded);
	}
	
	public String getDisplayableLabel(SelectableExternalPublicKeyEntry entry)
	{
		return fontHelper.getDisplayable(entry.getLabel());
	}
	
	public MiniLocalization getLocalization()
	{
		return localization;
	}
	
	public int getRowCount() 
	{
		return entries.size();
	}

	public void selectRow(int row)
	{
		SelectableHeadquartersEntry entry = (SelectableHeadquartersEntry)entries.get(row);
		entry.setSelected(true);
	}
	
	public void removeRow(int row)
	{
		entries.remove(row);
		fireTableRowsDeleted(row, row);
	}

	public String getLabel(int row)
	{
		SelectableHeadquartersEntry entry = (SelectableHeadquartersEntry)entries.get(row);
		return getDisplayableLabel(entry);
	}

	public String getPublicCode(int row)
	{
		SelectableHeadquartersEntry entry = (SelectableHeadquartersEntry)entries.get(row);
		return entry.getPublicCode();
	}

	public boolean contains(SelectableHeadquartersEntry entry)
	{
		return entries.contains(entry);
	}
	
	public void setLabel(int row, String newLabel)
	{
		SelectableExternalPublicKeyEntry entry = getRawEntry(row);
		entry.setLabel(newLabel);
	}
	
	protected Vector getRawEntries()
	{
		return entries;
	}
	
	protected SelectableExternalPublicKeyEntry getRawEntry(int row)
	{
		return (SelectableExternalPublicKeyEntry)getRawEntries().get(row);
	}
	
	private MiniLocalization localization;
	private UiFontEncodingHelper fontHelper;
	private Vector entries;
}
