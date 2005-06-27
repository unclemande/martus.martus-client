/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2001-2005, Beneficent
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;

import org.martus.client.core.LanguageChangeListener;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.swing.UiComboBox;
import org.martus.swing.UiLanguageDirection;

public class UiChoiceEditor extends UiChoice implements ActionListener
{
	public UiChoiceEditor(FieldSpec dropDownSpec)
	{
		super(dropDownSpec);
	}
	
	protected void initialize()
	{
		widget = new UiComboBox();
		updateChoices();
		widget.addActionListener(this);
		widget.setRenderer(new UiChoiceListCellRenderer());
	}
	
	class UiChoiceListCellRenderer extends DefaultListCellRenderer
	{
		
		public Component getListCellRendererComponent(JList list, Object code, int index, boolean isSelected, boolean cellHasFocus)
		{
			Component cellRenderer = super.getListCellRendererComponent(list, spec.getDisplayString((String)code), index, isSelected,
					cellHasFocus);
			cellRenderer.setComponentOrientation(UiLanguageDirection.getComponentOrientation());
			return cellRenderer;
		}
		
	}

	public String getText()
	{
		return (String)widget.getSelectedItem();
	}

	public void setText(String newCode)
	{
		int rowToSelect = spec.findCode(newCode);
		widget.setSelectedIndex(rowToSelect);
	}

	public void updateChoices()
	{
		widget.removeAllItems();
		for(int i = 0; i < spec.getCount(); ++i)
			widget.addItem(spec.getChoice(i).getCode());

		widget.updateUI();
	}

	public void actionPerformed(ActionEvent e) 
	{
		if(observer != null)
			observer.languageChanged(getText());
	}

	public JComponent getComponent()
	{
		return widget;
	}

	public JComponent[] getFocusableComponents()
	{
		return new JComponent[]{widget};
	}

	public void setLanguageListener(LanguageChangeListener listener)
	{
		observer = listener;
	}
		
	UiComboBox widget;
	LanguageChangeListener observer;
}

