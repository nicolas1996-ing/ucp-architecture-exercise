// Implementación de las 3 Funcionalidades Principales del Backend
// Sistema de Transporte de Mercancías

package com.transport.optimization;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import java.util.*;
import java.util.stream.Collectors;

// ============================================================================
// FUNCIONALIDAD 1: OPTIMIZACIÓN DE RUTAS CON ALGORITMO DIJKSTRA
// ============================================================================

@Service
public class RouteOptimizationServiceImpl implements RouteOptimizationService {

    @Autowired
    private CityInformationService cityService;

    @Autowired
    private CostCalculationService costService;

    @Override
    public OptimizedRoute findBestRoute(String origin, String destination,
                                      double weight, OptimizationCriteria criteria) {

        // Validación de entrada
        if (origin == null || destination == null || weight <= 0) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        // Implementación del algoritmo Dijkstra modificado
        Map<String, RouteNode> nodes = new HashMap<>();
        PriorityQueue<RouteNode> queue = new PriorityQueue<>(
            (a, b) -> Double.compare(a.totalCost, b.totalCost)
        );

        // Inicialización
        RouteNode startNode = new RouteNode(origin, null, 0.0, null);
        nodes.put(origin, startNode);
        queue.offer(startNode);

        Set<String> visited = new HashSet<>();

        while (!queue.isEmpty()) {
            RouteNode current = queue.poll();

            if (current.city.equals(destination)) {
                return buildOptimizedRoute(current, weight, criteria);
            }

            if (visited.contains(current.city)) {
                continue;
            }

            visited.add(current.city);

            // Explorar conexiones desde la ciudad actual
            List<Connection> connections = cityService.getConnectionsFromCity(current.city);

            for (Connection conn : connections) {
                String nextCity = conn.getCity2();

                if (visited.contains(nextCity)) {
                    continue;
                }

                double transportCost = costService.calculateTransportCost(
                    current.city, nextCity, conn.getTransportType(), weight
                );

                double newCost = current.totalCost + transportCost;

                // Aplicar criterios de optimización
                if (criteria == OptimizationCriteria.MIN_TRANSFERS) {
                    // Penalizar cambios de transporte
                    if (current.lastConnection != null &&
                        !current.lastConnection.getTransportType().equals(conn.getTransportType())) {
                        newCost += 1000; // Penalización por cambio de transporte
                    }
                }

                // Verificar si es mejor ruta
                if (!nodes.containsKey(nextCity) || newCost < nodes.get(nextCity).totalCost) {
                    RouteNode nextNode = new RouteNode(nextCity, current, newCost, conn);
                    nodes.put(nextCity, nextNode);
                    queue.offer(nextNode);
                }
            }
        }

        throw new RouteNotFoundException("No se encontró ruta entre " + origin + " y " + destination);
    }

    @Override
    public List<OptimizedRoute> findAlternativeRoutes(String origin, String destination,
                                                     double weight, int maxAlternatives) {

        List<OptimizedRoute> alternatives = new ArrayList<>();

        try {
            // Primera ruta (mejor)
            OptimizedRoute bestRoute = findBestRoute(origin, destination, weight, OptimizationCriteria.MIN_COST);
            alternatives.add(bestRoute);

            // Rutas alternativas usando diferentes criterios
            if (maxAlternatives > 1) {
                try {
                    OptimizedRoute timeRoute = findBestRoute(origin, destination, weight, OptimizationCriteria.MIN_TIME);
                    if (!timeRoute.equals(bestRoute)) {
                        alternatives.add(timeRoute);
                    }
                } catch (Exception e) {
                    // Ignorar si no hay ruta alternativa
                }
            }

            if (maxAlternatives > 2) {
                try {
                    OptimizedRoute transferRoute = findBestRoute(origin, destination, weight, OptimizationCriteria.MIN_TRANSFERS);
                    if (!alternatives.contains(transferRoute)) {
                        alternatives.add(transferRoute);
                    }
                } catch (Exception e) {
                    // Ignorar si no hay ruta alternativa
                }
            }

        } catch (Exception e) {
            throw new RouteNotFoundException("No se encontraron rutas alternativas");
        }

        return alternatives.stream()
            .limit(maxAlternatives)
            .collect(Collectors.toList());
    }

    private OptimizedRoute buildOptimizedRoute(RouteNode destinationNode, double weight, OptimizationCriteria criteria) {
        List<RouteSegment> segments = new ArrayList<>();
        List<TransportTransfer> transfers = new ArrayList<>();

        RouteNode current = destinationNode;
        RouteNode previous = null;

        while (current != null) {
            if (current.lastConnection != null) {
                // Crear segmento de ruta
                RouteSegment segment = new RouteSegment();
                segment.setFromCity(current.lastConnection.getCity1());
                segment.setToCity(current.lastConnection.getCity2());
                segment.setTransportType(current.lastConnection.getTransportType());
                segment.setCost(costService.calculateTransportCost(
                    segment.getFromCity(), segment.getToCity(),
                    segment.getTransportType(), weight
                ));
                segment.setWeight(weight);
                segment.setTime(current.lastConnection.getEstimatedTime());

                segments.add(0, segment); // Insertar al inicio para mantener orden

                // Verificar si hay traspaso necesario
                if (previous != null && previous.lastConnection != null) {
                    if (!previous.lastConnection.getTransportType().equals(current.lastConnection.getTransportType())) {
                        // Buscar traspaso disponible
                        try {
                            double transferCost = costService.calculateTransferCost(
                                current.city,
                                previous.lastConnection.getTransportType(),
                                current.lastConnection.getTransportType()
                            );

                            TransportTransfer transfer = new TransportTransfer();
                            transfer.setFromTransport(previous.lastConnection.getTransportType());
                            transfer.setToTransport(current.lastConnection.getTransportType());
                            transfer.setFixedCost(transferCost);
                            transfer.setCityName(current.city);

                            transfers.add(0, transfer);
                        } catch (Exception e) {
                            // Traspaso no disponible, continuar
                        }
                    }
                }
            }

            previous = current;
            current = current.previous;
        }

        // Calcular costos totales
        double totalCost = segments.stream().mapToDouble(RouteSegment::getCost).sum() +
                          transfers.stream().mapToDouble(TransportTransfer::getFixedCost).sum();

        int totalTime = segments.stream().mapToInt(RouteSegment::getTime).sum();

        OptimizedRoute route = new OptimizedRoute();
        route.setOriginCity(destinationNode.city);
        route.setDestinationCity(destinationNode.city);
        route.setSegments(segments);
        route.setTotalCost(totalCost);
        route.setTotalTime(totalTime);
        route.setTransfers(transfers);

        return route;
    }

    // Clase interna para nodos del algoritmo
    private static class RouteNode {
        String city;
        RouteNode previous;
        double totalCost;
        Connection lastConnection;

        RouteNode(String city, RouteNode previous, double totalCost, Connection lastConnection) {
            this.city = city;
            this.previous = previous;
            this.totalCost = totalCost;
            this.lastConnection = lastConnection;
        }
    }
}

// ============================================================================
// FUNCIONALIDAD 2: CÁLCULO DE COSTOS CON ESTRATEGIAS
// ============================================================================

@Service
public class CostCalculationServiceImpl implements CostCalculationService {

    @Autowired
    private CityInformationService cityService;

    @Override
    public double calculateTransportCost(String origin, String destination,
                                      TransportType transport, double weight) {

        // Validación de entrada
        if (origin == null || destination == null || transport == null || weight <= 0) {
            throw new IllegalArgumentException("Parámetros inválidos para cálculo de costo");
        }

        // Buscar conexión específica
        Connection connection = findConnection(origin, destination, transport);
        if (connection == null) {
            throw new ConnectionNotFoundException(
                "No existe conexión con " + transport + " entre " + origin + " y " + destination
            );
        }

        // Calcular costo base por kg
        double baseCost = connection.getCostPerKg() * weight;

        // Aplicar factores de corrección según tipo de transporte
        double correctionFactor = getTransportCorrectionFactor(transport);

        return baseCost * correctionFactor;
    }

    @Override
    public double calculateTransferCost(String city, TransportType from, TransportType to) {

        // Validación de entrada
        if (city == null || from == null || to == null) {
            throw new IllegalArgumentException("Parámetros inválidos para cálculo de traspaso");
        }

        if (from.equals(to)) {
            return 0.0; // No hay traspaso necesario
        }

        List<TransportTransfer> transfers = cityService.getTransfersInCity(city);

        for (TransportTransfer transfer : transfers) {
            if (transfer.getFromTransport().equals(from) &&
                transfer.getToTransport().equals(to)) {
                return transfer.getFixedCost();
            }
        }

        throw new TransferNotAvailableException(
            "Traspaso de " + from + " a " + to + " no disponible en " + city
        );
    }

    @Override
    public double calculateTotalRouteCost(OptimizedRoute route) {

        if (route == null || route.getSegments() == null) {
            throw new IllegalArgumentException("Ruta inválida para cálculo de costo total");
        }

        double totalCost = 0.0;

        // Costo de transporte por segmentos
        for (RouteSegment segment : route.getSegments()) {
            totalCost += segment.getCost();
        }

        // Costo de traspasos
        if (route.getTransfers() != null) {
            for (TransportTransfer transfer : route.getTransfers()) {
                totalCost += transfer.getFixedCost();
            }
        }

        return totalCost;
    }

    // Métodos auxiliares
    private Connection findConnection(String origin, String destination, TransportType transport) {
        List<Connection> connections = cityService.getConnectionsFromCity(origin);

        return connections.stream()
            .filter(conn -> conn.getCity2().equals(destination) &&
                           conn.getTransportType().equals(transport))
            .findFirst()
            .orElse(null);
    }

    private double getTransportCorrectionFactor(TransportType transport) {
        switch (transport) {
            case PLANE:
                return 1.0; // Sin corrección
            case SHIP:
                return 0.9; // 10% de descuento por volumen
            case TRUCK:
                return 1.1; // 10% de recargo por flexibilidad
            default:
                return 1.0;
        }
    }

    // Método adicional para calcular costos con descuentos por volumen
    public double calculateTransportCostWithVolumeDiscount(String origin, String destination,
                                                        TransportType transport, double weight) {

        double baseCost = calculateTransportCost(origin, destination, transport, weight);

        // Aplicar descuentos por volumen
        if (weight > 1000) { // Más de 1 tonelada
            baseCost *= 0.95; // 5% de descuento
        } else if (weight > 500) { // Más de 500 kg
            baseCost *= 0.98; // 2% de descuento
        }

        return baseCost;
    }
}

// ============================================================================
// FUNCIONALIDAD 3: GESTIÓN DE DATOS DE CIUDADES CON CACHE
// ============================================================================

@Service
public class CityInformationServiceImpl implements CityInformationService {

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private ConnectionRepository connectionRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private MapDataService mapDataService;

    @Override
    @Cacheable(value = "cities", key = "#cityName")
    public City getCityInfo(String cityName) {

        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de ciudad inválido");
        }

        return cityRepository.findByName(cityName)
            .orElseThrow(() -> new CityNotFoundException("Ciudad no encontrada: " + cityName));
    }

    @Override
    @Cacheable(value = "connections", key = "#cityName")
    public List<Connection> getConnectionsFromCity(String cityName) {

        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de ciudad inválido");
        }

        List<Connection> connections = connectionRepository.findByCity1(cityName);

        if (connections.isEmpty()) {
            // Intentar cargar datos desde archivos XML si no hay datos en BD
            refreshCityData();
            connections = connectionRepository.findByCity1(cityName);
        }

        return connections;
    }

    @Override
    @Cacheable(value = "transfers", key = "#cityName")
    public List<TransportTransfer> getTransfersInCity(String cityName) {

        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Nombre de ciudad inválido");
        }

        return transferRepository.findByCityName(cityName);
    }

    @Override
    public boolean hasTransferCapability(String cityName, TransportType from, TransportType to) {

        if (cityName == null || from == null || to == null) {
            return false;
        }

        List<TransportTransfer> transfers = getTransfersInCity(cityName);

        return transfers.stream()
            .anyMatch(transfer -> transfer.getFromTransport().equals(from) &&
                                 transfer.getToTransport().equals(to));
    }

    @Override
    @CacheEvict(value = {"cities", "connections", "transfers"}, allEntries = true)
    public void refreshCityData() {

        try {
            // Cargar datos desde archivos XML
            MapData mapData = mapDataService.loadMapData();

            // Actualizar ciudades
            for (City city : mapData.getCities()) {
                cityRepository.save(city);
            }

            // Actualizar conexiones
            for (Connection connection : mapData.getConnections()) {
                connectionRepository.save(connection);
            }

            // Actualizar traspasos
            for (TransportTransfer transfer : mapData.getTransfers()) {
                transferRepository.save(transfer);
            }

            System.out.println("Datos de ciudades actualizados exitosamente");

        } catch (Exception e) {
            throw new DataLoadException("Error al cargar datos de ciudades: " + e.getMessage());
        }
    }

    // Métodos adicionales para gestión avanzada
    public List<City> getAllCities() {
        return cityRepository.findAll();
    }

    public List<City> getCitiesByRegion(String region) {
        return cityRepository.findByRegion(region);
    }

    public Map<String, List<Connection>> getAllConnections() {
        List<Connection> allConnections = connectionRepository.findAll();

        return allConnections.stream()
            .collect(Collectors.groupingBy(Connection::getCity1));
    }

    public boolean cityExists(String cityName) {
        return cityRepository.findByName(cityName).isPresent();
    }

    public void addCity(City city) {
        if (city == null || city.getName() == null) {
            throw new IllegalArgumentException("Ciudad inválida");
        }

        if (cityExists(city.getName())) {
            throw new CityAlreadyExistsException("La ciudad ya existe: " + city.getName());
        }

        cityRepository.save(city);

        // Limpiar cache
        refreshCityData();
    }

    public void updateCity(String cityName, City updatedCity) {
        if (cityName == null || updatedCity == null) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }

        City existingCity = getCityInfo(cityName);
        existingCity.setName(updatedCity.getName());
        existingCity.setTransfers(updatedCity.getTransfers());
        existingCity.setConnections(updatedCity.getConnections());
        existingCity.setMetadata(updatedCity.getMetadata());

        cityRepository.save(existingCity);

        // Limpiar cache
        refreshCityData();
    }

    public void deleteCity(String cityName) {
        if (cityName == null) {
            throw new IllegalArgumentException("Nombre de ciudad inválido");
        }

        if (!cityExists(cityName)) {
            throw new CityNotFoundException("Ciudad no encontrada: " + cityName);
        }

        // Eliminar conexiones relacionadas
        List<Connection> connections = getConnectionsFromCity(cityName);
        connectionRepository.deleteAll(connections);

        // Eliminar traspasos relacionados
        List<TransportTransfer> transfers = getTransfersInCity(cityName);
        transferRepository.deleteAll(transfers);

        // Eliminar ciudad
        cityRepository.deleteByName(cityName);

        // Limpiar cache
        refreshCityData();
    }
}

// ============================================================================
// EXCEPCIONES PERSONALIZADAS
// ============================================================================

public class RouteNotFoundException extends RuntimeException {
    public RouteNotFoundException(String message) {
        super(message);
    }
}

public class ConnectionNotFoundException extends RuntimeException {
    public ConnectionNotFoundException(String message) {
        super(message);
    }
}

public class TransferNotAvailableException extends RuntimeException {
    public TransferNotAvailableException(String message) {
        super(message);
    }
}

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String message) {
        super(message);
    }
}

public class CityAlreadyExistsException extends RuntimeException {
    public CityAlreadyExistsException(String message) {
        super(message);
    }
}

public class DataLoadException extends RuntimeException {
    public DataLoadException(String message) {
        super(message);
    }
}
