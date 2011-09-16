/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2006-2007, Beneficent
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
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.martus.common.MiniLocalization;
import org.martus.common.fieldspec.MiniFieldSpec;
import org.martus.common.fieldspec.PopUpTreeFieldSpec;
import org.martus.common.fieldspec.SearchFieldTreeModel;
import org.martus.common.fieldspec.SearchableFieldChoiceItem;
import org.martus.swing.UiButton;
import org.martus.swing.UiLabel;


public class UiPopUpFieldChooserEditor extends UiField implements ActionListener
{
	public UiPopUpFieldChooserEditor(MiniLocalization localizationToUse)
	{
		super(localizationToUse);
		localization = localizationToUse;
		
		listeners = new Vector();
		
		panel = new JPanel(new BorderLayout());
		label = new UiLabel();
		button = new UiButton(localization.getButtonLabel("PopUpTreeChoose"));
		button.addActionListener(this);
		
		panel.add(label, BorderLayout.CENTER);
		panel.add(button, BorderLayout.AFTER_LINE_ENDS);
	}

	public JComponent getComponent()
	{
		return panel;
	}
	
	public void simulateButtonPress()
	{
		doPopUp();
	}

	public JComponent[] getFocusableComponents()
	{
		return new JComponent[] {button};
	}

	public String getText()
	{
		if(selectedItem == null)
			return "";
		
		String text = selectedItem.getCode();
		return text;
	}

	public void setText(String newText)
	{
		selectedItem = spec.findSearchTag(newText);
		if(selectedItem == null)
			System.out.println("UiPopUpTreeEditor couldn't setText: " + newText);
		else
			label.setText(selectedItem.toString());
	}
	
	public void select(String codeToSelect)
	{
		setText("");
		SearchableFieldChoiceItem item = spec.findSearchTag(codeToSelect);
		if(item == null)
			return;
		
		selectedItem = item;
		label.setText(selectedItem.toString());
	}
	
	public MiniFieldSpec getSelectedMiniFieldSpec()
	{
		return new MiniFieldSpec(selectedItem.getSpec());
	}
	
	public void setSpec(PopUpTreeFieldSpec specToUse)
	{
		spec = specToUse;
	}
	
	public void actionPerformed(ActionEvent event)
	{
		doPopUp();
	}

	private void doPopUp()
	{
		FieldTreeDialog dlg = createFieldChooserDialog();
		dlg.selectCode(getText());
		dlg.setVisible(true);
		DefaultMutableTreeNode selectedNode = dlg.getSelectedNode();
		if(selectedNode == null)
			return;
		
		selectedItem = (SearchableFieldChoiceItem)selectedNode.getUserObject();
		label.setText(selectedNode.toString());
		notifyListeners();
	}

	private FieldTreeDialog createFieldChooserDialog()
	{
		Container topLevel = panel.getTopLevelAncestor();
		return new FieldTreeDialog((JDialog)topLevel, panel.getLocationOnScreen(), spec, localization);
	}
	
	void notifyListeners()
	{
		ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "none");
		for(int i = 0; i < listeners.size(); ++i)
		{
			ActionListener listener = (ActionListener)listeners.get(i);
			listener.actionPerformed(event);
		}
	}
	
	public void addActionListener(ActionListener listenerToAdd)
	{
		listeners.add(listenerToAdd);
	}
	
	static class BlankLeafRenderer extends DefaultTreeCellRenderer
	{
		public BlankLeafRenderer()
		{
			
		}

		public Icon getLeafIcon()
		{
			return new BlankIcon();
		}
		
		static class BlankIcon implements Icon
		{
			public int getIconHeight()
			{
				return 0;
			}

			public int getIconWidth()
			{
				return 0;
			}

			public void paintIcon(Component c, Graphics g, int x, int y)
			{
			}
			
		}
		
	}
	
	static class SearchFieldTree extends JTree
	{
		public SearchFieldTree(TreeModel model)
		{
			super(model);
		}
		
		public void selectNodeContainingItem(SearchableFieldChoiceItem selectedItem)
		{
			SearchFieldTreeModel model = (SearchFieldTreeModel)getModel();
			TreePath rootPath = new TreePath(model.getRoot());
			TreePath foundPath = model.findObject(rootPath, selectedItem.getCode());
			if(foundPath == null)
				throw new RuntimeException("Unable to find in tree: " + selectedItem);
			
			clearSelection();
			addSelectionPath(foundPath);
			scrollPathToVisible(foundPath);
		}
	}

	MiniLocalization localization;
	PopUpTreeFieldSpec spec;
	JPanel panel;
	UiLabel label;
	UiButton button;
	SearchableFieldChoiceItem selectedItem;
	Vector listeners;
}
