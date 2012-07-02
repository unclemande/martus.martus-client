/*

The Martus(tm) free, social justice documentation and
monitoring software. Copyright (C) 2012, Beneficent
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
package org.martus.client.swingui.actions;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.TickUnitSource;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.title.DateTitle;
import org.jfree.chart.title.ShortTextTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.martus.client.core.PartialBulletin;
import org.martus.client.core.SortableBulletinList;
import org.martus.client.reports.ChartAnswers;
import org.martus.client.search.SaneCollator;
import org.martus.client.search.SearchTreeNode;
import org.martus.client.swingui.UiMainWindow;
import org.martus.client.swingui.dialogs.UiChartPreviewDlg;
import org.martus.common.MartusLogger;
import org.martus.common.fieldspec.MiniFieldSpec;
import org.martus.swing.PrintUtilities;
import org.martus.swing.UiFileChooser;
import org.martus.swing.Utilities;
import org.martus.util.TokenReplacement;
import org.martus.util.TokenReplacement.TokenInvalidException;

public class ActionMenuCharts extends UiMenuAction
{
	public ActionMenuCharts(UiMainWindow mainWindowToUse)
	{
		super(mainWindowToUse, "Charts");
	}

	@Override
	public void actionPerformed(ActionEvent events)
	{
		try
		{
			// Re-enable the following when we allow saving chart templates
//			MartusLocalization localization = mainWindow.getLocalization();
//			
////			String runButtonLabel = localization.getButtonLabel("RunChart");
//			String createChartButtonLabel = localization.getButtonLabel("CreateChart");
//			String cancelButtonLabel = localization.getButtonLabel("cancel");
//			String[] buttonLabels = {/*runButtonLabel,*/ createChartButtonLabel, cancelButtonLabel, };
//			String title = mainWindow.getLocalization().getWindowTitle("RunOrCreateChart");
//			UiPushbuttonsDlg runOrCreate = new UiPushbuttonsDlg(mainWindow, title, buttonLabels);
//			runOrCreate.setVisible(true);
//			String pressed = runOrCreate.getPressedButtonLabel();
//			if(pressed == null || pressed.equals(cancelButtonLabel))
//				return;
//			
			ChartAnswers answers = null;
////			if(pressed.equals(runButtonLabel))
////			{
////				answers = chooseAndLoad();
////			}
////			if(pressed.equals(createChartButtonLabel))
////			{
				answers = createAndSave();
////			}

			if(answers == null)
				return;
			
			runChart(answers);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			mainWindow.notifyDlgBeep("UnexpectedError");
		}
	}
	
	private ChartAnswers createAndSave()
	{
		CreateChartDialog dialog = new CreateChartDialog(getMainWindow());
		Utilities.centerDlg(dialog);
		dialog.setVisible(true);
		if(!dialog.getResult())
			return null;
		
		return dialog.getAnswers();
	}

	private void runChart(ChartAnswers answers)
	{
		try
		{
			SearchTreeNode searchTree = getMainWindow().askUserForSearchCriteria();
			if(searchTree == null)
				return;
			
			MiniFieldSpec fieldToCount = answers.getFieldToCount();
			MiniFieldSpec[] extraSpecs = new MiniFieldSpec[] { fieldToCount };
			SortableBulletinList sortableList = getMainWindow().doSearch(searchTree, extraSpecs, new MiniFieldSpec[]{}, "ReportSearchProgress");
			if(sortableList == null)
				return;

			HashMap<String, Integer> counts = extractBulletinCounts(fieldToCount, sortableList);


			// TODO: Use or delete these
//			ChartRenderingInfo info = new ChartRenderingInfo();
//			EntityCollection entities = new StandardEntityCollection();
			
//			JFreeChart bar3dChart = create3DBarChart(counts, labelText);
			
			JFreeChart chart = createChart(answers, fieldToCount, counts);
			
			chart.removeSubtitle(new DateTitle());
			
			UiChartPreviewDlg preview = new UiChartPreviewDlg(getMainWindow(), chart);
			preview.setVisible(true);		
			if(preview.wasCancelButtonPressed())
				return;			
			boolean sendToDisk = preview.wantsPrintToDisk();
			
			boolean didPrint = false;
			if(sendToDisk)
				didPrint = printToDisk(chart);
			else
				didPrint = printToPrinter(chart);
				
			if(didPrint)
				mainWindow.notifyDlg("ChartCompleted");
		}
		catch(Exception e)
		{
			MartusLogger.logException(e);
		}
	}

	private JFreeChart createChart(ChartAnswers answers,
			MiniFieldSpec fieldToCount, HashMap<String, Integer> counts)
			throws Exception, TokenInvalidException
	{
		String selectedFieldLabel = fieldToCount.getLabel();
		if(selectedFieldLabel.equals(""))
			selectedFieldLabel = getLocalization().getFieldLabel(fieldToCount.getTag());
		
		JFreeChart chart = createRawChart(answers, counts, selectedFieldLabel);
		chart.addSubtitle(new TextTitle(answers.getSubtitle()));
		
		String today = getLocalization().formatDateTime(new Date().getTime());
		String chartCreatedOnLabel = getLocalization().getFieldLabel("ChartCreatedOn");
		chartCreatedOnLabel = TokenReplacement.replaceToken(chartCreatedOnLabel, "#Date#", today);
		chart.addSubtitle(new ShortTextTitle(chartCreatedOnLabel));
		return chart;
	}

	private JFreeChart createRawChart(ChartAnswers answers,
			HashMap<String, Integer> counts, String selectedFieldLabel) throws Exception
	{
		if(answers.isBarChart())
			return createBarChart(counts, selectedFieldLabel);
		if(answers.is3DBarChart())
			return create3DBarChart(counts, selectedFieldLabel);
		if(answers.isPieChart())
			return createPieChart(counts, selectedFieldLabel);
		
		throw new RuntimeException("Unsupported chart type: " + answers.getChartType());
	}

	private HashMap<String, Integer> extractBulletinCounts(MiniFieldSpec selectedSpec, SortableBulletinList sortableList)
	{
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		
		PartialBulletin[] partialBulletins = sortableList.getUnsortedPartialBulletins();
		for (PartialBulletin partialBulletin : partialBulletins)
		{
			String data = partialBulletin.getData(selectedSpec.getTag());
			String value = selectedSpec.getType().convertStoredToSearchable(data, getLocalization());
			Integer oldCount = counts.get(value);
			if(oldCount == null)
				oldCount = 0;
			int newCount = oldCount + 1;
			counts.put(value, newCount);
		}
		return counts;
	}

	private boolean printToDisk(JFreeChart chart) throws IOException
	{
		File destFile = chooseDestinationFile();
		if(destFile == null)
			return false;

		int CHART_WIDTH_IN_PIXELS = 800;
		int CHART_HEIGHT_IN_PIXELS = 600;
		ChartUtilities.saveChartAsJPEG(destFile, chart, CHART_WIDTH_IN_PIXELS, CHART_HEIGHT_IN_PIXELS);
		return true;
	}

	File chooseDestinationFile()
	{
		String title = getLocalization().getWindowTitle("PrintToWhichFile");
		File destination = new File(getLocalization().getFieldLabel("DefaultPrintChartToDiskFileName"));
		
		while(true)
		{
			UiFileChooser.FileDialogResults results = UiFileChooser.displayFileSaveDialog(mainWindow, title, destination);
			if(results.wasCancelChoosen())
				return null;
			
			destination = results.getChosenFile();
			if(!destination.getName().toLowerCase().endsWith(JPEG_EXTENSION))
				destination = new File(destination.getAbsolutePath() + JPEG_EXTENSION);
			if(!destination.exists())
				break;
			if(mainWindow.confirmDlg(mainWindow, "OverWriteExistingFile"))
				break;
		}
		
		return destination;
	}
	
	private boolean printToPrinter(JFreeChart chart) throws PrinterException
	{
		PrinterJob printJob = PrinterJob.getPrinterJob();
		printJob.setPrintable(new PrintableChart(chart));
		HashPrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
		if(!printJob.printDialog(attributes))
			return false;
		
		printJob.print(attributes);
		return true;
	}
	
	class PrintableChart implements Printable
	{
		public PrintableChart(JFreeChart chartToWrap)
		{
			chart = chartToWrap;
		}

		@Override
		public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException
		{
			if(pageIndex != 0)
				return Printable.NO_SUCH_PAGE;
			
			JComponent viewer = createPrintableComponent();

			// for faster printing, turn off double buffering
			PrintUtilities.disableDoubleBuffering(viewer);
			Graphics2D g2 = PrintUtilities.getTranslatedGraphics(graphics, pageFormat, 0, viewer);
			viewer.paint(g2); // repaint the page for printing
			PrintUtilities.enableDoubleBuffering(viewer);

			return Printable.PAGE_EXISTS;
		}
		
		private JComponent createPrintableComponent()
		{
			JLabel viewer = UiChartPreviewDlg.createChartComponent(chart);
			ActionPrint.setReasonableSize(viewer);
			return viewer;
		}
		

		
		private JFreeChart chart;
	}

	private JFreeChart createBarChart(HashMap<String, Integer> counts, String selectedFieldLabel) throws Exception
	{
		DefaultCategoryDataset dataset = createBarChartDataset(counts);

		boolean showLegend = true;
		boolean showTooltips = true;
		boolean showUrls = false;
		JFreeChart barChart = ChartFactory.createBarChart(
			getXAxisTitle(selectedFieldLabel), selectedFieldLabel, getYAxisTitle(), 
			dataset, PlotOrientation.VERTICAL,
			showLegend, showTooltips, showUrls);
		
		configureBarChartPlot(barChart);
		
		return barChart;
	}

	private JFreeChart create3DBarChart(HashMap<String, Integer> counts, String selectedFieldLabel) throws Exception
	{
		DefaultCategoryDataset dataset = createBarChartDataset(counts);

		boolean showLegend = true;
		boolean showTooltips = true;
		boolean showUrls = false;
		JFreeChart barChart = ChartFactory.createBarChart3D(
			getXAxisTitle(selectedFieldLabel), selectedFieldLabel, getYAxisTitle(), 
			dataset, PlotOrientation.VERTICAL,
			showLegend, showTooltips, showUrls);
		
		configureBarChartPlot(barChart);
		
		return barChart;
	}

	private String getXAxisTitle(String selectedFieldLabel)	throws TokenInvalidException
	{
		String chartTitle = getLocalization().getFieldLabel("ChartTitle");
		chartTitle = TokenReplacement.replaceToken(chartTitle, "#SelectedField#", selectedFieldLabel);
		return chartTitle;
	}

	private String getYAxisTitle()
	{
		return getLocalization().getFieldLabel("ChartYAxisTitle");
	}

	private DefaultCategoryDataset createBarChartDataset(
			HashMap<String, Integer> counts)
	{
		String seriesTitle = getLocalization().getFieldLabel("ChartSeriesTitle");
		DefaultCategoryDataset dataset = new DefaultCategoryDataset();
		Vector<String> keys = new Vector<String>(counts.keySet());
		Collections.sort(keys, new SaneCollator(getLocalization().getCurrentLanguageCode()));
		for (String value : keys)
		{
			dataset.addValue(counts.get(value), seriesTitle, value);
		}
		return dataset;
	}

	private void configureBarChartPlot(JFreeChart barChart)
	{
		CategoryPlot plot = (CategoryPlot) barChart.getPlot();
		NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
		TickUnitSource units = NumberAxis.createIntegerTickUnits();
		rangeAxis.setStandardTickUnits(units);
		
		CategoryAxis domainAxis = plot.getDomainAxis();
		CategoryLabelPositions newPositions = CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 2.0);
		domainAxis.setCategoryLabelPositions(newPositions);
	}

	private JFreeChart createPieChart(HashMap<String, Integer> counts, String selectedFieldLabel) throws Exception
	{
		DefaultPieDataset pieDataset = createPieDataset(counts);
		
		JFreeChart pieChart = ChartFactory.createPieChart(
		        getXAxisTitle(selectedFieldLabel),   // Title
		        pieDataset,           // Dataset
		        true,                 // Show legend
		        true,					// tooltips
		        new Locale(getLocalization().getCurrentLanguageCode())
		        );
		return pieChart;
	}

	private DefaultPieDataset createPieDataset(HashMap<String, Integer> counts)
	{
		DefaultPieDataset pieDataset = new DefaultPieDataset();
		Vector<String> keys = new Vector<String>(counts.keySet());
		Collections.sort(keys, new SaneCollator(getLocalization().getCurrentLanguageCode()));
		for (String value : keys)
		{
			pieDataset.setValue(value, counts.get(value));
		}
		return pieDataset;
	}

	// FIXME: Enable or delete these not-yet-used methods
//	private JFreeChart createDateCountChart(HashMap<String, Integer> counts,
//			String labelText) throws IOException
//	{
//		TimeTableXYDataset dataset = new TimeTableXYDataset(); 
//		for (String value : counts.keySet())
//		{
//			MultiCalendar calendar = MultiCalendar.createFromIsoDateString(value);
//			TimePeriod timePeriod = new Day(calendar.getGregorianDay(), calendar.getGregorianMonth(), calendar.getGregorianYear());
//			dataset.add(timePeriod, counts.get(value), "Number of Martus bulletins by date entered");
//		}
//
//		JFreeChart chart = ChartFactory.createXYBarChart(
//		                     "Martus Bulletin Counts by " + labelText, // Title
//		                      labelText,              // X-Axis label
//		                      true,				// date axis
//		                      "Count",                 // Y-Axis label
//		                      dataset,         // Dataset
//		                      PlotOrientation.VERTICAL,
//		                      true,                     // Show legend
//		                      true,		// tooltips?
//		                      false		// urls
//		                     );
//		return chart;
//	}
//
	private final static String JPEG_EXTENSION = ".jpeg";
}
