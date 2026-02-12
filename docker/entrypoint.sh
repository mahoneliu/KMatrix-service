#!/bin/bash
set -e

# Define default data directory
export PGDATA="/var/lib/postgresql/data"

# Handle single volume mount /kmatrix-data
if [ -d "/kmatrix-data" ]; then
    echo "Detected /kmatrix-data mount. Configuring environment..."
    
    # Ensure directory structure exists in the mounted volume
    mkdir -p /kmatrix-data/postgres-data
    mkdir -p /kmatrix-data/redis-data
    mkdir -p /kmatrix-data/uploads
    mkdir -p /kmatrix-data/models
    mkdir -p /kmatrix-data/logs
    
    # 1. Configure Postgres to use new path directly (no symlink)
    export PGDATA="/kmatrix-data/postgres-data"
    echo "Using Postgres Data Directory: $PGDATA"
    
    # Function to replace directory with symlink
    link_dir() {
        src=$1
        dest=$2
        if [ -d "$src" ]; then
            if [ -d "$dest" ]; then
                echo "Replacing directory $dest with symlink to $src"
                # IMPORTANT: Only safe if dest is NOT a VOLUME
                rm -rf "$dest"
            fi
            ln -s "$src" "$dest"
        fi
    }

    # Link Data Directories (Safe now as we removed VOLUMEs from Dockerfile)
    # Postgres is handled via PGDATA, so no link needed for it.
    link_dir "/kmatrix-data/redis-data" "/var/lib/redis"
    link_dir "/kmatrix-data/uploads" "/data/uploads"
    link_dir "/kmatrix-data/models" "/opt/models"
    link_dir "/kmatrix-data/logs" "/var/log/supervisor"
    
    # Link Config Files if they exist
    if [ -f "/kmatrix-data/configs/supervisord.conf" ]; then
        echo "Using custom supervisord.conf"
        ln -sf /kmatrix-data/configs/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
    fi
    if [ -f "/kmatrix-data/configs/redis.conf" ]; then
        echo "Using custom redis.conf"
        ln -sf /kmatrix-data/configs/redis.conf /etc/redis/redis.conf
    fi
    if [ -f "/kmatrix-data/configs/nginx.conf" ]; then
        echo "Using custom nginx.conf"
        ln -sf /kmatrix-data/configs/nginx.conf /etc/nginx/nginx.conf
    fi
    if [ -f "/kmatrix-data/configs/application-docker.yml" ]; then
        echo "Using custom application-docker.yml"
        # Ensure target directory exists
        mkdir -p /app/config
        ln -sf /kmatrix-data/configs/application-docker.yml /app/config/application-docker.yml
    fi

    # Fix permissions for data directories (Postgres is picky)
    if [ -d "$PGDATA" ]; then
        chown -R postgres:postgres "$PGDATA"
        chmod 700 "$PGDATA"
    fi
    chown -R redis:redis /var/lib/redis
fi

# Initialize Postgres Data Directory if empty
if [ -z "$(ls -A "$PGDATA" 2>/dev/null)" ]; then
    echo "Initializing PostgreSQL data directory at $PGDATA..."
    
    # Create directory if it doesn't exist (e.g. if we are using default path and it was wiped)
    mkdir -p "$PGDATA"
    chown -R postgres:postgres "$PGDATA"
    chmod 700 "$PGDATA"

    su - postgres -c "/usr/lib/postgresql/17/bin/initdb -D \"$PGDATA\""
    
    # Configure pg_hba.conf and postgresql.conf
    echo "host all all 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
    echo "listen_addresses='*'" >> "$PGDATA/postgresql.conf"
    
    # Start temporary postgres server to run init scripts
    su - postgres -c "/usr/lib/postgresql/17/bin/pg_ctl -D \"$PGDATA\" -w start"
    
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
    
    su - postgres -c "/usr/lib/postgresql/17/bin/pg_ctl -D \"$PGDATA\" -m fast -w stop"
else
    echo "PostgreSQL data directory at $PGDATA is not empty. Skipping initialization."
    chown -R postgres:postgres "$PGDATA"
    chmod 700 "$PGDATA"
fi

# Ensure directories exist and have correct permissions
mkdir -p /data/uploads /data/temp /opt/models

# Generate final configs from templates if needed (using envsubst)

echo "Starting Supervisor..."
exec "$@"
