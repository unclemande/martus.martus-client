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

import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.martus.common.bulletin.Bulletin;
import org.martus.common.clientside.UiLocalization;
import org.martus.common.fieldspec.FieldSpec;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.utilities.MartusFlexidate;
import org.martus.swing.UiComboBox;
import org.martus.swing.UiLabel;
import org.martus.swing.UiParagraphPanel;
import org.martus.swing.UiRadioButton;

public class UiFlexiDateEditor extends UiField
{
	public UiFlexiDateEditor(UiLocalization localizationToUse, FieldSpec specToUse)
	{
		localization = localizationToUse;
		spec = specToUse;
		init();
	}	
	
	private void init()
	{
		component = new JPanel();
		component.setLayout(new BorderLayout());		
		UiParagraphPanel dateSelection = new UiParagraphPanel();				
		exactDateRB = new UiRadioButton(localization.getFieldLabel("DateExact"), true);			
		flexiDateRB = new UiRadioButton(localization.getFieldLabel("DateRange"));		

		ButtonGroup radioGroup = new ButtonGroup();
		radioGroup.add(exactDateRB);
		radioGroup.add(flexiDateRB);		

		dateSelection.addComponents(exactDateRB, flexiDateRB);
						
		flexiDateRB.addItemListener(new RadioItemListener());
		exactDateRB.addItemListener(new RadioItemListener());
		component.add(dateSelection, BorderLayout.NORTH);
		component.add(buildExactDatePanel(), BorderLayout.CENTER);
	}
	
	private JPanel buildFlexiDatePanel()
	{	
		UiParagraphPanel beginDate = new UiParagraphPanel();
		beginDate.addComponents(new UiLabel(localization.getFieldLabel("DateRangeFrom")), buildBeginDateBox());		
		UiParagraphPanel endDate = new UiParagraphPanel();
		endDate.addComponents(new UiLabel(localization.getFieldLabel("DateRangeTo")), buildEndDateBox());

		flexiDatePanel = new UiParagraphPanel();
		flexiDatePanel.addComponents(beginDate, endDate);
		return flexiDatePanel;
	}
	
	private JPanel buildExactDatePanel()
	{		
		UiParagraphPanel singleDatePanel = new UiParagraphPanel();
		JLabel exactDateLabel = new UiLabel(localization.getFieldLabel("DateRangeFrom"));		
		singleDatePanel.addComponents(exactDateLabel,  buildBeginDateBox());					
		exactDateLabel.setForeground(component.getBackground());		

		extDatePanel = new UiParagraphPanel();		
		extDatePanel.addOnNewLine(singleDatePanel);
		return extDatePanel;			
	}
				
	private Box buildBeginDateBox()
	{				
		if (bgDateBox  == null)
		{	
			bgDateBox = Box.createHorizontalBox();								
			bgDayCombo = new UiComboBox();	
			bgMonthCombo = new UiComboBox(localization.getMonthLabels());
			bgYearCombo = new UiComboBox();
			if(StandardFieldSpecs.isCustomFieldTag(spec.getTag()))					
				UiDateEditor.buildCustomDate(bgDateBox, localization, bgYearCombo, bgMonthCombo, bgDayCombo);
			else
				UiDateEditor.buildDate(bgDateBox, localization, bgYearCombo, bgMonthCombo, bgDayCombo);	
		}		
		
		return bgDateBox;											
	}

	private Box buildEndDateBox()
	{		
		boolean needToSetDefaultValue=false;		
		if (endDateBox == null)
		{
			endDateBox = Box.createHorizontalBox();		
			endDayCombo = new UiComboBox();	
			endMonthCombo = new UiComboBox(localization.getMonthLabels());
			endYearCombo = new UiComboBox();
			needToSetDefaultValue=true;	
			
			if(StandardFieldSpecs.isCustomFieldTag(spec.getTag()))					
				UiDateEditor.buildCustomDate(endDateBox, localization, endYearCombo, endMonthCombo, endDayCombo);
			else					
				UiDateEditor.buildDate(endDateBox, localization, endYearCombo, endMonthCombo, endDayCombo);
		}
		
		if (needToSetDefaultValue)
		{				
			endYearCombo.setSelectedItem(bgYearCombo.getSelectedItem());
			endMonthCombo.setSelectedItem(bgMonthCombo.getSelectedItem());
			endDayCombo.setSelectedItem(bgDayCombo.getSelectedItem());			
		}
			
		return endDateBox;		
	}
		
	public JComponent getComponent()
	{
		return component;
	}

	public JComponent[] getFocusableComponents()
	{
		return (flexiDateRB.isSelected())? loadFlexidatePanelComponents():loadExactDatePanelComponents();				
	}
	
	private JComponent[] loadFlexidatePanelComponents()
	{
		return new JComponent[]{exactDateRB, flexiDateRB, bgDayCombo, bgMonthCombo, bgYearCombo,
		endDayCombo, endMonthCombo, endYearCombo};
	}
	
	private JComponent[] loadExactDatePanelComponents()
	{
		return new JComponent[]{exactDateRB, flexiDateRB, bgDayCombo, bgMonthCombo, bgYearCombo,};
	}

	private final class RadioItemListener implements ItemListener
	{
		public void itemStateChanged(ItemEvent e)
		{
			if (isFlexiDate())														
				removeExactDatePanel();									
			
			if (isExactDate())				
				removeFlexidatePanel();					
		}
	}
	
	void removeExactDatePanel()
	{
		component.remove(extDatePanel);						
		component.add(buildFlexiDatePanel());																	
		component.revalidate();		
	}
	
	void removeFlexidatePanel()
	{
		component.remove(flexiDatePanel);						
		component.add(buildExactDatePanel());
		component.revalidate();		
	}

	public void validate() throws UiField.DataInvalidException 
	{
		
		if(isFlexiDate())
		{
			if(getEndDate().before(getBeginDate()))
			{
				bgDayCombo.requestFocus();
				throw new DateRangeInvertedException();
			}
		}
		
		if(StandardFieldSpecs.isCustomFieldTag(spec.getTag()))
			return;		
		
		Date today = new Date();
		if (getBeginDate().after(today))
		{
			bgDayCombo.requestFocus();	
			throw new UiDateEditor.DateFutureException();
		}
		if (isFlexiDate())
		{		
			if (getEndDate().after(today))
			{
				bgDayCombo.requestFocus();	
				throw new UiDateEditor.DateFutureException();				
			}
		}
	}
	
	boolean isFlexiDate()
	{
		return flexiDateRB.isSelected();
	}
	
	boolean isExactDate()
	{
		return exactDateRB.isSelected();
	}

	public String getText()
	{
		DateFormat df = Bulletin.getStoredDateFormat();
		String dateText = null;
		if(isExactDate())
			dateText = df.format(getBeginDate());
		else
			dateText = df.format(getBeginDate())+ MartusFlexidate.DATE_RANGE_SEPARATER+
						MartusFlexidate.toFlexidateFormat(getBeginDate(), (isFlexiDate())? getEndDate():getBeginDate());						
		return dateText;
	}	
	
	private Date getBeginDate() 
	{		
		return UiDateEditor.getDate(bgYearCombo, bgMonthCombo, bgDayCombo);
	}
	
	private Date getEndDate() 
	{				
		return UiDateEditor.getDate(endYearCombo, endMonthCombo, endDayCombo);
	}	
		
	public void setText(String newText)
	{		
		MartusFlexidate mfd = MartusFlexidate.createFromMartusDateString(newText);
		UiDateEditor.setDate(MartusFlexidate.toStoredDateFormat(mfd.getBeginDate()), bgYearCombo, bgMonthCombo, bgDayCombo);
			
		if (mfd.hasDateRange())
		{
			flexiDateRB.setSelected(true);
			UiDateEditor.setDate(MartusFlexidate.toStoredDateFormat(mfd.getEndDate()), endYearCombo, endMonthCombo, endDayCombo);
		}		
	}
	
	public static class DateRangeInvertedException extends UiField.DataInvalidException
	{
		public DateRangeInvertedException()
		{
			super(null);
		}
		
		public DateRangeInvertedException(String tag)
		{
			super(tag);
		}

	}
		
	JComponent 					component;
	
	UiComboBox 					bgMonthCombo;
	UiComboBox 					bgDayCombo;
	UiComboBox 					bgYearCombo;	
	UiComboBox 					endMonthCombo;
	UiComboBox 					endDayCombo;
	UiComboBox 					endYearCombo;
		
	private UiLocalization localization;	
	private UiRadioButton 		exactDateRB;
	private UiRadioButton 		flexiDateRB;
	private UiParagraphPanel 	flexiDatePanel;
	private UiParagraphPanel 	extDatePanel;
	private Box					bgDateBox = null;
	private Box					endDateBox = null;
	private FieldSpec			spec;
}
