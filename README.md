# Robot Communication System

A Java-based application for automating communication with intelligent verification systems. The project focuses on interactions with robots through API endpoints, handling challenges that require LLM verification, and processing robot authentication workflows.

## Technologies

- **Java 21** - Core programming language
- **Spring Boot 3.2.x** - Application framework
- **Maven** - Dependency management
- **OpenAI API** - LLM integration for intelligent response generation
- **Jackson** - JSON serialization/deserialization
- **SLF4J/Logback** - Logging

## Setup & Configuration

1. Clone this repository
2. Copy `example-application.properties` to `application.properties` and update with your credentials:
   - OpenAI API key
   - Robot login credentials

## Running the Application

First, install dependencies:
```bash
mvn install
```

Then start the application:
```bash
mvn spring-boot:run
```

## Architecture

This project uses a simple, straightforward architecture to maintain focus on AI integration:

- **Adapters**: Interfaces to external services (e.g., OpenAI)
- **Services**: Business logic and coordination
- **DTOs**: Data transfer objects for API communication

## Design Decisions

- **Simple Error Handling**: We prioritize readability over complex error handling patterns
- **Interface-based Design**: LLM adapters use a common interface for easy provider switching
- **Minimal Architecture**: We keep the solution compact to maintain focus on AI development
- **Externalized Configuration**: Sensitive values are kept in properties files

## Weekly Challenges

The project is structured around weekly challenges for robot communication:

- **Week 1, Day 1**: Year extraction task and robot authentication
- **Week 1, Day 2**: Robot verification procedure with knowledge override

## Note

This is a learning-focused project that demonstrates how to integrate LLMs with traditional applications, emphasizing simplicity in architecture to showcase AI development techniques. 