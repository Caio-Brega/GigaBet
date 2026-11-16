# Base de desenvolvimento com Maven e JDK 17
FROM maven:3.9-eclipse-temurin-17-alpine AS dev
WORKDIR /app

# Cache das dependências do Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código-fonte
COPY src ./src

# Comando padrão em desenvolvimento: compila alterações e inicia o console
CMD ["mvn", "compile", "exec:java"]

# Estágio de build para gerar pacote executável
FROM dev AS builder
RUN mvn clean package -DskipTests

# Estágio final enxuto para produção (apenas JRE Alpine)
FROM eclipse-temurin:17-jre-alpine AS prod
WORKDIR /app
COPY --from=builder /app/target/apostas-esportivas-*.jar app.jar
ENTRYPOINT ["java", "-cp", "app.jar", "br.ufmg.dcc603.apostas.console.AplicacaoConsole"]
