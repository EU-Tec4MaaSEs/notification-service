# tec4maases-notification-service

## Overview

Notification service is responsible for notifying users in T4M Platform regarding events from Platform Components regarding Service Compositions / Decompositions, Negotiation Processes and any other related Platform Events.

Notification service is directly communicating with UI via WebSockets informing real-time the Platform users and exposes an API so the T4M Platform can monitor all the notifications.

It is based on Java Spring Boot framework utilizing Java 21 and Virtual Threads.

## Table of Contents

1. [Installation](#installation)
2. [Usage](#usage)
3. [Deployment](#deployment)
4. [License](#license)
5. [Contributors](#contributors)

### Installation

1. Clone the repository:

    ```sh
    git clone https://github.com/EU-Tec4MaaSEs/notification-service.git
    cd notification-service
    ```

2. Install the dependencies:

    ```sh
    mvn install
    ```

3. Instantiate an instance of PostgresSQL and configure the following variables:

   ```sh
   server.port=${APP_PORT:8093}
   application.url=${APP_URL:http://localhost:8094}
   keycloak.url=${KEYCLOAK_URL:###}
   keycloak.realm=${KEYCLOAK_REALM:###}
   keycloak.client-id=${KEYCLOAK_CLIENT_ID:###}
   keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET:###}
   spring.security.cors.domains=${CORS_DOMAINS:http://localhost:3000}
   ```

### Usage

1. Instantiate PostgresSQL db from provided docker compose file

   ```sh
    docker compose up -d
    ```

2. Start the Spring Boot application

    ```sh
    mvn spring-boot:run
    ```

3. The application will start on `http://localhost:8093`.

4. Access the OpenAPI documentation at `http://localhost:8093/api/notification-service/swagger-ui/index.html`.

### Deployment

For local deployment Docker containers can be utilized to deploy the microservice with the following procedure:

1. Ensure Docker is installed and running.

2. Build the maven project:

    ```sh
    mvn package
    ```

3. Build the Docker container:

    ```sh
    docker build -t tec4maases-notification-service:latest .
    ```

4. Run the Docker container including the environmental variables:

    ```sh
    docker run -d -p 8093:8093 --name notification-service notification-service:latest
    ```

   ``NOTE``: The following environmental variable should be configured:

   ```sh
    APP_PORT=..
    APP_URL=..
    KEYCLOAK_URL=..
    KEYCLOAK_REALM=..
    KEYCLOAK_CLIENT_ID=..
    KEYCLOAK_CLIENT_SECRET=..
    CORS_DOMAINS=..
   ```

5. To stop container run:

    ```sh
    docker stop tec4maases-notification-service
    ```

6. To deploy Keycloak with PostgresSQL just execute via Docker the following command in the project directory:

    ```sh
    docker compose up -d
    ```

## License

TThis project has received funding from the European Union's Horizon 2022 research and innovation program, under Grant Agreement 101091996.

For more details about the licence, see the [LICENSE](LICENSE) file.

## Contributors

- Alkis Aznavouridis (<a.aznavouridis@atc.gr>)
