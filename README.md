# Vehicle Parking Management System (Web-Based)

This is a web-based Vehicle Parking Management System built using core Java and its built-in HTTP server. It allows users to add vehicle entries, record exits, and automatically assign available parking slots. The system also includes a dashboard and parking layout view, and stores all data in a text file.
It is designed as WebServer instead of traditional system(manual pass or token) to manage parking slots efficiently for both two-wheelers and cars within an organization or campus.


## The system allows users to:

- Add vehicle entries
- Mark vehicle exits
- View real-time parking status
- Analyze parking data through a dashboard
- Visualize parking slot occupancy

It uses a lightweight approach without external frameworks, making it easy to run and understand.


## Features
- Add vehicle entry (name, phone, vehicle number, type)
- Exit vehicle and update records
- Automatic slot allocation
- Dashboard with live statistics (Pie Chart)
- Visual parking layout (Free / Occupied slots)
- File-based database (no external DB required)
- Multi-threaded server handling
  


  
## Technologies Used
- Java (Core Java)
- com.sun.net.httpserver (built-in HTTP server)
- HTML + CSS (frontend)
- Chart.js (for dashboard visualization)
- File Handling (.txt database)

## Project Structure
- ParkingWebServer.java   --> Main server file
- student_database.txt   --> Stores parking records (Will Create Automatically)



## How to Run the Project

1. Compile the Code
```
javac ParkingWebServer.java
```

2. Run the Server
```
java ParkingWebServer
```

3. Open in Browser:
```http://localhost:8000```

## Usage Guide
- Home Page
- Navigate to: Add / Exit Vehicle
- Dashboard
- Parking Layout
- Add Vehicle


## Tech Stack

**Client:** HTML + CSS

**Server:** Chart.js, com.sun.net.httpserver


## Parking Layout
#### Visual grid of slots:
- ##### 🟡 Yellow → Free
- ##### 🔴 Red → Occupied

## Separate sections for:
- Two Wheelers
- Cars

##  Data Storage
#### Data is stored in:
###### student_database.txt


## Format:
#### Name,Phone,Vehicle,Type,EntryTime,ExitTime

### Example:
```
Rahul,951xxxxxxx,TN01AB1234,Car,10:20:30,12:22:53
```

## Key Functional Logic

- #### Parking Allocation
    - private boolean parkVehicle(String name, String phone, String vehicle, String type)
    -  Finds first free slot Marks it occupied
   Stores entry in records Vehicle Exit,
   private boolean exitVehicle(String vehicle),
   Updates exit time,Frees the occupied slot.

- #### Load Database
   - private void loadDatabase()
       - Reads file
Restores parking state on server restart
Save Database
private void saveDatabase(). Writes all records back to file





## Advantages
- No external database required  
- Lightweight and fast  
- Easy to understand and modify  
- Runs on any system with Java installed  

## Limitations
- Uses a text file for storage (not suitable for large-scale systems)  
- No authentication or login system  
- Basic user interface  
- No real-time auto-refresh  

## Future Improvements
- Add authentication system (Admin/User)  
- Integrate database such as MySQL or MongoDB  
- Add search and filter options  
- Enable real-time updates  
- Improve UI for mobile responsiveness  
- Add payment or billing system(Malls or Other)

##  Outcome
This project helps in understanding:
- How HTTP servers work in Java  
- Handling GET and POST requests  
- File-based data storage  
- Integration of backend logic with frontend  
- Multi-threading in server applications  

## Conclusion
This project presents a complete implementation of a parking management system using core Java. It is designed as WebServer instead of traditional system(manual pass or token) to manage parking slots efficiently for both two-wheelers and cars.It demonstrates how backend logic, file handling, and a simple frontend can be combined to build a functional real-world application suitable for small-scale environments such as colleges, offices, or residential areas.
