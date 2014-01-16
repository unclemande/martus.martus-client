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
package org.martus.client.swingui.jfx;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.Vector;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;

import org.martus.client.swingui.UiMainWindow;
import org.martus.common.fieldspec.ChoiceItem;

public class FxSelectLanguageController extends MartusFxController implements Initializable
{
	public FxSelectLanguageController(UiMainWindow mainWindowToUse)
	{
		super(mainWindowToUse);
	}
	
	public void initialize(URL url, ResourceBundle resourceBundle)
	{
		ObservableList<String> availableLanguages = FXCollections.observableArrayList(getAvailableLanguages());
		languagesDropdown.setItems(availableLanguages);
		String currentLanguageCode = getLocalization().getCurrentLanguageCode();
		String currentLanguageName = getLocalization().getLanguageName(currentLanguageCode);
		languagesDropdown.getSelectionModel().select(currentLanguageName);
	}

	@FXML
	protected void handleNext(ActionEvent event) 
	{
		getStage().handleNavigationEvent(WizardStage.NAVIGATION_NEXT);
	}

	private ObservableList<String> getAvailableLanguages()
	{
		Vector<String> languages = new Vector<String>();
		ChoiceItem[] allUILanguagesSupported = getLocalization().getUiLanguages();
		for(int i = 0; i < allUILanguagesSupported.length; ++i)
		{
			String currentCode = allUILanguagesSupported[i].getCode();
			getLocalization().setCurrentLanguageCode(currentCode);
			String languageName = getLocalization().getLanguageName(currentCode);

			languages.add(languageName);
		}

		return FXCollections.observableArrayList(languages);
	}

	@FXML // fx:id="languagesDropdown"
	private ChoiceBox<String> languagesDropdown; // Value injected by FXMLLoader
}
