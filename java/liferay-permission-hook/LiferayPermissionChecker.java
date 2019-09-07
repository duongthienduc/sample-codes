package org.my.permission;

import com.liferay.portal.kernel.bean.PortletBeanLocatorUtil;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.util.ArrayUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.Resource;
import com.liferay.portal.model.Role;
import com.liferay.portal.security.permission.AdvancedPermissionChecker;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.InvokableLocalService;
import com.liferay.portal.service.ResourceLocalServiceUtil;
import com.liferay.portal.service.RoleLocalServiceUtil;
import com.liferay.portlet.dynamicdatalists.model.DDLRecordSet;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

/**
 * @author Duc Duong
 */
public class LiferayPermissionChecker extends AdvancedPermissionChecker {

	@Override
	public LiferayPermissionChecker clone() {
		return new LiferayPermissionChecker();
	}

	@Override
	protected boolean doCheckPermission(
			long companyId, long groupId, String name, String primKey,
			String actionId, StopWatch stopWatch)
		throws Exception {

		if (shouldCheckPermissionForApplicant(groupId, name)) {

			return doCheckPermissionForApplicant(
				companyId, groupId, name, primKey, actionId, stopWatch);
		}

		return super.doCheckPermission(
			companyId, groupId, name, primKey, actionId, stopWatch);
	}

	protected boolean doCheckPermissionForApplicant(
			long companyId, long groupId, String name, String primKey,
			String actionId, StopWatch stopWatch)
		throws Exception {

		logHasUserPermission(groupId, name, primKey, actionId, stopWatch, 1);

		if (super.doCheckPermission(
			companyId, DEFAULT_GROUP_ID, name, primKey, actionId, stopWatch)) {

			return true;
		}

		logHasUserPermission(groupId, name, primKey, actionId, stopWatch, 2);

		Group group = GroupLocalServiceUtil.getGroup(groupId);

		long userId = group.getClassPK();

		long[] groupIds = getApplicantEntityGroupIds(userId);

		long sendingGroupId = groupIds[0];

		List<Resource> sendingResources = getResources(
			companyId, sendingGroupId, name, primKey, actionId);

		if (checkSendingPermission(
			sendingGroupId, sendingResources, actionId)) {

			return true;
		}

		logHasUserPermission(
			sendingGroupId, name, primKey, actionId, stopWatch, 3);

		if (groupIds.length > 1 && groupIds[1] > 0L) {

			long receivingGroupId = groupIds[1];

			List<Resource> receivingResources = getResources(
				companyId, receivingGroupId, name, primKey, actionId);

			logHasUserPermission(
				receivingGroupId, name, primKey, actionId, stopWatch, 4);

			return checkReceivingPermission(
				receivingGroupId, receivingResources, actionId);
		}

		return false;
	}

	/**
	 * Should apply applicant permission check mechanism in case the resource
	 * is from MY Applicant package or {@link DDLRecordSet} which is using
	 * in the old Sites of the MY Applicants
	 *
	 * @param groupId
	 * @param name
	 * @return
	 * @throws PortalException
	 * @throws SystemException
	 */
	protected boolean shouldCheckPermissionForApplicant(
			long groupId, String name)
		throws PortalException, SystemException {

		if (groupId <= 0L || !isApplicantResourceName(name)) {

			return false;
		}

		Group group = GroupLocalServiceUtil.fetchGroup(groupId);

		if (Validator.isNull(group) || !group.isUser()) {

			return false;
		}

		Object generalInfo = getApplicantGeneralInfo(group.getClassPK());

		return Validator.isNotNull(generalInfo);
	}

	private boolean checkReceivingPermission(
			long receivingGroupId,  List<Resource> resources, String actionId)
		throws PortalException, SystemException {

		long[] excludedSendingRoleIds = excludeRoles(receivingGroupId, SENDING);

		return ResourceLocalServiceUtil.hasUserPermissions(
			user.getUserId(), receivingGroupId, resources, actionId,
			excludedSendingRoleIds);
	}

	private boolean checkSendingPermission(
			long sendingGroupId, List<Resource> resources, String actionId)
		throws PortalException, SystemException {

		long[] excludedReceivingRoleIds =
			excludeRoles(sendingGroupId, RECEIVING);

		return ResourceLocalServiceUtil.hasUserPermissions(
			user.getUserId(), sendingGroupId, resources, actionId,
			excludedReceivingRoleIds);
	}

	private long[] excludeRoles(long groupId, String roleLabel)
		throws PortalException, SystemException {

		long[] roleIds = getRoleIds(user.getUserId(), groupId);

		List<Role> userRoles = RoleLocalServiceUtil.getRoles(roleIds);

		List<Long> excludeRoleIds = new ArrayList<>();

		for (Role userRole : userRoles) {

			if (!StringUtils.contains(
				userRole.getName().toLowerCase(), roleLabel)) {

				excludeRoleIds.add(userRole.getRoleId());
			}
		}

		return ArrayUtil.toLongArray(excludeRoleIds);
	}

	private Object getApplicantGeneralInfo(long userId)
		throws PortalException {

		try {
			return getGeneralInfoLocalService().invokeMethod(
				"fetchGeneralInfoByUserId", new String[] {
					"long"
				}, new Object[] {
					userId
				});
		}
		catch (Throwable e) {

			throw new PortalException(e);
		}
	}

	private long[] getApplicantEntityGroupIds(long userId)
		throws PortalException {

		try {
			return (long[]) getGeneralInfoLocalService().invokeMethod(
				"getGroupIdsByApplicantId", new String[] {
					"long"
				}, new Object[] {
					userId
				});
		}
		catch (Throwable e) {

			throw new PortalException(e);
		}
	}

	private InvokableLocalService getGeneralInfoLocalService() {

		if (_generalInfoLocalService == null) {

			_generalInfoLocalService =
				(InvokableLocalService) PortletBeanLocatorUtil.locate(
					MY_APPLICANT_PORTLET_CONTEXT,
					GENERAL_INFO_LOCAL_SERVICE_CLASS_NAME);
		}

		return _generalInfoLocalService;
	}

	private boolean isApplicantResourceName(String name) {

		return name.startsWith(ORG_MY_APPLICANT_PREFIX) ||
			name.equals(DDLRecordSet.class.getName());
	}

	private InvokableLocalService _generalInfoLocalService;

	private static final int DEFAULT_GROUP_ID = 0;

	private static final String GENERAL_INFO_LOCAL_SERVICE_CLASS_NAME =
		"org.my.applicant.service.GeneralInfoLocalService";

	private static final String ORG_MY_APPLICANT_PREFIX = "org.my.applicant";

	private static final String SENDING = "sending";

	private static final String MY_APPLICANT_PORTLET_CONTEXT =
		"my-applicant-portlet";

	private static final String RECEIVING = "receiving";
}
