package org.my.admin.export.impl;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.service.ServiceContext;

import java.util.ArrayList;
import java.util.List;

import org.my.admin.export.MyWorkbookConstants;
import org.my.admin.util.MyAdminUtil;
import org.my.applicant.service.GeneralInfoServiceUtil;

/**
 * @author Duc Duong
 */
public class EntityInfoWorkbookBuilder
	extends MyWorkbookBuilderImpl {

	@Override
	protected String[] getHeaderColumns() {

		return HEADER_COLUMN_KEYS;
	}

	@Override
	public String[] getParameters() {
		return new String[] {
			MyWorkbookConstants.PARAM_ORGANIZATION_ID
		};
	}

	@Override
	protected String[] getSheetNames() {

		return MyAdminUtil.getLocalizedText(
			new String[] {
				SENDING_ENTITY_INFO,
				RECEIVING_ENTITY_INFO
			});
	}

	@Override
	protected List<Object[]> retrieveDataOnPage(
			int start, int end, ServiceContext serviceContext)
		throws PortalException, SystemException {

		long organizationId = GetterUtil.getLong(
			serviceContext.getAttribute(
				MyWorkbookConstants.PARAM_ORGANIZATION_ID));

		List<Object[]> results = new ArrayList<>();

		if (getCurrentSheetIndex() == SHEET_INDEX_SENDING_ENTITIES) {

			results = GeneralInfoServiceUtil.getSendingEntityInfo(
				organizationId, start, end);
		}
		else if (getCurrentSheetIndex() == SHEET_INDEX_RECEIVING_ENTITIES) {

			results = GeneralInfoServiceUtil.getReceivingEntityInfo(
				organizationId, start, end);
		}

		return results;
	}

	private static final String[] HEADER_COLUMN_KEYS = new String[] {
		"first-name", "last-name", "family-id", "entity", "my-id", "primary-email"
	};

	private static final String RECEIVING_ENTITY_INFO = "receiving-entity-information";

	private static final String SENDING_ENTITY_INFO = "sending-entity-information";

	private static final int SHEET_INDEX_SENDING_ENTITIES = 0;

	private static final int SHEET_INDEX_RECEIVING_ENTITIES = 1;
}
