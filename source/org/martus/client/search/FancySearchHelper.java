/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2005-2006, Beneficent
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

package org.martus.client.search;

//FIXME: Stuff to fix:
//3. look into tab focus
//4. space bar should pop up the dialog
//5. set tree dialog position appropriately
//7. set good leaf icon

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.client.swingui.dialogs.UiDialogLauncher;
import org.martus.clientside.UiLocalization;
import org.martus.common.GridData;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.field.MartusDateRangeField;
import org.martus.common.field.MartusGridField;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.DropDownFieldSpec;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.fieldspec.FieldType;
import org.martus.common.fieldspec.FieldTypeAnyField;
import org.martus.common.fieldspec.FieldTypeDate;
import org.martus.common.fieldspec.FieldTypeNormal;
import org.martus.common.fieldspec.GridFieldSpec;
import org.martus.common.fieldspec.PopUpTreeFieldSpec;
import org.martus.common.fieldspec.SearchFieldTreeModel;
import org.martus.common.fieldspec.SearchableFieldChoiceItem;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.fieldspec.GridFieldSpec.UnsupportedFieldTypeException;
import org.martus.util.TokenReplacement;
import org.martus.util.TokenReplacement.TokenInvalidException;

public class FancySearchHelper
{
	public FancySearchHelper(ClientBulletinStore storeToUse, UiDialogLauncher dlgLauncherToUse)
	{
		dlgLauncher = dlgLauncherToUse;
		model = new FancySearchTableModel(getGridSpec(storeToUse), dlgLauncherToUse.GetLocalization());
	}
	
	UiLocalization getLocalization()
	{
		return dlgLauncher.GetLocalization();
	}
	
	UiDialogLauncher getDialogLauncher()
	{
		return dlgLauncher;
	}
	
	FancySearchTableModel getModel()
	{
		return model;
	}
	
	public PopUpTreeFieldSpec createFieldColumnSpec(ClientBulletinStore storeToUse)
	{
		FieldChoicesByLabel allAvailableFields = new FieldChoicesByLabel();
		allAvailableFields.add(createAnyFieldChoice());
		allAvailableFields.add(createLastSavedDateChoice());
		allAvailableFields.addAll(convertToChoiceItems(storeToUse.getAllKnownFieldSpecs()));
		
		SearchFieldTreeModel fieldChoiceModel = new SearchFieldTreeModel(allAvailableFields.asTree(getLocalization()));
		PopUpTreeFieldSpec fieldColumnSpec = new PopUpTreeFieldSpec(fieldChoiceModel);
		fieldColumnSpec.setLabel(getLocalization().getFieldLabel("SearchGridHeaderField"));
		return fieldColumnSpec;
	}

	private ChoiceItem createAnyFieldChoice()
	{
		String tag = "";
		String label = getLocalization().getFieldLabel("SearchAnyField");
		FieldType type = new FieldTypeAnyField();
		FieldSpec spec = FieldSpec.createCustomField(tag, label, type);
		return new SearchableFieldChoiceItem("", spec);
	}

	private ChoiceItem createLastSavedDateChoice()
	{
		String tag = Bulletin.PSEUDOFIELD_LAST_SAVED_DATE;
		String label = getLocalization().getFieldLabel(Bulletin.TAGLASTSAVED);
		FieldType type = new FieldTypeDate();
		FieldSpec spec = FieldSpec.createCustomField(tag, label, type);
		return new SearchableFieldChoiceItem(spec);
	}

	private Set convertToChoiceItems(Set specs)
	{
		Set allChoices = new HashSet();
		Iterator iter = specs.iterator();
		while(iter.hasNext())
		{
			FieldSpec spec = (FieldSpec)iter.next();
			allChoices.addAll(getChoiceItemsForThisField(spec));
		}
			
		return allChoices;
	}

	public Set getChoiceItemsForThisField(FieldSpec spec)
	{
		return getChoiceItemsForThisField(spec, spec.getTag(), "");
	}
	
	public Set getChoiceItemsForThisField(FieldSpec spec, String fullTagChain, String displayPrefix)
	{

		Set choicesForThisField = new HashSet();
		final FieldType thisType = spec.getType();
		String displayString = spec.getLabel();
		if(StandardFieldSpecs.isStandardFieldTag(fullTagChain))
			displayString = getLocalization().getFieldLabel(fullTagChain);
		else if(displayString.trim().equals(""))
			displayString = fullTagChain;

		displayString = displayPrefix + displayString;

		// unknown types (Lewis had one) should not appear in the list at all
		if(thisType.isUnknown())
			return choicesForThisField;

		// dateranges create multiple entries
		if(thisType.isDateRange())
		{
			choicesForThisField.addAll(getDateRangeChoiceItem(fullTagChain, MartusDateRangeField.SUBFIELD_BEGIN, displayString));
			choicesForThisField.addAll(getDateRangeChoiceItem(fullTagChain, MartusDateRangeField.SUBFIELD_END, displayString));
			return choicesForThisField;
		}
		
		// dropdowns MUST be a DropDownFieldSpec, not a plain FieldSpec
		if(thisType.isDropdown())
		{
			DropDownFieldSpec originalSpec = (DropDownFieldSpec)spec;
			DropDownFieldSpec specWithBetterLabel = new DropDownFieldSpec(originalSpec.getAllChoices());
			specWithBetterLabel.setTag(fullTagChain);
			specWithBetterLabel.setLabel(displayString);
			choicesForThisField.add(new SearchableFieldChoiceItem(specWithBetterLabel));
			return choicesForThisField;
		}

		// add one choice per column
		if(thisType.isGrid())
		{
			GridFieldSpec gridSpec = (GridFieldSpec)spec;
			for(int i=0; i < gridSpec.getColumnCount(); ++i)
			{
				final FieldSpec columnSpec = gridSpec.getFieldSpec(i);
				String columnTag = fullTagChain + "." + MartusGridField.sanitizeLabel(columnSpec.getLabel());
				choicesForThisField.addAll(getChoiceItemsForThisField(columnSpec, columnTag, displayString + ": "));
			}
			return choicesForThisField;
		}

		// many types just create a choice with their own type,
		// but we need to default to NORMAL for safety
		FieldType choiceSpecType = new FieldTypeNormal();
		if(shouldSearchSpecTypeBeTheFieldSpecType(thisType))
			choiceSpecType = thisType;

		FieldSpec thisSpec = FieldSpec.createCustomField(fullTagChain, displayString, choiceSpecType);
		ChoiceItem choiceItem = new SearchableFieldChoiceItem(thisSpec);
		choicesForThisField.add(choiceItem);
		return choicesForThisField;
	}

	private boolean shouldSearchSpecTypeBeTheFieldSpecType(final FieldType thisType)
	{
		return (thisType.isDate() || thisType.isLanguage() || thisType.isBoolean()); 
	}
	
	private Set getDateRangeChoiceItem(String tag, String subfield, String baseDisplayString) 
	{
		Set itemIfAny = new HashSet();
		String fullTag = tag + "." + subfield;
		String displayTemplate = dlgLauncher.GetLocalization().getFieldLabel("DateRangeTemplate" + subfield);
		try
		{
			String fullDisplayString = TokenReplacement.replaceToken(displayTemplate, "#FieldLabel#", baseDisplayString);
			FieldSpec dateSpec = FieldSpec.createCustomField(fullTag, fullDisplayString, new FieldTypeDate());
			itemIfAny.add(new SearchableFieldChoiceItem(dateSpec));
		}
		catch (TokenInvalidException e)
		{
			// bad translation--not much we can do about it
			e.printStackTrace();
		}
		
		return itemIfAny;
	}
	
	public GridFieldSpec getGridSpec(ClientBulletinStore storeToUse)
	{
		GridFieldSpec spec = new GridFieldSpec();

		try
		{
			spec.addColumn(createFieldColumnSpec(storeToUse));
			
			spec.addColumn(FancySearchTableModel.getCurrentOpColumnSpec(new FieldTypeAnyField(), getLocalization()));
			
			String valueColumnTag = "value";
			String valueColumnHeader = getLocalization().getFieldLabel("SearchGridHeaderValue");
			spec.addColumn(FieldSpec.createCustomField(valueColumnTag, valueColumnHeader, new FieldTypeNormal()));
			spec.addColumn(createAndOrColumnSpec());
		}
		catch (UnsupportedFieldTypeException e)
		{
			// TODO: better error handling?
			e.printStackTrace();
			throw new RuntimeException();
		}
		return spec;
	}
	
	public DropDownFieldSpec createAndOrColumnSpec()
	{
		ChoiceItem[] choices =
		{
			createLocalizedChoiceItem(SearchParser.ENGLISH_AND_KEYWORD),
			createLocalizedChoiceItem(SearchParser.ENGLISH_OR_KEYWORD),
		};
		return new DropDownFieldSpec(choices);
	}
	
	private ChoiceItem createLocalizedChoiceItem(String tag)
	{
		return new ChoiceItem(tag, getLocalization().getKeyword(tag));
	}

	public SearchTreeNode getSearchTree(GridData gridData)
	{
		final int firstRow = 0;
		SearchTreeNode thisNode = createAmazonStyleNode(gridData, firstRow);
		return getSearchTree(thisNode, gridData, firstRow);
	}
	
	// loop through all rows with recursion, building a search tree that 
	// is grouped to the left, like:     (a and b) or c
	public SearchTreeNode getSearchTree(SearchTreeNode existingLeftNode, GridData gridData, int opRow)
	{
		int rightValueRow = opRow + 1;
		
		if(rightValueRow >= gridData.getRowCount())
			return existingLeftNode;
			
		int op = getAndOr(gridData, opRow);
		
		SearchTreeNode newRightNode = createAmazonStyleNode(gridData, rightValueRow);
		SearchTreeNode newOpNode = new SearchTreeNode(op, existingLeftNode, newRightNode);
		return getSearchTree(newOpNode, gridData, opRow + 1);
	}

	private int getAndOr(GridData gridData, int opRow)
	{
		String andOr = gridData.getValueAt(opRow, 3); 
		if(andOr.equals(SearchParser.ENGLISH_AND_KEYWORD))
			return SearchTreeNode.AND;
		if(andOr.equals(SearchParser.ENGLISH_OR_KEYWORD))
			return SearchTreeNode.OR;

		throw new RuntimeException("Unknown and/or keyword: " + andOr);
	}

	// Amazon style allows the user to enter something like:    a or b
	// into the value area, and the same field is applied to each value
	private SearchTreeNode createAmazonStyleNode(GridData gridData, int row)
	{
		String fieldName = gridData.getValueAt(row, 0);
		String op = gridData.getValueAt(row, 1);
		String value = gridData.getValueAt(row, 2);
		value = value.trim();

		PopUpTreeFieldSpec fieldColumnSpec = (PopUpTreeFieldSpec)gridData.getSpec().getFieldSpec(0);
		FieldSpec specForThisValue = FancySearchTableModel.getFieldSpecForChosenField(fieldName, fieldColumnSpec);
		
		String localAnd = getLocalization().getKeyword(SearchParser.ENGLISH_AND_KEYWORD);
		String localOr = getLocalization().getKeyword(SearchParser.ENGLISH_OR_KEYWORD);
		SearchParser parser = new SearchParser(localAnd, localOr);
		return parser.parse(specForThisValue, op, value);
	}
	
	public static SearchableFieldChoiceItem findSearchTag(PopUpTreeFieldSpec specOfFieldColumn, String tagToFind)
	{
		return specOfFieldColumn.findSearchTag(tagToFind);
	}

	public static final int COLUMN_ROW_NUMBER = 0;
	public static final int COLUMN_FIELD = 1;
	public static final int COLUMN_COMPARE_HOW = 2;
	public static final int COLUMN_VALUE = 3;
	
	FancySearchTableModel model;
	UiDialogLauncher dlgLauncher;
}

