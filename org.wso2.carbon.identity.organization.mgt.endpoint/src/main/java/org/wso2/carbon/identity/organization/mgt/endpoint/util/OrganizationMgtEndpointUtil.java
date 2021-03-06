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

package org.wso2.carbon.identity.organization.mgt.endpoint.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.mgt.core.OrganizationManager;
import org.wso2.carbon.identity.organization.mgt.core.constant.ConditionType;
import org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants;
import org.wso2.carbon.identity.organization.mgt.core.exception.OrganizationManagementClientException;
import org.wso2.carbon.identity.organization.mgt.core.exception.OrganizationManagementException;
import org.wso2.carbon.identity.organization.mgt.core.model.Attribute;
import org.wso2.carbon.identity.organization.mgt.core.model.Organization;
import org.wso2.carbon.identity.organization.mgt.core.model.OrganizationAdd;
import org.wso2.carbon.identity.organization.mgt.core.model.UserStoreConfig;
import org.wso2.carbon.identity.organization.mgt.core.search.ComplexCondition;
import org.wso2.carbon.identity.organization.mgt.core.search.Condition;
import org.wso2.carbon.identity.organization.mgt.core.search.PrimitiveCondition;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.AttributeDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.BasicOrganizationDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.ErrorDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.MetaDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.MetaUserDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.OrganizationAddDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.OrganizationDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.ParentDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.UserStoreConfigDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.BadRequestException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.ConflictRequestException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.ForbiddenException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.InternalServerErrorException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.NotFoundException;
import org.wso2.carbon.identity.organization.user.role.mgt.core.OrganizationUserRoleManager;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_INVALID_DATE_FORMAT;
import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_UNEXPECTED;
import static org.wso2.carbon.identity.organization.mgt.core.util.Utils.handleClientException;

/**
 * This class provides util functions to the Organization Management endpoint.
 */
public class OrganizationMgtEndpointUtil {

    private static final Log log = LogFactory.getLog(OrganizationMgtEndpointUtil.class);

    public static OrganizationManager getOrganizationManager() {

        return (OrganizationManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(OrganizationManager.class, null);
    }

    public static OrganizationUserRoleManager getOrganizationUserRoleManager() {

        return (OrganizationUserRoleManager) PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getOSGiService(OrganizationUserRoleManager.class, null);
    }

    public static OrganizationAdd getOrganizationAddFromDTO(OrganizationAddDTO organizationAddDTO) {

        OrganizationAdd organizationAdd = new OrganizationAdd();
        organizationAdd.setName(organizationAddDTO.getName());
        organizationAdd.setDisplayName(organizationAddDTO.getDisplayName());
        organizationAdd.setDescription(organizationAddDTO.getDescription());
        organizationAdd.getParent().setId(organizationAddDTO.getParentId());
        organizationAdd.setAttributes(
                organizationAddDTO.getAttributes().stream().map(OrganizationMgtEndpointUtil::getAttributeFromDTO)
                        .collect(Collectors.toList()));
        organizationAdd.setUserStoreConfigs(organizationAddDTO.getUserStoreConfigs().stream()
                .map(OrganizationMgtEndpointUtil::getUserStoreConfigsFromDTO).collect(Collectors.toList()));
        return organizationAdd;
    }

    public static BasicOrganizationDTO getBasicOrganizationDTOFromOrganization(Organization organization) {

        BasicOrganizationDTO basicOrganizationDTO = new BasicOrganizationDTO();
        basicOrganizationDTO.setId(organization.getId());
        basicOrganizationDTO.setName(organization.getName());
        basicOrganizationDTO.setDisplayName(organization.getDisplayName());
        basicOrganizationDTO.setDescription(organization.getDescription());
        basicOrganizationDTO.setStatus(BasicOrganizationDTO.StatusEnum.valueOf(organization.getStatus().toString()));
        // Set parent
        ParentDTO parentDTO = new ParentDTO();
        parentDTO.setId(organization.getParent().getId());
        parentDTO.setName(organization.getParent().getName());
        parentDTO.setDisplayName(organization.getParent().getDisplayName());
        parentDTO.setRef(organization.getParent().getRef());
        basicOrganizationDTO.setParent(parentDTO);
        // Set metadata
        MetaDTO metaDTO = new MetaDTO();
        metaDTO.setCreatedBy(new MetaUserDTO());
        metaDTO.setLastModifiedBy(new MetaUserDTO());
        metaDTO.setCreated(organization.getMetadata().getCreated());
        metaDTO.setLastModified(organization.getMetadata().getLastModified());
        metaDTO.getCreatedBy().setId(organization.getMetadata().getCreatedBy().getId());
        metaDTO.getCreatedBy().setRef(organization.getMetadata().getCreatedBy().getRef());
        metaDTO.getLastModifiedBy().setId(organization.getMetadata().getLastModifiedBy().getId());
        metaDTO.getLastModifiedBy().setRef(organization.getMetadata().getLastModifiedBy().getRef());
        basicOrganizationDTO.setMeta(metaDTO);
        return basicOrganizationDTO;
    }

    public static OrganizationDTO getOrganizationDTOFromOrganization(Organization organization) {

        OrganizationDTO organizationDTO = new OrganizationDTO();
        organizationDTO.setId(organization.getId());
        organizationDTO.setName(organization.getName());
        organizationDTO.setDisplayName(organization.getDisplayName());
        organizationDTO.setDescription(organization.getDescription());
        organizationDTO.setStatus(OrganizationDTO.StatusEnum.valueOf(organization.getStatus().toString()));
        // Set parent
        ParentDTO parentDTO = new ParentDTO();
        parentDTO.setId(organization.getParent().getId());
        parentDTO.setName(organization.getParent().getName());
        parentDTO.setDisplayName(organization.getParent().getDisplayName());
        parentDTO.setRef(organization.getParent().getRef());
        organizationDTO.setParent(parentDTO);
        // Set metadata
        MetaDTO metaDTO = new MetaDTO();
        metaDTO.setCreatedBy(new MetaUserDTO());
        metaDTO.setLastModifiedBy(new MetaUserDTO());
        metaDTO.setCreated(organization.getMetadata().getCreated());
        metaDTO.setLastModified(organization.getMetadata().getLastModified());
        metaDTO.getCreatedBy().setId(organization.getMetadata().getCreatedBy().getId());
        metaDTO.getCreatedBy().setRef(organization.getMetadata().getCreatedBy().getRef());
        metaDTO.getCreatedBy().setUsername(organization.getMetadata().getCreatedBy().getUsername());
        metaDTO.getLastModifiedBy().setId(organization.getMetadata().getLastModifiedBy().getId());
        metaDTO.getLastModifiedBy().setRef(organization.getMetadata().getLastModifiedBy().getRef());
        metaDTO.getLastModifiedBy().setUsername(organization.getMetadata().getLastModifiedBy().getUsername());
        organizationDTO.setMeta(metaDTO);
        organizationDTO.setAttributes(organization.getAttributes().values().stream()
                .map(OrganizationMgtEndpointUtil::getAttributeDTOFromAttribute).collect(Collectors.toList()));
        return organizationDTO;
    }

    public static Attribute getAttributeFromDTO(AttributeDTO attributeDTO) {

        return new Attribute(attributeDTO.getKey(), attributeDTO.getValue());
    }

    public static UserStoreConfig getUserStoreConfigsFromDTO(UserStoreConfigDTO userStoreConfigDTO) {

        return new UserStoreConfig(userStoreConfigDTO.getKey().toString(), userStoreConfigDTO.getValue());
    }

    public static List<UserStoreConfigDTO> getUserStoreConfigDTOsFromUserStoreConfigs(
            Collection<UserStoreConfig> configs) {

        List<UserStoreConfigDTO> configDTOs = new ArrayList<>();
        for (UserStoreConfig config : configs) {
            UserStoreConfigDTO configDTO = new UserStoreConfigDTO();
            configDTO.setKey(UserStoreConfigDTO.KeyEnum.valueOf(config.getKey()));
            configDTO.setValue(config.getValue());
            configDTOs.add(configDTO);
        }
        return configDTOs;
    }

    public static List<OrganizationDTO> getOrganizationDTOsFromOrganizations(List<Organization> organizations) {

        List<OrganizationDTO> organizationDTOs = new ArrayList<>();
        for (Organization org : organizations) {
            OrganizationDTO organizationDTO = new OrganizationDTO();
            organizationDTO.setId(org.getId());
            organizationDTO.setName(org.getName());
            organizationDTO.setDisplayName(org.getDisplayName());
            organizationDTO.setDescription(org.getDescription());
            organizationDTO.setStatus(OrganizationDTO.StatusEnum.valueOf(org.getStatus().toString()));
            // Set parent
            ParentDTO parentDTO = new ParentDTO();
            parentDTO.setId(org.getParent().getId());
            parentDTO.setName(org.getParent().getName());
            parentDTO.setDisplayName(org.getParent().getDisplayName());
            parentDTO.setRef(org.getParent().getRef());
            organizationDTO.setParent(parentDTO);
            // Set metadata
            MetaDTO metaDTO = new MetaDTO();
            metaDTO.setCreatedBy(new MetaUserDTO());
            metaDTO.setLastModifiedBy(new MetaUserDTO());
            metaDTO.setCreated(org.getMetadata().getCreated());
            metaDTO.setLastModified(org.getMetadata().getLastModified());
            metaDTO.getCreatedBy().setId(org.getMetadata().getCreatedBy().getId());
            metaDTO.getCreatedBy().setRef(org.getMetadata().getCreatedBy().getRef());
            metaDTO.getLastModifiedBy().setId(org.getMetadata().getLastModifiedBy().getId());
            metaDTO.getLastModifiedBy().setRef(org.getMetadata().getLastModifiedBy().getRef());
            organizationDTO.setMeta(metaDTO);
            // Set attributes if any
            if (org.hasAttributes()) {
                organizationDTO.setAttributes(org.getAttributes().values().stream()
                        .map(OrganizationMgtEndpointUtil::getAttributeDTOFromAttribute).collect(Collectors.toList()));
            }
            organizationDTOs.add(organizationDTO);
        }
        return organizationDTOs;
    }

    public static AttributeDTO getAttributeDTOFromAttribute(Attribute attribute) {

        AttributeDTO attributeDTO = new AttributeDTO();
        attributeDTO.setKey(attribute.getKey());
        attributeDTO.setValue(attribute.getValue());
        return attributeDTO;
    }

    public static Response handleBadRequestResponse(OrganizationManagementClientException e, Log log) {

        if (isNotFoundError(e)) {
            throw OrganizationMgtEndpointUtil.buildNotFoundRequestException(e.getMessage(), e.getErrorCode(), log, e);
        }

        if (isConflictError(e)) {
            throw OrganizationMgtEndpointUtil.buildConflictRequestException(e.getMessage(), e.getErrorCode(), log, e);
        }

        if (isForbiddenError(e)) {
            throw OrganizationMgtEndpointUtil.buildForbiddenException(e.getMessage(), e.getErrorCode(), log, e);
        }
        throw OrganizationMgtEndpointUtil.buildBadRequestException(e.getMessage(), e.getErrorCode(), log, e);
    }

    public static Response handleServerErrorResponse(OrganizationManagementException e, Log log) {

        throw buildInternalServerErrorException(e.getErrorCode(), log, e);
    }

    public static Response handleUnexpectedServerError(Throwable e, Log log) {

        throw buildInternalServerErrorException(ERROR_CODE_UNEXPECTED.getCode(), log, e);
    }

    private static boolean isNotFoundError(OrganizationManagementClientException e) {

        for (OrganizationMgtConstants.NotFoundErrorMessages notFoundError :
                OrganizationMgtConstants.NotFoundErrorMessages.values()) {
            if (notFoundError.toString().equals(e.getErrorCode())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConflictError(OrganizationManagementClientException e) {

        //TODO implement once error codes are finalized
        return false;
    }

    private static boolean isForbiddenError(OrganizationManagementClientException e) {

        for (OrganizationMgtConstants.ForbiddenErrorMessages forbiddenError :
                OrganizationMgtConstants.ForbiddenErrorMessages.values()) {
            if (forbiddenError.toString().equals(e.getErrorCode())) {
                return true;
            }
        }
        return false;
    }

    public static NotFoundException buildNotFoundRequestException(String description, String code, Log log,
            Throwable e) {

        ErrorDTO errorDTO = getErrorDTO(Response.Status.NOT_FOUND.toString(), description, code);
        logDebug(log, e);
        return new NotFoundException(errorDTO);
    }

    public static ConflictRequestException buildConflictRequestException(String description, String code, Log log,
            Throwable e) {

        ErrorDTO errorDTO = getErrorDTO(Response.Status.BAD_REQUEST.toString(), description, code);
        logDebug(log, e);
        return new ConflictRequestException(errorDTO);
    }

    public static ForbiddenException buildForbiddenException(String description, String code, Log log, Throwable e) {

        ErrorDTO errorDTO = getErrorDTO(Response.Status.BAD_REQUEST.toString(), description, code);
        logDebug(log, e);
        return new ForbiddenException(errorDTO);
    }

    public static BadRequestException buildBadRequestException(String description, String code, Log log, Throwable e) {

        ErrorDTO errorDTO = getErrorDTO(Response.Status.BAD_REQUEST.toString(), description, code);
        logDebug(log, e);
        return new BadRequestException(errorDTO);
    }

    public static InternalServerErrorException buildInternalServerErrorException(String code, Log log, Throwable e) {

        ErrorDTO errorDTO = getErrorDTO(Response.Status.INTERNAL_SERVER_ERROR.toString(),
                Response.Status.INTERNAL_SERVER_ERROR.toString(), code);
        logError(log, e);
        return new InternalServerErrorException(errorDTO);
    }

    public static <T> Condition getSearchCondition(SearchContext searchContext, Class<T> reference)
            throws OrganizationManagementClientException {

        SearchCondition<T> searchCondition = searchContext.getCondition(reference);
        return buildSearchCondition(searchCondition);
    }

    private static Condition buildSearchCondition(SearchCondition searchCondition)
            throws OrganizationManagementClientException {

        // No search condition defined
        if (searchCondition == null) {
            return null;
        }
        if (searchCondition.getStatement() != null) {
            PrimitiveStatement primitiveStatement = searchCondition.getStatement();
            if (primitiveStatement.getProperty() != null) {
                if ("created".equals(primitiveStatement.getProperty()) ||
                        "lastModified".equals(primitiveStatement.getProperty())) {
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss.SSS");
                    Date date = null;
                    try {
                        date = format.parse(primitiveStatement.getValue().toString());
                    } catch (ParseException e) {
                        throw handleClientException(ERROR_CODE_INVALID_DATE_FORMAT,
                                "Invalid date format.");
                    }
                    PrimitiveStatement statement = new PrimitiveStatement(primitiveStatement.getProperty(),
                            new Timestamp(date.getTime()), Timestamp.class, primitiveStatement.getCondition());
                    return new PrimitiveCondition(statement.getProperty(),
                            getPrimitiveOperatorFromOdata(statement.getCondition()),
                            statement.getValue());
                }
                return new PrimitiveCondition(primitiveStatement.getProperty(),
                        getPrimitiveOperatorFromOdata(primitiveStatement.getCondition()),
                        primitiveStatement.getValue());

            }
            return null;
        } else {
            List<Condition> conditions = new ArrayList<>();
            for (Object condition : searchCondition.getSearchConditions()) {
                Condition buildCondition = buildSearchCondition((SearchCondition) condition);
                if (buildCondition != null) {
                    conditions.add(buildCondition);
                }
            }
            return new ComplexCondition(getComplexOperatorFromOdata(searchCondition.getConditionType()), conditions);
        }
    }

    private static ConditionType.PrimitiveOperator getPrimitiveOperatorFromOdata(
            org.apache.cxf.jaxrs.ext.search.ConditionType odataConditionType) {

        ConditionType.PrimitiveOperator primitiveConditionType = null;
        switch (odataConditionType) {
            case EQUALS:
                primitiveConditionType = ConditionType.PrimitiveOperator.EQUALS;
                break;
            case GREATER_OR_EQUALS:
                primitiveConditionType = ConditionType.PrimitiveOperator.GREATER_OR_EQUALS;
                break;
            case LESS_OR_EQUALS:
                primitiveConditionType = ConditionType.PrimitiveOperator.LESS_OR_EQUALS;
                break;
            case GREATER_THAN:
                primitiveConditionType = ConditionType.PrimitiveOperator.GREATER_THAN;
                break;
            case NOT_EQUALS:
                primitiveConditionType = ConditionType.PrimitiveOperator.NOT_EQUALS;
                break;
            case LESS_THAN:
                primitiveConditionType = ConditionType.PrimitiveOperator.LESS_THAN;
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Invalid primitive operator found: " + odataConditionType);
                }
        }
        return primitiveConditionType;
    }

    private static ConditionType.ComplexOperator getComplexOperatorFromOdata(
            org.apache.cxf.jaxrs.ext.search.ConditionType odataConditionType) {

        ConditionType.ComplexOperator complexConditionType = null;
        switch (odataConditionType) {
            case OR:
                complexConditionType = ConditionType.ComplexOperator.OR;
                break;
            case AND:
                complexConditionType = ConditionType.ComplexOperator.AND;
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Invalid complex operator found: " + odataConditionType);
                }
        }
        return complexConditionType;
    }

    private static ErrorDTO getErrorDTO(String message, String description, String code) {

        ErrorDTO errorDTO = new ErrorDTO();
        errorDTO.setCode(code);
        errorDTO.setMessage(message);
        errorDTO.setDescription(description);
        return errorDTO;
    }

    private static void logDebug(Log log, Throwable throwable) {

        if (log.isDebugEnabled()) {
            log.debug(Response.Status.BAD_REQUEST, throwable);
        }
    }

    private static void logError(Log log, Throwable throwable) {

        log.error(throwable.getMessage(), throwable);
    }
}
