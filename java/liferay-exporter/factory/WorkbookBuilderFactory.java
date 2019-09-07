package org.my.admin.export.factory;

import com.liferay.portal.kernel.configuration.Filter;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.InstanceFactory;
import com.liferay.portal.kernel.util.PropsUtil;

import org.my.admin.export.MyWorkbookBuilder;

public class WorkbookBuilderFactory {

	private WorkbookBuilderFactory() {
	}

	public static WorkbookBuilderFactory getInstance() {

		if (instance == null) {
			synchronized (WorkbookBuilderFactory.class) {
				if (instance == null) {// Double check locking pattern
					instance = new WorkbookBuilderFactory();
				}
			}
		}

		return instance;
	}

	public MyWorkbookBuilder createWorkbookBuilder(String queryForm)
		throws PortalException {

		String clazzName =
			PropsUtil.get(REPORT_BUILDER_KEY, new Filter(queryForm));

		try {

			MyWorkbookBuilder newInstance =
				(MyWorkbookBuilder) InstanceFactory.newInstance(clazzName);

			newInstance.init();

			return newInstance;
		}
		catch (Exception e) {
			throw new PortalException(
				"Cannot instantiate class " + clazzName, e);
		}
	}

	private static volatile WorkbookBuilderFactory instance;

	private static final String REPORT_BUILDER_KEY = "my.report.builder.class";
}
