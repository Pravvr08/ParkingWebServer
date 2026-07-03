import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

public class ParkingWebServer {
    private static final int PORT = 8000;
    private static final int TWO_WHEELER_CAPACITY = 140; 
    private static final int CAR_CAPACITY = 100;
    private static final File DB = new File("student_database.txt");
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss");

    private final List<Record> records = Collections.synchronizedList(new ArrayList<>());
    private final List<Slot> twoSlots = Collections.synchronizedList(new ArrayList<>());
    private final List<Slot> carSlots = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws Exception {
        ParkingWebServer app = new ParkingWebServer();
        app.initModel();
        app.startServer();
    }

    private void initModel() {
        for (int i = 1; i <= TWO_WHEELER_CAPACITY; i++) twoSlots.add(new Slot(i, "Two Wheeler"));
        for (int i = 1; i <= CAR_CAPACITY; i++) carSlots.add(new Slot(i, "Car"));
        loadDatabase();
    }

    private void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", this::handleIndex);
        server.createContext("/add", this::handleAdd);
        server.createContext("/exit", this::handleExit);
        server.createContext("/dashboard", this::handleDashboard);
        server.createContext("/layout", this::handleLayout);
        server.createContext("/static", this::handleStatic);
        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();
        System.out.println("  Server started at http://localhost:" + PORT);
    }

    private void handleIndex(HttpExchange ex) throws IOException {
        String html = """
        <!doctype html>
        <html>
        <head>
          <meta charset='utf-8'>
          <title>Smart Parking System</title>
          <link rel='stylesheet' href='/static/style.css'>
        </head>
        <body>
          <div class='container'>
            <h1>  Vehicle Parking Management System</h1>
            <div class='cards'>
              <a class='card' href='/add'>
                <h2>Add / Exit Vehicle</h2>
                <p>Register new entries or mark vehicles as exited.</p>
              </a>
              <a class='card' href='/dashboard'>
                <h2>Dashboard</h2>
                <p>View all records and parking statistics.</p>
              </a>
              <a class='card' href='/layout'>
                <h2>Parking Layout</h2>
                <p>See real-time visual layout of slots.</p>
              </a>
            </div>
          </div>
        </body>
        </html>
        """;
        sendHtml(ex, html);
    }

    private void handleAdd(HttpExchange ex) throws IOException {
        if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            String html = """
            <!doctype html>
            <html><head><meta charset='utf-8'><title>Add Vehicle</title>
            <link rel='stylesheet' href='/static/style.css'></head><body>
            <div class='container'>
            <h2>Add Vehicle Entry</h2>
            <form method='post' action='/add'>
              <label>Name:</label><input name='name' required><br>
              <label>Phone:</label><input name='phone' required pattern='[0-9]+'><br>
              <label>Vehicle No:</label><input name='vehicle' required><br>
              <label>Type:</label>
              <select name='type'><option>Two Wheeler</option><option>Car</option></select><br>
              <button type='submit'>Add Entry</button>
            </form>
            <hr>
            <h2>Exit Vehicle</h2>
            <form method='post' action='/exit'>
              <label>Vehicle No:</label><input name='vehicle' required>
              <button type='submit'>Exit</button>
            </form>
            <p><a href='/'> Home</a> | <a href='/dashboard'> Dashboard</a></p>
            </div></body></html>
            """;
            sendHtml(ex, html);
            return;
        }

        Map<String, String> params = parsePostParameters(ex);
        String name = params.getOrDefault("name", "").trim();
        String phone = params.getOrDefault("phone", "").trim();
        String vehicle = params.getOrDefault("vehicle", "").trim().toUpperCase();
        String type = params.getOrDefault("type", "Two Wheeler").trim();

        String message;
        if (name.isEmpty() || phone.isEmpty() || vehicle.isEmpty()) {
            message = "Missing fields.";
        } else {
            boolean parked = parkVehicle(name, phone, vehicle, type);
            message = parked ? "  Vehicle parked successfully." : "   No free slot for " + type;
        }
        redirectWithMessage(ex, "/add", message);
    }
// Handle 
    private void handleExit(HttpExchange ex) throws IOException {
        Map<String, String> params = parsePostParameters(ex);
        String vehicle = params.getOrDefault("vehicle", "").trim().toUpperCase();
        String message;
        if (vehicle.isEmpty()) {
            message = "Enter a vehicle number.";
        } else {
            boolean ok = exitVehicle(vehicle);
            message = ok ? "  Exit recorded for " + vehicle : "Vehicle not found or already exited.";
        }
        redirectWithMessage(ex, "/add", message);
    }

    private void handleDashboard(HttpExchange ex) throws IOException {
        loadDatabase();
        long activeTwo = records.stream().filter(r -> r.type.equals("Two Wheeler") && (r.exitTime == null || r.exitTime.isEmpty())).count();
        long activeCar = records.stream().filter(r -> r.type.equals("Car") && (r.exitTime == null || r.exitTime.isEmpty())).count();

        StringBuilder sb = new StringBuilder();
        sb.append("""
        <!doctype html><html><head><meta charset='utf-8'><title>Dashboard</title>
        <link rel='stylesheet' href='/static/style.css'>
        <script src='https://cdn.jsdelivr.net/npm/chart.js'></script>
        </head><body><div class='container'>
        <h2>Dashboard Overview</h2>
        <nav><a href='/'>   Home</a> | <a href='/add'> Add</a> | <a href='/layout'> Layout</a></nav>
        """);

        sb.append("<div class='chart-box'><canvas id='chart' width='280' height='280'></canvas></div>");
        sb.append("<script>")
          .append("const ctx=document.getElementById('chart').getContext('2d');")
          .append("new Chart(ctx,{type:'pie',data:{labels:['Two Wheeler','Car'],datasets:[{data:[")
          .append(activeTwo).append(",").append(activeCar)
          .append("],backgroundColor:['#f1c40f','#3498db']}]},options:{responsive:true,maintainAspectRatio:true,plugins:{legend:{position:'bottom'}}}});")
          .append("</script>");

        sb.append("<table class='records'><thead><tr><th>Name</th><th>Phone</th><th>Vehicle</th><th>Type</th><th>Entry</th><th>Exit</th></tr></thead><tbody>");
        synchronized (records) {
            for (Record r : records) {
                boolean active = r.exitTime == null || r.exitTime.trim().isEmpty();
                sb.append("<tr class='").append(active ? "active" : "done").append("'>")
                  .append("<td>").append(escapeHtml(r.name)).append("</td>")
                  .append("<td>").append(escapeHtml(r.phone)).append("</td>")
                  .append("<td>").append(escapeHtml(r.vehicle)).append("</td>")
                  .append("<td>").append(escapeHtml(r.type)).append("</td>")
                  .append("<td>").append(escapeHtml(r.entryTime)).append("</td>")
                  .append("<td>").append(escapeHtml(r.exitTime == null ? "" : r.exitTime)).append("</td></tr>");
            }
        }
        sb.append("</tbody></table><p><small>Highlighted = Active vehicles.</small></p></div></body></html>");
        sendHtml(ex, sb.toString());
    }

    private void handleLayout(HttpExchange ex) throws IOException {
        loadDatabase();
        StringBuilder sb = new StringBuilder();
        sb.append("""
        <!doctype html><html><head><meta charset='utf-8'><title>Layout</title>
        <link rel='stylesheet' href='/static/style.css'></head><body><div class='container'>
        <h2>Parking Layout</h2><nav><a href='/'>   Home</a> | <a href='/dashboard'>Dashboard</a></nav>
        """);

        sb.append("<h3>Two Wheeler (" + TWO_WHEELER_CAPACITY + ")</h3><div class='grid'>");
        for (Slot s : twoSlots) {
            String cls = s.occupied ? "slot occupied" : "slot free";
            sb.append("<div class='").append(cls).append("' title='").append(s.vehicle).append("'>TW-").append(s.slotNo).append("</div>");
        }
        sb.append("</div><h3>Car (" + CAR_CAPACITY + ")</h3><div class='grid'>");
        for (Slot s : carSlots) {
            String cls = s.occupied ? "slot occupied" : "slot free";
            sb.append("<div class='").append(cls).append("' title='").append(s.vehicle).append("'>C-").append(s.slotNo).append("</div>");
        }
        sb.append("</div></div></body></html>");
        sendHtml(ex, sb.toString());
    }

    private boolean parkVehicle(String name, String phone, String vehicle, String type) {
        synchronized (this) {
            List<Slot> target = type.equalsIgnoreCase("Car") ? carSlots : twoSlots;
            for (Slot s : target) {
                if (!s.occupied) {
                    s.occupied = true;
                    s.vehicle = vehicle;
                    s.owner = name;
                    s.time = TIME_FMT.format(new Date());
                    Record r = new Record(name, phone, vehicle, type, s.time, "");
                    records.add(r);
                    saveDatabase();
                    return true;
                }
            }
            return false;
        }
    }

    private boolean exitVehicle(String vehicle) {
        synchronized (this) {
            boolean found = false;
            for (Record r : records) {
                if (r.vehicle.equalsIgnoreCase(vehicle) && (r.exitTime == null || r.exitTime.isEmpty())) {
                    r.exitTime = TIME_FMT.format(new Date());
                    found = true;
                    break;
                }
            }
            if (!found) return false;

            for (Slot s : twoSlots) if (s.occupied && s.vehicle.equalsIgnoreCase(vehicle)) s.occupied = false;
            for (Slot s : carSlots) if (s.occupied && s.vehicle.equalsIgnoreCase(vehicle)) s.occupied = false;
            saveDatabase();
            return true;
        }
    }

    private void loadDatabase() {
        synchronized (this) {
            records.clear();
            for (Slot s : twoSlots) { s.occupied = false; s.vehicle = ""; }
            for (Slot s : carSlots) { s.occupied = false; s.vehicle = ""; }
            if (!DB.exists()) return;
            try (BufferedReader br = new BufferedReader(new FileReader(DB))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(",", -1);
                    if (p.length < 6) continue;
                    Record r = new Record(p[0], p[1], p[2], p[3], p[4], p[5]);
                    records.add(r);
                    if (r.exitTime.isEmpty()) {
                        List<Slot> target = "Car".equalsIgnoreCase(r.type) ? carSlots : twoSlots;
                        for (Slot s : target) if (!s.occupied) { s.occupied = true; s.vehicle = r.vehicle; break; }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private void saveDatabase() {
        synchronized (this) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(DB, false))) {
                for (Record r : records) {
                    bw.write(safe(r.name) + "," + safe(r.phone) + "," + safe(r.vehicle) + "," +
                             safe(r.type) + "," + safe(r.entryTime) + "," + safe(r.exitTime));
                    bw.newLine();
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    private static Map<String, String> parsePostParameters(HttpExchange ex) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(ex.getRequestBody(), StandardCharsets.UTF_8));
        String raw = br.readLine();
        Map<String, String> map = new HashMap<>();
        if (raw != null) for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(URLDecoder.decode(kv[0], "UTF-8"), kv.length > 1 ? URLDecoder.decode(kv[1], "UTF-8") : "");
        }
        return map;
    }

    private static void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
    }

    private static void redirectWithMessage(HttpExchange ex, String path, String msg) throws IOException {
        String loc = path + "?msg=" + URLEncoder.encode(msg, "UTF-8");
        ex.getResponseHeaders().set("Location", loc);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    private static String escapeHtml(String in) {
        return in == null ? "" : in.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }
    private static String safe(String s) { return s == null ? "" : s.replace(",", " "); }

    private void handleStatic(HttpExchange ex) throws IOException {
        String css = """
        body{font-family:'Segoe UI',Arial,sans-serif;background:#f8fafc;color:#333;margin:0;padding:0;}
        .container{max-width:960px;margin:20px auto;padding:20px;background:#fff;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}
        h1,h2,h3{text-align:center;color:#2c3e50;}
        nav{text-align:center;margin-bottom:15px;}
        nav a{margin:0 10px;text-decoration:none;color:#0078d7;}
        nav a:hover{text-decoration:underline;}
        .cards{display:flex;justify-content:center;gap:20px;flex-wrap:wrap;margin-top:20px;}
        .card{background:#0078d7;color:#fff;padding:20px;width:250px;border-radius:10px;text-align:center;text-decoration:none;transition:0.3s;}
        .card:hover{background:#005fa3;transform:translateY(-3px);}
        .records{width:100%;border-collapse:collapse;margin-top:20px;}
        .records th,.records td{border:1px solid #ddd;padding:8px;text-align:center;}
        .records tr.active{background:#fff8c2;}
        .records tr.done{background:#f2f2f2;}
        form label{display:inline-block;width:120px;}
        form input,form select{padding:5px;margin-bottom:10px;}
        button{padding:6px 12px;border:none;border-radius:5px;background:#0078d7;color:#fff;cursor:pointer;}
        button:hover{background:#005fa3;}
        .grid{display:grid;grid-template-columns:repeat(20,1fr);gap:6px;margin:10px 0;}
        .slot{padding:6px;text-align:center;border-radius:5px;font-size:12px;}
        .slot.free{background:#f1c40f;}
        .slot.occupied{background:#e74c3c;color:#fff;}
        .chart-box{text-align:center;margin:20px auto;max-width:320px;}
        """;
        byte[] bs = css.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/css; charset=utf-8");
        ex.sendResponseHeaders(200, bs.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(bs); }
    }

    private static class Record {
        String name, phone, vehicle, type, entryTime, exitTime;
        Record(String n,String p,String v,String t,String e,String x){name=n;phone=p;vehicle=v;type=t;entryTime=e;exitTime=x;}
    }

    private static class Slot {
        int slotNo; boolean occupied=false; String vehicle="", owner="", time="", type;
        Slot(int n,String t){slotNo=n;type=t;}
    }
}
