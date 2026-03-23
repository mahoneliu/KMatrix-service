package org.dromara.ai.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.linpeilie.Converter;
import org.dromara.ai.BaseUnitTest;
import org.dromara.ai.domain.KmSkill;
import org.dromara.ai.domain.bo.KmSkillBo;
import org.dromara.ai.domain.vo.KmSkillVo;
import org.dromara.ai.mapper.KmSkillMapper;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KmSkillServiceImplTest extends BaseUnitTest {

    @Mock
    private KmSkillMapper baseMapper;

    private MockedStatic<SpringUtil> springUtilsMockedStatic;
    private MockedStatic<MapstructUtils> mapstructMockedStatic;
    private Converter converter;

    private KmSkillServiceImpl skillService;

    @BeforeEach
    void setUp() {
        springUtilsMockedStatic = mockStatic(SpringUtil.class);
        converter = mock(Converter.class);
        springUtilsMockedStatic.when(() -> SpringUtil.getBean(eq(Converter.class))).thenReturn(converter);
        
        // 直接 Mock MapstructUtils 以绕过静态初始化器问题
        mapstructMockedStatic = mockStatic(MapstructUtils.class);

        MockitoAnnotations.openMocks(this);
        // 手动实例化以确保构造函数注入成功
        skillService = new KmSkillServiceImpl(baseMapper);
    }

    @AfterEach
    void tearDown() {
        if (springUtilsMockedStatic != null) {
            springUtilsMockedStatic.close();
        }
        if (mapstructMockedStatic != null) {
            mapstructMockedStatic.close();
        }
    }

    @Test
    void testQueryPageList() {
        KmSkillBo bo = new KmSkillBo();
        PageQuery pageQuery = new PageQuery();
        
        IPage<KmSkillVo> page = new Page<>();
        when(baseMapper.selectVoPage(any(IPage.class), any(Wrapper.class))).thenReturn(page);
        
        TableDataInfo<KmSkillVo> result = skillService.queryPageList(bo, pageQuery);
        assertNotNull(result);
    }

    @Test
    void testQueryList() {
        KmSkillBo bo = new KmSkillBo();
        List<KmSkillVo> list = new ArrayList<>();
        when(baseMapper.selectVoList(any(Wrapper.class))).thenReturn(list);
        
        List<KmSkillVo> result = skillService.queryList(bo);
        assertNotNull(result);
    }

    @Test
    void testQueryById() {
        KmSkillVo vo = new KmSkillVo();
        vo.setSkillId(1L);
        when(baseMapper.selectVoById(1L)).thenReturn(vo);
        
        KmSkillVo result = skillService.queryById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getSkillId());
    }

    @Test
    void testInsertByBo() {
        KmSkillBo bo = new KmSkillBo();
        bo.setSkillName("Test");
        
        KmSkill skill = new KmSkill();
        skill.setSkillId(100L);
        
        // 使用 mapstructMockedStatic 来模拟转换逻辑
        mapstructMockedStatic.when(() -> MapstructUtils.convert(any(KmSkillBo.class), eq(KmSkill.class)))
                .thenReturn(skill);
        when(baseMapper.insert(any(KmSkill.class))).thenReturn(1);
        
        Boolean result = skillService.insertByBo(bo);
        assertTrue(result);
        assertEquals(100L, bo.getSkillId());
    }

    @Test
    void testUpdateByBo() {
        KmSkillBo bo = new KmSkillBo();
        bo.setSkillId(1L);
        
        KmSkill skill = new KmSkill();
        skill.setSkillId(1L);
        
        mapstructMockedStatic.when(() -> MapstructUtils.convert(any(KmSkillBo.class), eq(KmSkill.class)))
                .thenReturn(skill);
        when(baseMapper.updateById(any(KmSkill.class))).thenReturn(1);
        
        Boolean result = skillService.updateByBo(bo);
        assertTrue(result);
    }

    @Test
    void testDeleteWithValidIds() {
        Collection<Long> ids = List.of(1L, 2L);
        when(baseMapper.deleteByIds(ids)).thenReturn(2);
        
        Boolean result = skillService.deleteWithValidByIds(ids, true);
        assertTrue(result);
    }
}
