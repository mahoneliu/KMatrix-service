package org.dromara.ai.storage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.dromara.ai.storage.domain.KmTempFile;

/**
 * 临时文件 Mapper
 *
 * @author Mahone
 * @date 2026-03-28
 */
@Mapper
public interface KmTempFileMapper extends BaseMapper<KmTempFile> {
}
