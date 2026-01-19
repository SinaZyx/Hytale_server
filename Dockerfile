FROM eclipse-temurin:25-jdk
WORKDIR /app
EXPOSE 5520/udp
# On lance le serveur qui est dans le volume /var/lib/hytale-server
CMD ["java", "-Xmx4G", "-XX:AOTCache=Server/HytaleServer.aot", "-jar", "Server/HytaleServer.jar", "--assets", "Assets.zip"]
