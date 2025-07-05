# Diagrama de Arquitectura - Sistema de Transporte de Mercancías

## Arquitectura General del Sistema

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTE / FRONTEND                                   │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Web App   │  │  Mobile App │  │  Desktop    │  │   API       │            │
│  │  (React)    │  │  (React     │  │  (Electron) │  │  Clients    │            │
│  │             │  │  Native)    │  │             │  │             │            │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              API GATEWAY                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │
│  │   Auth      │  │   Rate      │  │   Load      │  │   Logging    │            │
│  │   Service   │  │   Limiting  │  │   Balancer  │  │   &         │            │
│  │             │  │             │  │             │  │   Monitoring │            │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘            │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              MICROSERVICIOS                                       │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                    ROUTE OPTIMIZATION SERVICE                              │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │ Dijkstra    │  │ A*          │  │ Bellman-    │  │ Floyd-      │    │    │
│  │  │ Algorithm   │  │ Algorithm   │  │ Ford        │  │ Warshall    │    │    │
│  │  │             │  │             │  │ Algorithm   │  │ Algorithm   │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                    TRANSPORT COST SERVICE                                 │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │ Transport   │  │ Transfer    │  │ Total Cost  │  │ Cost        │    │    │
│  │  │ Cost Calc   │  │ Cost Calc   │  │ Calculator  │  │ Optimizer   │    │    │
│  │  │             │  │             │  │             │  │             │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                    CITY INFORMATION SERVICE                                │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │ City Data   │  │ Connection  │  │ Transfer    │  │ Map Data    │    │    │
│  │  │ Manager     │  │ Manager     │  │ Manager     │  │ Manager     │    │    │
│  │  │             │  │             │  │             │  │             │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                    MAP DATA SERVICE                                        │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │ XML Parser  │  │ File        │  │ Data        │  │ Cache       │    │    │
│  │  │             │  │ Manager     │  │ Validator   │  │ Manager     │    │    │
│  │  │             │  │             │  │             │  │             │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              DATA LAYER                                           │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                              CACHE LAYER                                   │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │   Redis     │  │   Redis     │  │   Redis     │  │   Redis     │    │    │
│  │  │   Cities    │  │ Connections │  │   Routes    │  │   Costs     │    │    │
│  │  │   Cache     │  │   Cache     │  │   Cache     │  │   Cache     │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                            DATABASE LAYER                                  │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │ PostgreSQL  │  │ PostgreSQL  │  │ PostgreSQL  │  │ PostgreSQL  │    │    │
│  │  │ Cities      │  │ Connections │  │ Transfers   │  │ Routes      │    │    │
│  │  │ Table       │  │ Table       │  │ Table       │  │ Table       │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
│                                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────┐    │
│  │                            FILE SYSTEM                                      │    │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │    │
│  │  │ XML Map     │  │ XML Map     │  │ XML Map     │  │ XML Map     │    │    │
│  │  │ Files       │  │ Files       │  │ Files       │  │ Files       │    │    │
│  │  │ (Region 1)  │  │ (Region 2)  │  │ (Region 3)  │  │ (Region N)  │    │    │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

## Flujo de Datos Principal

```
1. CLIENTE → API GATEWAY
   - Autenticación
   - Rate Limiting
   - Load Balancing

2. API GATEWAY → MICROSERVICIOS
   - Route Optimization Service (cálculo de rutas)
   - Transport Cost Service (cálculo de costos)
   - City Information Service (datos de ciudades)
   - Map Data Service (archivos XML)

3. MICROSERVICIOS → DATA LAYER
   - Cache (Redis) para datos frecuentes
   - Database (PostgreSQL) para datos persistentes
   - File System para archivos XML

4. RESPUESTA
   - OptimizedRoute con segmentos y costos
   - Información de ciudades y conexiones
   - Costos de traspasos y transportes
```

## Componentes Clave

### 1. Route Optimization Service
- **Responsabilidad**: Encontrar la mejor ruta entre dos ciudades
- **Algoritmos**: Dijkstra, A*, Bellman-Ford
- **Criterios**: Costo mínimo, tiempo mínimo, menos traspasos

### 2. Transport Cost Service
- **Responsabilidad**: Calcular costos de transporte y traspasos
- **Funciones**: Costo por kg, costo fijo de traspaso, costo total de ruta

### 3. City Information Service
- **Responsabilidad**: Gestionar información de ciudades
- **Funciones**: Datos de ciudades, conexiones, traspasos disponibles

### 4. Map Data Service
- **Responsabilidad**: Manejar archivos XML de mapas
- **Funciones**: Parsear XML, validar datos, cargar a base de datos

## Patrones de Diseño Utilizados

1. **Repository Pattern**: Para acceso a datos
2. **Strategy Pattern**: Para diferentes algoritmos de optimización
3. **Factory Pattern**: Para crear diferentes tipos de rutas
4. **Observer Pattern**: Para notificaciones de cambios
5. **Cache-Aside Pattern**: Para mejorar rendimiento
6. **Circuit Breaker Pattern**: Para manejo de fallos
