# ==========================================
# Final Image
# ==========================================
# Using official pgvector image
FROM pgvector/pgvector:pg17

# Install dependencies
USER root
# Use Aliyun mirror for Debian Bookworm
RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources || sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    nginx \
    redis-server \
    supervisor \
    gettext-base \
    && rm -rf /var/lib/apt/lists/*

# Install pg_jieba
RUN apt-get update && apt-get install -y \
    git \
    make \
    cmake \
    gcc \
    g++ \
    postgresql-server-dev-17 \
    && git clone https://github.com/jaiminpan/pg_jieba.git \
    && cd pg_jieba \
    && git submodule update --init --recursive \
    && mkdir build \
    && cd build \
    && cmake -DPostgreSQL_TYPE_INCLUDE_DIR=/usr/include/postgresql/17/server .. \
    && make \
    && make install \
    && cd ../.. \
    && rm -rf pg_jieba \
    && apt-get remove -y git make cmake gcc g++ postgresql-server-dev-17 \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# Create directories
WORKDIR /app
RUN mkdir -p /var/log/supervisor \
    && mkdir -p /usr/share/nginx/html/admin \
    && mkdir -p /usr/share/nginx/html/chat \
    && mkdir -p /docker-entrypoint-initdb.d \
    && mkdir -p /opt/models \
    && mkdir -p /data/uploads \
    && mkdir -p /data/temp

# Copy Backend Artifacts (Pre-built)
COPY kmatrix-service/ruoyi-admin/target/lib /app/lib
COPY kmatrix-service/ruoyi-admin/target/ruoyi-admin.jar /app/ruoyi-admin.jar
# Copy Frontend Artifacts (Pre-built)
COPY kmatrix-ui/apps/admin/dist /usr/share/nginx/html/admin
COPY kmatrix-ui/apps/chat/dist /usr/share/nginx/html/chat

# Copy Config Files
COPY kmatrix-service/docker/redis.conf /etc/redis/redis.conf
COPY kmatrix-service/docker/nginx.conf /etc/nginx/nginx.conf
COPY kmatrix-service/docker/supervisord.conf /etc/supervisor/conf.d/supervisord.conf
COPY kmatrix-service/docker/entrypoint.sh /app/entrypoint.sh

# Copy SQL Scripts for Init
# You need to gather all necessary SQLs.
# For now, we assume there is a concatenated init.sql or we copy individual files.
# Ideally, we should have a single init script or a folder of them.
# Given the user's file structure, we might need a step to aggregate SQLs.
# TODO: Copy specific SQL files to /docker-entrypoint-initdb.d/
# This part might need manual adjustment or a script to collect SQLs.
# For this template, I will copy a placeholder or specific files if known.
# Copy SQL Scripts for Init
COPY kmatrix-service/script/sql /docker-entrypoint-initdb.d/source_sql

# Set permissions
RUN chmod +x /app/entrypoint.sh

# Environment Variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV TZ=Asia/Shanghai

# Expose Ports
EXPOSE 80 8080 5432

# Mounts


# Entrypoint
ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["/usr/bin/supervisord"]
