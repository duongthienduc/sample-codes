package org.my.admin.export.impl;

import com.liferay.portal.kernel.dao.orm.QueryUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.DateFormatFactoryUtil;
import com.liferay.portal.kernel.util.ListUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.service.ServiceContext;

import java.text.DateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.poi.ss.format.CellFormatType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.my.admin.enums.ExportCellStyle;
import org.my.admin.export.MyWorkbookBuilder;
import org.my.admin.export.MyWorkbookConstants;
import org.my.admin.util.MyAdminQueryUtil;
import org.my.admin.util.MyAdminUtil;

public abstract class MyWorkbookBuilderImpl implements MyWorkbookBuilder {

	public MyWorkbookBuilderImpl() {
		// default constructor
	}

	@Override
	public Workbook build() {
		return this.workbook;
	}

	@Override
	public MyWorkbookBuilder createReportBook(ServiceContext serviceContext)
		throws PortalException, SystemException {

		for (String sheetName : getSheetNames()) {
			createSheet(sheetName, serviceContext);
		}

		return this;
	}

	@Override
	public void init() throws PortalException {

		this.workbook = MyAdminQueryUtil.createWorkbook();

		this.initCellStyles();
	}

	@Override
	public MyWorkbookBuilder setQueryDate(String queryDate) {

		this.queryDate = queryDate;
		return this;
	}

	@Override
	public MyWorkbookBuilder setReportName(String reportName) {

		this.reportName = reportName;
		return this;
	}

	/**
	 * New a sheet and fill data into sheet by using own data query in subclass
	 * @param serviceContext
	 * @return Workbook builder itsself
	 * @throws SystemException
	 * @throws PortalException
	 */
	protected MyWorkbookBuilder createSheet(
			String sheetName, ServiceContext serviceContext)
		throws PortalException, SystemException {

		Sheet sheet =
			MyAdminQueryUtil.createSheetForWorkbook(workbook, sheetName);

		this.currentSheet = sheet;

		sheet = MyAdminQueryUtil.createHeaderSheet(
			workbook, sheet, sheetName, this.getHeaderColumns(),
			this.getHeaderRowNum(), this.getTitleRowNum(),
			this.getDateTitle(serviceContext));

		fillSheetData(sheet, serviceContext);

		return this;
	}

	protected int[] getColumnIndexHiddens() {

		return COLUMN_INDEX_HIDDENS;
	}

	protected int getCurrentSheetIndex() {

		return workbook.getSheetIndex(currentSheet);
	}

	protected DateFormat getDateFormat(ServiceContext serviceContext) {

		DateFormat dateFormat = (DateFormat) serviceContext.getAttribute(
			MyWorkbookConstants.DATE_FORMAT);

		if (dateFormat == null) {
			dateFormat =
				DateFormatFactoryUtil.getDate(LocaleUtil.getSiteDefault());
		}

		return dateFormat;
	}

	protected int getFillMode() {

		return MyWorkbookConstants.FILL_BY_PAGING;
	}

	protected abstract String[] getHeaderColumns();

	protected int getHeaderRowNum() {

		return MyWorkbookConstants.WORKBOOK_HEADER_ROWNUM_4;
	}

	protected String getQueryDate() {

		return this.queryDate;
	}

	protected String[] getSheetNames() {

		return new String[] {this.reportName};
	}

	protected String getDateTitle(ServiceContext serviceContext) {

		return MyAdminQueryUtil.getQueryDateForTitle(
			MyWorkbookConstants.DEFAULT_DATE_LABEL, getQueryDate());
	}

	protected int getStartRowNum() {

		return MyWorkbookConstants.WORKBOOK_RECORDS_START_ROWNUM_6;
	}

	protected int getTitleRowNum() {

		return MyWorkbookConstants.WORKBOOK_TITLE_ROWNUM_1;
	}

	protected String[] getTranslatedHeaders() {

		return StringPool.EMPTY_ARRAY;
	}

	protected boolean isHighlightCell(Object[] row) {

		return false;
	}

	protected List<Object[]> retrieveAllDataAtOnce(
			ServiceContext serviceContext)
		throws PortalException, SystemException {

		return this.retrieveDataOnPage(
			QueryUtil.ALL_POS, QueryUtil.ALL_POS, serviceContext);
	}

	protected abstract List<Object[]> retrieveDataOnPage(
			int start, int end, ServiceContext serviceContext)
		throws PortalException, SystemException;

	protected void transformRowData(Object[] rowData) {

		if (ArrayUtil.isEmpty(this.translatedColumnIndexes)) {
			return;
		}

		for (int index : this.translatedColumnIndexes) {

			rowData[index] =
				MyAdminUtil.getLocalizedText((String) rowData[index]);
		}
	}

	private void fillAllDataAtOnce(
			Sheet sheet, ServiceContext serviceContext)
		throws PortalException, SystemException {

		List<Object[]> rows = this.retrieveAllDataAtOnce(serviceContext);

		this.fillWorkbookRows(sheet, rows, this.getStartRowNum());
	}

	private void fillDataByPage(
			Sheet sheet, ServiceContext serviceContext)
		throws PortalException, SystemException {

		int start = 0;
		int end = 0;
		int itemsPerPage = MyAdminQueryUtil.ITEMS_PER_PAGE;
		int startRownum = this.getStartRowNum();

		while (start >= end) {

			end += itemsPerPage;

			List<Object[]> rows =
				this.retrieveDataOnPage(start, end, serviceContext);

			fillWorkbookRows(sheet, rows, startRownum);

			start += rows.size();
			startRownum += itemsPerPage;
		}
	}

	private void fillSheetData(Sheet sheet, ServiceContext serviceContext)
		throws PortalException, SystemException {

		// Init translateColumnIndexes
		this.initTranslatedColumnIndexes();

		if (this.getFillMode() == MyWorkbookConstants.FILL_ALL_AT_ONCE) {
			this.fillAllDataAtOnce(sheet, serviceContext);
		}
		else {
			this.fillDataByPage(sheet, serviceContext);
		}
	}

	private void fillWorkbookRows(
		Sheet sheet, List<Object[]> rows, int startRownum) {

		if (Validator.isNull(sheet) || ListUtil.isEmpty(rows)) {
			return;
		}

		int rownum = startRownum;

		for (Object[] rowData : rows) {

			int indexVisible = 0;

			this.transformRowData(rowData);

			Row row = sheet.createRow(rownum);

			for (int i = 0; i < rowData.length; i++) {

				if (ArrayUtil.contains(getColumnIndexHiddens(), i)) {

					continue;
				}

				Cell cell = row.createCell(indexVisible);

				Object value = rowData[i];

				MyAdminQueryUtil.setCellValue(cell, value);

				cell.setCellStyle(getCellStyle(value, rowData));

				indexVisible++;
			}

			rownum++;
		}
	}

	private CellStyle getCellStyle(Object value, Object[] row) {

		CellFormatType formatType = CellFormatType.TEXT;

		short fontColor = Font.COLOR_NORMAL;

		if (value instanceof Date) {

			formatType = CellFormatType.DATE;
		}

		if (isHighlightCell(row)) {

			fontColor = Font.COLOR_RED;
		}

		return this.cellStyles.get(
			ExportCellStyle.getCellStyle(formatType, fontColor));
	}

	private void initCellStyles() {

		this.cellStyles = new EnumMap<>(ExportCellStyle.class);

		for (ExportCellStyle exportCellStyle : ExportCellStyle.values()) {

			CellStyle cellStyle =
				MyAdminQueryUtil.createCellStyle(this.workbook);

			Font font = MyAdminQueryUtil.createCellFont(this.workbook);

			font.setColor(exportCellStyle.getFontColor());

			cellStyle.setFont(font);

			if (Validator.equals(
				exportCellStyle.getCellFormatType(), CellFormatType.DATE)) {

				cellStyle.setDataFormat(DATA_FORMAT_M_D_YY);
			}

			this.cellStyles.put(exportCellStyle, cellStyle);
		}
	}

	private void initTranslatedColumnIndexes() throws PortalException {

		String[] headers = getHeaderColumns();

		String[] translatedHeaders = this.getTranslatedHeaders();

		this.translatedColumnIndexes = new int[translatedHeaders.length];

		for (int i = 0; i < translatedHeaders.length; i++) {

			int headerColIndex =
				ArrayUtils.indexOf(headers, translatedHeaders[i]);

			if (headerColIndex == ArrayUtils.INDEX_NOT_FOUND) {
				throw new PortalException(
					"Invalid translated header: " + translatedHeaders[i]);
			}

			this.translatedColumnIndexes[i] = headerColIndex;
		}
	}

	private EnumMap<ExportCellStyle, CellStyle> cellStyles;

	private Sheet currentSheet;

	private String queryDate;

	private String reportName;

	private int[] translatedColumnIndexes = {};

	private Workbook workbook;

	private static final int[] COLUMN_INDEX_HIDDENS = {};

	private static final short DATA_FORMAT_M_D_YY = 14;

}
