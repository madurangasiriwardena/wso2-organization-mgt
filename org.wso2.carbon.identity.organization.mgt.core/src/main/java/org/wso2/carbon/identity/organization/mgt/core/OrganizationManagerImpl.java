/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.mgt.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.mgt.core.dao.OrganizationAuthorizationDao;
import org.wso2.carbon.identity.organization.mgt.core.dao.OrganizationAuthorizationDaoImpl;
import org.wso2.carbon.identity.organization.mgt.core.dao.OrganizationMgtDao;
import org.wso2.carbon.identity.organization.mgt.core.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.mgt.core.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.mgt.core.exception.OrganizationManagementServerException;
import org.wso2.carbon.identity.organization.mgt.core.internal.OrganizationMgtDataHolder;
import org.wso2.carbon.identity.organization.mgt.core.model.Attribute;
import org.wso2.carbon.identity.organization.mgt.core.model.MetaUser;
import org.wso2.carbon.identity.organization.mgt.core.model.Metadata;
import org.wso2.carbon.identity.organization.mgt.core.model.Operation;
import org.wso2.carbon.identity.organization.mgt.core.model.Organization;
import org.wso2.carbon.identity.organization.mgt.core.model.OrganizationAdd;
import org.wso2.carbon.identity.organization.mgt.core.model.OrganizationMgtRole;
import org.wso2.carbon.identity.organization.mgt.core.model.UserStoreConfig;
import org.wso2.carbon.identity.organization.mgt.core.search.Condition;
import org.wso2.carbon.identity.organization.mgt.core.usermgt.AbstractOrganizationMgtUserStoreManager;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.DN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_CHILDREN_GET_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_CONFIG_GET_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_CONFIG_PATCH_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_DELETE_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_GET_BY_ID_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_GET_ID_BY_NAME_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_GET_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_ORGANIZATION_ADD_ERROR;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_UNAUTHORIZED_ACTION;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_USER_ROLE_ORG_AUTHORIZATION_ERROR;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ORGANIZATION_CREATE_PERMISSION;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ORGANIZATION_RESOURCE_BASE_PATH;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.OrganizationMgtRoles.ORGANIZATION_MGT_ROLE;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.OrganizationMgtRoles.ORGANIZATION_ROLE_MGT_ROLE;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.OrganizationMgtRoles.ORGANIZATION_USER_MGT_ROLE;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_OP_ADD;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_OP_REMOVE;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_OP_REPLACE;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_PATH_ORG_ATTRIBUTES;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_PATH_ORG_DESCRIPTION;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_PATH_ORG_DISPLAY_NAME;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_PATH_ORG_NAME;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_PATH_ORG_PARENT_ID;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PATCH_PATH_ORG_STATUS;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.PRIMARY;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.RDN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ROOT;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.SCIM2_USER_RESOURCE_BASE_PATH;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.UI_EXECUTE;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.USER_STORE_DOMAIN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_CREATED_TIME_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_DESCRIPTION_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_DISPLAY_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_LAST_MODIFIED_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_PARENT_DISPLAY_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_PARENT_NAME_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.constant.SQLConstants.VIEW_STATUS_COLUMN;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.checkForActiveUsers;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.generateUniqueID;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.getLdapRootDn;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.getUserIDFromUserName;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.getUserNameFromUserID;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.handleClientException;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.handleServerException;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.logOrganizationAddObject;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.logOrganizationObject;

/**
 * This class implements the {@link OrganizationManager} interface.
 */
public class OrganizationManagerImpl implements OrganizationManager {

    private static final Log log = LogFactory.getLog(OrganizationManagerImpl.class);
    private OrganizationMgtDao organizationMgtDao = OrganizationMgtDataHolder.getInstance().getOrganizationMgtDao();
    private OrganizationAuthorizationDao authorizationDao = OrganizationMgtDataHolder.getInstance()
            .getOrganizationAuthDao();

    @Override
    public Organization addOrganization(OrganizationAdd organizationAdd, boolean isImport)
            throws OrganizationManagementException {

        logOrganizationAddObject(organizationAdd);
        validateAddOrganizationRequest(organizationAdd);
        Organization organization = generateOrganizationFromRequest(organizationAdd);
        // We can't perform this from the authorization valve. Hence, authorize from here
        boolean isAuthorized = isUserAuthorizedToCreateOrganization(organization.getParent().getId());
        if (!isAuthorized) {
            throw handleClientException(ERROR_CODE_UNAUTHORIZED_ACTION,
                    "User not authorized to create organizations under : " + organization.getParent().getId());
        }
        // Validate attributes
        for (Map.Entry entry : organization.getAttributes().entrySet()) {
            OrganizationMgtDataHolder.getInstance().getAttributeValidator()
                    .validateAttribute((Attribute) entry.getValue());
        }
        organization.setId(generateUniqueID());
        organization.setTenantId(getTenantId());
        // Set metadata
        organization.getMetadata().getCreatedBy().setId(getAuthenticatedUserId());
        organization.getMetadata().getCreatedBy()
                .setRef(String.format(SCIM2_USER_RESOURCE_BASE_PATH, getTenantDomain(), getAuthenticatedUserId()));
        organization.getMetadata().getLastModifiedBy().setId(getAuthenticatedUserId());
        organization.getMetadata().getLastModifiedBy()
                .setRef(String.format(SCIM2_USER_RESOURCE_BASE_PATH, getTenantDomain(), getAuthenticatedUserId()));
        setUserStoreConfigs(organization);
        logOrganizationObject(organization);
        if (!isImport) {
            createLdapDirectory(getTenantId(), organization.getUserStoreConfigs().get(USER_STORE_DOMAIN).getValue(),
                    organization.getUserStoreConfigs().get(DN).getValue());
            if (log.isDebugEnabled()) {
                log.debug("Creating LDAP subdirectory for the organization id : " + organization.getId());
            }
        }
        organizationMgtDao.addOrganization(getTenantId(), organization);
        grantCreatorWithFullPermission(organization.getId());
        return organization;
    }

    @Override
    public Organization getOrganization(String organizationId) throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_GET_BY_ID_REQUEST,
                    "Provided organization ID is empty");
        }
        Organization organization = organizationMgtDao.getOrganization(getTenantId(), organizationId.trim());
        if (organization == null) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_GET_BY_ID_REQUEST,
                    "Organization id " + organizationId + " doesn't exist in this tenant : " + getTenantId());
        }
        // Set derivable attributes
        if (!ROOT.equals(organization.getParent().getId())) {
            organization.getParent().setRef(String.format(ORGANIZATION_RESOURCE_BASE_PATH, getTenantDomain(),
                    organization.getParent().getId()));
        }
        organization.getMetadata().getCreatedBy().setRef(
                String.format(SCIM2_USER_RESOURCE_BASE_PATH, getTenantDomain(),
                        organization.getMetadata().getCreatedBy().getId()));
        organization.getMetadata().getCreatedBy()
                .setUsername(getUserNameFromUserID(organization.getMetadata().getCreatedBy().getId(), getTenantId()));
        organization.getMetadata().getLastModifiedBy().setRef(
                String.format(SCIM2_USER_RESOURCE_BASE_PATH, getTenantDomain(),
                        organization.getMetadata().getLastModifiedBy().getId()));
        organization.getMetadata().getLastModifiedBy().setUsername(
                getUserNameFromUserID(organization.getMetadata().getLastModifiedBy().getId(), getTenantId()));
        return organization;
    }

    @Override
    public List<Organization> getOrganizations(Condition condition, int offset, int limit, String sortBy,
            String sortOrder, List<String> requestedAttributes) throws OrganizationManagementException {

        // Validate pagination and sorting parameters
        sortBy = getMatchingColumnNameForSortingParameter(sortBy);
        List<Organization> organizations = organizationMgtDao.getOrganizations(
                condition,
                getTenantId(),
                offset,
                limit,
                sortBy,
                sortOrder,
                requestedAttributes,
                getAuthenticatedUserId()
        );
        // Populate derivable information of the organizations
        for (Organization organization : organizations) {
            if (!ROOT.equals(organization.getParent().getId())) {
                organization.getParent().setRef(String.format(ORGANIZATION_RESOURCE_BASE_PATH, getTenantDomain(),
                        organization.getParent().getId()));
            }
            organization.getMetadata().getCreatedBy().setRef(
                    String.format(SCIM2_USER_RESOURCE_BASE_PATH, getTenantDomain(),
                            organization.getMetadata().getCreatedBy().getId()));
            organization.getMetadata().getLastModifiedBy().setRef(
                    String.format(SCIM2_USER_RESOURCE_BASE_PATH, getTenantDomain(),
                            organization.getMetadata().getLastModifiedBy().getId()));
        }
        return organizations;
    }

    @Override
    public String getOrganizationIdByName(String organizationName) throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationName)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_GET_ID_BY_NAME_REQUEST,
                    "Provided organization name is empty.");
        }
        organizationName = organizationName.trim();
        String organizationId = organizationMgtDao.getOrganizationIdByName(getTenantId(), organizationName);
        if (organizationId == null) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_GET_ID_BY_NAME_REQUEST,
                    "Organization name " + organizationName + " doesn't exist in this tenant " + getTenantId());
        }
        return organizationId;
    }

    @Override
    public void patchOrganization(String organizationId, List<Operation> operations)
            throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                    "Provided organization ID is empty");
        }
        organizationId = organizationId.trim();
        if (!isOrganizationExistById(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                    "Organization Id " + organizationId + " doesn't exist in this tenant " + getTenantId());
        }
        validateOrganizationPatchOperations(operations, organizationId);
        validatePatchingAttributes(operations);
        for (Operation operation : operations) {
            organizationMgtDao.patchOrganization(organizationId, operation);
        }
        // Update metadata
        Metadata metadata = new Metadata();
        metadata.setLastModifiedBy(new MetaUser());
        metadata.getLastModifiedBy().setId(getAuthenticatedUserId());
        organizationMgtDao.modifyOrganizationMetadata(organizationId, metadata);
    }

    @Override
    public void deleteOrganization(String organizationId) throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_DELETE_REQUEST,
                    "Provided organization ID is empty");
        }
        if (!isOrganizationExistById(organizationId.trim())) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_DELETE_REQUEST,
                    "Organization Id " + organizationId + " doesn't exist in this tenant " + getTenantId());
        }
        organizationMgtDao.deleteOrganization(getTenantId(), organizationId.trim());
    }

    @Override
    public boolean isOrganizationExistByName(String organizationName) throws OrganizationManagementException {

        return organizationMgtDao.isOrganizationExistByName(getTenantId(), organizationName);
    }

    @Override
    public boolean isOrganizationExistById(String id) throws OrganizationManagementException {

        return organizationMgtDao.isOrganizationExistById(getTenantId(), id);
    }

    @Override
    public Map<String, UserStoreConfig> getUserStoreConfigs(String organizationId)
            throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_CONFIG_GET_REQUEST,
                    "Provided organization Id is empty");
        }
        organizationId = organizationId.trim();
        if (organizationMgtDao.isOrganizationExistById(getTenantId(), organizationId)) {
            return organizationMgtDao.getUserStoreConfigsByOrgId(getTenantId(), organizationId);
        } else {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_CONFIG_GET_REQUEST,
                    "Provided organization Id " + organizationId + " doesn't exist in this tenant " + getTenantId());
        }
    }

    @Override
    public List<String> getChildOrganizationIds(String organizationId) throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_CHILDREN_GET_REQUEST,
                    "Provided organization Id is empty");
        }
        organizationId = organizationId.trim();
        if (organizationMgtDao.isOrganizationExistById(getTenantId(), organizationId)) {
            return organizationMgtDao.getChildOrganizationIds(organizationId, getAuthenticatedUserId());
        } else {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_CHILDREN_GET_REQUEST,
                    " Provided organization Id " + organizationId + " doesn't exist in this tenant " + getTenantId());
        }
    }

    @Override
    public void patchUserStoreConfigs(String organizationId, List<Operation> operations)
            throws OrganizationManagementException {

        if (StringUtils.isBlank(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_CONFIG_PATCH_REQUEST,
                    "Provided organization Id is empty");
        }
        organizationId = organizationId.trim();
        if (!isOrganizationExistById(organizationId)) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_CONFIG_PATCH_REQUEST,
                    "Provided organization Id " + organizationId + " doesn't exist in this tenant " + getTenantId());
        }
        validateUserStoreConfigPatchOperations(operations, organizationId);
        for (Operation operation : operations) {
            organizationMgtDao.patchUserStoreConfigs(organizationId, operation);
        }
        // Update metadata
        Metadata metadata = new Metadata();
        metadata.setLastModifiedBy(new MetaUser());
        metadata.getLastModifiedBy().setId(getAuthenticatedUserId());
        organizationMgtDao.modifyOrganizationMetadata(organizationId, metadata);
    }

    private void validateAddOrganizationRequest(OrganizationAdd organizationAdd)
            throws OrganizationManagementException {

        // Check required fields.
        if (StringUtils.isBlank(organizationAdd.getName())) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST, "Required fields are empty");
        }
        organizationAdd.setName(organizationAdd.getName().trim());
        // Trim display name
        if (StringUtils.isNotBlank(organizationAdd.getDisplayName())) {
            organizationAdd.setDisplayName(organizationAdd.getDisplayName().trim());
        } else {
            organizationAdd.setDisplayName(null);
        }
        // Attribute keys cannot be empty
        for (Attribute attribute : organizationAdd.getAttributes()) {
            if (StringUtils.isBlank(attribute.getKey())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                        "Attribute keys cannot be empty.");
            }
            // Sanitize input
            attribute.setKey(attribute.getKey().trim());
            attribute.setValue(attribute.getValue().trim());
        }
        // Check if attribute keys are duplicated
        Set<String> tempSet = new HashSet<>(
                organizationAdd.getAttributes().stream().map(Attribute::getKey).collect(Collectors.toList()));
        if (organizationAdd.getAttributes().size() > tempSet.size()) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                    "Duplicate attribute keys detected");
        }
        // User store config keys and values can't be empty
        List<UserStoreConfig> userStoreConfigs = organizationAdd.getUserStoreConfigs();
        for (int i = 0; i < userStoreConfigs.size(); i++) {
            UserStoreConfig config = userStoreConfigs.get(i);
            if (StringUtils.isBlank(config.getKey()) || StringUtils.isBlank(config.getValue())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                        "User store config attribute keys or values cannot be empty.");
            }
            // Sanitize input
            config.setKey(config.getKey().trim().toUpperCase(Locale.ENGLISH));
            config.setValue(config.getValue().trim());
            // Set user store domain value to upper case
            if (config.getKey().equals(USER_STORE_DOMAIN)) {
                config.setValue(config.getValue().toUpperCase(Locale.ENGLISH));
            }
            // User store configs may only contain RDN and USER_STORE_DOMAIN. (DN to be derived and added later)
            if (!(config.getKey().equals(RDN) || config.getKey().equals(USER_STORE_DOMAIN))) {
                userStoreConfigs.remove(i);
                if (log.isDebugEnabled()) {
                    log.debug("Dropping additional user store configs. "
                            + "Only 'USER_STORE_DOMAIN' and 'RDN' are allowed." + config.getKey());
                }
            }
        }
        // Check if the organization name already exists for the given tenant
        if (isOrganizationExistByName(organizationAdd.getName())) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                    "Organization name " + organizationAdd.getName() + " already exists in this tenant.");
        }
        // Check if the parent organization exists
        String parentId = organizationAdd.getParent().getId();
        if (StringUtils.isNotBlank(parentId) && !isOrganizationExistById(parentId.trim())) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                    "Defined parent organization doesn't exist in this tenant. " + parentId.trim());
        }
        parentId = StringUtils.isNotBlank(parentId) ? parentId.trim() : ROOT;
        organizationAdd.getParent().setId(parentId);
        // Load the parent organization, if not 'ROOT'
        Organization parentOrg = null;
        if (!ROOT.equals(parentId)) {
            parentOrg = getOrganization(parentId);
            // populate parent's properties in the 'OrganizationAdd' object to avoid duplicate DB calls down the lane
            organizationAdd.getParent().setName(parentOrg.getName());
            organizationAdd.getParent().setDisplayName(parentOrg.getDisplayName());
            organizationAdd.getParent()
                    .setRef(String.format(ORGANIZATION_RESOURCE_BASE_PATH, getTenantDomain(), parentId));
        } else {
            organizationAdd.getParent().setName(ROOT);
            organizationAdd.getParent().setDisplayName(ROOT);
        }
        // Check if the parent organization is active for non ROOT organizations
        if (!ROOT.equals(parentId) && parentOrg.getStatus() != Organization.OrgStatus.ACTIVE) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                    "Defined parent organization : " + parentId + " is not ACTIVE");
        }
        // Check if the user store domain matches that of the parent, for non ROOT organizations
        if (!ROOT.equals(parentId)) {
            String parentUserStoreDomain = getUserStoreConfigs(parentId).get(USER_STORE_DOMAIN).getValue();
            for (UserStoreConfig config : organizationAdd.getUserStoreConfigs()) {
                if (USER_STORE_DOMAIN.equals(config.getKey()) && !parentUserStoreDomain.equals(config.getValue())) {
                    throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                            "Defined user store domain : " + config.getValue() + ", doesn't match that of the parent : "
                                    + parentUserStoreDomain);
                }
            }
        }
    }

    private String constructDn(String parentId, String parentDn, String rdn, String userStoreDomain)
            throws OrganizationManagementException {

        boolean rootOrg = ROOT.equals(parentId);
        String dn;
        if (rootOrg) {
            String ldapRoot = getLdapRootDn(userStoreDomain, getTenantId());
            dn = "ou=".concat(rdn).concat(",").concat(ldapRoot);
        } else {
            dn = "ou=".concat(rdn).concat(",").concat(parentDn);
        }
        return dn;
    }

    private Organization generateOrganizationFromRequest(OrganizationAdd organizationAdd) {

        Organization organization = new Organization();
        organization.setName(organizationAdd.getName());
        organization.setDisplayName(organizationAdd.getDisplayName());
        organization.setDescription(organizationAdd.getDescription());
        organization.getParent().setId(organizationAdd.getParent().getId());
        organization.getParent().setName(organizationAdd.getParent().getName());
        organization.getParent().setDisplayName(organizationAdd.getParent().getDisplayName());
        organization.getParent().setRef(organizationAdd.getParent().getRef());
        organization.setStatus(Organization.OrgStatus.ACTIVE);
        organization.setHasAttributes(!organizationAdd.getAttributes().isEmpty());
        // Convert attributes 'list' to a 'map' for better accessibility
        organization.setAttributes(organizationAdd.getAttributes().stream()
                .collect(Collectors.toMap(Attribute::getKey, attribute -> attribute)));
        organization.setUserStoreConfigs(organizationAdd.getUserStoreConfigs().stream()
                .collect(Collectors.toMap(UserStoreConfig::getKey, config -> config)));
        return organization;
    }

    private void setUserStoreConfigs(Organization organization) throws OrganizationManagementException {

        Map<String, UserStoreConfig> parentConfigs = new HashMap<>();
        if (!ROOT.equals(organization.getParent().getId())) {
            parentConfigs = getUserStoreConfigs(organization.getParent().getId());
        }
        if (organization.getUserStoreConfigs().get(USER_STORE_DOMAIN) == null) {
            // If user store domain is not defined for a non-root organization, defaults to parent's domain
            if (!ROOT.equals(organization.getParent().getId())) {
                organization.getUserStoreConfigs().put(USER_STORE_DOMAIN,
                        new UserStoreConfig(USER_STORE_DOMAIN, parentConfigs.get(USER_STORE_DOMAIN).getValue()));
            } else {
                // If user store domain is not defined for a root organization, defaults to PRIMARY
                organization.getUserStoreConfigs()
                        .put(USER_STORE_DOMAIN, new UserStoreConfig(USER_STORE_DOMAIN, PRIMARY));
            }
        }
        // If RDN is not provided, defaults to organization ID
        if (organization.getUserStoreConfigs().get(RDN) == null) {
            organization.getUserStoreConfigs().put(RDN, new UserStoreConfig(RDN, organization.getId()));
        }
        // Check if the RDN is already taken
        boolean isAvailable = organizationMgtDao.isRdnAvailable(organization.getUserStoreConfigs().get(RDN).getValue(),
                organization.getParent().getId(), getTenantId());
        if (!isAvailable) {
            throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                    "RDN : " + organization.getUserStoreConfigs().get(RDN) + ", is not available for the parent : "
                            + organization.getParent().getId());
        }
        // Construct and set DN using RDN, User store domain and the parent ID
        String dn = constructDn(organization.getParent().getId(),
                ROOT.equals(organization.getParent().getId()) ? null : parentConfigs.get(DN).getValue(),
                organization.getUserStoreConfigs().get(RDN).getValue(),
                organization.getUserStoreConfigs().get(USER_STORE_DOMAIN).getValue());
        organization.getUserStoreConfigs().put(DN, new UserStoreConfig(DN, dn));
    }

    private String getMatchingColumnNameForSortingParameter(String sortBy)
            throws OrganizationManagementClientException {

        if (sortBy == null) {
            return null;
        }
        switch (sortBy.trim().toLowerCase(Locale.ENGLISH)) {
            case "name":
                return VIEW_NAME_COLUMN;
            case "displayname":
                return VIEW_DISPLAY_NAME_COLUMN;
            case "description":
                return VIEW_DESCRIPTION_COLUMN;
            case "created":
                return VIEW_CREATED_TIME_COLUMN;
            case "lastmodified":
                return VIEW_LAST_MODIFIED_COLUMN;
            case "status":
                return VIEW_STATUS_COLUMN;
            case "parentname":
                return VIEW_PARENT_NAME_COLUMN;
            case "parentdisplayname":
                return VIEW_PARENT_DISPLAY_NAME_COLUMN;
            default:
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_GET_REQUEST,
                        "Invalid sort parameter. 'sortOrder' [ASC | DESC] and 'sortBy' [name | description |"
                                + " displayName | status | lastModified | created | parentName | parentDisplayName]");
        }
    }

    private void validateOrganizationPatchOperations(List<Operation> operations, String organizationId)
            throws OrganizationManagementException {

        for (Operation operation : operations) {
            // Validate op
            if (StringUtils.isBlank(operation.getOp())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch operation is not defined");
            }
            String op = operation.getOp().trim().toLowerCase(Locale.ENGLISH);
            if (!(PATCH_OP_ADD.equals(op) || PATCH_OP_REMOVE.equals(op) || PATCH_OP_REPLACE.equals(op))) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch op must be either ['add', 'replace', 'remove']");
            }

            // Validate path
            if (StringUtils.isBlank(operation.getPath())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch operation path is not defined");
            }
            String path = operation.getPath().trim();
            // Set path to lower case
            if (path.toLowerCase(Locale.ENGLISH).startsWith(PATCH_PATH_ORG_ATTRIBUTES)) {
                // Convert only the '/attributes/' part to lower case to treat the attribute name case sensitively
                path = path.replaceAll("(?i)" + Pattern.quote(PATCH_PATH_ORG_ATTRIBUTES), PATCH_PATH_ORG_ATTRIBUTES);
            } else if (path.equalsIgnoreCase(PATCH_PATH_ORG_DISPLAY_NAME)) {
                path = PATCH_PATH_ORG_DISPLAY_NAME;
            } else {
                path = path.toLowerCase(Locale.ENGLISH);
            }
            // Is valid path
            if (!(path.equals(PATCH_PATH_ORG_NAME) || path.equals(PATCH_PATH_ORG_DISPLAY_NAME) || path
                    .equals(PATCH_PATH_ORG_DESCRIPTION) || path.equals(PATCH_PATH_ORG_STATUS) || path
                    .equals(PATCH_PATH_ORG_PARENT_ID) || path.startsWith(PATCH_PATH_ORG_ATTRIBUTES))) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Invalid Patch operation path : " + path);
            }

            // Validate value
            String value;
            // Value is mandatory for Add and Replace operations
            if (StringUtils.isBlank(operation.getValue()) && !PATCH_OP_REMOVE.equals(op)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch operation value is not defined");
            } else {
                // Avoid NPEs down the road
                value = operation.getValue() != null ? operation.getValue().trim() : "";
            }
            // You can only remove attributes and non-mandatory fields (displayName, description) only
            if (PATCH_OP_REMOVE.equals(op) && !(path.startsWith(PATCH_PATH_ORG_ATTRIBUTES) || path
                    .equals(PATCH_PATH_ORG_DISPLAY_NAME) || path.equals(PATCH_PATH_ORG_DESCRIPTION))) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Can not remove mandatory field : " + path);
            }
            // Mandatory fields can only be 'Replaced'
            if (!op.equals(PATCH_OP_REPLACE) && !(path.startsWith(PATCH_PATH_ORG_ATTRIBUTES) || path
                    .equals(PATCH_PATH_ORG_DISPLAY_NAME) || path.equals(PATCH_PATH_ORG_DESCRIPTION))) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Mandatory organization fields can only be replaced. Provided op : " + op + ", Path : " + path);
            }
            // STATUS may only contain 'ACTIVE' and 'DISABLED' values
            if (path.equals(PATCH_PATH_ORG_STATUS)) {
                try {
                    value = Organization.OrgStatus.valueOf(value.toUpperCase(Locale.ENGLISH)).toString();
                } catch (IllegalArgumentException e) {
                    throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                            "STATUS field could only contain 'ACTIVE' and 'DISABLED'. Provided : " + value);
                }
            }
            // You can't deactivate an organization which is having any ACTIVE users or ACTIVE child organizations.
            if (path.equals(PATCH_PATH_ORG_STATUS) && value.equals(Organization.OrgStatus.DISABLED.toString())
                    && !canDisable(organizationId)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Can't disable organization : " + organizationId
                                + " as it has one or more ACTIVE organization/s or user/s.");
            }
            // You can't activate an organization, whom's parent organization is not in ACTIVE state.
            if (path.equals(PATCH_PATH_ORG_STATUS) && value.equals(Organization.OrgStatus.ACTIVE.toString())
                    && !canEnable(organizationId)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Can't activate organization : " + organizationId
                                + " as its parent organization is not ACTIVE.");
            }
            // Check if the new parent exist and ACTIVE before patching the /parent/id field
            if (path.equals(PATCH_PATH_ORG_PARENT_ID) && !(isOrganizationExistById(value)
                    && Organization.OrgStatus.ACTIVE.equals(getOrganization(value).getStatus()))) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Provided parent id doesn't represent an ACTIVE organization : " + value);
            }
            // Check if the new organization name already exists
            if (path.equals(PATCH_PATH_ORG_NAME) && isOrganizationExistByName(value)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Provided organization name already exists : " + value);
            }
            if (path.startsWith(PATCH_PATH_ORG_ATTRIBUTES)) {
                String attributeKey = path.replace(PATCH_PATH_ORG_ATTRIBUTES, "").trim();
                // Attribute key can not be empty
                if (StringUtils.isBlank(attributeKey)) {
                    throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                            "Attribute key is not defined in the path : " + path);
                }
                boolean attributeExist = organizationMgtDao
                        .isAttributeExistByKey(getTenantId(), organizationId, attributeKey);
                // If attribute key to be added already exists, update its value
                if (op.equals(PATCH_OP_ADD) && attributeExist) {
                    op = PATCH_OP_REPLACE;
                }
                if (op.equals(PATCH_OP_REMOVE) && !attributeExist) {
                    throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                            "Can not remove non existing attribute key : " + path);
                }
            }

            // Set sanitized input
            operation.setOp(op);
            operation.setPath(path);
            operation.setValue(value);
        }
    }

    private void validateUserStoreConfigPatchOperations(List<Operation> operations, String organizationId)
            throws OrganizationManagementException {

        for (Operation operation : operations) {
            // Validate op
            if (StringUtils.isBlank(operation.getOp())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch operation is not defined");
            }
            String op = operation.getOp().trim().toLowerCase(Locale.ENGLISH);
            if (!PATCH_OP_REPLACE.equals(op)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Configuration patch may only contain 'replace' operation");
            }

            // Validate path
            if (StringUtils.isBlank(operation.getPath())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch operation path is not defined");
            }
            String path = operation.getPath().trim().toUpperCase(Locale.ENGLISH);
            // Only the RDN can be patched
            if (!RDN.equalsIgnoreCase(path)) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "UserStore configuration patch may only have 'RDN' as path. Provided:" + path);
            }
            // Validate value
            // Value is mandatory for user store config patch operations
            if (StringUtils.isBlank(operation.getValue())) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "Patch operation value is not defined");
            }
            // Check if the RDN is available for this parent
            // Note, only the RDN can be patched
            String parentId = organizationMgtDao.getOrganization(getTenantId(), organizationId).getParent().getId();
            boolean isAvailable = organizationMgtDao.isRdnAvailable(operation.getValue(), parentId, getTenantId());
            if (!isAvailable) {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_PATCH_REQUEST,
                        "RDN : " + operation.getValue() + ", is not available for the parent : " + parentId);
            }
            operation.setOp(PATCH_OP_REPLACE);
            operation.setPath(path);
            operation.setValue(operation.getValue().trim());
        }
    }

    /**
     * To disable an organization, it shouldn't have any 'ACTIVE' organizations as its sub-organizations.
     * To disable an organization, all user accounts should be disabled.
     *
     * @param organizationId
     * @return
     * @throws OrganizationManagementException
     */
    private boolean canDisable(String organizationId) throws OrganizationManagementException {

        int tenantId = getTenantId();
        //TODO test this
        if (checkForActiveUsers(organizationId, tenantId)) {
            return false;
        }
        List<String> children = organizationMgtDao.getChildOrganizationIds(organizationId, null);
        for (String child : children) {
            Organization organization = getOrganization(child);
            if (organization.getStatus() == Organization.OrgStatus.ACTIVE) {
                if (log.isDebugEnabled()) {
                    log.debug("Active child organization detected : " + organization.getId());
                }
                return false;
            }
        }
        return true;
    }

    private boolean canEnable(String organizationId) throws OrganizationManagementException {

        Organization organization = getOrganization(organizationId);
        String parentId = organization.getParent().getId();
        // You can enable any root level organization
        if (ROOT.equals(parentId)) {
            return true;
        } else if (Organization.OrgStatus.ACTIVE.equals(getOrganization(parentId).getStatus())) {
            // non-root organizations should have an ACTIVE parent to change to ACTIVE state.
            return true;
        }
        return false;
    }

    private void createLdapDirectory(int tenantId, String userStoreDomain, String dn)
            throws OrganizationManagementException {

        try {
            UserRealm tenantUserRealm = ((UserRealm) OrganizationMgtDataHolder.getInstance().getRealmService()
                    .getTenantUserRealm(tenantId));
            if (tenantUserRealm == null) {
                throw handleServerException(ERROR_CODE_ORGANIZATION_ADD_ERROR,
                        "Error obtaining tenant realm for the tenant id : " + tenantId);
            }
            if (tenantUserRealm.getUserStoreManager() == null
                    || tenantUserRealm.getUserStoreManager().getSecondaryUserStoreManager(userStoreDomain) == null) {
                throw handleServerException(ERROR_CODE_ORGANIZATION_ADD_ERROR,
                        "Error obtaining user store manager for the domain : " + userStoreDomain + ", tenant id : "
                                + tenantId);
            }
            UserStoreManager userStoreManager = tenantUserRealm.getUserStoreManager()
                    .getSecondaryUserStoreManager(userStoreDomain);
            if (userStoreManager instanceof AbstractOrganizationMgtUserStoreManager) {
                ((AbstractOrganizationMgtUserStoreManager) userStoreManager).createOu(dn);
                if (log.isDebugEnabled()) {
                    log.debug("Created subdirectory : " + dn + ", in the user store domain : " + userStoreDomain);
                }
            } else {
                throw handleClientException(ERROR_CODE_INVALID_ORGANIZATION_ADD_REQUEST,
                        "User store manager doesn't support adding LDAP directories. Tenant id : " + tenantId
                                + ", Domain : " + userStoreDomain);
            }
        } catch (UserStoreException e) {
            throw handleServerException(ERROR_CODE_ORGANIZATION_ADD_ERROR,
                    "Error creating the DN : " + dn + " in the user store domain : " + userStoreDomain, e);
        }
    }

    private void validatePatchingAttributes(List<Operation> operations) throws OrganizationManagementException {

        for (Operation operation : operations) {
            // Validate all attribute additions and replacements
            if (operation.getPath().startsWith(PATCH_PATH_ORG_ATTRIBUTES) && !PATCH_OP_REMOVE
                    .equals(operation.getOp())) {
                Attribute attribute = new Attribute();
                attribute.setKey(operation.getPath().replace(PATCH_PATH_ORG_ATTRIBUTES, "").trim());
                attribute.setValue(operation.getValue());
                OrganizationMgtDataHolder.getInstance().getAttributeValidator().validateAttribute(attribute);
            }
        }
    }

    private boolean isUserAuthorizedToCreateOrganization(String parentId) throws OrganizationManagementException {

        // If you are going to create a root level organization,
        // you should have '/permission/admin/organizations/create' assigned to at least one of the roles that you
        // are bearing
        if (ROOT.equals(parentId)) {
            // check using the authorization manager
            try {
                AuthorizationManager authorizationManager = OrganizationMgtDataHolder.getInstance().
                        getRealmService().getTenantUserRealm(getTenantId()).getAuthorizationManager();
                return authorizationManager
                        .isUserAuthorized(getAuthenticatedUsername(), ORGANIZATION_CREATE_PERMISSION, UI_EXECUTE);
            } catch (UserStoreException e) {
                throw handleServerException(ERROR_CODE_USER_ROLE_ORG_AUTHORIZATION_ERROR,
                        "Error while checking if the user is authorized to create ROOT level organization.", e);
            }
        } else {
            // If you are going to create a sub organization,
            // you should have '/permission/admin/organizations/create' over the parent organization
            OrganizationAuthorizationDao authorizationDao = new OrganizationAuthorizationDaoImpl();
            return authorizationDao
                    .isUserAuthorized(getAuthenticatedUserId(), parentId, ORGANIZATION_CREATE_PERMISSION);
        }
    }

    private void grantCreatorWithFullPermission(String organizationId) throws OrganizationManagementException {

        List<String> tempGroupIds = new ArrayList<>();
        Map<String, OrganizationMgtRole> organizationMgtRoles = OrganizationMgtDataHolder.getInstance()
                .getOrganizationMgtRoles();
        OrganizationMgtRole organizationManager = organizationMgtRoles.get(ORGANIZATION_MGT_ROLE.toString());
        tempGroupIds.add(organizationManager.getGroupId());
        // Add an entry for 'organization management' purposes
        authorizationDao.addOrganizationAndUserRoleMapping(
                getAuthenticatedUserId(),
                organizationManager.getGroupId(),
                organizationManager.getHybridRoleId(),
                getTenantId(),
                organizationId
        );
        OrganizationMgtRole organizationUserManager = organizationMgtRoles.get(ORGANIZATION_USER_MGT_ROLE.toString());
        // If the 'OrganizationMgtRole' and the 'OrganizationUserMgtRole' are the same, avoid adding duplicate entries
        if (!tempGroupIds.contains(organizationUserManager.getGroupId())) {
            tempGroupIds.add(organizationUserManager.getGroupId());
            // Add an entry for 'organization user management' purposes
            authorizationDao.addOrganizationAndUserRoleMapping(
                    getAuthenticatedUserId(),
                    organizationUserManager.getGroupId(),
                    organizationUserManager.getHybridRoleId(),
                    getTenantId(),
                    organizationId
            );
        }
        OrganizationMgtRole organizationRoleManager = organizationMgtRoles.get(ORGANIZATION_ROLE_MGT_ROLE.toString());
        // If the 'OrganizationRoleMgtRole' is the same as any of the above, avoid adding duplicate entries
        if (!tempGroupIds.contains(organizationRoleManager.getGroupId())) {
            // Add an entry for 'organization role management' purposes
            authorizationDao.addOrganizationAndUserRoleMapping(
                    getAuthenticatedUserId(),
                    organizationRoleManager.getGroupId(),
                    organizationRoleManager.getHybridRoleId(),
                    getTenantId(),
                    organizationId
            );
        }
    }

    private String getTenantDomain() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    private int getTenantId() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
    }

    private String getAuthenticatedUserId() throws OrganizationManagementServerException {

        return getUserIDFromUserName(getAuthenticatedUsername(), getTenantId());
    }

    private String getAuthenticatedUsername() {

        return PrivilegedCarbonContext.getThreadLocalCarbonContext().getUsername();
    }
}
