-- 临时文件表
CREATE TABLE km_temp_file
(
    id                 BIGSERIAL PRIMARY KEY,
    dataset_id         BIGINT       NOT NULL,
    original_filename  VARCHAR(500) NOT NULL,
    file_extension     VARCHAR(50),
    file_size          BIGINT,
    temp_path          VARCHAR(1000) NOT NULL,
    create_time        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    expire_time        TIMESTAMP    NOT NULL,
    CONSTRAINT fk_temp_file_dataset FOREIGN KEY (dataset_id) REFERENCES km_dataset (id) ON DELETE CASCADE
);

COMMENT ON TABLE km_temp_file IS '临时文件表';
COMMENT ON COLUMN km_temp_file.id IS '临时文件ID';
COMMENT ON COLUMN km_temp_file.dataset_id IS '数据集ID';
COMMENT ON COLUMN km_temp_file.original_filename IS '原始文件名';
COMMENT ON COLUMN km_temp_file.file_extension IS '文件扩展名';
COMMENT ON COLUMN km_temp_file.file_size IS '文件大小(字节)';
COMMENT ON COLUMN km_temp_file.temp_path IS '临时存储路径';
COMMENT ON COLUMN km_temp_file.create_time IS '创建时间';
COMMENT ON COLUMN km_temp_file.expire_time IS '过期时间';

-- 创建索引
CREATE INDEX idx_temp_file_dataset ON km_temp_file(dataset_id);
CREATE INDEX idx_temp_file_expire ON km_temp_file(expire_time);
