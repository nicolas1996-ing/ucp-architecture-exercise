# Información de Mercancías - Análisis de Datos

## 1. Traspasos de Carga por Ciudad

### Bogotá
| Transporte Origen | Transporte Destino | Costo |
|-------------------|-------------------|-------|
| Avión | Camión | $450 |
| Barco | Camión | $300 |
| Avión | Barco | $150 |

### Cali
| Transporte Origen | Transporte Destino | Costo |
|-------------------|-------------------|-------|
| Avión | Barco | $350 |

## 2. Conexiones entre Ciudades

### Desde Bogotá
| Ciudad Destino | Tipo de Transporte | Costo |
|----------------|-------------------|-------|
| Cali | Avión | $2,000 |
| Cali | Barco | $1,000 |

### Desde Cali
| Ciudad Destino | Tipo de Transporte | Costo |
|----------------|-------------------|-------|
| Bogotá | Camión | $750 |
| Pasto | Avión | $4,500 |

## 3. Resumen de Costos

### Traspasos de Carga
- **Bogotá**: 3 opciones de traspaso disponibles
- **Cali**: 1 opción de traspaso disponible
- **Costo más bajo**: $150 (Avión → Barco en Bogotá)
- **Costo más alto**: $450 (Avión → Camión en Bogotá)

### Conexiones entre Ciudades
- **Bogotá ↔ Cali**: 3 rutas disponibles (2 desde Bogotá, 1 desde Cali)
- **Cali ↔ Pasto**: 1 ruta disponible
- **Costo más bajo**: $750 (Cali → Bogotá en Camión)
- **Costo más alto**: $4,500 (Cali → Pasto en Avión)

## 4. Tipos de Transporte Disponibles
- **Avión**: Usado en conexiones y traspasos
- **Barco**: Usado en conexiones y traspasos
- **Camión**: Usado en conexiones y traspasos


- Bogotá tiene la mayor flexibilidad con 3 opciones de traspaso
- La ruta más costosa es Cali → Pasto en avión ($4,500)
- La ruta más económica es Cali → Bogotá en camión ($750)
- El traspaso más barato es Avión → Barco en Bogotá ($150)
