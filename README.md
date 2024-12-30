# Project Overview

This project is powered by **Maven** and **Docker Compose**. It includes a GUI component and requires specific configurations to run seamlessly.

## IMPORTANT NOTES

### X11 Connection for GUI
Before building & running the project:

1. Run the command:
   ```bash
   xhost +local:docker
   ```
   This gives permission for X11 connections. Which is important for GUI functionality.

2. Ensure there exists a directory:
   ```bash
   /tmp/.X11-unix
   ```
   This directory must be there for the X11 connection.

### Permissions
For GUI functionality, administrative privileges (`sudo`) are important when running Docker-related commands.

## Building and Running the Project

### Automated Run
Use the provided `run.sh` script for building and running the project. Simply execute:
```bash
bash run.sh
```

### Manual Execution
If you prefer to build and run the project manually:

1. **Clean and Install Maven Dependencies**
   ```bash
   mvn clean install
   ```

2. **Build the Docker Images**
   ```bash
   sudo docker compose build
   ```

3. **Run the Docker Containers**
   ```bash
   sudo docker compose up
   ```


