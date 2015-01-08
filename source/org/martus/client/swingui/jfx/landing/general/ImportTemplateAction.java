/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2014, Beneficent
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
package org.martus.client.swingui.jfx.landing.general;

import java.io.File;
import java.util.Vector;

import javafx.application.Platform;

import org.martus.client.core.MartusApp;
import org.martus.client.swingui.MartusLocalization;
import org.martus.client.swingui.UiMainWindow;
import org.martus.client.swingui.actions.ActionDoer;
import org.martus.client.swingui.filefilters.MCTFileFilter;
import org.martus.client.swingui.filefilters.XmlFileFilter;
import org.martus.clientside.FormatFilter;
import org.martus.common.MartusLogger;

public class ImportTemplateAction implements ActionDoer
{
	public ImportTemplateAction(ManageTemplatesController manageTemplatesControllerToUse)
	{
		manageTemplatesController = manageTemplatesControllerToUse;
	}
	
	@Override
	public void doAction()
	{
		Vector<FormatFilter> filters = new Vector();
		filters.add(new MCTFileFilter(getLocalization()));
		filters.add(new XmlFileFilter(getLocalization()));
		
		File selectedFile = getMainWindow().showFileOpenDialog("ImportTemplate", filters);
		if(selectedFile == null)
			return;
		
		Platform.runLater(new ImportFormTemplateRunner(selectedFile));
	}
	
	protected MartusLocalization getLocalization()
	{
		return getMainWindow().getLocalization();
	}

	private MartusApp getApp()
	{
		return getMainWindow().getApp();
	}
	
	protected UiMainWindow getMainWindow()
	{
		return manageTemplatesController.getMainWindow();
	}

	protected ManageTemplatesController getManageTemplatesController()
	{
		return manageTemplatesController;
	}
	
	protected class ImportFormTemplateRunner implements Runnable
	{
		public ImportFormTemplateRunner(File templateFileToImportToUse)
		{
			templateFileToImport = templateFileToImportToUse;
		}
		
		@Override
		public void run()
		{
			try
			{
				String lowerCaseFileName = templateFileToImport.getName().toLowerCase();
				if(lowerCaseFileName.endsWith(new MCTFileFilter(getLocalization()).getExtension().toLowerCase()))
					getManageTemplatesController().importFormTemplateFromMctFile(templateFileToImport);
				else
					getManageTemplatesController().importXmlFormTemplate(templateFileToImport);
					
			}
			catch (Exception e)
			{
				MartusLogger.logException(e);
				UiMainWindow.showNotifyDlgOnSwingThread(getMainWindow(), "PublicInfoFileError");
			}
		}

		private File templateFileToImport;
	}

	private ManageTemplatesController manageTemplatesController;
}
