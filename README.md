# Device diagnostic tool

That's an utility for testing connection with some devices connected via TCP.
Supported protocols:
* ModBus RTU over TCP
* A large number of symbolic protocols

You could send generated ModBus command using **Device ID**, **Function Code**, **First Register** and **Register Count** or make such command by yourself writing HEX codes.

Additional functionality:
* CRC16 calculator for HEX codes
* Saving exchange log with the device

## Project build

Assume you are using Java 8 and Apache Maven.

Run command from project root directory:

```mvn clean install```

## Project run

From project root directory:

```java -jar ./target/ddt-<PROJECT_VERSION>-launcher.jar```

OR just double-click on file **ddt-<PROJECT_VERSION>-launcher.jar**
