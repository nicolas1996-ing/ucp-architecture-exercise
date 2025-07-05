# Análisis Arquitectónico - Sistema de Transporte de Mercancías

## 1. Análisis del Problema

### Requisitos Funcionales:
- Calcular la mejor ruta entre dos ciudades
- Considerar múltiples medios de transporte (Camión, Avión, Barco)
- Incluir costos de traspaso entre medios de transporte
- Optimizar según criterios establecidos (costo, tiempo, etc.)
- Consultar información por ciudad
- Manejar diferentes archivos de mapas

### Requisitos No Funcionales:
- Escalabilidad para múltiples ciudades
- Flexibilidad para diferentes criterios de optimización
- Mantenibilidad del código
- Extensibilidad para nuevos tipos de transporte

## 2. Arquitectura Propuesta

### Patrón Arquitectónico: **Arquitectura en Capas con Microservicios - SOA**

```
┌─────────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                      │
├─────────────────────────────────────────────────────────────┤
│  REST API Controllers | GraphQL | WebSocket (Real-time)   │
├─────────────────────────────────────────────────────────────┤
│                    BUSINESS LAYER                          │
├─────────────────────────────────────────────────────────────┤
│  Route Optimization | Cost Calculation | Transport Logic   │
├─────────────────────────────────────────────────────────────┤
│                    DATA ACCESS LAYER                       │
├─────────────────────────────────────────────────────────────┤
│  Repository Pattern | Data Mappers | Cache Layer          │
├─────────────────────────────────────────────────────────────┤
│                    INFRASTRUCTURE LAYER                    │
├─────────────────────────────────────────────────────────────┤
│  Database | File System | External APIs | Message Queue   │
└─────────────────────────────────────────────────────────────┘
```

### Microservicios Propuestos:
1. **Route Optimization Service** - Algoritmos de optimización
2. **Transport Cost Service** - Cálculo de costos
3. **City Information Service** - Gestión de datos de ciudades
4. **Map Data Service** - Manejo de archivos de mapas

## 3. Modelo de Clases

### Entidades Principales:

```java
// Entidad Ciudad
class City {
    private String name;
    private List<TransportTransfer> transfers;
    private List<Connection> connections;
    private Map<String, Object> metadata;
}

// Entidad Conexión entre Ciudades
class Connection {
    private String city1;
    private String city2;
    private TransportType transportType;
    private double costPerKg;
    private double distance;
    private int estimatedTime;
}

// Entidad Traspaso de Transporte
class TransportTransfer {
    private TransportType fromTransport;
    private TransportType toTransport;
    private double fixedCost;
    private String cityName;
}

// Entidad Ruta Optimizada
class OptimizedRoute {
    private String originCity;
    private String destinationCity;
    private List<RouteSegment> segments;
    private double totalCost;
    private int totalTime;
    private List<TransportTransfer> transfers;
}

// Entidad Segmento de Ruta
class RouteSegment {
    private String fromCity;
    private String toCity;
    private TransportType transportType;
    private double cost;
    private double weight;
    private int time;
}

// Enumeración Tipos de Transporte
enum TransportType {
    TRUCK,    // Camión
    PLANE,    // Avión
    SHIP      // Barco
}

// Enumeración Criterios de Optimización
enum OptimizationCriteria {
    MIN_COST,      // Menor costo
    MIN_TIME,      // Menor tiempo
    MIN_TRANSFERS, // Menos traspasos
    BALANCED       // Equilibrado
}
```

### Servicios de Negocio:

```java
// Servicio de Optimización de Rutas
interface RouteOptimizationService {
    OptimizedRoute findBestRoute(String origin, String destination,
                                double weight, OptimizationCriteria criteria);
    List<OptimizedRoute> findAlternativeRoutes(String origin, String destination,
                                              double weight, int maxAlternatives);
}

// Servicio de Cálculo de Costos
interface CostCalculationService {
    double calculateTransportCost(String origin, String destination,
                                TransportType transport, double weight);
    double calculateTransferCost(String city, TransportType from, TransportType to);
    double calculateTotalRouteCost(OptimizedRoute route);
}

// Servicio de Información de Ciudades
interface CityInformationService {
    City getCityInfo(String cityName);
    List<Connection> getConnectionsFromCity(String cityName);
    List<TransportTransfer> getTransfersInCity(String cityName);
    boolean hasTransferCapability(String cityName, TransportType from, TransportType to);
}
```

## 4. Implementación de 3 Funcionalidades Backend

### Funcionalidad 1: Optimización de Rutas con Algoritmo Dijkstra

```java
@Service
public class RouteOptimizationServiceImpl implements RouteOptimizationService {

    @Autowired
    private CityInformationService cityService;

    @Autowired
    private CostCalculationService costService;

    public OptimizedRoute findBestRoute(String origin, String destination,
                                      double weight, OptimizationCriteria criteria) {

        // Implementación del algoritmo Dijkstra modificado
        Map<String, RouteNode> nodes = new HashMap<>();
        PriorityQueue<RouteNode> queue = new PriorityQueue<>();

        // Inicialización
        RouteNode startNode = new RouteNode(origin, null, 0.0, null);
        nodes.put(origin, startNode);
        queue.offer(startNode);

        while (!queue.isEmpty()) {
            RouteNode current = queue.poll();

            if (current.city.equals(destination)) {
                return buildOptimizedRoute(current, weight);
            }

            // Explorar conexiones desde la ciudad actual
            List<Connection> connections = cityService.getConnectionsFromCity(current.city);

            for (Connection conn : connections) {
                String nextCity = conn.getCity2();
                double newCost = current.totalCost +
                               costService.calculateTransportCost(current.city, nextCity,
                                                               conn.getTransportType(), weight);

                // Verificar si es mejor ruta
                if (!nodes.containsKey(nextCity) ||
                    newCost < nodes.get(nextCity).totalCost) {

                    RouteNode nextNode = new RouteNode(nextCity, current, newCost, conn);
                    nodes.put(nextCity, nextNode);
                    queue.offer(nextNode);
                }
            }
        }

        throw new RouteNotFoundException("No se encontró ruta entre " + origin + " y " + destination);
    }
}
```

### Funcionalidad 2: Cálculo de Costos con Estrategias

```java
@Service
public class CostCalculationServiceImpl implements CostCalculationService {

    @Autowired
    private CityInformationService cityService;

    public double calculateTransportCost(String origin, String destination,
                                      TransportType transport, double weight) {

        Connection connection = findConnection(origin, destination, transport);
        if (connection == null) {
            throw new ConnectionNotFoundException("No existe conexión con " + transport);
        }

        return connection.getCostPerKg() * weight;
    }

    public double calculateTransferCost(String city, TransportType from, TransportType to) {

        List<TransportTransfer> transfers = cityService.getTransfersInCity(city);

        for (TransportTransfer transfer : transfers) {
            if (transfer.getFromTransport() == from &&
                transfer.getToTransport() == to) {
                return transfer.getFixedCost();
            }
        }

        throw new TransferNotAvailableException("Traspaso no disponible en " + city);
    }

    public double calculateTotalRouteCost(OptimizedRoute route) {
        double totalCost = 0.0;

        // Costo de transporte
        for (RouteSegment segment : route.getSegments()) {
            totalCost += segment.getCost();
        }

        // Costo de traspasos
        for (TransportTransfer transfer : route.getTransfers()) {
            totalCost += transfer.getFixedCost();
        }

        return totalCost;
    }
}
```

### Funcionalidad 3: Gestión de Datos de Ciudades con Cache

```java
@Service
public class CityInformationServiceImpl implements CityInformationService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Cacheable("cities")
    public City getCityInfo(String cityName) {
        return cityRepository.findByName(cityName)
            .orElseThrow(() -> new CityNotFoundException("Ciudad no encontrada: " + cityName));
    }

    @Cacheable("connections")
    public List<Connection> getConnectionsFromCity(String cityName) {
        return connectionRepository.findByCity1(cityName);
    }

    @Cacheable("transfers")
    public List<TransportTransfer> getTransfersInCity(String cityName) {
        return transferRepository.findByCityName(cityName);
    }

    public boolean hasTransferCapability(String cityName, TransportType from, TransportType to) {
        List<TransportTransfer> transfers = getTransfersInCity(cityName);

        return transfers.stream()
            .anyMatch(transfer -> transfer.getFromTransport() == from &&
                                 transfer.getToTransport() == to);
    }

    @CacheEvict(value = {"cities", "connections", "transfers"}, allEntries = true)
    public void refreshCityData() {
        // Método para refrescar datos desde archivos XML
        loadMapDataFromFiles();
    }

    private void loadMapDataFromFiles() {
        // Implementación para cargar datos desde archivos XML
        // y actualizar la base de datos
    }
}
```

## 5. Tecnologías Recomendadas

### Backend:
- **Framework**: Spring Boot 3.x
- **Base de Datos**: PostgreSQL + Redis (cache)
- **Algoritmos**: Implementación propia de Dijkstra + A*
- **API**: REST + GraphQL
- **Testing**: JUnit 5 + Mockito

### Infraestructura:
- **Contenedores**: Docker + Kubernetes
- **Monitoreo**: Prometheus + Grafana
- **Logs**: ELK Stack
- **CI/CD**: GitHub Actions

## 6. Consideraciones de Escalabilidad

1. **Cache Distribuido**: Redis para datos frecuentemente consultados
2. **Algoritmos Optimizados**: Implementación paralela de algoritmos de rutas
3. **Microservicios**: Separación por dominio funcional
4. **Base de Datos**: Particionamiento por regiones geográficas
5. **API Gateway**: Rate limiting y autenticación centralizada
