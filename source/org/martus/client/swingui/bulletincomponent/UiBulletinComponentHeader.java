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

package org.martus.client.swingui.bulletincomponent;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.martus.client.swingui.UiLocalization;
import org.martus.client.swingui.UiMainWindow;
import org.martus.common.crypto.MartusCrypto;
import org.martus.swing.ParagraphLayout;
import org.martus.util.TokenReplacement;


public class UiBulletinComponentHeader extends UiBulletinComponentSection
{
	UiBulletinComponentHeader(UiMainWindow mainWindowToUse, String tagQualifierToUse)
	{
		super(mainWindowToUse);
		UiMainWindow mainWindow = getMainWindow();
		updateSectionBorder(false);
		
		String iconFileName = "BulletinViewHeading.png";
		UiLocalization localization = mainWindow.getLocalization();
		setSectionIconAndTitle(iconFileName, localization.getFieldLabel("BulletinViewHeading"));

		summary = new HqSummary(mainWindow, tagQualifierToUse);
		add(new JLabel(""), ParagraphLayout.NEW_PARAGRAPH);
		add(summary);
	}

	public void setHqKeys(Vector hqKeys)
	{
		summary.setHqKeys(hqKeys);
	}

	static class HqSummary extends JPanel
	{
		HqSummary(UiMainWindow mainWindowToUse, String tagQualifierToUse)
		{
			mainWindow = mainWindowToUse;
			tagQualifier = tagQualifierToUse;
			
			label = new JLabel();
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			
			String buttonText = getLocalization().getButtonLabel("HQDetails");
			JButton detailsButton = new JButton(buttonText);
			detailsButton.addActionListener(new DetailsListener());
			add(label, BorderLayout.WEST);
			add(detailsButton, BorderLayout.EAST);
		}
		
		void setHqKeys(Vector hqKeys)
		{
			hqList = hqKeys;
			int numberOfHqs = hqList.size();
			if(numberOfHqs > 0)
			{
				label.setText(getSummaryString(numberOfHqs));
				setVisible(true);
			}
			else
			{
				label.setText("");
				setVisible(false);
			}
		}
		
		private UiLocalization getLocalization()
		{
			return mainWindow.getLocalization();
		}
		
		private String getSummaryString(int numberOfHqs)
		{
			String summaryText = getLocalization().getFieldLabel(tagQualifier + "BulletinHQInfo");
			try
			{
				HashMap tokenReplacement = new HashMap();
				tokenReplacement.put("#N#", Integer.toString(numberOfHqs));
				summaryText = TokenReplacement.replaceTokens(summaryText, tokenReplacement);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return summaryText;
		}
		
		class DetailsListener implements ActionListener
		{
			public void actionPerformed(ActionEvent e)
			{
				String listOfHqPublicKeys = "";
				for(int i=0; i < hqList.size(); ++i)
				{
					String thisHqCode = getHqPublicCode(i);
					listOfHqPublicKeys += thisHqCode + "\n";
				}
				HashMap map = new HashMap();
				map.put("#L#", listOfHqPublicKeys);
				
				JFrame parent = mainWindow.getCurrentActiveFrame();
				mainWindow.notifyDlg(parent, tagQualifier + "ViewHqList", map);
			}

			private String getHqPublicCode(int i)
			{
				try
				{
					String thisHqKey = (String)hqList.get(i);
					return MartusCrypto.getFormattedPublicCode(thisHqKey);
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				return "???";
			}
		}
		
		UiMainWindow mainWindow;
		String tagQualifier;
		JLabel label;
		Vector hqList;
	}

	HqSummary summary;
}
