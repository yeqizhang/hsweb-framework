/*
 * Copyright 2019 http://www.hswebframework.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hswebframework.web.controller;


import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Permission;
import org.hswebframework.web.authorization.User;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.Logical;
import org.hswebframework.web.commons.entity.RecordCreationEntity;
import org.hswebframework.web.commons.entity.RecordModifierEntity;
import org.hswebframework.web.controller.message.ResponseMessage;
import org.hswebframework.web.logging.AccessLogger;
import org.hswebframework.web.service.CreateEntityService;
import org.hswebframework.web.service.UpdateService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 通用更新控制器
 *
 * @author zhouhao
 */
public interface UpdateController<E, PK, M> {
    <S extends UpdateService<E, PK> & CreateEntityService<E>> S getService();

    @Authorize(action = Permission.ACTION_UPDATE)
    @PutMapping(path = "/{id}")
    @ApiOperation("修改数据")
    default ResponseMessage<Integer> updateByPrimaryKey(@PathVariable PK id, @RequestBody M data) {
        E entity = modelToEntity(data, getService().createEntity());
        if (entity instanceof RecordModifierEntity) {
            RecordModifierEntity creationEntity = (RecordModifierEntity) entity;
            creationEntity.setModifyTimeNow();
            creationEntity.setModifierId(Authentication.current()
                    .map(Authentication::getUser)
                    .map(User::getId)
                    .orElse(null));
        }
        return ResponseMessage.ok(getService().updateByPk(id, entity));
    }

    @Authorize(action = {Permission.ACTION_UPDATE, Permission.ACTION_ADD}, logical = Logical.AND)
    @PatchMapping
    @ApiOperation("新增或者修改")
    default ResponseMessage<PK> saveOrUpdate(@RequestBody M data) {
        E entity = modelToEntity(data, getService().createEntity());
        //自动添加创建人和创建时间
        if (entity instanceof RecordCreationEntity) {
            RecordCreationEntity creationEntity = (RecordCreationEntity) entity;
            creationEntity.setCreateTimeNow();
            creationEntity.setCreatorId(Authentication.current()
                    .map(Authentication::getUser)
                    .map(User::getId)
                    .orElse(null));
        }
        //修改人和修改时间
        if (entity instanceof RecordModifierEntity) {
            RecordModifierEntity creationEntity = (RecordModifierEntity) entity;
            creationEntity.setModifyTimeNow();
            creationEntity.setModifierId(Authentication.current()
                    .map(Authentication::getUser)
                    .map(User::getId)
                    .orElse(null));
        }
        return ResponseMessage.ok(getService().saveOrUpdate(entity));
    }

    /**
     * 将model转为entity
     *
     * @param model
     * @param entity
     * @return 转换后的结果
     * @see org.hswebframework.web.commons.model.Model
     * @see org.hswebframework.web.commons.entity.Entity
     */
    @Authorize(ignore = true)
    E modelToEntity(M model, E entity);
}
