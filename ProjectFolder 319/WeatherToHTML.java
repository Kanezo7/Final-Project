import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherToHTML {

    public static void main(String[] args) {
        try {
            Scanner input = new Scanner(System.in);
            System.out.print("Enter a city (e.g., Indiana, PA): ");
            String city = input.nextLine();
            input.close();

            // GEO LOOKUP
            String encodedCity = URLEncoder.encode(city, "UTF-8");
            String geoURL =
                    "https://nominatim.openstreetmap.org/search?q=" +
                    encodedCity + "&format=json&limit=1";

            JSONArray geoData = new JSONArray(getHTTP(geoURL));

            if (geoData.length() == 0) {
                System.out.println("City not found.");
                return;
            }

            JSONObject location = geoData.getJSONObject(0);
            double lat = Double.parseDouble(location.getString("lat"));
            double lon = Double.parseDouble(location.getString("lon"));

            // WEATHER DATA
            String weatherURL =
                "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&current_weather=true" +
                "&hourly=apparent_temperature,precipitation" +
                "&temperature_unit=fahrenheit" +
                "&windspeed_unit=mph";

            JSONObject weatherData = new JSONObject(getHTTP(weatherURL));
            JSONObject current = weatherData.getJSONObject("current_weather");

            double temp = current.getDouble("temperature");
            double wind = current.getDouble("windspeed");
            double windDirDeg = current.getDouble("winddirection");
            int weatherCode = current.getInt("weathercode");

            double feelsLike =
                weatherData.getJSONObject("hourly")
                           .getJSONArray("apparent_temperature")
                           .getDouble(0);

            double precipitation =
                weatherData.getJSONObject("hourly")
                           .getJSONArray("precipitation")
                           .getDouble(0);

            String windDirText = windDirection(windDirDeg);
            String icon = weatherIcon(weatherCode);

            // CLOTHING SUGGESTION
            String outfit = "";
            if (temp < 20) outfit = "Extremely cold — heavy coat, gloves, hat.";
            else if (temp < 32) outfit = "Freezing — wear a heavy coat, gloves, and hat.";
            else if (temp < 50) outfit = "Cold — wear a warm coat or hoodie.";
            else if (temp < 65) outfit = "Cool — a light hoodie is fine.";
            else if (temp < 80) outfit = "Comfortable — t-shirt weather.";
            else outfit = "Hot — wear light clothing.";

            if (precipitation > 0.1)
                outfit += " It's raining — bring an umbrella.";

            // HTML OUTPUT
            String html = "";
            html += "<!DOCTYPE html><html><head>";
            html += "<meta charset='UTF-8'>";
            html += "<title>Weather Report</title>";

            html += "<style>";
            html += "body {background:#111; color:white; font-family:Arial;}";
            html += ".card {max-width:700px; margin:40px auto; padding:25px;";
            html += "background:#000000aa; border-radius:20px; text-align:center;}";
            html += ".grid {display:flex; justify-content:space-around; margin-top:20px;}";
            html += ".item {background:#222; padding:20px; border-radius:15px; width:150px;}";
            html += ".value {font-size:28px; color:#66aaff;}";
            html += ".icon {font-size:50px; margin-bottom:10px;}";
            html += "</style></head><body>";

            html += "<div class='card'>";
            html += "<h1>Today's Weather</h1>";
            html += "<h3>" + city + "</h3>";
            html += "<div class='icon'>" + icon + "</div>";

            html += "<div class='grid'>";
            html += item("Temperature (°F)", temp);
            html += item("Feels Like (°F)", feelsLike);
            html += item("Wind (mph)", wind);
            html += item("Direction", windDirText);
            html += item("Precipitation", precipitation);
            html += "</div>";

            html += "<h2 style='margin-top:25px;'>" + outfit + "</h2>";
            html += "</div></body></html>";

            FileWriter fw = new FileWriter("weather.html");
            fw.write(html);
            fw.close();

            System.out.println("\nweather.html generated successfully for " + city + "!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ------------------------------
    // Weather card builder (double)
    // ------------------------------
    public static String item(String label, double value) {
        return "<div class='item'>" +
               "<div class='value'>" + value + "</div>" +
               "<div class='label'>" + label + "</div>" +
               "</div>";
    }

    // ------------------------------
    // Weather card builder (String)
    // ------------------------------
    public static String item(String label, String value) {
        return "<div class='item'>" +
               "<div class='value'>" + value + "</div>" +
               "<div class='label'>" + label + "</div>" +
               "</div>";
    }

    public static String windDirection(double deg) {
        String[] dirs = { "N", "NE", "E", "SE", "S", "SW", "W", "NW" };
        return dirs[(int)Math.round(deg / 45) % 8];
    }

    public static String weatherIcon(int code) {
        if (code == 0) return "☀️";
        if (code <= 2) return "⛅";
        if (code == 3) return "☁️";
        if (code == 45 || code == 48) return "🌫️";
        if (code == 51 || code == 61 || code == 80) return "🌧️";
        if (code == 63 || code == 65 || code == 82) return "🌧️";
        if (code == 71 || code == 73 || code == 75) return "🌨️";
        if (code == 95) return "⛈️";
        return "🌡️";
    }

    public static String getHTTP(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        Scanner sc = new Scanner(conn.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNext()) sb.append(sc.nextLine());
        sc.close();
        return sb.toString();
    }
}

