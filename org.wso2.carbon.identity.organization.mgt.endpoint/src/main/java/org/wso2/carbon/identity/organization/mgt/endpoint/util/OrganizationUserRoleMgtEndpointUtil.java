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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.organization.mgt.endpoint.util;

import org.apache.commons.logging.Log;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.ErrorDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.dto.UserDTO;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.BadRequestException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.ConflictRequestException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.ForbiddenException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.InternalServerErrorException;
import org.wso2.carbon.identity.organization.mgt.endpoint.exceptions.NotFoundException;
import org.wso2.carbon.identity.organization.user.role.mgt.core.exception.OrganizationUserRoleMgtClientException;
import org.wso2.carbon.identity.organization.user.role.mgt.core.exception.OrganizationUserRoleMgtException;
import org.wso2.carbon.identity.organization.user.role.mgt.core.model.User;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import static org.wso2.carbon.identity.organization.mgt.core.constant.OrganizationMgtConstants.ErrorMessages.ERROR_CODE_UNEXPECTED;

/**
 * Organization User Role Mgt Endpoint Util.
 */
public class OrganizationUserRoleMgtEndpointUtil {

    public static Response handleBadRequestResponse(OrganizationUserRoleMgtClientException e, Log log) {

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

    public static Response handleServerErrorResponse(OrganizationUserRoleMgtException e, Log log) {

        throw buildInternalServerErrorException(e.getErrorCode(), log, e);
    }

    public static Response handleUnexpectedServerError(Throwable e, Log log) {

        throw buildInternalServerErrorException(ERROR_CODE_UNEXPECTED.getCode(), log, e);
    }

    private static boolean isNotFoundError(OrganizationUserRoleMgtClientException e) {

        //TODO implement once error codes are finalized
        return false;
    }

    private static boolean isConflictError(OrganizationUserRoleMgtClientException e) {

        //TODO implement once error codes are finalized
        return false;
    }

    private static boolean isForbiddenError(OrganizationUserRoleMgtClientException e) {

        //TODO implement once error codes are finalized
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

    public static List<UserDTO> getUserDTOsFromUsers(List<User> users) {

        List<UserDTO> userDTOs = new ArrayList<>();
        for (User user : users) {
            UserDTO userDTO = new UserDTO();
            userDTO.setUserId(user.getUserId());
            userDTO.setUsername(user.getUserName());
            userDTOs.add(userDTO);
        }
        return userDTOs;
    }
}
