# WeatherApp

## Overview

WeatherApp is a Java application that provides weather information for a specified location. It supports fetching current weather data and hourly weather forecasts using the Open Meteo API. The application allows users to view temperature, weather conditions, humidity, and windspeed, and it also features a basic location search.

## Features

- **Current Weather Data**: Retrieves and displays current weather conditions including temperature, weather condition, humidity, and windspeed.
- **Hourly Weather Data**: Provides detailed hourly weather data for temperature, humidity, windspeed, and weather conditions.
- **Location Search**: Allows users to search for a location and obtain weather data for that location.

## Dependencies

- `org.json.simple` for JSON parsing
- Java standard libraries (e.g., `java.net`, `java.io`, `java.time`)

## Installation

1. Clone the repository:

   ```sh
   git clone https://github.com/yourusername/WeatherApp.git
   ```

2. Navigate to the project directory:

   ```sh
   cd WeatherApp
   ```

3. Compile the Java files:

   ```sh
   javac -cp .:json-simple-1.1.1.jar WeatherApp.java
   ```

4. Run the application:

   ```sh
   java -cp .:json-simple-1.1.1.jar WeatherApp
   ```

## Usage

1. **Choose an Option**: 
   - **1. Current Weather**: To get current weather data.
   - **2. Hourly Weather**: To get hourly weather forecast.
   - **3. Exit**: To exit the application.

2. **Enter a Location**: Provide the location name when prompted.

3. **View Results**: The application will display weather data based on your selection.

## Example

```plaintext
Choose an option:
1. Current Weather
2. Hourly Weather
3. Exit
--------------------
Enter a location: New York
--------------------
Current Weather Data for New York:
Temperature: 22.5 C
Weather Condition: Clear
Humidity: 60%
Windspeed: 15.0 km/h
--------------------
```

## Code Structure

- `LocationNotFoundException.java`: Custom exception for handling location errors.
- `ApiClient.java`: Abstract class for API interaction.
- `WeatherService.java`: Interface defining methods for weather data retrieval.
- `WeatherApp.java`: Main application class implementing `WeatherService` and running the application.

## Contributing

Contributions are welcome! Please fork the repository and submit a pull request with your changes. 
