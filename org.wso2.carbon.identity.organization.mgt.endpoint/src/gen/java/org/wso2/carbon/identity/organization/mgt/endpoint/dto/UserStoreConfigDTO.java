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

package org.wso2.carbon.identity.organization.mgt.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.NotNull;

@ApiModel(description = "")
public class UserStoreConfigDTO {

    public enum KeyEnum {
        USER_STORE_DOMAIN, RDN, DN,
    }

    ;
    @NotNull
    private KeyEnum key = null;

    @NotNull
    private String value = null;

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("key")
    public KeyEnum getKey() {
        return key;
    }

    public void setKey(KeyEnum key) {
        this.key = key;
    }

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("value")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class UserStoreConfigDTO {\n");

        sb.append("  key: ").append(key).append("\n");
        sb.append("  value: ").append(value).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
