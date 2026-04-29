# ==========================================
# Final Image
# ==========================================
# Using official pgvector image
FROM pgvector/pgvector:pg17

# Install dependencies
USER root
# Use Aliyun mirror for Debian Bookworm
RUN sed -i 's/deb.debian.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apt/sources.list.d/debian.sources || sed -i 's/deb.debian.org/mirrors.tuna.tsinghua.edu.cn/g' /etc/apt/sources.list
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    nginx \
    redis-server \
    supervisor \
    gettext-base \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Install PGroonga
RUN apt-get update && apt-get install -y curl \
    && curl -L https://packages.groonga.org/debian/groonga-apt-source-latest-bookworm.deb -o groonga-apt-source-latest-bookworm.deb \
    && apt-get install -y ./groonga-apt-source-latest-bookworm.deb \
    && apt-get update && apt-get install -y postgresql-17-pgdg-pgroonga \
    && rm -rf /var/lib/apt/lists/*

# Create directories
WORKDIR /app
RUN mkdir -p /var/log/supervisor \
    && mkdir -p /usr/share/nginx/html/admin \
    && mkdir -p /usr/share/nginx/html/chat \
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
COPY kmatrix-service/ruoyi-admin/src/main/resources/application-docker.yml /app/config/application-docker.yml
COPY kmatrix-service/ruoyi-admin/src/main/resources/application-product.yml /app/config/application-product.yml
COPY kmatrix-service/docker/entrypoint.sh /app/entrypoint.sh



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
