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

package org.martus.client.network;

import org.martus.client.core.MartusApp;
import org.martus.common.ProgressMeterInterface;

public class BackgroundRetriever
{
	public BackgroundRetriever(MartusApp appToUse, ProgressMeterInterface progressMeterToUse)
	{
		app = appToUse;
		progressMeter = progressMeterToUse;
	}
	
	public boolean hasWorkToDo()
	{
		return (app.getCurrentRetrieveCommand().getRemainingToRetrieveCount() > 0);
	}
	
	public String getRetrieveFolderName()
	{
		return app.getCurrentRetrieveCommand().getFolderName();
	}
	
	public void retrieveNext() throws Exception
	{
		try
		{
			app.retrieveNextBackgroundBulletin();
		}
		finally
		{
			RetrieveCommand rc = app.getCurrentRetrieveCommand();
			progressMeter.updateProgressMeter(rc.getRetrievedCount(), rc.getTotalCount());
		}
	}
	
	MartusApp app;
	ProgressMeterInterface progressMeter;
}
