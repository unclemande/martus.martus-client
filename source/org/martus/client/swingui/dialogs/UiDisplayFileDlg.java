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

package org.martus.client.swingui.dialogs;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

import org.martus.client.swingui.UiMainWindow;
import org.martus.common.clientside.UiBasicLocalization;
import org.martus.swing.UiWrappedTextArea;
import org.martus.swing.Utilities;
import org.martus.util.UnicodeReader;



public class UiDisplayFileDlg extends JDialog
{
	public UiDisplayFileDlg(UiMainWindow owner, String baseTag, InputStream fileStream, String tagMessage, InputStream fileStreamToc, String tagTOCMessage)
	{
		super(owner, "", true);
		mainWindow = owner;
		previouslyFoundIndex = -1;
		tocList = null;
		UiBasicLocalization localization = owner.getLocalization();

		setTitle(localization.getWindowTitle(baseTag));
		JPanel helpPanel = new JPanel();
		helpPanel.setBorder(new EmptyBorder(10,10,10,10));

		helpPanel.setLayout(new BoxLayout(helpPanel, BoxLayout.Y_AXIS));

		message = getFileContents(fileStream);
		if(message == null)
		{
			dispose();
			return;
		}
		lowercaseMessage = message.toLowerCase();

		msgArea = new UiWrappedTextArea(message);
		highliter = new BasicTextUI.BasicHighlighter();
		msgArea.setHighlighter(highliter);
		msgArea.addKeyListener(new TabToOkButton());
		msgArea.setRows(14);
		msgArea.setColumns(80);
		msgAreaScrollPane = new JScrollPane(msgArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		Vector messageTOC = getFileVectorContents(fileStreamToc);
		if(messageTOC != null)
		{
			tocList = new JList(messageTOC);
			tocList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tocList.addListSelectionListener(new ListHandler());
			JScrollPane tocMsgAreaScrollPane = new JScrollPane(tocList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			tocMsgAreaScrollPane.setPreferredSize(new Dimension(580, 100));
			helpPanel.add(tocMsgAreaScrollPane);
			tocList.setSelectedIndex(0);
		}

		close = new JButton(localization.getButtonLabel("close"));
		close.addActionListener(new CloseHandler());
		close.addKeyListener(new MakeEnterKeyExit());
		helpPanel.add(msgAreaScrollPane);
		
		searchField = new JTextField(20);
		searchField.setMaximumSize(searchField.getPreferredSize());
		searchField.addActionListener(new searchFieldListener());
		searchButton = new JButton(localization.getButtonLabel("inputsearchok"));
		searchButton.addActionListener(new SearchActionListener());
		Box hBox = Box.createHorizontalBox();

		Dimension msgAreaSize = msgAreaScrollPane.getPreferredSize();
		msgAreaSize.setSize(msgAreaSize.getWidth(), close.getPreferredSize().getHeight());
		hBox.setPreferredSize(msgAreaSize);
		
		hBox.add(searchField);
		hBox.add(searchButton);
		hBox.add(Box.createHorizontalGlue());
		hBox.add(close);
		helpPanel.add(hBox);
		
		getContentPane().add(helpPanel);
		getRootPane().setDefaultButton(close);
		close.requestFocus();

		Utilities.centerDlg(this);
		setResizable(true);
		show();
	}

	public String getFileContents(InputStream fileStream)
	{
		StringBuffer message = new StringBuffer();
		if(fileStream == null)
		{
			System.out.println("UiDisplayFileDlg: null stream");
			return null;
		}
		try
		{
			BufferedReader reader = new BufferedReader(new UnicodeReader(fileStream));
			while(true)
			{
				String lineIn = reader.readLine();
				if(lineIn == null)
					break;
				message.append(lineIn);
				message.append('\n');
			}
			reader.close();
		}
		catch(IOException e)
		{
			System.out.println("UiDisplayFileDlg: " + e);
			return null;
		}
		return new String(message);
	}

	public Vector getFileVectorContents(InputStream fileStream)
	{
		Vector message = new Vector();
		if(fileStream == null)
		{
			System.out.println("UiDisplayFileDlg: null stream");
			return null;
		}
		try
		{
			BufferedReader reader = new BufferedReader(new UnicodeReader(fileStream));
			while(true)
			{
				String lineIn = reader.readLine();
				if(lineIn == null)
					break;
				message.add(lineIn);
			}
			reader.close();
		}
		catch(IOException e)
		{
			System.out.println("UiDisplayFileDlg: " + e);
			return null;
		}
		return message;
	}



	class ListHandler implements ListSelectionListener
	{
		public void valueChanged(ListSelectionEvent arg0)
		{
			Object selectedValue = tocList.getSelectedValue();
			String searchString = "-\n" + (String)selectedValue;
			int foundAt = message.indexOf(searchString);
			int startHighlight = foundAt+2;
			int endHighlight = foundAt + searchString.length();
			if(foundAt < 0)
			{
				startHighlight=0;
				foundAt = 0;
			}
			scrollToPosition(foundAt);
			
			highlightText(startHighlight, endHighlight);
		}
		
	}
	
	class SearchActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			searchMessage();
		}

		private void searchMessage()
		{
			String searchString = searchField.getText();
			String lowerCaseSearchText = searchString.toLowerCase();
			if(previouslyFoundIndex != msgArea.getCaretPosition())
				previouslyFoundIndex = msgArea.getCaretPosition();
			int startIndex = lowercaseMessage.indexOf(lowerCaseSearchText,previouslyFoundIndex+1);
			if(startIndex < 0)
			{
				HashMap tokenReplacement = new HashMap();
				tokenReplacement.put("#S#", searchString);
				if(mainWindow.confirmDlg(null, "helpStringNotFound", tokenReplacement))
				{
					msgArea.setCaretPosition(0);
					previouslyFoundIndex = 0;
					searchMessage();
				}
				return;
			}
			
			tocList.clearSelection();
			previouslyFoundIndex = startIndex;
			scrollToPosition(startIndex);
			int endIndex = startIndex+lowerCaseSearchText.length();
			highlightText(startIndex, endIndex);
		}

	}

	void highlightText(int startIndex, int endIndex)
	{
		try
		{
			highliter.removeAllHighlights();
			Color highlightColor = new Color(255,255,0);
			DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(highlightColor);
			highliter.addHighlight(startIndex, endIndex, painter);
		}
		catch (BadLocationException e1)
		{
		}
	}

	public void scrollToPosition(int position)
	{
		msgArea.setCaretPosition(message.length());
		msgAreaScrollPane.getVerticalScrollBar().setValue(msgAreaScrollPane.getVerticalScrollBar().getMaximum());
		msgArea.setCaretPosition(position);
	}
	
	class searchFieldListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			searchButton.doClick();
		}
	}

	class CloseHandler implements ActionListener
	{
		public void actionPerformed(ActionEvent ae)
		{
			dispose();
		}
	}
	class MakeEnterKeyExit extends KeyAdapter
	{
		public void keyPressed(KeyEvent ke)
		{
			if (ke.getKeyCode() == KeyEvent.VK_ENTER)
				dispose();
		}
	}

	class TabToOkButton extends KeyAdapter
	{
		public void keyPressed(KeyEvent ke)
		{
			if (ke.getKeyCode() == KeyEvent.VK_TAB)
			{
				close.requestFocus();
			}
		}
	}
	String message;
	String lowercaseMessage;
	JButton searchButton;
	JTextField searchField;
	JButton close;
	JList tocList;
	UiWrappedTextArea msgArea;
	JScrollPane msgAreaScrollPane;
	BasicTextUI.BasicHighlighter highliter;
	int previouslyFoundIndex;
	UiMainWindow mainWindow;
}
