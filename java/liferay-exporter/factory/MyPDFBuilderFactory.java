
package org.my.admin.export.factory;

import com.liferay.portal.kernel.configuration.Filter;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.util.InstanceFactory;
import com.liferay.portal.kernel.util.PropsUtil;

import org.my.admin.export.pdf.MyPDFBuilder;

/**
 * @author PhuongNQ
 *
 */
public class MyPDFBuilderFactory {

	private MyPDFBuilderFactory() {

		// Prevent default contructor
	}

	public static MyPDFBuilderFactory getInstance() {

		if (instance == null) {
			synchronized (MyPDFBuilderFactory.class) {

				// Double check locking singleton pattern
				if (instance == null) {
					instance = new MyPDFBuilderFactory();
				}
			}
		}

		return instance;
	}

	public MyPDFBuilder createPDFBuilder(String queryForm)
		throws PortalException {

		String clazzName =
			PropsUtil.get(REPORT_PDF_BUILDER_KEY, new Filter(queryForm));

		try {

			MyPDFBuilder newInstance =
				(MyPDFBuilder) InstanceFactory.newInstance(clazzName);

			newInstance.init();

			return newInstance;
		}
		catch (Exception e) {
			throw new PortalException(
				"Cannot instantiate class " + clazzName, e);
		}
	}

	private static volatile MyPDFBuilderFactory instance;

	private static final String REPORT_PDF_BUILDER_KEY =
		"my.report.pdf.builder.class";
}
