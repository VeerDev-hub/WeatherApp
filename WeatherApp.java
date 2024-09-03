import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

class LocationNotFoundException extends Exception {
    public LocationNotFoundException(String message) {
        super(message);
    }
}

abstract class ApiClient {
    protected HttpURLConnection fetchApiResponse(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        return conn;
    }
}

interface WeatherService {
    JSONObject getCurrentWeatherData(String locationName) throws LocationNotFoundException;
    JSONObject getHourlyWeatherData(String locationName) throws LocationNotFoundException;
}

public class WeatherApp extends ApiClient implements WeatherService {
    public static void main(String[] args) {
        WeatherApp app = new WeatherApp();
        Scanner in = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("Choose an option:");
            System.out.println("1. Current Weather");
            System.out.println("2. Hourly Weather");
            System.out.println("3. Exit");
            System.out.println("-".repeat(20));

            int option = in.nextInt();
            in.nextLine(); // Consume newline left by nextInt()

            if (option == 3) {
                exit = true;
                System.out.println("Exiting WeatherApp. Goodbye!");
                System.out.println("-".repeat(20));
                continue;
            }

            System.out.print("Enter a location: ");
            String locationName = in.nextLine();

            System.out.println("-".repeat(20));

            try {
                switch (option) {
                    case 1:
                        JSONObject weatherData = app.getCurrentWeatherData(locationName);
                        displayCurrentWeatherData(locationName, weatherData);
                        break;
                    case 2:
                        JSONObject hourlyData = app.getHourlyWeatherData(locationName);
                        displayHourlyWeatherData(hourlyData);
                        break;
                    default:
                        System.out.println("Invalid option.");
                        break;
                }
            } catch (LocationNotFoundException e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println("-".repeat(20));
        }

        in.close();
    }

    private static void displayCurrentWeatherData(String locationName, JSONObject weatherData) {
        System.out.println("Current Weather Data for " + locationName + ":");
        System.out.println("Temperature: " + weatherData.get("temperature") + " C");
        System.out.println("Weather Condition: " + weatherData.get("weather_condition"));
        System.out.println("Humidity: " + weatherData.get("humidity") + "%");
        System.out.println("Windspeed: " + weatherData.get("windspeed") + " km/h");
    }

    private static void displayHourlyWeatherData(JSONObject hourlyData) {
        Scanner in = new Scanner(System.in);
        if (hourlyData == null) {
            System.out.println("Hourly weather data is not available.");
            return;
        }

        JSONArray time = (JSONArray) hourlyData.get("time");
        JSONArray temperature = (JSONArray) hourlyData.get("temperature");
        JSONArray humidity = (JSONArray) hourlyData.get("humidity");
        JSONArray windspeed = (JSONArray) hourlyData.get("windspeed");
        JSONArray weathercode = (JSONArray) hourlyData.get("weathercode");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
        System.out.println("Enter the Interval: ");
        int skip = in.nextInt();
        System.out.println("-".repeat(20));
        System.out.printf("%-20s %-15s %-15s %-15s %-15s%n", "Time", "Temperature (C)", "Condition", "Humidity (%)", "Windspeed (km/h)");
        System.out.println("-".repeat(90));
        for (int i = 0; i < time.size(); i += skip) {
            LocalDateTime currentTime = LocalDateTime.parse((String) time.get(i), formatter);
            double currentTemperature = (double) temperature.get(i);
            String currentCondition = convertWeatherCode((long) weathercode.get(i));
            long currentHumidity = (long) humidity.get(i);
            double currentWindspeed = (double) windspeed.get(i);

            System.out.printf("%-20s %-15.1f %-15s %-15d %-15.1f%n", currentTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), currentTemperature, currentCondition, currentHumidity, currentWindspeed);
        }
    }

    @Override
    public JSONObject getCurrentWeatherData(String locationName) throws LocationNotFoundException {
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            throw new LocationNotFoundException("Location not found.");
        }

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=America%2FLos_Angeles";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn.getResponseCode() != 200) {
                throw new IOException("Could not connect to API");
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner in = new Scanner(conn.getInputStream());
            while (in.hasNext()) {
                resultJson.append(in.nextLine());
            }
            in.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(resultJson.toString());

            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public JSONObject getHourlyWeatherData(String locationName) throws LocationNotFoundException {
        JSONArray locationData = getLocationData(locationName);

        if (locationData == null || locationData.isEmpty()) {
            throw new LocationNotFoundException("Location not found.");
        }

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=America%2FLos_Angeles";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn.getResponseCode() != 200) {
                throw new IOException("Could not connect to API");
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner in = new Scanner(conn.getInputStream());
            while (in.hasNext()) {
                resultJson.append(in.nextLine());
            }
            in.close();
            conn.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(resultJson.toString());

            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            JSONArray time = (JSONArray) hourly.get("time");
            JSONArray temperature = (JSONArray) hourly.get("temperature_2m");
            JSONArray humidity = (JSONArray) hourly.get("relativehumidity_2m");
            JSONArray windspeed = (JSONArray) hourly.get("windspeed_10m");
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");

            JSONObject hourlyData = new JSONObject();
            hourlyData.put("time", time);
            hourlyData.put("temperature", temperature);
            hourlyData.put("humidity", humidity);
            hourlyData.put("windspeed", windspeed);
            hourlyData.put("weathercode", weathercode);

            return hourlyData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");

        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try {
            HttpURLConnection conn = fetchApiResponse(urlString);

            if (conn.getResponseCode() != 200) {
                throw new IOException("Could not connect to API");
            } else {
                StringBuilder resultJson = new StringBuilder();
                Scanner in = new Scanner(conn.getInputStream());
                while (in.hasNext()) {
                    resultJson.append(in.nextLine());
                }
                in.close();
                conn.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(resultJson.toString());

                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();
        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }
        }
        return 0; 
    }

    private static String getCurrentTime() {
        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return currentDateTime.format(formatter);
    }

    private static String convertWeatherCode(long weathercode) {
        String weatherCondition = "";
        if (weathercode == 0L) {
            weatherCondition = "Clear";
        } else if (weathercode > 0L && weathercode <= 3L) {
            weatherCondition = "Cloudy";
        } else if ((weathercode >= 51L && weathercode <= 67L) || (weathercode >= 80L && weathercode <= 99L)) {
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L) {
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}
