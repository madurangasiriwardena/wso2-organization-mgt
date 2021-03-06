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
public class BasicOrganizationDTO {

    @NotNull
    private String id = null;

    @NotNull
    private String name = null;

    private String displayName = null;

    private String description = null;

    public enum StatusEnum {
        ACTIVE, DISABLED,
    }

    ;
    @NotNull
    private StatusEnum status = null;

    @NotNull
    private ParentDTO parent = null;

    @NotNull
    private MetaDTO meta = null;

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("id")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("displayName")
    public String getDisplayName() {
        return displayName != null ? displayName : "";
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     *
     **/
    @ApiModelProperty(value = "")
    @JsonProperty("description")
    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("status")
    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("parent")
    public ParentDTO getParent() {
        return parent;
    }

    public void setParent(ParentDTO parent) {
        this.parent = parent;
    }

    /**
     *
     **/
    @ApiModelProperty(required = true,
                      value = "")
    @JsonProperty("meta")
    public MetaDTO getMeta() {
        return meta;
    }

    public void setMeta(MetaDTO meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class BasicOrganizationDTO {\n");

        sb.append("  id: ").append(id).append("\n");
        sb.append("  name: ").append(name).append("\n");
        sb.append("  displayName: ").append(displayName).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  status: ").append(status).append("\n");
        sb.append("  parent: ").append(parent).append("\n");
        sb.append("  meta: ").append(meta).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
