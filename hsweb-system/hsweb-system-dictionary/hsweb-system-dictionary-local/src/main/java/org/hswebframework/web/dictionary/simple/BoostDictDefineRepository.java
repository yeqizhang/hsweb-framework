package org.hswebframework.web.dictionary.simple;

import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.commons.entity.DataStatus;
import org.hswebframework.web.commons.entity.param.QueryParamEntity;
import org.hswebframework.web.dict.DictDefine;
import org.hswebframework.web.dict.EnumDict;
import org.hswebframework.web.dict.defaults.DefaultDictDefine;
import org.hswebframework.web.dict.defaults.DefaultDictDefineRepository;
import org.hswebframework.web.dictionary.api.DictionaryItemService;
import org.hswebframework.web.dictionary.api.DictionaryService;
import org.hswebframework.web.dictionary.api.entity.DictionaryEntity;
import org.hswebframework.web.dictionary.api.entity.DictionaryItemEntity;
import org.hswebframework.web.dictionary.api.events.ClearDictionaryCacheEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhouhao
 * @since 3.0
 */
@Service
@CacheConfig(cacheNames = "dictionary")
@Slf4j
@SuppressWarnings("all")
public class BoostDictDefineRepository extends DefaultDictDefineRepository {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryItemService itemService;

    @TransactionalEventListener
    @CacheEvict(key = "'dictDefineById:'+#event.dictionaryId", condition = "#event!=null")
    public void clearCache(ClearDictionaryCacheEvent event) {
        log.info("clear dictionary define cache:{}", event.getDictionaryId());
    }

    @Override
    @Cacheable(key = "'dictDefineById:'+#id")
    public DictDefine getDefine(String id) {
        DictionaryEntity entity = dictionaryService.selectByPk(id);
        if (entity == null) {
            return super.getDefine(id);
        }
        List<EnumDict<Object>> items = (List) itemService.selectByDictId(id)
                .stream()
                .filter(e -> DataStatus.STATUS_ENABLED.equals(e.getStatus()))
                .collect(Collectors.toList());

        return DefaultDictDefine.builder()
                .id(id)
                .comments(entity.getDescribe())
                .items(items)
                .build();
    }

    @Override
    public List<DictDefine> getAllDefine() {
        //查询所有的字典项并按字典ID分组
        Map<String, List<DictionaryItemEntity>> items = QueryParamEntity.newQuery()
                .where(DictionaryItemEntity::getStatus, DataStatus.STATUS_ENABLED)
                .noPaging()
                .execute(itemService::select)
                .stream().collect(Collectors.groupingBy(DictionaryItemEntity::getDictId));

        //转换为字段
        List<DictDefine> all =QueryParamEntity.newQuery()
                .where(DictionaryEntity::getStatus,DataStatus.STATUS_ENABLED)
                .noPaging()
                .execute(dictionaryService::select)
                .stream()
                .map(dict -> DefaultDictDefine.builder()
                        .id(dict.getId())
                        .comments(dict.getDescribe())
                        .items((List) items.getOrDefault(dict.getId(),Collections.emptyList()))
                        .build())
                .collect(Collectors.toList());
        //添加默认的字典
        all.addAll(super.getAllDefine());
        return all;
    }
}
