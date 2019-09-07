package org.my.admin.export;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.service.ServiceContext;

import org.apache.poi.ss.usermodel.Workbook;

public interface MyWorkbookBuilder {

	Workbook build();

	MyWorkbookBuilder createReportBook(ServiceContext serviceContext)
		throws PortalException, SystemException;

	String[] getParameters();

	void init() throws PortalException;

	MyWorkbookBuilder setQueryDate(String queryDate);

	MyWorkbookBuilder setReportName(String reportName);
}
