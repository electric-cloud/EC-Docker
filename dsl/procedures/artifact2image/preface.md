The following artifacts are supported:
 * .war (will be treated as web application and image will be built with Jetty image as base)
 * .jar (will be treated as Springboot application)
 * .NET (built application with web.config and *.dll is expected).
 * .csproj (will be built)

For .csproj artifact one needs to specify Command field.

#### Jetty

If .war file is found in the artifact folder, the artifact will be treated as web application.
Dockerfile will look like below:

    FROM ${BASE_IMAGE:'jetty:9.4.7-jre8-alpine'} # Will use Base Image parameter or jetty:9.4.7-jre8-alpine by default

    COPY ${FILENAME} /var/lib/jetty/webapps/ROOT.war # FILENAME is the filename of artifact, e.g. hello-world.war
    EXPOSE ${PORTS:8080} # Will use Ports parameter or 8080 by default
    <% if (ENV) { %> # Will use Environment Variables parameter if provided
    ENV ${ENV}
    <% } %>

    <% if (COMMAND) { %> # Will use Command parameter if provided
    CMD [${COMMAND}]
    <% } %>

#### Springboot

If .jar file is found in the artifact folder, the artifact wil be treated as Springboot application.
Dockerfile will look like below:

    FROM ${BASE_IMAGE:'openjdk:8-jdk-alpine'}

    ADD ${FILENAME} app.jar
    EXPOSE ${PORTS:8080}
    <% if (ENV) { %>
    ENV ${ENV}
    <% } %>

    CMD [${COMMAND:'"java", "-jar", "/app.jar"'}]

#### ASP.NET

If web.config is found in the artifact folder, the artifact will be treated as .NET application.
Dockerfile will look like below:

    FROM ${BASE_IMAGE:'microsoft/aspnetcore:2.0'}

    EXPOSE ${PORTS:80}
    <% if (ENV) { %>
    ENV ${ENV}
    <% } %>

    WORKDIR /app
    COPY . .
    RUN rm Dockerfile

    <%
    DEFAULT_COMMAND = '"dotnet", ' + '"' + FILENAME + '"'
    %>
    ENTRYPOINT [${COMMAND:DEFAULT_COMMAND}]

#### CSPROJ

If .csproj file is found in the artifact folder, the artifact will be treated as raw .NET application.
*NB*: In this case Command field will be needed in order to build a correct Dockerfile.

    FROM microsoft/aspnetcore-build:2.0 AS build-env
    WORKDIR /app

    COPY ${FILENAME} ./
    RUN dotnet restore

    COPY . ./
    RUN dotnet publish -c Release -o out

    # build runtime image
    FROM ${BASE_IMAGE:'microsoft/aspnetcore:2.0'}
    WORKDIR /app
    COPY --from=build-env /app/out .

    EXPOSE ${PORTS:80}
    <% if (ENV) { %>
    ENV ${ENV}
    <% } %>

    CMD [${COMMAND}]


