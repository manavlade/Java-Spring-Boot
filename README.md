# Java-Spring-Boot - Excel Upload Application

This project is a full-stack web application designed for uploading, validating, and processing Excel files containing employee and user data. It consists of a Java Spring Boot backend and an Angular frontend designed with a modern user interface.

## Project Structure

The repository is divided into two main parts:

- `demo/`: The backend REST API built with Java Spring Boot.
- `excelfrontend/`: The frontend web application built with Angular, Tailwind CSS, and Spartan UI.

## Backend (`demo/`)

The backend is responsible for receiving Excel file uploads, parsing the data (using Apache POI), validating the rows, and storing the information in a relational database.

### Tech Stack
- **Java 21**
- **Spring Boot 4.x** (Web MVC, Data JPA)
- **Apache POI** (5.4.0) for reading Excel `.xlsx` files
- **Database**: PostgreSQL / H2 Database
- **Lombok** (for reducing boilerplate code)

### Key Components
- `ExcelParser` & `ExcelValidator`: Handles the extraction and validation of Excel data.
- `EmployeeUploadController` & `UserController`: Exposes REST API endpoints for frontend consumption.
- Custom Exceptions (`ExcelValidationException`, `RowValidationException`) and a `GlobalExceptionHandler` for robust error handling.

### How to Run
1. Navigate to the `demo` directory: `cd demo`
2. Run the application using the Maven wrapper:
   - On Windows: `mvnw.cmd spring-boot:run`
   - On Mac/Linux: `./mvnw spring-boot:run`
   *(Ensure your database is configured correctly in `src/main/resources/application.properties`)*

## Frontend (`excelfrontend/`)

The frontend provides a user interface for selecting and uploading Excel files, along with displaying data using charts and modern components. It includes a landing page, file upload capability, and data validation feedback.

### Tech Stack
- **Angular 21**
- **Tailwind CSS 4.x**
- **Spartan UI** (for accessible, modern UI components)
- **Apache ECharts** (for data visualization)
- **Lucide Icons**
- **TypeScript**

### How to Run
1. Navigate to the frontend directory: `cd excelfrontend`
2. Install the necessary dependencies:
   ```bash
   npm install
   ```
3. Start the Angular development server:
   ```bash
   npm start
   ```
   *(Alternatively, use `ng serve`)*
4. Open your browser and navigate to `http://localhost:4200/`.