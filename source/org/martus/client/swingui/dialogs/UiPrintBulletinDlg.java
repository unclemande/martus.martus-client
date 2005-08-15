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
package org.martus.client.swingui.dialogs;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import org.martus.client.swingui.UiMainWindow;
import org.martus.clientside.UiLocalization;
import org.martus.common.bulletin.Bulletin;
import org.martus.swing.UiButton;
import org.martus.swing.UiRadioButton;
import org.martus.swing.UiVBox;
import org.martus.swing.UiWrappedTextArea;
import org.martus.swing.Utilities;


public class UiPrintBulletinDlg extends JDialog implements ActionListener
{
	public UiPrintBulletinDlg(UiMainWindow mainWindowToUse, Vector bulletinsToPrint)
	{
		super(mainWindowToUse, "", true);
		mainWindow = mainWindowToUse;
		allPrivateData = isAnyBulletinAllPrivate(bulletinsToPrint);	
		init();	
	}
	
	private void init()
	{
		UiLocalization localization = mainWindow.getLocalization();
		setTitle(localization.getWindowTitle("PrintOptions"));
		
		//includePrivate = new UiCheckBox(localization.getFieldLabel("PrintPrivateData"));
		ok = new UiButton(localization.getButtonLabel("Continue"));
		ok.addActionListener(this);		
		cancel = new UiButton(localization.getButtonLabel("cancel"));
		cancel.addActionListener(this);	
		
		publicOnly = new UiRadioButton(localization.getButtonLabel("PrintOnlyPublic"));
		publicAndPrivate = new UiRadioButton(localization.getButtonLabel("PrintPublicAndPrivate"));
		ButtonGroup privacyGroup = new ButtonGroup();
		privacyGroup.add(publicOnly);
		privacyGroup.add(publicAndPrivate);
		publicOnly.setSelected(true);
		
		toPrinter = new UiRadioButton(localization.getButtonLabel("PrintToPrinter"));
		toDisk = new UiRadioButton(localization.getButtonLabel("PrintToDisk"));
		ButtonGroup destinationGroup = new ButtonGroup();
		destinationGroup.add(toPrinter);
		destinationGroup.add(toDisk);
		toPrinter.setSelected(true);

		Box privacyPanel = new UiVBox();
		privacyPanel.setBorder(new LineBorder(Color.BLACK));
		privacyPanel.add(new UiWrappedTextArea(localization.getFieldLabel("PrintPrivateDataMessage")));
		privacyPanel.add(publicOnly);
		privacyPanel.add(publicAndPrivate);
		
		Box destinationPanel = new UiVBox();
		destinationPanel.setBorder(new LineBorder(Color.BLACK));
		destinationPanel.add(new UiWrappedTextArea(localization.getFieldLabel("PrintToPrinterOrDisk")));
		destinationPanel.add(toPrinter);
		destinationPanel.add(toDisk);
		
		UiVBox mainBox = new UiVBox();
		mainBox.setBorder(new EmptyBorder(10, 10, 10, 10));
		mainBox.addCentered(privacyPanel);
		mainBox.addSpace();
		mainBox.addCentered(destinationPanel);
		mainBox.addSpace();
		mainBox.add(new Component[] {ok, cancel});
	
		getContentPane().add(mainBox);
		getRootPane().setDefaultButton(ok);
		Utilities.centerDlg(this);
		setResizable(true);
	}
	
	public boolean wantsPrivateData()
	{
		return publicAndPrivate.isSelected();
	}
	
	public boolean wantsToPrintToDisk()
	{
		return toDisk.isSelected();
	}
	
	public boolean wasContinueButtonPressed()
	{
		return pressContinue;
	}
	
	public void actionPerformed(ActionEvent ae)
	{
		if(ae.getSource().equals(ok))
		{
			if (!wantsPrivateData() && allPrivateData)
			{		
				if(mainWindow.confirmDlg("PrintAllPrivateData"))
					return;
			}	
			pressContinue = true;
		}
		dispose();
	}
	
	private boolean isAnyBulletinAllPrivate(Vector currentSelectedBulletins)
	{
		boolean isAnyAllPrivate = false;
		Iterator iter = currentSelectedBulletins.iterator();
		while(iter.hasNext())
		{
			Bulletin bulletin = (Bulletin) iter.next();
			if(bulletin.isAllPrivate())
			{
				isAnyAllPrivate = true;
				break;
			}
		}
		return isAnyAllPrivate;
	}
	
	UiMainWindow mainWindow;	
	UiRadioButton publicOnly;
	UiRadioButton publicAndPrivate;
	UiRadioButton toPrinter;
	UiRadioButton toDisk;
	JButton ok;
	JButton cancel;
	boolean pressContinue=false;
	private boolean allPrivateData;
	
}
