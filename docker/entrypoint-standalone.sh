#!/bin/bash
set -e

# ==========================================
# Standalone Entrypoint Script
# 自包含版本入口脚本:所有配置都在镜像内,无需外部挂载
# ==========================================

# Initialize /kmatrix-data structure
DATA_DIR="/kmatrix-data"
mkdir -p "$DATA_DIR/postgres" "$DATA_DIR/redis" "$DATA_DIR/config" "$DATA_DIR/logs" "$DATA_DIR/uploads" "$DATA_DIR/models"

# Function to handle config files: Copy to data dir if missing, then symlink back
handle_config() {
    local src="$1"
    local dest="$2"
    if [ ! -f "$dest" ]; then
        echo "Initializing config: $dest"
        cp "$src" "$dest"
    fi
    ln -sf "$dest" "$src"
}

# Symlink/Copy Configs
handle_config "/etc/redis/redis.conf" "$DATA_DIR/config/redis.conf"
handle_config "/etc/nginx/nginx.conf" "$DATA_DIR/config/nginx.conf"
handle_config "/etc/supervisor/conf.d/supervisord.conf" "$DATA_DIR/config/supervisord.conf"
handle_config "/app/config/application-docker.yml" "$DATA_DIR/config/application-docker.yml"

# Symlink Data Directories
# Redis
rm -rf /var/lib/redis
ln -sf "$DATA_DIR/redis" /var/lib/redis
# Logs
rm -rf /var/log/supervisor
ln -sf "$DATA_DIR/logs" /var/log/supervisor
# Uploads
rm -rf /data/uploads
ln -sf "$DATA_DIR/uploads" /data/uploads
# Models - Only override if user provides models
if [ "$(ls -A $DATA_DIR/models)" ]; then
    echo "Custom models detected in $DATA_DIR/models. Using them."
    rm -rf /opt/models
    ln -sf "$DATA_DIR/models" /opt/models
else
    echo "No custom models found. Using built-in models."
fi

# Initialize Postgres Data Directory
PG_DATA_DIR="$DATA_DIR/postgres"

# Initialize Postgres Data Directory if empty
if [ -z "$(ls -A $PG_DATA_DIR)" ]; then
    echo "Initializing PostgreSQL data directory at $PG_DATA_DIR..."
    chown -R postgres:postgres "$PG_DATA_DIR"
    chown -R postgres:postgres "$DATA_DIR"
    
    su - postgres -c "/usr/lib/postgresql/17/bin/initdb -D $PG_DATA_DIR"
    
    # Configure potential pgvector extension here if needed or handled by init scripts
    echo "host all all 0.0.0.0/0 md5" >> "$PG_DATA_DIR/pg_hba.conf"
    echo "listen_addresses='*'" >> "$PG_DATA_DIR/postgresql.conf"
    
    # [FIX] Ensure socket directory exists to prevent startup crash (Exit 2)
    echo "Creating PostgreSQL socket directory..."
    mkdir -p /var/run/postgresql
    chown -R postgres:postgres /var/run/postgresql
    chmod 2777 /var/run/postgresql

    # Start temporary postgres server to run init scripts
    su - postgres -c "/usr/lib/postgresql/17/bin/pg_ctl -D $PG_DATA_DIR -w start"
    
    # Create database and user
    su - postgres -c "psql -c \"CREATE USER root WITH SUPERUSER PASSWORD 'root';\""
    su - postgres -c "psql -c \"CREATE DATABASE \"kmatrix\";\""
    su - postgres -c "psql -d \"kmatrix\" -c \"CREATE EXTENSION IF NOT EXISTS vector;\""
    
    # Init SQL scripts
    SQL_FILE="/docker-entrypoint-initdb.d/source_sql/postgres/kmatrix_complete.sql"
    if [ -f "$SQL_FILE" ]; then
        echo "Running init script: $SQL_FILE..."
        su - postgres -c "psql -d \"kmatrix\" -f \"$SQL_FILE\""
    else
        echo "Warning: $SQL_FILE not found!"
    fi
    
    su - postgres -c "/usr/lib/postgresql/17/bin/pg_ctl -D $PG_DATA_DIR -m fast -w stop"
else
    echo "PostgreSQL data directory is not empty. Skipping initialization."
    chown -R postgres:postgres "$PG_DATA_DIR"
    chmod 700 "$PG_DATA_DIR"
fi

# Ensure directories exist and have correct permissions
# (Already created at top, but ensure permissions if needed)
# mkdir -p /data/uploads /data/temp /opt/models

# 自包含版本说明
echo "=========================================="
echo "KMatrix Standalone Docker Container"
echo "所有配置文件已内置于镜像中"
echo "=========================================="

echo "Starting Supervisor..."
# Cleanup stale pid file if exists
rm -f "$DATA_DIR/postgres/postmaster.pid"
exec "$@"
